package pe.banco.prestamos.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Entidad JPA que representa una cuota de pago de un préstamo.
 * 
 * PATRÓN: Active Record (extiende PanacheEntity)
 * 
 * Una cuota es una fracción del préstamo que el cliente debe pagar
 * mensualmente. Cada préstamo se divide en N cuotas según el plazo.
 * 
 * Relación con otras entidades:
 * - Muchas cuotas pertenecen a UN préstamo (N:1)
 * 
 * Ciclo de vida de una cuota:
 * 1. GENERADA: Al crear préstamo (automático)
 * 2. PENDIENTE: pagada = false, fechaPago = null
 * 3. PAGADA: pagada = true, fechaPago = hoy
 * 
 * Mapeo ORM:
 * ┌──────────────────────────────────────┐
 * │ Clase Java: Cuota                    │
 * │                                      │
 * │ - id: Long (heredado)                │
 * │ - prestamo: Prestamo (FK)            │
 * │ - numeroCuota: Integer (1, 2, 3...)  │
 * │ - monto: BigDecimal                  │
 * │ - fechaVencimiento: LocalDate        │
 * │ - fechaPago: LocalDate (nullable)    │
 * │ - pagada: Boolean                    │
 * └──────────────────────────────────────┘
 *                  ↓
 * ┌──────────────────────────────────────┐
 * │ Tabla SQL: cuotas                    │
 * │                                      │
 * │ - id BIGINT PRIMARY KEY              │
 * │ - prestamo_id BIGINT NOT NULL (FK)   │
 * │ - numero_cuota INTEGER NOT NULL      │
 * │ - monto DECIMAL(10,2) NOT NULL       │
 * │ - fecha_vencimiento DATE NOT NULL    │
 * │ - fecha_pago DATE NULL               │
 * │ - pagada BOOLEAN NOT NULL            │
 * │                                      │
 * │ FOREIGN KEY (prestamo_id)            │
 * │   REFERENCES prestamos(id)           │
 * └──────────────────────────────────────┘
 * 
 * Ejemplo de cuotas para préstamo de $10,000 a 12 meses:
 * ┌────┬─────────┬──────────┬──────────────────┬────────────┬────────┐
 * │ ID │ Número  │  Monto   │ Vencimiento      │ FechaPago  │ Pagada │
 * ├────┼─────────┼──────────┼──────────────────┼────────────┼────────┤
 * │ 1  │    1    │  962.50  │ 2025-11-12       │ 2025-10-15 │  true  │
 * │ 2  │    2    │  962.50  │ 2025-12-12       │ null       │  false │
 * │ 3  │    3    │  962.50  │ 2026-01-12       │ null       │  false │
 * │... │   ...   │   ...    │ ...              │ ...        │  ...   │
 * │ 12 │   12    │  962.50  │ 2026-10-12       │ null       │  false │
 * └────┴─────────┴──────────┴──────────────────┴────────────┴────────┘
 * 
 * Analogía: Como un cupón de pago mensual en una libreta de préstamo.
 * Cada cupón tiene número, monto, fecha límite, y espacio para
 * marcar cuando se paga.
 */

// ============================================
// ANOTACIONES DE ENTIDAD JPA
// ============================================

/**
 * @Entity
 * Marca esta clase como entidad JPA que se mapea a tabla 'cuotas'.
 * 
 * Hibernate automáticamente:
 * - Crea tabla al iniciar (con database.generation=update)
 * - Genera INSERT/UPDATE/DELETE/SELECT para esta entidad
 * - Mapea ResultSet ↔ Objeto Cuota
 */
@Entity

