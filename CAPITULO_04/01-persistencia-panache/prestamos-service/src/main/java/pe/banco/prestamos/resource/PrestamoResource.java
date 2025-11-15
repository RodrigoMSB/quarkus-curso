package pe.banco.prestamos.resource;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pe.banco.prestamos.model.Cliente;
import pe.banco.prestamos.model.Cuota;
import pe.banco.prestamos.model.Prestamo;
import pe.banco.prestamos.repository.ClienteRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * REST Resource para gestión de préstamos bancarios.
 * 
 * FUNCIONALIDAD PRINCIPAL:
 * - Crear préstamos con generación automática de cuotas
 * - Listar préstamos (todos o por cliente)
 * - Pagar cuotas individuales
 * - Cambiar estado de préstamo a PAGADO cuando se completa
 * 
 * ARQUITECTURA:
 * ┌─────────────────────────────────────────┐
 * │ PrestamoResource (esta clase)           │ ← HTTP/REST
 * ├─────────────────────────────────────────┤
 * │ ClienteRepository (validación)          │ ← Verificar cliente
 * ├─────────────────────────────────────────┤
 * │ Prestamo + Cuota (Active Record)        │ ← Persistencia
 * ├─────────────────────────────────────────┤
 * │ Database                                │ ← PostgreSQL/H2
 * └─────────────────────────────────────────┘
 * 
 * PATRÓN MIXTO:
 * - Cliente: Repository Pattern (ClienteRepository)
 * - Prestamo/Cuota: Active Record (extends PanacheEntity)
 * 
 * ¿Por qué mix?
 * - Cliente tiene lógica compleja (validaciones duplicados)
 * - Prestamo/Cuota más simples, Active Record suficiente
 * - Ambos patrones conviven bien
 * 
 * ENDPOINTS:
 * GET    /prestamos                    → Listar todos
 * GET    /prestamos/{id}               → Obtener uno
 * POST   /prestamos                    → Crear (con cuotas)
 * PUT    /prestamos/{id}/pagar-cuota/{n} → Pagar cuota
 * GET    /prestamos/cliente/{id}       → Por cliente
 * 
 * Analogía: Como la ventanilla de préstamos del banco.
 * - Recibe solicitudes
 * - Genera plan de cuotas automático
 * - Procesa pagos mensuales
 * - Actualiza estado del préstamo
 */

