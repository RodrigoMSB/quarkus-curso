package pe.banco.prestamos.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Entidad JPA que representa un préstamo bancario.
 * 
 * PATRÓN: Active Record (extiende PanacheEntity)
 * 
 * Un préstamo es un acuerdo financiero donde el banco presta dinero
 * a un cliente, quien se compromete a devolverlo en cuotas mensuales
 * con una tasa de interés.
 * 
 * ENTIDAD CENTRAL del dominio:
 * - Cliente solicita préstamo
 * - Banco aprueba y desembolsa
 * - Cliente paga en cuotas mensuales
 * - Préstamo pasa por estados: ACTIVO → PAGADO
 * 
 * Relaciones:
 * - Pertenece a UN cliente (N:1)
 * - Tiene MUCHAS cuotas (1:N)
 * 
 * Ciclo de vida:
 * 1. CREACIÓN: Cliente solicita, sistema crea préstamo ACTIVO
 * 2. GENERACIÓN CUOTAS: Se calculan N cuotas automáticamente
 * 3. PAGOS: Cliente paga cuotas una por una
 * 4. FINALIZACIÓN: Todas pagadas → estado PAGADO
 * 
 * Mapeo ORM:
 * ┌────────────────────────────────────────────┐
 * │ Clase Java: Prestamo                       │
 * │                                            │
 * │ - id: Long (heredado)                      │
 * │ - cliente: Cliente (FK)                    │
 * │ - monto: BigDecimal (12,2)                 │
 * │ - plazoMeses: Integer                      │
 * │ - tasaInteres: BigDecimal (5,2)            │
 * │ - fechaDesembolso: LocalDate               │
 * │ - estado: EstadoPrestamo (ENUM)            │
 * │ - cuotas: List<Cuota>                      │
 * └────────────────────────────────────────────┘
 *                     ↓
 * ┌────────────────────────────────────────────┐
 * │ Tabla SQL: prestamos                       │
 * │                                            │
 * │ - id BIGINT PRIMARY KEY                    │
 * │ - cliente_id BIGINT NOT NULL (FK)          │
 * │ - monto DECIMAL(12,2) NOT NULL             │
 * │ - plazo_meses INTEGER NOT NULL             │
 * │ - tasa_interes DECIMAL(5,2) NOT NULL       │
 * │ - fecha_desembolso DATE NOT NULL           │
 * │ - estado VARCHAR(20) NOT NULL              │
 * │                                            │
 * │ FOREIGN KEY (cliente_id)                   │
 * │   REFERENCES clientes(id)                  │
 * └────────────────────────────────────────────┘
 * 
 * Ejemplo de préstamo:
 * - Monto: $10,000.00
 * - Plazo: 12 meses
 * - Tasa: 15.50% anual
 * - Desembolso: 2025-10-12
 * - Estado: ACTIVO
 * - Cuotas: 12 × $962.50
 * 
 * Analogía: Como un contrato de préstamo en papel, pero con
 * superpoderes para guardarse, calcularse y administrarse solo.
 */

// ============================================
// ANOTACIONES DE ENTIDAD JPA
// ============================================

/**
 * @Entity
 * Marca esta clase como entidad JPA persistente.
 * Hibernate la mapea a tabla 'prestamos' automáticamente.
 */
@Entity

/**
 * @Table(name = "prestamos")
 * Nombre explícito de la tabla en base de datos.
 * 
 * Best Practice:
 * - Tabla plural: prestamos
 * - Clase singular: Prestamo
 * 
 * SQL generado:
 * CREATE TABLE prestamos (
 *     id BIGINT NOT NULL,
 *     cliente_id BIGINT NOT NULL,
 *     monto DECIMAL(12,2) NOT NULL,
 *     plazo_meses INTEGER NOT NULL,
 *     tasa_interes DECIMAL(5,2) NOT NULL,
 *     fecha_desembolso DATE NOT NULL,
 *     estado VARCHAR(20) NOT NULL,
 *     PRIMARY KEY (id),
 *     FOREIGN KEY (cliente_id) REFERENCES clientes(id)
 * );
 */
@Table(name = "prestamos")

/**
 * extends PanacheEntity
 * 
 * ACTIVE RECORD PATTERN
 * 
 * Hereda automáticamente:
 * - public Long id (PK auto-generada)
 * - Métodos estáticos: persist(), findById(), listAll(), etc.
 * - Métodos de instancia: persist(), delete(), isPersistent()
 * 
 * Uso:
 * Prestamo prestamo = new Prestamo(...);
 * prestamo.persist();  // Se guarda solo
 * 
 * Prestamo p = Prestamo.findById(1L);
 * List<Prestamo> todos = Prestamo.listAll();
 * List<Prestamo> activos = Prestamo.find("estado", EstadoPrestamo.ACTIVO).list();
 */