/**
 * @Table(name = "cuotas")
 * Nombre explícito de la tabla en base de datos.
 * 
 * Best Practice:
 * - Tabla en plural: cuotas
 * - Clase en singular: Cuota
 * 
 * SQL generado por Hibernate:
 * CREATE TABLE cuotas (
 *     id BIGINT NOT NULL,
 *     prestamo_id BIGINT NOT NULL,
 *     numero_cuota INTEGER NOT NULL,
 *     monto DECIMAL(10,2) NOT NULL,
 *     fecha_vencimiento DATE NOT NULL,
 *     fecha_pago DATE,
 *     pagada BOOLEAN NOT NULL,
 *     PRIMARY KEY (id),
 *     FOREIGN KEY (prestamo_id) REFERENCES prestamos(id)
 * );
 */
@Table(name = "cuotas")

/**
 * extends PanacheEntity
 * 
 * ACTIVE RECORD: La cuota puede operarse a sí misma
 * 
 * Métodos heredados disponibles:
 * - Cuota.persist(cuota)          // Guardar
 * - Cuota.findById(1L)            // Buscar por ID
 * - Cuota.listAll()               // Listar todas
 * - Cuota.find("pagada", false)   // Buscar pendientes
 * - cuota.delete()                // Eliminarse
 * 
 * Incluye automáticamente:
 * public Long id;  // Primary Key auto-generado
 */
public class Cuota extends PanacheEntity {
    
    // ============================================
    // RELACIÓN CON PRÉSTAMO
    // ============================================
    