@Path("/prestamos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PrestamoResource {
    
    // ============================================
    // INYECCIÓN DE DEPENDENCIAS
    // ============================================
    
    /**
     * Repository de clientes inyectado por CDI.
     * 
     * USADO PARA:
     * - Validar existencia del cliente antes de crear préstamo
     * - No podemos crear préstamo sin cliente válido
     * 
     * ¿Por qué ClienteRepository y no PrestamoRepository?
     * - Prestamo usa Active Record (Prestamo.persist())
     * - Solo necesitamos validar Cliente (Repository Pattern)
     * 
     * Patrón mixto:
     * @Inject ClienteRepository clienteRepository; ← Repository
     * Prestamo.persist();                          ← Active Record
     * 
     * Ambos conviven perfectamente.
     */
    @Inject
    ClienteRepository clienteRepository;
    
    // ============================================
    // ENDPOINTS REST
    // ============================================
    
    /**
     * GET /prestamos
     * Lista todos los préstamos.
     * 
     * ACTIVE RECORD:
     * Prestamo.listAll() - método estático heredado
     * 
     * No necesita repository porque Prestamo extends PanacheEntity.
     * 
     * Request:
     * GET http://localhost:8080/prestamos
     * 
     * Response 200 OK:
     * [
     *   {
     *     "id": 1,
     *     "cliente": { ... },
     *     "monto": 10000.00,
     *     "plazoMeses": 12,
     *     "tasaInteres": 15.50,
     *     "fechaDesembolso": "2025-10-12",
     *     "estado": "ACTIVO",
     *     "cuotas": [
     *       { "id": 1, "numeroCuota": 1, "monto": 962.50, ... },
     *       { "id": 2, "numeroCuota": 2, ... }
     *     ]
     *   }
     * ]
     * 
     * NOTA: cuotas NO tienen 'prestamo' por @JsonIgnore
     * (evita loop infinito Prestamo → Cuota → Prestamo)
     * 
     * SQL:
     * SELECT * FROM prestamos
     * SELECT * FROM clientes WHERE id IN (...)  -- EAGER fetch
     * SELECT * FROM cuotas WHERE prestamo_id IN (...) -- LAZY fetch on access
     * 
     * MEJORA FUTURA (evitar N+1):
     * return Prestamo.find(
     *     "SELECT p FROM Prestamo p " +
     *     "LEFT JOIN FETCH p.cuotas " +
     *     "LEFT JOIN FETCH p.cliente"
     * ).list();
     */
    @GET
    public List<Prestamo> listar() {
        return Prestamo.listAll();
    }
    
    /**
     * GET /prestamos/{id}
     * Obtiene un préstamo específico.
     * 
     * ACTIVE RECORD + Optional:
     * Prestamo.findByIdOptional(id)
     * - Retorna Optional<Prestamo>
     * - .map() si existe
     * - .orElse() si no
     * 
     * Request:
     * GET http://localhost:8080/prestamos/1
     * 
     * Response 200 OK:
     * {
     *   "id": 1,
     *   "cliente": {
     *     "id": 1,
     *     "nombre": "María González",
     *     "dni": "12345678",
     *     "email": "maria@example.com",
     *     "telefono": "987654321"
     *   },
     *   "monto": 10000.00,
     *   "plazoMeses": 12,
     *   "tasaInteres": 15.50,
     *   "fechaDesembolso": "2025-10-12",
     *   "estado": "ACTIVO",
     *   "cuotas": [...]
     * }
     * 
     * Response 404 Not Found:
     * "Préstamo no encontrado"
     * 
     * PROGRAMACIÓN FUNCIONAL:
     * .map(prestamo -> Response.ok(prestamo).build())
     * → Si Optional tiene valor, crea Response 200
     * 
     * .orElse(Response.status(404)...)
     * → Si Optional vacío, crea Response 404
     */
    @GET
    @Path("/{id}")
    public Response obtener(@PathParam("id") Long id) {
        return Prestamo.findByIdOptional(id)
                .map(prestamo -> Response.ok(prestamo).build())
                .orElse(Response.status(404).entity("Préstamo no encontrado").build());
    }
    
    /**
     * POST /prestamos
     * Crea un nuevo préstamo con cuotas generadas automáticamente.
     * 
     * FLUJO COMPLETO:
     * 1. Validar que cliente existe
     * 2. Crear préstamo con estado ACTIVO
     * 3. Generar N cuotas (según plazoMeses)
     * 4. Persistir préstamo (cascade persiste cuotas)
     * 5. Retornar préstamo creado
     * 
     * Request:
     * POST http://localhost:8080/prestamos
     * Content-Type: application/json
     * 
     * {
     *   "clienteId": 1,
     *   "monto": 10000.00,
     *   "plazoMeses": 12,
     *   "tasaInteres": 15.50
     * }
     * 
     * Response 201 Created:
     * {
     *   "id": 1,
     *   "cliente": { ... },
     *   "monto": 10000.00,
     *   "plazoMeses": 12,
     *   "tasaInteres": 15.50,
     *   "fechaDesembolso": "2025-10-12",
     *   "estado": "ACTIVO",
     *   "cuotas": [
     *     {
     *       "id": 1,
     *       "numeroCuota": 1,
     *       "monto": 962.50,
     *       "fechaVencimiento": "2025-11-12",
     *       "fechaPago": null,
     *       "pagada": false
     *     },
     *     ... (11 cuotas más)
     *   ]
     * }
     * 
     * Response 404 Not Found:
     * "Cliente no encontrado"
     * 
     * @Transactional CRÍTICO:
     * - Inserta préstamo
     * - Inserta 12 cuotas (cascade)
     * - Todo en una transacción
     * - Rollback si falla algo
     * 
     * DTO PrestamoRequest:
     * - Separa input del modelo
     * - Solo campos necesarios para crear
     * - No incluye ID (auto-generado)
     * - No incluye fechaDesembolso (LocalDate.now())
     * - No incluye estado (ACTIVO por defecto)
     * 
     * VALIDACIÓN:
     * Cliente debe existir antes de crear préstamo.
     * 
     * clienteRepository.findById(request.clienteId)
     * - Si null → 404 Not Found
     * - Si existe → continuar
     * 
     * CONSTRUCTOR Prestamo:
     * new Prestamo(cliente, monto, plazo, tasa, fecha)
     * - Setea estado = ACTIVO automáticamente
     * - cuotas aún null (se asignan después)
     * 
     * GENERACIÓN DE CUOTAS:
     * generarCuotas(prestamo)
     * - Calcula monto de cuota
     * - Crea N cuotas (1 por mes)
     * - Fechas vencimiento: +1, +2, +3... meses
     * - Todas con pagada=false
     * 
     * prestamo.cuotas = cuotas
     * - Asigna lista generada
     * 
     * prestamo.persist()
     * - Guarda préstamo
     * - cascade=ALL → guarda cuotas también
     * 
     * SQL ejecutado:
     * -- Validar cliente
     * SELECT * FROM clientes WHERE id = ?
     * 
     * -- Insertar préstamo
     * INSERT INTO prestamos (cliente_id, monto, plazo_meses, 
     *                        tasa_interes, fecha_desembolso, estado)
     * VALUES (1, 10000, 12, 15.50, '2025-10-12', 'ACTIVO')
     * 
     * -- Insertar cuotas (12 INSERTs)
     * INSERT INTO cuotas (prestamo_id, numero_cuota, monto, 
     *                     fecha_vencimiento, pagada)
     * VALUES (1, 1, 962.50, '2025-11-12', false);
     * INSERT INTO cuotas ... (cuota 2)
     * ... (10 más)
     * 
     * @param request DTO con datos del préstamo a crear
     * @return Response 201 con préstamo creado, o 404 si cliente no existe
     */
    @POST
    @Transactional
    public Response crear(PrestamoRequest request) {
        // 1. VALIDAR CLIENTE
        Cliente cliente = clienteRepository.findById(request.clienteId);
        if (cliente == null) {
            return Response.status(404).entity("Cliente no encontrado").build();
        }
        
        // 2. CREAR PRÉSTAMO
        Prestamo prestamo = new Prestamo(
            cliente,
            request.monto,
            request.plazoMeses,
            request.tasaInteres,
            LocalDate.now()  // Fecha desembolso: hoy
        );
        // Constructor setea: estado = ACTIVO
        
        // 3. GENERAR CUOTAS
        List<Cuota> cuotas = generarCuotas(prestamo);
        prestamo.cuotas = cuotas;
        
        // 4. PERSISTIR (cascade guarda cuotas)
        prestamo.persist();
        
        // 5. RETORNAR
        return Response.status(201).entity(prestamo).build();
    }
    
    /**
     * PUT /prestamos/{id}/pagar-cuota/{numeroCuota}
     * Marca una cuota como pagada.
     * 
     * LÓGICA DE NEGOCIO:
     * 1. Buscar préstamo por ID
     * 2. Buscar cuota por número
     * 3. Validar que no esté pagada
     * 4. Marcar como pagada (fechaPago = hoy)
     * 5. Si todas pagadas → préstamo.estado = PAGADO
     * 6. Retornar cuota actualizada
     * 
     * Request:
     * PUT http://localhost:8080/prestamos/1/pagar-cuota/1
     * (sin body)
     * 
     * Response 200 OK:
     * {
     *   "id": 1,
     *   "numeroCuota": 1,
     *   "monto": 962.50,
     *   "fechaVencimiento": "2025-11-12",
     *   "fechaPago": "2025-10-12",
     *   "pagada": true
     * }
     * 
     * Response 404 Not Found (préstamo):
     * "Préstamo no encontrado"
     * 
     * Response 404 Not Found (cuota):
     * "Cuota no encontrada"
     * 
     * Response 409 Conflict (ya pagada):
     * "Cuota ya pagada"
     * 
     * @Transactional:
     * - Actualiza cuota
     * - Actualiza préstamo si aplica
     * - Commit automático
     * 
     * DOS @PathParam:
     * @PathParam("id") Long prestamoId
     * @PathParam("numeroCuota") Integer numeroCuota
     * 
     * URL: /prestamos/1/pagar-cuota/5
     *                  ↑              ↑
     *             prestamoId=1   numeroCuota=5
     * 
     * BUSCAR PRÉSTAMO:
     * Prestamo.findById(prestamoId)
     * - Active Record, método estático
     * - Retorna Prestamo o null
     * 
     * BUSCAR CUOTA:
     * prestamo.cuotas.stream()
     *     .filter(c -> c.numeroCuota.equals(numeroCuota))
     *     .findFirst()
     *     .orElse(null)
     * 
     * Stream API:
     * - Filtra cuotas por número
     * - findFirst() → Optional<Cuota>
     * - orElse(null) → Cuota o null
     * 
     * Alternativa con loop:
     * for (Cuota c : prestamo.cuotas) {
     *     if (c.numeroCuota.equals(numeroCuota)) {
     *         cuota = c;
     *         break;
     *     }
     * }
     * 
     * VALIDACIÓN YA PAGADA:
     * if (cuota.pagada) {
     *     return 409 Conflict;
     * }
     * 
     * Evita doble pago.
     * 
     * MARCAR COMO PAGADA:
     * cuota.pagada = true;
     * cuota.fechaPago = LocalDate.now();
     * 
     * Hibernate detecta cambio (dirty checking):
     * → UPDATE cuotas SET pagada=true, fecha_pago=? WHERE id=?
     * 
     * VERIFICAR PRÉSTAMO COMPLETO:
     * boolean todasPagadas = prestamo.cuotas.stream()
     *     .allMatch(c -> c.pagada);
     * 
     * .allMatch():
     * - true si TODAS las cuotas cumplen condición
     * - false si al menos una no cumple
     * 
     * if (todasPagadas) {
     *     prestamo.estado = EstadoPrestamo.PAGADO;
     * }
     * 
     * Transición de estado:
     * ACTIVO → PAGADO (cuando última cuota se paga)
     * 
     * SQL ejecutado:
     * -- Buscar préstamo (con cuotas LAZY)
     * SELECT * FROM prestamos WHERE id = ?
     * SELECT * FROM clientes WHERE id = ?  -- EAGER cliente
     * SELECT * FROM cuotas WHERE prestamo_id = ? -- Acceso a cuotas
     * 
     * -- Actualizar cuota
     * UPDATE cuotas 
     * SET pagada = true, fecha_pago = '2025-10-12'
     * WHERE id = ?
     * 
     * -- Si todas pagadas, actualizar préstamo
     * UPDATE prestamos
     * SET estado = 'PAGADO'
     * WHERE id = ?
     * 
     * @param prestamoId ID del préstamo (de URL)
     * @param numeroCuota Número de cuota a pagar (de URL)
     * @return Response 200 con cuota pagada, o error
     */
    @PUT
    @Path("/{id}/pagar-cuota/{numeroCuota}")
    @Transactional
    public Response pagarCuota(
            @PathParam("id") Long prestamoId, 
            @PathParam("numeroCuota") Integer numeroCuota) {
        
        // 1. BUSCAR PRÉSTAMO
        Prestamo prestamo = Prestamo.findById(prestamoId);
        if (prestamo == null) {
            return Response.status(404).entity("Préstamo no encontrado").build();
        }
        
        // 2. BUSCAR CUOTA
        Cuota cuota = prestamo.cuotas.stream()
                .filter(c -> c.numeroCuota.equals(numeroCuota))
                .findFirst()
                .orElse(null);
        
        if (cuota == null) {
            return Response.status(404).entity("Cuota no encontrada").build();
        }
        
        // 3. VALIDAR NO PAGADA
        if (cuota.pagada) {
            return Response.status(409).entity("Cuota ya pagada").build();
        }
        
        // 4. MARCAR COMO PAGADA
        cuota.pagada = true;
        cuota.fechaPago = LocalDate.now();
        
        // 5. VERIFICAR SI PRÉSTAMO COMPLETADO
        boolean todasPagadas = prestamo.cuotas.stream().allMatch(c -> c.pagada);
        if (todasPagadas) {
            prestamo.estado = Prestamo.EstadoPrestamo.PAGADO;
        }
        
        // 6. RETORNAR CUOTA ACTUALIZADA
        return Response.ok(cuota).build();
    }
    
    /**
     * GET /prestamos/cliente/{clienteId}
     * Lista préstamos de un cliente específico.
     * 
     * QUERY PERSONALIZADA con Active Record:
     * Prestamo.find("cliente.id", clienteId).list()
     * 
     * Genera HQL:
     * SELECT p FROM Prestamo p WHERE p.cliente.id = ?1
     * 
     * "cliente.id":
     * - Navega relación @ManyToOne
     * - p.cliente → JOIN con tabla clientes
     * - .id → filtra por ID
     * 
     * Request:
     * GET http://localhost:8080/prestamos/cliente/1
     * 
     * Response 200 OK:
     * [
     *   {
     *     "id": 1,
     *     "cliente": { ... },
     *     "monto": 10000.00,
     *     ...
     *   },
     *   {
     *     "id": 5,
     *     ...
     *   }
     * ]
     * 
     * Si cliente no tiene préstamos:
     * → [] (lista vacía, no 404)
     * 
     * SQL generado:
     * SELECT p.* 
     * FROM prestamos p
     * JOIN clientes c ON p.cliente_id = c.id
     * WHERE c.id = ?
     * 
     * MEJORA (verificar cliente existe):
     * Cliente cliente = clienteRepository.findById(clienteId);
     * if (cliente == null) {
     *     return Response.status(404)
     *         .entity("Cliente no encontrado")
     *         .build();
     * }
     * List<Prestamo> prestamos = Prestamo.find("cliente", cliente).list();
     * return Response.ok(prestamos).build();
     * 
     * Alternativa con Repository Pattern:
     * Si tuviéramos PrestamoRepository:
     * return prestamoRepository.findByClienteId(clienteId);
     * 
     * @param clienteId ID del cliente
     * @return Response con lista de préstamos del cliente
     */
    @GET
    @Path("/cliente/{clienteId}")
    public Response listarPorCliente(@PathParam("clienteId") Long clienteId) {
        List<Prestamo> prestamos = Prestamo.find("cliente.id", clienteId).list();
        return Response.ok(prestamos).build();
    }
    
    // ============================================
    // MÉTODOS AUXILIARES PRIVADOS
    // ============================================
    
    /**
     * Genera la lista de cuotas para un préstamo.
     * 
     * ALGORITMO:
     * 1. Calcular monto de cada cuota
     * 2. Para cada mes del plazo:
     *    - Crear cuota con número secuencial
     *    - Fecha vencimiento = desembolso + N meses
     *    - Estado inicial: no pagada
     * 3. Retornar lista completa
     * 
     * Ejemplo:
     * Préstamo $10,000 a 12 meses, 15.5% anual
     * Desembolso: 2025-10-12
     * 
     * Genera:
     * Cuota 1: $962.50, vence 2025-11-12, no pagada
     * Cuota 2: $962.50, vence 2025-12-12, no pagada
     * ...
     * Cuota 12: $962.50, vence 2026-10-12, no pagada
     * 
     * CÁLCULO DE MONTO:
     * calcularMontoCuota(monto, tasa, plazo)
     * - Fórmula financiera simplificada
     * - Divide monto + intereses entre plazo
     * 
     * LOOP:
     * for (int i = 1; i <= prestamo.plazoMeses; i++)
     * - i = número de cuota (1, 2, 3...)
     * - Crea cuota por cada mes
     * 
     * FECHA VENCIMIENTO:
     * prestamo.fechaDesembolso.plusMonths(i)
     * - LocalDate inmutable
     * - Suma i meses a fecha desembolso
     * - Ejemplo: 2025-10-12 + 3 meses = 2026-01-12
     * 
     * CONSTRUCTOR CUOTA:
     * new Cuota(prestamo, i, montoCuota, fechaVencimiento)
     * - prestamo: relación @ManyToOne
     * - i: número de cuota
     * - montoCuota: monto a pagar
     * - fechaVencimiento: fecha límite
     * - Constructor setea: pagada=false, fechaPago=null
     * 
     * RETORNO:
     * Lista con N cuotas (N = plazoMeses)
     * 
     * Esta lista se asigna a:
     * prestamo.cuotas = generarCuotas(prestamo);
     * 
     * Al hacer prestamo.persist():
     * - cascade=ALL persiste todas las cuotas
     * 
     * @param prestamo Préstamo para el cual generar cuotas
     * @return Lista de cuotas generadas (N cuotas)
     */
    private List<Cuota> generarCuotas(Prestamo prestamo) {
        List<Cuota> cuotas = new ArrayList<>();
        
        // Calcular monto de cada cuota
        BigDecimal montoCuota = calcularMontoCuota(
            prestamo.monto, 
            prestamo.tasaInteres, 
            prestamo.plazoMeses
        );
        
        // Generar una cuota por cada mes
        for (int i = 1; i <= prestamo.plazoMeses; i++) {
            LocalDate fechaVencimiento = prestamo.fechaDesembolso.plusMonths(i);
            Cuota cuota = new Cuota(prestamo, i, montoCuota, fechaVencimiento);
            cuotas.add(cuota);
        }
        
        return cuotas;
    }
    
    /**
     * Calcula el monto de cada cuota.
     * 
     * FÓRMULA SIMPLIFICADA:
     * monto * (1 + tasa_mensual * plazo) / plazo
     * 
     * Esta es una aproximación. En producción usar:
     * - Fórmula de cuota francesa (sistema francés)
     * - Tabla de amortización
     * - Librerías financieras
     * 
     * PASOS:
     * 
     * 1. CALCULAR TASA MENSUAL:
     * tasaInteres = 15.50 (%)
     * tasaInteres / 100 = 0.155 (decimal)
     * 0.155 / 12 = 0.01291... (mensual)
     * 
     * BigDecimal tasaMensual = tasaInteres
     *     .divide(BigDecimal.valueOf(100 * 12), 6, HALF_UP);
     * 
     * Precisión 6 decimales para exactitud.
     * HALF_UP: redondeo comercial.
     * 
     * 2. CALCULAR FACTOR:
     * 1 + (tasa_mensual * plazo)
     * 
     * BigDecimal factor = BigDecimal.ONE.add(
     *     tasaMensual.multiply(BigDecimal.valueOf(plazoMeses))
     * );
     * 
     * Ejemplo:
     * 1 + (0.01291 * 12) = 1.155
     * 
     * 3. CALCULAR CUOTA:
     * (monto * factor) / plazo
     * 
     * return monto.multiply(factor)
     *            .divide(BigDecimal.valueOf(plazoMeses), 2, HALF_UP);
     * 
     * Ejemplo:
     * (10000 * 1.155) / 12 = 962.50
     * 
     * PRECISIÓN:
     * - 6 decimales en tasa (0.012916)
     * - 2 decimales en cuota (962.50)
     * - RoundingMode.HALF_UP (comercial)
     * 
     * FÓRMULA REAL (sistema francés):
     * C = P * [i * (1+i)^n] / [(1+i)^n - 1]
     * 
     * Donde:
     * P = monto préstamo
     * i = tasa mensual
     * n = plazo en meses
     * C = cuota
     * 
     * Implementación real:
     * BigDecimal i = tasaMensual;
     * BigDecimal uno_mas_i = BigDecimal.ONE.add(i);
     * BigDecimal uno_mas_i_n = uno_mas_i.pow(plazoMeses);
     * 
     * BigDecimal numerador = monto.multiply(i)
     *                             .multiply(uno_mas_i_n);
     * BigDecimal denominador = uno_mas_i_n.subtract(BigDecimal.ONE);
     * 
     * BigDecimal cuota = numerador.divide(denominador, 2, HALF_UP);
     * 
     * Para este ejercicio, la fórmula simplificada es suficiente.
     * 
     * @param monto Monto total del préstamo
     * @param tasaInteres Tasa de interés anual (%)
     * @param plazoMeses Plazo en meses
     * @return Monto de cada cuota
     */
    private BigDecimal calcularMontoCuota(BigDecimal monto, BigDecimal tasaInteres, Integer plazoMeses) {
        // Tasa mensual (anual / 100 / 12)
        BigDecimal tasaMensual = tasaInteres.divide(
            BigDecimal.valueOf(100 * 12), 
            6,  // 6 decimales de precisión
            RoundingMode.HALF_UP  
        );
        
        // Factor: 1 + (tasa * plazo)
        BigDecimal factor = BigDecimal.ONE.add(
            tasaMensual.multiply(BigDecimal.valueOf(plazoMeses))
        );
        
        // Cuota: (monto * factor) / plazo
        return monto.multiply(factor).divide(
            BigDecimal.valueOf(plazoMeses), 
            2,  // 2 decimales (centavos)
            RoundingMode.HALF_UP  
        );
    }
    
    // ============================================
    // DTO (Data Transfer Object)
    // ============================================
    
    /**
     * DTO para request de creación de préstamo.
     * 
     * SEPARACIÓN INPUT vs MODEL:
     * 
     * PrestamoRequest (DTO):
     * - Solo campos de entrada
     * - Sin ID (auto-generado)
     * - Sin fecha (LocalDate.now())
     * - Sin estado (ACTIVO por defecto)
     * - Sin cuotas (generadas automáticamente)
     * 
     * Prestamo (entidad):
     * - Todos los campos
     * - ID, fecha, estado, cuotas
     * - Anotaciones JPA
     * 
     * VENTAJAS DTO:
     * ✅ Control de qué se puede enviar
     * ✅ Validación específica de input
     * ✅ No expone estructura interna
     * ✅ Evita mass assignment
     * 
     * JSON Request:
     * {
     *   "clienteId": 1,
     *   "monto": 10000.00,
     *   "plazoMeses": 12,
     *   "tasaInteres": 15.50
     * }
     * 
     * Jackson deserializa → PrestamoRequest
     * 
     * Luego:
     * new Prestamo(cliente, dto.monto, dto.plazo, dto.tasa, hoy)
     * 
     * CLASE ESTÁTICA ANIDADA:
     * public static class PrestamoRequest { ... }
     * 
     * ¿Por qué static?
     * - No necesita instancia de PrestamoResource
     * - Puede usarse independientemente
     * - Jackson puede instanciarla
     * 
     * ¿Por qué anidada?
     * - Cohesión: relacionada con PrestamoResource
     * - Namespace: PrestamoResource.PrestamoRequest
     * - Organización: todo en un archivo
     * 
     * Alternativa (archivo separado):
     * package pe.banco.prestamos.dto;
     * public class PrestamoRequest { ... }
     * 
     * CAMPOS PÚBLICOS:
     * - Jackson accede directo
     * - Sin getters/setters
     * - Simple y conciso
     * 
     * MEJORAS FUTURAS (Cap 5):
     * public static class PrestamoRequest {
     *     @NotNull
     *     public Long clienteId;
     *     
     *     @DecimalMin("100.00")
     *     @DecimalMax("100000.00")
     *     public BigDecimal monto;
     *     
     *     @Min(1) @Max(60)
     *     public Integer plazoMeses;
     *     
     *     @DecimalMin("0.01") @DecimalMax("100.00")
     *     public BigDecimal tasaInteres;
     * }
     */
    public static class PrestamoRequest {
        public Long clienteId;
        public BigDecimal monto;
        public Integer plazoMeses;
        public BigDecimal tasaInteres;
    }
}