public class Prestamo extends PanacheEntity {
    
    // ============================================
    // RELACIONES CON OTRAS ENTIDADES
    // ============================================
    
    /**
     * Cliente titular del préstamo.
     * 
     * RELACIÓN: Many-to-One (N:1)
     * - Muchos préstamos → Un cliente
     * - Prestamo es el lado DUEÑO (tiene FK cliente_id)
     * - Cliente es el lado INVERSO (mappedBy)
     * 
     * @ManyToOne(optional = false)
     * - optional = false → Cliente es OBLIGATORIO
     * - No puede existir préstamo sin cliente
     * - NOT NULL en FK
     * 
     * Sin optional=false:
     * - Permite prestamo.cliente = null
     * - Préstamo huérfano (sin sentido en dominio bancario)
     * 
     * FETCH TYPE por defecto:
     * @ManyToOne → EAGER (carga inmediata)
     * 
     * Comportamiento:
     * Prestamo p = Prestamo.findById(1L);
     * // SQL con JOIN:
     * // SELECT p.*, c.*
     * // FROM prestamos p
     * // JOIN clientes c ON p.cliente_id = c.id
     * // WHERE p.id = 1
     * 
     * // cliente ya cargado:
     * System.out.println(p.cliente.nombre);  // ✅ Sin lazy exception
     * 
     * VENTAJA EAGER:
     * - Siempre disponible, no lazy exception
     * - Útil cuando casi siempre necesitas el cliente
     * 
     * DESVENTAJA EAGER:
     * - Carga cliente incluso si no lo usas
     * - JOIN en cada query (puede ser pesado)
     * 
     * Para cambiar a LAZY:
     * @ManyToOne(fetch = FetchType.LAZY, optional = false)
     * // Solo carga cliente cuando lo accedes
     * // Más eficiente si no siempre lo necesitas
     * 
     * @JoinColumn(name = "cliente_id")
     * - Nombre de columna FK en tabla prestamos
     * - Hibernate genera esta columna automáticamente
     * 
     * SQL:
     * ALTER TABLE prestamos
     * ADD COLUMN cliente_id BIGINT NOT NULL,
     * ADD FOREIGN KEY (cliente_id) REFERENCES clientes(id);
     * 
     * Integridad referencial:
     * - No puedes insertar préstamo con cliente_id inexistente
     * - Al eliminar cliente (depende de cascade):
     *   · ON DELETE CASCADE → elimina préstamos
     *   · ON DELETE RESTRICT → error si tiene préstamos
     * 
     * En nuestra implementación:
     * - Cliente.prestamos tiene cascade=ALL, orphanRemoval=true
     * - Al eliminar cliente, se eliminan sus préstamos
     * 
     * Uso en código:
     * Cliente cliente = Cliente.findById(1L);
     * 
     * Prestamo prestamo = new Prestamo();
     * prestamo.cliente = cliente;  // Asignar relación
     * prestamo.persist();
     * 
     * // Hibernate genera:
     * // INSERT INTO prestamos (cliente_id, ...) VALUES (1, ...)
     * 
     * Analogía: Como el nombre del titular en un contrato de préstamo.
     * El contrato (préstamo) debe tener un titular (cliente) siempre.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id")
    public Cliente cliente;
    
    // ============================================
    // DATOS FINANCIEROS DEL PRÉSTAMO
    // ============================================
    
    /**
     * Monto total del préstamo solicitado.
     * 
     * @Column(precision = 12, scale = 2)
     * Define DECIMAL en SQL:
     * - precision = 12 → Total de dígitos
     * - scale = 2 → Dígitos decimales
     * 
     * Rango permitido:
     * - Mínimo: -9,999,999,999.99
     * - Máximo:  9,999,999,999.99
     * 
     * Para préstamos bancarios:
     * - 12 dígitos permite montos hasta $9,999M (suficiente)
     * - 2 decimales para centavos
     * 
     * BigDecimal: OBLIGATORIO en finanzas
     * 
     * ❌ NUNCA usar double/float para dinero:
     * double monto = 10000.00;
     * double cuota = monto / 12;
     * // cuota = 833.3333333333334 (impreciso)
     * 
     * ✅ SIEMPRE BigDecimal:
     * BigDecimal monto = new BigDecimal("10000.00");
     * BigDecimal cuota = monto.divide(
     *     new BigDecimal("12"), 
     *     2, 
     *     RoundingMode.HALF_UP
     * );
     * // cuota = 833.33 (exacto)
     * 
     * Constructor String vs double:
     * new BigDecimal("10000.00")  // ✅ Precisión exacta
     * new BigDecimal(10000.00)    // ⚠️ Puede perder precisión
     * 
     * Operaciones BigDecimal (INMUTABLES):
     * BigDecimal total = monto.add(interes);           // Suma
     * BigDecimal resto = monto.subtract(pago);         // Resta
     * BigDecimal interes = monto.multiply(tasa);       // Multiplicación
     * BigDecimal cuota = monto.divide(plazo, 2, RM);   // División
     * 
     * int comparacion = monto1.compareTo(monto2);
     * // -1 si monto1 < monto2
     * //  0 si monto1 == monto2
     * //  1 si monto1 > monto2
     * 
     * Validaciones recomendadas (Cap 5):
     * @DecimalMin(value = "100.00", message = "Monto mínimo $100")
     * @DecimalMax(value = "100000.00", message = "Monto máximo $100K")
     * public BigDecimal monto;
     * 
     * Reglas de negocio típicas:
     * - Monto mínimo: $100
     * - Monto máximo: depende del perfil del cliente
     * - Múltiplo de: $100 (ej: 1500, 2000, no 1532.47)
     * 
     * Cálculo de monto total a pagar:
     * BigDecimal tasaMensual = tasaInteres
     *     .divide(new BigDecimal("100"), 6, HALF_UP)  // % a decimal
     *     .divide(new BigDecimal("12"), 6, HALF_UP);  // Anual a mensual
     * 
     * BigDecimal factor = BigDecimal.ONE.add(
     *     tasaMensual.multiply(new BigDecimal(plazoMeses))
     * );
     * 
     * BigDecimal totalPagar = monto.multiply(factor);
     * 
     * Mapeo SQL:
     * monto DECIMAL(12, 2) NOT NULL
     * 
     * Analogía: Como el monto escrito en el contrato.
     * Debe ser exacto, sin ambigüedades, centavo por centavo.
     */
    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal monto;
    