    /**
     * Préstamo al que pertenece esta cuota.
     * 
     * RELACIÓN: Many-to-One (N:1)
     * - Muchas cuotas → Un préstamo
     * - Cuota es el lado DUEÑO (tiene FK)
     * - Prestamo es el lado INVERSO (mappedBy)
     * 
     * @JsonIgnore
     * CRÍTICO: Evita referencias circulares infinitas
     * 
     * Sin @JsonIgnore:
     * Cuota → Prestamo → Cuotas → Prestamo → Cuotas → ...
     * Jackson entra en loop infinito al serializar JSON
     * 
     * Stack overflow error:
     * com.fasterxml.jackson.databind.JsonMappingException:
     * Infinite recursion (StackOverflowError)
     * 
     * Con @JsonIgnore:
     * JSON de cuota NO incluye prestamo completo
     * {
     *   "id": 1,
     *   "numeroCuota": 1,
     *   "monto": 962.50,
     *   "fechaVencimiento": "2025-11-12",
     *   "fechaPago": "2025-10-15",
     *   "pagada": true
     *   // prestamo NO aparece
     * }
     * 
     * Si necesitas ver el préstamo:
     * GET /prestamos/{id}  // Trae préstamo con sus cuotas
     * 
     * @ManyToOne(optional = false)
     * - optional = false → Cuota SIEMPRE tiene préstamo
     * - Equivalente a NOT NULL en FK
     * - No puede existir cuota huérfana
     * 
     * Sin optional = false:
     * - Permite cuota.prestamo = null
     * - FK nullable en BD
     * 
     * FETCH TYPE por defecto:
     * @ManyToOne → EAGER (carga inmediata)
     * 
     * Significa:
     * Cuota cuota = Cuota.findById(1L);
     * // SQL con JOIN automático:
     * // SELECT c.*, p.*
     * // FROM cuotas c
     * // JOIN prestamos p ON c.prestamo_id = p.id
     * // WHERE c.id = 1
     * 
     * Ventaja: prestamo ya cargado, no lazy exception
     * Desventaja: Siempre carga prestamo (puede ser innecesario)
     * 
     * Para cambiar a LAZY:
     * @ManyToOne(fetch = FetchType.LAZY)
     * 
     * @JoinColumn(name = "prestamo_id")
     * - name = "prestamo_id" → Nombre exacto de FK en tabla SQL
     * - Hibernate genera esta columna en cuotas
     * 
     * SQL:
     * ALTER TABLE cuotas
     * ADD COLUMN prestamo_id BIGINT NOT NULL,
     * ADD FOREIGN KEY (prestamo_id) REFERENCES prestamos(id);
     * 
     * Restricción de integridad referencial:
     * - No puedes insertar cuota con prestamo_id inexistente
     * - Al eliminar préstamo (con cascade), elimina sus cuotas
     * 
     * Analogía: Como el código del préstamo en cada cupón de pago.
     * Cada cupón sabe a qué préstamo pertenece.
     */
    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "prestamo_id")
    public Prestamo prestamo;
    
    // ============================================
    // DATOS DE LA CUOTA
    // ============================================
    
    /**
     * Número secuencial de la cuota dentro del préstamo.
     * 
     * Ejemplos:
     * - Préstamo a 12 meses: cuotas 1, 2, 3... 12
     * - Préstamo a 6 meses: cuotas 1, 2, 3, 4, 5, 6
     * 
     * @Column(nullable = false)
     * - Obligatorio (NOT NULL)
     * - Tipo Integer en Java → INTEGER en SQL
     * 
     * NO es UNIQUE porque:
     * - Diferentes préstamos pueden tener cuota #1
     * - Unicidad es por (prestamo_id, numero_cuota)
     * 
     * Para garantizar unicidad:
     * @Table(
     *     uniqueConstraints = {
     *         @UniqueConstraint(columnNames = {"prestamo_id", "numero_cuota"})
     *     }
     * )
     * 
     * Uso:
     * - Identificar cuota dentro del préstamo
     * - Ordenar cuotas: ORDER BY numero_cuota ASC
     * - Buscar cuota específica:
     *   prestamo.cuotas.stream()
     *     .filter(c -> c.numeroCuota.equals(5))
     *     .findFirst()
     * 
     * Mapeo SQL:
     * numero_cuota INTEGER NOT NULL
     */
    @Column(nullable = false)
    public Integer numeroCuota;
    
    /**
     * Monto a pagar de esta cuota.
     * 
     * @Column(precision = 10, scale = 2)
     * Define tipo DECIMAL en SQL con:
     * - precision = 10 → Total de dígitos (10)
     * - scale = 2 → Dígitos decimales (2)
     * 
     * Rango permitido:
     * - Mínimo: -99,999,999.99
     * - Máximo:  99,999,999.99
     * 
     * Para préstamos:
     * precision = 10 permite montos hasta $99M
     * scale = 2 para centavos
     * 
     * BigDecimal vs double/float:
     * ❌ double monto = 962.50;
     *    - Errores de redondeo
     *    - 0.1 + 0.2 = 0.30000000000000004
     *    - Inaceptable en finanzas
     * 
     * ✅ BigDecimal monto = new BigDecimal("962.50");
     *    - Precisión exacta
     *    - Operaciones aritméticas sin pérdida
     *    - Estándar en sistemas bancarios
     * 
     * Operaciones BigDecimal:
     * BigDecimal suma = monto1.add(monto2);
     * BigDecimal resta = monto1.subtract(monto2);
     * BigDecimal mult = monto1.multiply(monto2);
     * BigDecimal div = monto1.divide(monto2, 2, RoundingMode.HALF_UP);
     * 
     * IMPORTANTE: BigDecimal es INMUTABLE
     * monto.add(100);  // ❌ No modifica monto
     * monto = monto.add(new BigDecimal("100"));  // ✅ Correcto
     * 
     * Cálculo de monto de cuota (simplificado):
     * BigDecimal montoPrestamo = new BigDecimal("10000");
     * BigDecimal tasaMensual = new BigDecimal("0.01291"); // 15.5% / 12
     * Integer plazo = 12;
     * 
     * BigDecimal factor = BigDecimal.ONE
     *     .add(tasaMensual.multiply(new BigDecimal(plazo)));
     * BigDecimal montoCuota = montoPrestamo
     *     .multiply(factor)
     *     .divide(new BigDecimal(plazo), 2, RoundingMode.HALF_UP);
     * // montoCuota ≈ 962.50
     * 
     * Mapeo SQL:
     * monto DECIMAL(10, 2) NOT NULL
     */
    @Column(nullable = false, precision = 10, scale = 2)
    public BigDecimal monto;
    
    /**
     * Fecha límite para pagar esta cuota.
     * 
     * LocalDate (Java 8+):
     * - Solo fecha, sin hora: 2025-11-12
     * - Inmutable y thread-safe
     * - Mejor que java.util.Date (deprecated para fechas)
     * 
     * @Column(nullable = false)
     * - Obligatorio (NOT NULL)
     * - Hibernate mapea LocalDate → DATE en SQL
     * 
     * Generación automática:
     * Si préstamo desembolsado el 2025-10-12:
     * - Cuota 1: 2025-11-12 (+ 1 mes)
     * - Cuota 2: 2025-12-12 (+ 2 meses)
     * - Cuota 3: 2026-01-12 (+ 3 meses)
     * ...
     * - Cuota 12: 2026-10-12 (+ 12 meses)
     * 
     * Código generación:
     * for (int i = 1; i <= plazoMeses; i++) {
     *     LocalDate vencimiento = fechaDesembolso.plusMonths(i);
     *     Cuota cuota = new Cuota(prestamo, i, montoCuota, vencimiento);
     *     cuotas.add(cuota);
     * }
     * 
     * Validaciones futuras (Cap 5):
     * - Vencimiento no puede ser en el pasado
     * - Vencimiento debe ser día hábil
     * - Si cae en fin de semana → mover al lunes siguiente
     * 
     * Uso en lógica de negocio:
     * // Verificar si está vencida
     * if (LocalDate.now().isAfter(fechaVencimiento) && !pagada) {
     *     // Cuota vencida → aplicar mora
     * }
     * 
     * // Días para vencimiento
     * long diasRestantes = ChronoUnit.DAYS.between(
     *     LocalDate.now(), 
     *     fechaVencimiento
     * );
     * 
     * Mapeo SQL:
     * fecha_vencimiento DATE NOT NULL
     * 
     * Analogía: Como la fecha límite en una factura.
     * Pagar antes = OK, pagar después = mora.
     */
    @Column(nullable = false)
    public LocalDate fechaVencimiento;
    
    /**
     * Fecha en que se realizó el pago de la cuota.
     * 
     * NULLABLE: null si aún no se ha pagado
     * 
     * @Column (sin nullable)
     * - Por defecto nullable = true
     * - Permite valores NULL en BD
     * 
     * Estados:
     * - NO PAGADA: fechaPago = null, pagada = false
     * - PAGADA: fechaPago = 2025-10-15, pagada = true
     * 
     * LocalDate (sin hora):
     * - Solo registra DÍA de pago
     * - Si necesitas hora exacta: usar LocalDateTime
     * 
     * LocalDateTime fechaPagoCompleto;  // 2025-10-15T14:30:00
     * 
     * Flujo de pago:
     * 1. Cliente paga cuota
     * 2. Sistema actualiza:
     *    cuota.pagada = true;
     *    cuota.fechaPago = LocalDate.now();
     * 3. Si todas las cuotas pagadas:
     *    prestamo.estado = EstadoPrestamo.PAGADO;
     * 
     * Validaciones:
     * - fechaPago debe ser >= fechaDesembolso
     * - Si fechaPago > fechaVencimiento → calcular mora
     * 
     * Cálculo de mora (ejemplo):
     * if (fechaPago.isAfter(fechaVencimiento)) {
     *     long diasMora = ChronoUnit.DAYS.between(
     *         fechaVencimiento, fechaPago
     *     );
     *     BigDecimal tasaMoraDiaria = new BigDecimal("0.001"); // 0.1% diario
     *     BigDecimal mora = monto.multiply(tasaMoraDiaria)
     *                            .multiply(new BigDecimal(diasMora));
     * }
     * 
     * Queries útiles:
     * // Cuotas pagadas
     * List<Cuota> pagadas = Cuota.find("fechaPago IS NOT NULL").list();
     * 
     * // Cuotas pagadas en rango
     * List<Cuota> cuotasMes = Cuota.find(
     *     "fechaPago BETWEEN ?1 AND ?2",
     *     inicio, fin
     * ).list();
     * 
     * Mapeo SQL:
     * fecha_pago DATE NULL
     */
    @Column
    public LocalDate fechaPago;
    
    /**
     * Indica si la cuota ya fue pagada.
     * 
     * Boolean (objeto) vs boolean (primitivo):
     * - Boolean puede ser null (no recomendado aquí)
     * - boolean siempre tiene valor (false por defecto)
     * 
     * Best Practice para flags:
     * - Usar Boolean con @Column(nullable = false)
     * - Inicializar en constructor: this.pagada = false;
     * - Evita NPE en comparaciones
     * 
     * @Column(nullable = false)
     * - Obligatorio (NOT NULL)
     * - Hibernate mapea Boolean → BOOLEAN en SQL
     * 
     * Estados válidos:
     * ✅ pagada = false, fechaPago = null (pendiente)
     * ✅ pagada = true,  fechaPago = hoy (pagada)
     * ❌ pagada = true,  fechaPago = null (inconsistente)
     * ❌ pagada = false, fechaPago = hoy (inconsistente)
     * 
     * Invariante de negocio:
     * pagada == true ⟺ fechaPago != null
     * 
     * Validación en setter (opcional):
     * public void marcarComoPagada() {
     *     this.pagada = true;
     *     this.fechaPago = LocalDate.now();
     *     // Mantiene consistencia
     * }
     * 
     * Uso en queries:
     * // Cuotas pendientes
     * List<Cuota> pendientes = Cuota.find("pagada", false).list();
     * 
     * // Cuotas pagadas
     * List<Cuota> pagadas = Cuota.find("pagada", true).list();
     * 
     * // Cuotas vencidas y no pagadas
     * List<Cuota> vencidas = Cuota.find(
     *     "pagada = false AND fechaVencimiento < ?1",
     *     LocalDate.now()
     * ).list();
     * 
     * Indexar para performance:
     * @Table(
     *     indexes = {
     *         @Index(name = "idx_pagada", columnList = "pagada")
     *     }
     * )
     * // Acelera queries con WHERE pagada = ?
     * 
     * Mapeo SQL:
     * pagada BOOLEAN NOT NULL DEFAULT false
     * 
     * Alternativa: Enum
     * public enum EstadoCuota { PENDIENTE, PAGADA, VENCIDA, MORA }
     * @Enumerated(EnumType.STRING)
     * public EstadoCuota estado;
     * 
     * Analogía: Como marcar con un sello "PAGADO" en el cupón.
     * Simple, binario, sin ambigüedad.
     */
    @Column(nullable = false)
    public Boolean pagada;
    
    // ============================================
    // CONSTRUCTORES
    // ============================================
    
    /**
     * Constructor vacío (sin parámetros).
     * 
     * OBLIGATORIO PARA JPA/HIBERNATE
     * 
     * Hibernate lo usa para crear instancias desde BD:
     * ResultSet rs = query.executeQuery();
     * while (rs.next()) {
     *     Cuota cuota = new Cuota();  // ← Constructor vacío
     *     cuota.id = rs.getLong("id");
     *     cuota.numeroCuota = rs.getInt("numero_cuota");
     *     // ... setea campos con reflection
     * }
     * 
     * También requerido por:
     * - Jackson (deserializar JSON → Objeto)
     * - Frameworks DI (CDI, Spring)
     */
    public Cuota() {
        // Constructor vacío para JPA
    }
    
    /**
     * Constructor con datos de negocio.
     * 
     * Usado para GENERAR cuotas al crear préstamo:
     * 
     * List<Cuota> cuotas = new ArrayList<>();
     * BigDecimal montoCuota = calcularMontoCuota(prestamo);
     * 
     * for (int i = 1; i <= prestamo.plazoMeses; i++) {
     *     LocalDate vencimiento = prestamo.fechaDesembolso.plusMonths(i);
     *     
     *     Cuota cuota = new Cuota(
     *         prestamo,        // Relación con préstamo
     *         i,               // Número de cuota (1, 2, 3...)
     *         montoCuota,      // Monto a pagar
     *         vencimiento      // Fecha límite
     *     );
     *     
     *     cuotas.add(cuota);
     * }
     * 
     * prestamo.cuotas = cuotas;
     * prestamo.persist();  // Cascade persiste cuotas también
     * 
     * VALORES POR DEFECTO:
     * - pagada = false     → Cuota recién creada está pendiente
     * - fechaPago = null   → No se ha pagado aún
     * 
     * Invariante inicial garantizado:
     * - Nueva cuota siempre es NO PAGADA
     * - Consistencia desde creación
     * 
     * NO incluye 'id':
     * - ID es auto-generado por BD
     * - Se asigna después de persist()
     * 
     * Ejemplo completo:
     * Prestamo prestamo = new Prestamo(
     *     cliente,
     *     new BigDecimal("10000"),
     *     12,
     *     new BigDecimal("15.50"),
     *     LocalDate.now()
     * );
     * 
     * BigDecimal montoCuota = new BigDecimal("962.50");
     * 
     * Cuota cuota1 = new Cuota(
     *     prestamo,
     *     1,
     *     montoCuota,
     *     LocalDate.now().plusMonths(1)  // +1 mes
     * );
     * 
     * Cuota cuota2 = new Cuota(
     *     prestamo,
     *     2,
     *     montoCuota,
     *     LocalDate.now().plusMonths(2)  // +2 meses
     * );
     * 
     * prestamo.cuotas = List.of(cuota1, cuota2);
     * prestamo.persist();
     * 
     * // Hibernate ejecuta:
     * // INSERT INTO prestamos (...)
     * // INSERT INTO cuotas (prestamo_id=1, numero_cuota=1, ...)
     * // INSERT INTO cuotas (prestamo_id=1, numero_cuota=2, ...)
     * 
     * @param prestamo Préstamo al que pertenece
     * @param numeroCuota Número secuencial (1, 2, 3...)
     * @param monto Monto a pagar
     * @param fechaVencimiento Fecha límite de pago
     */
    public Cuota(Prestamo prestamo, Integer numeroCuota, BigDecimal monto, LocalDate fechaVencimiento) {
        this.prestamo = prestamo;
        this.numeroCuota = numeroCuota;
        this.monto = monto;
        this.fechaVencimiento = fechaVencimiento;
        this.pagada = false;      // Estado inicial: NO PAGADA
        this.fechaPago = null;    // Sin fecha de pago aún
    }
    
    // ============================================
    // MÉTODOS ÚTILES (Opcionales)
    // ============================================
    
    /**
     * Marca la cuota como pagada.
     * Método de conveniencia que garantiza consistencia.
     */
    public void marcarComoPagada() {
        this.pagada = true;
        this.fechaPago = LocalDate.now();
    }
    
    /**
     * Verifica si la cuota está vencida.
     * 
     * @return true si no está pagada y ya pasó la fecha de vencimiento
     */
    public boolean estaVencida() {
        return !this.pagada && LocalDate.now().isAfter(this.fechaVencimiento);
    }
    
    /**
     * Calcula días de mora si aplica.
     * 
     * @return Días de atraso, 0 si no hay mora
     */
    public long calcularDiasMora() {
        if (!estaVencida()) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(
            this.fechaVencimiento,
            LocalDate.now()
        );
    }
    
    /**
     * Representación en String para debugging.
     */
    @Override
    public String toString() {
        return "Cuota{" +
                "id=" + id +
                ", numeroCuota=" + numeroCuota +
                ", monto=" + monto +
                ", fechaVencimiento=" + fechaVencimiento +
                ", fechaPago=" + fechaPago +
                ", pagada=" + pagada +
                '}';
    }
}