/**
 * ═══════════════════════════════════════════════════════════════
 * FLUJO COMPLETO: CREAR PRÉSTAMO Y PAGAR CUOTA
 * ═══════════════════════════════════════════════════════════════
 * 
 * 1. CLIENTE SOLICITA PRÉSTAMO:
 * 
 * POST /prestamos
 * {
 *   "clienteId": 1,
 *   "monto": 10000.00,
 *   "plazoMeses": 12,
 *   "tasaInteres": 15.50
 * }
 * 
 * 2. VALIDAR CLIENTE:
 * SELECT * FROM clientes WHERE id = 1
 * → Existe ✅
 * 
 * 3. CREAR PRÉSTAMO:
 * new Prestamo(cliente, 10000, 12, 15.50, 2025-10-12)
 * → estado = ACTIVO
 * 
 * 4. GENERAR CUOTAS:
 * Calcular: 10000 * 1.155 / 12 = 962.50 por cuota
 * 
 * for i = 1 to 12:
 *   Cuota(prestamo, i, 962.50, 2025-10-12 + i meses)
 * 
 * 5. PERSISTIR:
 * INSERT INTO prestamos (...)
 * INSERT INTO cuotas (prestamo_id=1, numero=1, monto=962.50, vence=2025-11-12)
 * INSERT INTO cuotas (prestamo_id=1, numero=2, monto=962.50, vence=2025-12-12)
 * ... (10 más)
 * 
 * 6. RESPONSE 201:
 * {
 *   "id": 1,
 *   "estado": "ACTIVO",
 *   "cuotas": [12 cuotas generadas]
 * }
 * 
 * ───────────────────────────────────────────────────────────────
 * 
 * 7. CLIENTE PAGA PRIMERA CUOTA:
 * 
 * PUT /prestamos/1/pagar-cuota/1
 * 
 * 8. BUSCAR PRÉSTAMO Y CUOTA:
 * SELECT * FROM prestamos WHERE id = 1
 * SELECT * FROM cuotas WHERE prestamo_id = 1
 * → Encuentra cuota #1
 * 
 * 9. VALIDAR:
 * cuota.pagada == false ✅
 * 
 * 10. MARCAR COMO PAGADA:
 * cuota.pagada = true
 * cuota.fechaPago = 2025-10-15
 * 
 * UPDATE cuotas SET pagada=true, fecha_pago='2025-10-15' WHERE id=1
 * 
 * 11. VERIFICAR PRÉSTAMO:
 * Todas pagadas? → NO (11 pendientes)
 * Estado sigue: ACTIVO
 * 
 * 12. RESPONSE 200:
 * {
 *   "id": 1,
 *   "numeroCuota": 1,
 *   "pagada": true,
 *   "fechaPago": "2025-10-15"
 * }
 * 
 * ───────────────────────────────────────────────────────────────
 * 
 * 13. CLIENTE PAGA CUOTAS 2-11:
 * PUT /prestamos/1/pagar-cuota/2
 * ...
 * PUT /prestamos/1/pagar-cuota/11
 * 
 * 14. CLIENTE PAGA ÚLTIMA CUOTA:
 * 
 * PUT /prestamos/1/pagar-cuota/12
 * 
 * 15. MARCAR COMO PAGADA:
 * cuota.pagada = true
 * UPDATE cuotas ...
 * 
 * 16. VERIFICAR PRÉSTAMO:
 * boolean todasPagadas = prestamo.cuotas.stream()
 *     .allMatch(c -> c.pagada);
 * → true ✅
 * 
 * 17. CAMBIAR ESTADO:
 * prestamo.estado = EstadoPrestamo.PAGADO
 * UPDATE prestamos SET estado='PAGADO' WHERE id=1
 * 
 * 18. RESPONSE 200:
 * Cuota #12 pagada
 * 
 * 19. VERIFICAR PRÉSTAMO:
 * GET /prestamos/1
 * → estado: "PAGADO" ✅
 * 
 * ═══════════════════════════════════════════════════════════════
 * PATRONES Y CONCEPTOS APLICADOS
 * ═══════════════════════════════════════════════════════════════
 * 
 * ✅ Active Record: Prestamo.persist(), Prestamo.findById()
 * ✅ Repository Pattern: ClienteRepository (validación)
 * ✅ DTO: PrestamoRequest (separar input de modelo)
 * ✅ Stream API: filtrar, verificar cuotas
 * ✅ BigDecimal: cálculos financieros precisos
 * ✅ Cascade: persist préstamo → persiste cuotas
 * ✅ Transacciones: @Transactional en operaciones
 * ✅ Dirty Checking: Hibernate detecta cambios
 * ✅ Programación funcional: Optional.map().orElse()
 * 
 * ═══════════════════════════════════════════════════════════════
 */