    /**
     * Plazo del préstamo en meses.
     * 
     * Define en cuántas cuotas mensuales se pagará el préstamo.
     * 
     * @Column(nullable = false)
     * - Obligatorio (NOT NULL)
     * - Integer en Java → INTEGER en SQL
     * 
     * Valores típicos:
     * - 6 meses (corto plazo)
     * - 12 meses (1 año, común)
     * - 24 meses (2 años)
     * - 36 meses (3 años)
     * - 60 meses (5 años, largo plazo)
     * 
     * Impacto en cuotas:
     * - Menor plazo → Cuotas más altas, menos interés total
     * - Mayor plazo → Cuotas más bajas, más interés total
     * 
     * Ejemplo:
     * Préstamo $10,000 al 15.5% anual
     * 
     * 6 meses:  cuota ≈ $1,729 → total ≈ $10,374
     * 12 meses: cuota ≈ $962  → total ≈ $11,550
     * 24 meses: cuota ≈ $528  → total ≈ $12,672
     * 
     * Relación con cuotas:
     * - plazoMeses = 12 → genera 12 cuotas
     * - plazoMeses = 6  → genera 6 cuotas
     * 
     * Generación de cuotas:
     * for (int i = 1; i <= prestamo.plazoMeses; i++) {
     *     Cuota cuota = new Cuota(
     *         prestamo,
     *         i,  // número de cuota
     *         montoCuota,
     *         fechaDesembolso.plusMonths(i)  // vencimiento
     *     );
     *     cuotas.add(cuota);
     * }
     * 
     * Validaciones (Cap 5):
     * @Min(value = 1, message = "Mínimo 1 mes")
     * @Max(value = 60, message = "Máximo 60 meses")
     * public Integer plazoMeses;
     * 
     * Reglas de negocio:
     * - Plazos permitidos: 6, 12, 18, 24, 36, 48, 60 meses
     * - No valores arbitrarios: 7, 13, 25 meses ❌
     * 
     * Cálculo de fecha final:
     * LocalDate fechaFinal = fechaDesembolso.plusMonths(plazoMeses);
     * // Desembolso: 2025-10-12, Plazo: 12
     * // Final: 2026-10-12
     * 
     * Mapeo SQL:
     * plazo_meses INTEGER NOT NULL
     */
    @Column(nullable = false)
    public Integer plazoMeses;
    