/**
 * ═══════════════════════════════════════════════════════════════
 * EJEMPLOS DE USO
 * ═══════════════════════════════════════════════════════════════
 * 
 * 1. BUSCAR CUOTA POR ID:
 * 
 *    Cuota cuota = Cuota.findById(1L);
 *    if (cuota != null) {
 *        System.out.println("Cuota #" + cuota.numeroCuota);
 *        System.out.println("Monto: " + cuota.monto);
 *    }
 * 
 * 2. LISTAR CUOTAS PENDIENTES:
 * 
 *    List<Cuota> pendientes = Cuota.find("pagada", false).list();
 *    pendientes.forEach(c -> 
 *        System.out.println("Cuota " + c.numeroCuota + " vence: " + c.fechaVencimiento)
 *    );
 * 
 * 3. PAGAR CUOTA:
 * 
 *    @PUT
 *    @Path("/{id}/pagar")
 *    @Transactional
 *    public Response pagarCuota(@PathParam("id") Long id) {
 *        Cuota cuota = Cuota.findById(id);
 *        
 *        if (cuota == null) {
 *            return Response.status(404).build();
 *        }
 *        
 *        if (cuota.pagada) {
 *            return Response.status(409).entity("Ya pagada").build();
 *        }
 *        
 *        cuota.marcarComoPagada();  // pagada=true, fechaPago=hoy
 *        
 *        return Response.ok(cuota).build();
 *    }
 * 
 * 4. CUOTAS VENCIDAS:
 * 
 *    List<Cuota> vencidas = Cuota.find(
 *        "pagada = false AND fechaVencimiento < ?1",
 *        LocalDate.now()
 *    ).list();
 *    
 *    System.out.println("Cuotas vencidas: " + vencidas.size());
 * 
 * 5. CUOTAS DE UN PRÉSTAMO:
 * 
 *    // Desde préstamo (mejor opción):
 *    Prestamo prestamo = Prestamo.findById(1L);
 *    List<Cuota> cuotas = prestamo.cuotas;
 *    
 *    // O con query:
 *    List<Cuota> cuotas = Cuota.find("prestamo.id", 1L).list();
 * 
 * 6. REPORTE DE PAGOS:
 * 
 *    LocalDate inicio = LocalDate.of(2025, 10, 1);
 *    LocalDate fin = LocalDate.of(2025, 10, 31);
 *    
 *    List<Cuota> pagosMes = Cuota.find(
 *        "fechaPago BETWEEN ?1 AND ?2",
 *        inicio, fin
 *    ).list();
 *    
 *    BigDecimal totalRecaudado = pagosMes.stream()
 *        .map(c -> c.monto)
 *        .reduce(BigDecimal.ZERO, BigDecimal::add);
 * 
 * ═══════════════════════════════════════════════════════════════
 * FLUJO COMPLETO: GENERAR Y PAGAR CUOTAS
 * ═══════════════════════════════════════════════════════════════
 * 
 * // 1. Crear préstamo
 * Prestamo prestamo = new Prestamo(cliente, monto, plazo, tasa, hoy);
 * 
 * // 2. Generar cuotas
 * List<Cuota> cuotas = new ArrayList<>();
 * BigDecimal montoCuota = calcularMontoCuota(prestamo);
 * 
 * for (int i = 1; i <= prestamo.plazoMeses; i++) {
 *     Cuota cuota = new Cuota(
 *         prestamo,
 *         i,
 *         montoCuota,
 *         prestamo.fechaDesembolso.plusMonths(i)
 *     );
 *     cuotas.add(cuota);
 * }
 * 
 * prestamo.cuotas = cuotas;
 * 
 * // 3. Guardar (cascade persiste cuotas)
 * prestamo.persist();
 * 
 * // 4. Pagar primera cuota
 * Cuota cuota1 = prestamo.cuotas.get(0);
 * cuota1.marcarComoPagada();
 * // Auto-persiste por estar en sesión Hibernate
 * 
 * // 5. Verificar si préstamo completado
 * boolean todasPagadas = prestamo.cuotas.stream()
 *     .allMatch(c -> c.pagada);
 * 
 * if (todasPagadas) {
 *     prestamo.estado = EstadoPrestamo.PAGADO;
 * }
 * 
 * ═══════════════════════════════════════════════════════════════
 */