    /**
     * Tasa de interés anual del préstamo (porcentaje).
     * 
     * @Column(precision = 5, scale = 2)
     * - Permite valores como: 15.50, 9.99, 100.00
     * - 5 dígitos totales, 2 decimales
     * - Rango: -999.99 a 999.99
     * 
     * BigDecimal para precisión exacta:
     * - 15.50% se guarda como new BigDecimal("15.50")
     * - NO como porcentaje decimal (0.155)
     * 
     * Formato almacenado:
     * - Base 100: 15.50 significa 15.50%
     * - Para cálculos: dividir entre 100
     * 
     * Conversión a decimal:
     * BigDecimal tasaDecimal = tasaInteres.divide(
     *     new BigDecimal("100"), 
     *     6,  // 6 decimales de precisión
     *     RoundingMode.HALF_UP
     * );
     * // 15.50 → 0.155000
     * 
     * Tasa mensual:
     * BigDecimal tasaMensual = tasaDecimal.divide(
     *     new BigDecimal("12"),
     *     6,
     *     RoundingMode.HALF_UP
     * );
     * // 0.155 / 12 = 0.012916... (≈ 1.29% mensual)
     * 
     * Cálculo de cuota (fórmula simplificada):
     * BigDecimal factor = BigDecimal.ONE.add(
     *     tasaMensual.multiply(new BigDecimal(plazoMeses))
     * );
     * 
     * BigDecimal montoCuota = monto
     *     .multiply(factor)
     *     .divide(new BigDecimal(plazoMeses), 2, HALF_UP);
     * 
     * Ejemplo real:
     * - Monto: $10,000
     * - Tasa: 15.50% anual
     * - Plazo: 12 meses
     * - Tasa mensual: ≈ 1.29%
     * - Cuota: ≈ $962.50
     * 
     * Tasas típicas en el mercado:
     * - Préstamo personal: 15% - 35%
     * - Préstamo hipotecario: 8% - 12%
     * - Tarjeta crédito: 30% - 60%
     * - Préstamo vehicular: 12% - 18%
     * 
     * Validaciones (Cap 5):
     * @DecimalMin(value = "0.01", message = "Tasa mínima 0.01%")
     * @DecimalMax(value = "100.00", message = "Tasa máxima 100%")
     * public BigDecimal tasaInteres;
     * 
     * IMPORTANTE:
     * Esta es tasa NOMINAL, no efectiva (TEA).
     * En sistema real, calcular TEA:
     * TEA = (1 + tasaMensual)^12 - 1
     * 
     * Comparación de almacenamiento:
     * ✅ Nuestro enfoque: 15.50 (como se ve)
     * ⚠️ Alternativa 1: 0.155 (decimal directo)
     * ⚠️ Alternativa 2: 1550 (en basis points × 10000)
     * 
     * Mapeo SQL:
     * tasa_interes DECIMAL(5, 2) NOT NULL
     * 
     * Analogía: Como el porcentaje de interés escrito en el contrato.
     * "15.50% de interés anual" - exacto, sin redondeos.
     */
    @Column(nullable = false, precision = 5, scale = 2)
    public BigDecimal tasaInteres;
    
    /**
     * Fecha en que se desembolsó (entregó) el préstamo.
     * 
     * LocalDate: Solo fecha, sin hora
     * - 2025-10-12 (año-mes-día)
     * - Inmutable y thread-safe
     * - API moderna de Java 8+
     * 
     * @Column(nullable = false)
     * - Obligatorio (NOT NULL)
     * - Hibernate mapea LocalDate → DATE en SQL
     * 
     * Significado:
     * - Día que el dinero se transfirió al cliente
     * - Inicio del periodo de pago
     * - Base para calcular vencimientos de cuotas
     * 
     * Uso en generación de cuotas:
     * // Cuota 1 vence: desembolso + 1 mes
     * LocalDate venc1 = fechaDesembolso.plusMonths(1);
     * 
     * // Cuota 2 vence: desembolso + 2 meses
     * LocalDate venc2 = fechaDesembolso.plusMonths(2);
     * 
     * // ...
     * 
     * // Cuota N vence: desembolso + N meses
     * LocalDate vencN = fechaDesembolso.plusMonths(plazoMeses);
     * 
     * Ejemplo:
     * Desembolso: 2025-10-12
     * Plazo: 12 meses
     * 
     * Vencimientos:
     * - Cuota 1:  2025-11-12
     * - Cuota 2:  2025-12-12
     * - Cuota 3:  2026-01-12
     * - ...
     * - Cuota 12: 2026-10-12
     * 
     * Validaciones típicas:
     * - No puede ser futuro (desembolso ya ocurrió)
     * - No puede ser muy antiguo (ej: > 10 años atrás)
     * - Debe ser día hábil (lunes-viernes)
     * 
     * Validación Bean Validation:
     * @PastOrPresent(message = "Fecha no puede ser futura")
     * public LocalDate fechaDesembolso;
     * 
     * Ajuste a día hábil (lógica adicional):
     * LocalDate fecha = LocalDate.now();
     * DayOfWeek dia = fecha.getDayOfWeek();
     * 
     * if (dia == DayOfWeek.SATURDAY) {
     *     fecha = fecha.plusDays(2);  // Lunes
     * } else if (dia == DayOfWeek.SUNDAY) {
     *     fecha = fecha.plusDays(1);  // Lunes
     * }
     * 
     * Cálculo de duración:
     * long diasTranscurridos = ChronoUnit.DAYS.between(
     *     fechaDesembolso,
     *     LocalDate.now()
     * );
     * 
     * long mesesTranscurridos = ChronoUnit.MONTHS.between(
     *     fechaDesembolso,
     *     LocalDate.now()
     * );
     * 
     * Fecha de finalización:
     * LocalDate fechaFinal = fechaDesembolso.plusMonths(plazoMeses);
     * 
     * Comportamiento por defecto:
     * En constructor se setea LocalDate.now() (hoy)
     * 
     * Mapeo SQL:
     * fecha_desembolso DATE NOT NULL
     * 
     * Alternativa con timestamp:
     * Si necesitas hora exacta:
     * @Column
     * public LocalDateTime fechaHoraDesembolso;
     * // 2025-10-12T14:30:00
     */
    @Column(nullable = false)
    public LocalDate fechaDesembolso;
    
    // ============================================
    // ESTADO Y CONTROL DEL PRÉSTAMO
    // ============================================
    
    /**
     * Estado actual del préstamo.
     * 
     * @Enumerated(EnumType.STRING)
     * Guarda el NOMBRE del enum, no el ordinal
     * 
     * EnumType.STRING:
     * - Guarda: "ACTIVO", "PAGADO", "VENCIDO", "CANCELADO"
     * - Legible en base de datos
     * - Robusto a cambios de orden en enum
     * - Recomendado SIEMPRE
     * 
     * EnumType.ORDINAL (NO usar):
     * - Guarda: 0, 1, 2, 3 (posición en enum)
     * - Ilegible en BD
     * - Frágil: si reordenas enum, rompe datos
     * 
     * @Column(length = 20)
     * - VARCHAR(20) en SQL
     * - 20 caracteres suficiente para nombres de estados
     * - "CANCELADO" tiene 9 chars
     * 
     * Estados del préstamo:
     * 
     * ACTIVO:
     * - Préstamo vigente, pagándose
     * - Tiene cuotas pendientes
     * - Estado inicial al crear
     * 
     * PAGADO:
     * - Todas las cuotas pagadas
     * - Préstamo finalizado exitosamente
     * - No requiere más pagos
     * 
     * VENCIDO:
     * - Tiene cuotas vencidas sin pagar
     * - Cliente en mora
     * - Puede aplicar penalidades
     * 
     * CANCELADO:
     * - Préstamo cancelado anticipadamente
     * - Cliente pagó todo el saldo restante
     * - O préstamo dado de baja
     * 
     * Transiciones de estado:
     * 
     *    [ACTIVO]
     *       ↓
     *   (paga cuotas)
     *       ↓
     *   [PAGADO]
     * 
     *    [ACTIVO]
     *       ↓
     *   (se vence sin pagar)
     *       ↓
     *   [VENCIDO]
     * 
     *    [ACTIVO]
     *       ↓
     *   (pago anticipado)
     *       ↓
     *   [CANCELADO]
     * 
     * Lógica de cambio de estado:
     * 
     * // Al pagar última cuota:
     * boolean todasPagadas = prestamo.cuotas.stream()
     *     .allMatch(c -> c.pagada);
     * 
     * if (todasPagadas) {
     *     prestamo.estado = EstadoPrestamo.PAGADO;
     * }
     * 
     * // Al detectar vencimiento:
     * boolean tieneVencidas = prestamo.cuotas.stream()
     *     .anyMatch(c -> !c.pagada && 
     *                    LocalDate.now().isAfter(c.fechaVencimiento));
     * 
     * if (tieneVencidas) {
     *     prestamo.estado = EstadoPrestamo.VENCIDO;
     * }
     * 
     * Queries por estado:
     * // Préstamos activos
     * List<Prestamo> activos = Prestamo.find(
     *     "estado", EstadoPrestamo.ACTIVO
     * ).list();
     * 
     * // Préstamos de un cliente en mora
     * List<Prestamo> vencidos = Prestamo.find(
     *     "cliente.id = ?1 AND estado = ?2",
     *     clienteId,
     *     EstadoPrestamo.VENCIDO
     * ).list();
     * 
     * Validaciones:
     * - Estado no puede ser null (nullable=false)
     * - Solo estados válidos del enum
     * - Transiciones válidas (state machine pattern)
     * 
     * Mapeo SQL:
     * estado VARCHAR(20) NOT NULL
     * 
     * Valores en BD:
     * 'ACTIVO', 'PAGADO', 'VENCIDO', 'CANCELADO'
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public EstadoPrestamo estado;
    
    // ============================================
    // RELACIÓN CON CUOTAS
    // ============================================
    
    /**
     * Lista de cuotas de pago del préstamo.
     * 
     * RELACIÓN: One-to-Many (1:N)
     * - Un préstamo tiene MUCHAS cuotas
     * - Una cuota pertenece a UN préstamo
     * 
     * @OneToMany(mappedBy = "prestamo", ...)
     * 
     * LADO INVERSO de la relación bidireccional
     * 
     * - mappedBy = "prestamo" → Campo en Cuota que apunta aquí
     *   Cuota.prestamo es el DUEÑO (tiene FK prestamo_id)
     *   Prestamo.cuotas es INVERSO (solo lectura)
     * 
     * - cascade = CascadeType.ALL
     *   Propaga TODAS las operaciones a cuotas
     * 
     *   prestamo.persist();
     *   → Automáticamente persiste todas sus cuotas
     * 
     *   prestamo.delete();
     *   → Automáticamente elimina todas sus cuotas
     * 
     *   Tipos de cascade:
     *   · PERSIST: Solo al guardar
     *   · MERGE: Solo al actualizar
     *   · REMOVE: Solo al eliminar
     *   · REFRESH: Recargar desde BD
     *   · ALL: Todas las anteriores ✅
     * 
     * - orphanRemoval = true
     *   Elimina cuotas "huérfanas" (sin préstamo)
     * 
     *   prestamo.cuotas.remove(cuota);
     *   → Hibernate ejecuta DELETE de esa cuota
     * 
     *   Sin orphanRemoval:
     *   → Solo pone cuota.prestamo_id = NULL (huérfana)
     * 
     * FETCH TYPE por defecto:
     * @OneToMany → LAZY (carga perezosa)
     * 
     * Comportamiento LAZY:
     * Prestamo p = Prestamo.findById(1L);
     * // SQL: SELECT * FROM prestamos WHERE id = 1
     * // NO carga cuotas aún
     * 
     * System.out.println(p.cuotas.size());
     * // AHORA ejecuta:
     * // SELECT * FROM cuotas WHERE prestamo_id = 1
     * 
     * VENTAJA: No carga datos innecesarios
     * DESVENTAJA: Problema N+1
     * 
     * List<Prestamo> prestamos = Prestamo.listAll();
     * for (Prestamo p : prestamos) {
     *     System.out.println(p.cuotas.size());
     *     // 1 query extra POR CADA préstamo ❌
     * }
     * 
     * Solución N+1 con JOIN FETCH:
     * List<Prestamo> prestamos = Prestamo.find(
     *     "SELECT p FROM Prestamo p LEFT JOIN FETCH p.cuotas"
     * ).list();
     * // 1 sola query con JOIN ✅
     * 
     * LazyInitializationException:
     * Al serializar JSON, si sesión cerró:
     * 
     * @GET
     * public Prestamo obtener(@PathParam("id") Long id) {
     *     Prestamo p = Prestamo.findById(id);
     *     // Sesión cierra aquí
     *     return p;  // Jackson serializa
     * }
     * 
     * // JSON intenta acceder p.cuotas
     * // ❌ LazyInitializationException: no session
     * 
     * Soluciones:
     * 1. JOIN FETCH en query
     * 2. @Transactional en método (mantiene sesión)
     * 3. DTO sin cuotas
     * 4. fetch = EAGER (siempre carga)
     * 
     * Generación automática de cuotas:
     * List<Cuota> cuotas = new ArrayList<>();
     * BigDecimal montoCuota = calcularMontoCuota(prestamo);
     * 
     * for (int i = 1; i <= prestamo.plazoMeses; i++) {
     *     LocalDate vencimiento = prestamo.fechaDesembolso.plusMonths(i);
     *     Cuota cuota = new Cuota(prestamo, i, montoCuota, vencimiento);
     *     cuotas.add(cuota);
     * }
     * 
     * prestamo.cuotas = cuotas;
     * prestamo.persist();  // Cascade guarda cuotas también
     * 
     * Navegación bidireccional:
     * // Desde préstamo a cuotas:
     * prestamo.cuotas.forEach(c -> 
     *     System.out.println("Cuota " + c.numeroCuota)
     * );
     * 
     * // Desde cuota a préstamo:
     * System.out.println("Préstamo #" + cuota.prestamo.id);
     * 
     * Mapeo SQL:
     * No genera columna en tabla 'prestamos'.
     * La relación vive en tabla 'cuotas':
     * 
     * CREATE TABLE cuotas (
     *     ...
     *     prestamo_id BIGINT NOT NULL,
     *     FOREIGN KEY (prestamo_id) REFERENCES prestamos(id)
     * );
     * 
     * Analogía: Como el plan de cuotas adjunto al contrato.
     * El contrato (préstamo) incluye el detalle de cada pago.
     */
    @OneToMany(mappedBy = "prestamo", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<Cuota> cuotas;
    
    // ============================================
    // CONSTRUCTORES
    // ============================================
    
    /**
     * Constructor vacío (sin parámetros).
     * 
     * OBLIGATORIO PARA JPA/HIBERNATE
     * 
     * Hibernate lo necesita para:
     * - Crear instancias desde ResultSet
     * - Usar reflection para setear campos
     * 
     * También requerido por:
     * - JAX-RS (deserializar JSON)
     * - CDI (inyección de dependencias)
     */
    public Prestamo() {
        // Constructor vacío para JPA
    }
    
    /**
     * Constructor con datos de negocio.
     * 
     * Crea un nuevo préstamo ACTIVO con los parámetros dados.
     * 
     * Uso típico:
     * Cliente cliente = Cliente.findById(1L);
     * 
     * Prestamo prestamo = new Prestamo(
     *     cliente,
     *     new BigDecimal("10000.00"),  // $10K
     *     12,                           // 12 meses
     *     new BigDecimal("15.50"),      // 15.5% anual
     *     LocalDate.now()               // Hoy
     * );
     * 
     * // Generar cuotas
     * prestamo.cuotas = generarCuotas(prestamo);
     * 
     * // Guardar todo (cascade persiste cuotas)
     * prestamo.persist();
     * 
     * Estado inicial:
     * - estado = EstadoPrestamo.ACTIVO
     * - Préstamo recién creado está activo
     * - Cliente puede empezar a pagar
     * 
     * NO incluye 'id':
     * - ID es auto-generado por BD
     * - Se asigna después de persist()
     * 
     * NO incluye 'cuotas':
     * - Se generan separadamente
     * - Se asignan antes de persist()
     * 
     * Flujo completo de creación:
     * 
     * @POST
     * @Transactional
     * public Response crear(PrestamoRequest request) {
     *     // 1. Buscar cliente
     *     Cliente cliente = Cliente.findById(request.clienteId);
     *     
     *     // 2. Crear préstamo
     *     Prestamo prestamo = new Prestamo(
     *         cliente,
     *         request.monto,
     *         request.plazoMeses,
     *         request.tasaInteres,
     *         LocalDate.now()
     *     );
     *     
     *     // 3. Generar cuotas
     *     prestamo.cuotas = generarCuotas(prestamo);
     *     
     *     // 4. Guardar (cascade persiste cuotas)
     *     prestamo.persist();
     *     
     *     // 5. Retornar
     *     return Response.status(201).entity(prestamo).build();
     * }
     * 
     * @param cliente Cliente titular del préstamo
     * @param monto Monto total a prestar
     * @param plazoMeses Número de meses para pagar
     * @param tasaInteres Tasa de interés anual (%)
     * @param fechaDesembolso Fecha de entrega del dinero
     */
    public Prestamo(Cliente cliente, BigDecimal monto, Integer plazoMeses, 
                    BigDecimal tasaInteres, LocalDate fechaDesembolso) {
        this.cliente = cliente;
        this.monto = monto;
        this.plazoMeses = plazoMeses;
        this.tasaInteres = tasaInteres;
        this.fechaDesembolso = fechaDesembolso;
        this.estado = EstadoPrestamo.ACTIVO;  // Estado inicial
        // cuotas se asigna después (no en constructor)
    }
    
    // ============================================
    // ENUM DE ESTADOS
    // ============================================
    
    /**
     * Estados posibles de un préstamo.
     * 
     * ENUM anidado en la entidad (alternativa: clase separada)
     * 
     * Ventajas de enum anidado:
     * - Cohesión: estado está cerca de Prestamo
     * - Namespace: EstadoPrestamo claramente relacionado
     * - Encapsulación: solo Prestamo lo usa
     * 
     * Uso:
     * prestamo.estado = Prestamo.EstadoPrestamo.ACTIVO;
     * 
     * O con import static:
     * import static pe.banco.prestamos.model.Prestamo.EstadoPrestamo.*;
     * prestamo.estado = ACTIVO;
     * 
     * Persistencia:
     * @Enumerated(EnumType.STRING) en el campo 'estado'
     * → Guarda "ACTIVO", "PAGADO", etc. en BD
     */
    public enum EstadoPrestamo {
        /**
         * Préstamo activo, en proceso de pago.
         * Estado inicial de todo préstamo nuevo.
         */
        ACTIVO,
        
        /**
         * Préstamo totalmente pagado.
         * Todas las cuotas fueron canceladas.
         */
        PAGADO,
        
        /**
         * Préstamo con cuotas vencidas.
         * Cliente en mora, debe regularizar.
         */
        VENCIDO,
        
        /**
         * Préstamo cancelado anticipadamente.
         * Cliente pagó todo el saldo restante, o fue dado de baja.
         */
        CANCELADO
    }
    
    // ============================================
    // MÉTODOS ÚTILES (Opcionales)
    // ============================================
    
    /**
     * Verifica si el préstamo está completamente pagado.
     */
    public boolean estaPagado() {
        return this.cuotas != null && 
               this.cuotas.stream().allMatch(c -> c.pagada);
    }
    
    /**
     * Verifica si tiene cuotas vencidas.
     */
    public boolean tieneVencidas() {
        return this.cuotas != null &&
               this.cuotas.stream().anyMatch(c -> c.estaVencida());
    }
    
    /**
     * Calcula el saldo pendiente (cuotas sin pagar).
     */
    public BigDecimal calcularSaldoPendiente() {
        if (this.cuotas == null) return BigDecimal.ZERO;
        
        return this.cuotas.stream()
            .filter(c -> !c.pagada)
            .map(c -> c.monto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Representación en String para debugging.
     */
    @Override
    public String toString() {
        return "Prestamo{" +
                "id=" + id +
                ", cliente=" + (cliente != null ? cliente.nombre : "null") +
                ", monto=" + monto +
                ", plazoMeses=" + plazoMeses +
                ", tasaInteres=" + tasaInteres +
                ", estado=" + estado +
                '}';
    }
}

/**
 * ═══════════════════════════════════════════════════════════════
 * FLUJO COMPLETO: CREAR PRÉSTAMO CON CUOTAS
 * ═══════════════════════════════════════════════════════════════
 * 
 * @POST
 * @Path("/prestamos")
 * @Transactional
 * public Response crear(PrestamoRequest request) {
 *     
 *     // 1. VALIDAR CLIENTE
 *     Cliente cliente = Cliente.findById(request.clienteId);
 *     if (cliente == null) {
 *         return Response.status(404)
 *             .entity("Cliente no encontrado")
 *             .build();
 *     }
 *     
 *     // 2. CREAR PRÉSTAMO
 *     Prestamo prestamo = new Prestamo(
 *         cliente,
 *         request.monto,              // $10,000
 *         request.plazoMeses,         // 12 meses
 *         request.tasaInteres,        // 15.50%
 *         LocalDate.now()             // Hoy
 *     );
 *     
 *     // 3. CALCULAR MONTO DE CUOTA
 *     BigDecimal tasaMensual = request.tasaInteres
 *         .divide(new BigDecimal("100"), 6, HALF_UP)
 *         .divide(new BigDecimal("12"), 6, HALF_UP);
 *     
 *     BigDecimal factor = BigDecimal.ONE.add(
 *         tasaMensual.multiply(new BigDecimal(request.plazoMeses))
 *     );
 *     
 *     BigDecimal montoCuota = request.monto
 *         .multiply(factor)
 *         .divide(new BigDecimal(request.plazoMeses), 2, HALF_UP);
 *     
 *     // 4. GENERAR CUOTAS
 *     List<Cuota> cuotas = new ArrayList<>();
 *     
 *     for (int i = 1; i <= prestamo.plazoMeses; i++) {
 *         LocalDate vencimiento = prestamo.fechaDesembolso.plusMonths(i);
 *         
 *         Cuota cuota = new Cuota(
 *             prestamo,
 *             i,              // Número cuota
 *             montoCuota,     // $962.50
 *             vencimiento     // 2025-11-12, 2025-12-12, ...
 *         );
 *         
 *         cuotas.add(cuota);
 *     }
 *     
 *     prestamo.cuotas = cuotas;
 *     
 *     // 5. PERSISTIR (cascade guarda cuotas también)
 *     prestamo.persist();
 *     
 *     // 6. RETORNAR
 *     return Response.status(201).entity(prestamo).build();
 * }
 * 
 * SQL GENERADO POR HIBERNATE:
 * 
 * -- 1. Insertar préstamo
 * INSERT INTO prestamos (
 *     cliente_id, monto, plazo_meses, tasa_interes, 
 *     fecha_desembolso, estado
 * ) VALUES (
 *     1, 10000.00, 12, 15.50, '2025-10-12', 'ACTIVO'
 * );
 * 
 * -- 2. Insertar cuotas (12 INSERTs)
 * INSERT INTO cuotas (prestamo_id, numero_cuota, monto, fecha_vencimiento, pagada)
 * VALUES (1, 1, 962.50, '2025-11-12', false);
 * 
 * INSERT INTO cuotas (prestamo_id, numero_cuota, monto, fecha_vencimiento, pagada)
 * VALUES (1, 2, 962.50, '2025-12-12', false);
 * 
 * -- ... 10 INSERTs más
 * 
 * ═══════════════════════════════════════════════════════════════
 */