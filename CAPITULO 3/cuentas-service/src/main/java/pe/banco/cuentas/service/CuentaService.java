package pe.banco.cuentas.service;

import pe.banco.cuentas.model.Cuenta;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de lógica de negocio para gestión de cuentas bancarias.
 * 
 * CAPA DE SERVICIO en arquitectura de 3 capas:
 * ┌─────────────────────────────────┐
 * │ Resource (REST)                 │ ← HTTP/JSON
 * ├─────────────────────────────────┤
 * │ Service (esta clase)            │ ← LÓGICA DE NEGOCIO
 * ├─────────────────────────────────┤
 * │ Data (Map/DB)                   │ ← Persistencia
 * └─────────────────────────────────┘
 * 
 * Responsabilidades:
 * ✅ Lógica de negocio y reglas del dominio
 * ✅ Validaciones complejas
 * ✅ Transacciones (futuro con DB)
 * ✅ Orquestación de operaciones
 * ✅ Transformaciones de datos
 * 
 * ❌ NO maneja HTTP/REST (eso es del Resource)
 * ❌ NO conoce JSON (trabaja con POJOs)
 * ❌ NO sabe de request/response
 * 
 * Analogía: Como el gerente de un banco que:
 * - Toma decisiones de negocio (validar saldo suficiente)
 * - Aplica políticas del banco (tasas, límites)
 * - Coordina operaciones (transferencias)
 * - Delega acceso a datos (bóveda/archivos)
 */

// ============================================
// ANOTACIÓN DE SCOPE - CDI
// ============================================

/**
 * @ApplicationScoped
 * Define que esta clase es un Bean CDI con scope de aplicación.
 * 
 * SINGLETON PATTERN AUTOMÁTICO:
 * - Una SOLA instancia en toda la aplicación
 * - Creada una vez (lazy) al primer uso
 * - Compartida por todos los que la inyecten
 * - Destruida al cerrar la aplicación
 * 
 * THREAD-SAFETY:
 * Al ser singleton, DEBE ser thread-safe porque:
 * - Múltiples requests HTTP concurrentes
 * - Todos usan la misma instancia
 * - Acceso simultáneo a `cuentas` Map
 * - Por eso usamos ConcurrentHashMap (thread-safe)
 * 
 * COMPARACIÓN DE SCOPES:
 * ┌─────────────────────┬──────────────┬────────────────┐
 * │ Scope               │ Instancias   │ Cuándo usar    │
 * ├─────────────────────┼──────────────┼────────────────┤
 * │ @ApplicationScoped  │ 1 (app)      │ Services, DAOs │
 * │ @RequestScoped      │ 1 (request)  │ Request data   │
 * │ @Dependent          │ N (cada uso) │ Utilidades     │
 * └─────────────────────┴──────────────┴────────────────┘
 * 
 * Analogía: @ApplicationScoped es como el gerente del banco:
 * - Solo hay UNO para toda la sucursal
 * - Todos los cajeros (Resources) lo consultan
 * - Está disponible todo el horario de apertura
 * - Al cerrar el banco, se retira
 */
@ApplicationScoped
public class CuentaService {
    
    // ============================================
    // ALMACENAMIENTO EN MEMORIA (Temporal)
    // ============================================
    
    /**
     * Almacén temporal de cuentas en memoria.
     * 
     * Map<String, Cuenta>:
     * - Key: número de cuenta (identificador único)
     * - Value: objeto Cuenta completo
     * 
     * ConcurrentHashMap vs HashMap:
     * ┌────────────────────┬──────────────┬───────────────┐
     * │ Característica     │ HashMap      │ ConcurrentH.  │
     * ├────────────────────┼──────────────┼───────────────┤
     * │ Thread-safe        │ ❌ NO        │ ✅ SÍ         │
     * │ Performance        │ Más rápido   │ Ligeramente - │
     * │ Bloqueo completo   │ N/A          │ ❌ NO         │
     * │ Null keys/values   │ ✅ Permite   │ ❌ NO permite │
     * └────────────────────┴──────────────┴───────────────┘
     * 
     * ¿Por qué ConcurrentHashMap?
     * - Service es @ApplicationScoped (singleton)
     * - Múltiples threads (requests HTTP) acceden simultáneamente
     * - Sin sincronización → condiciones de carrera (race conditions)
     * - ConcurrentHashMap garantiza operaciones atómicas
     * 
     * IMPORTANTE:
     * Este Map es TEMPORAL, solo para desarrollo/demos.
     * En producción se reemplaza por base de datos:
     * - Capítulo 4: Hibernate ORM + PostgreSQL
     * - Este Map desaparece → EntityManager + JPA Repository
     * 
     * Analogía: Como un archivero temporal en el escritorio del gerente.
     * Funciona para el día a día, pero lo ideal es una bóveda (DB).
     * 
     * Ejemplo de uso:
     * cuentas.put("123", cuenta);     // Guardar
     * Cuenta c = cuentas.get("123");  // Obtener
     * cuentas.remove("123");          // Eliminar
     */
    private final Map<String, Cuenta> cuentas = new ConcurrentHashMap<>();
    
    // ============================================
    // CONSTRUCTOR CON DATOS DE PRUEBA
    // ============================================
    
    /**
     * Constructor que inicializa datos de ejemplo.
     * 
     * CDI invoca este constructor automáticamente cuando:
     * 1. Primera inyección de CuentaService
     * 2. Creación del ApplicationContext
     * 
     * DATOS DE EJEMPLO (Seed Data):
     * - 3 cuentas pre-cargadas
     * - Permite testing inmediato sin DB
     * - En producción: esto se carga desde DB o archivos
     * 
     * PATRÓN CONSTRUCTOR:
     * - Sin parámetros (requerido por CDI)
     * - Inicialización de estado
     * - Equivalente a @PostConstruct (alternativa)
     * 
     * Alternativa con @PostConstruct:
     * @PostConstruct
     * void init() {
     *     // Inicialización después de construcción
     *     // Útil para operaciones más complejas
     * }
     * 
     * BigDecimal("5000.00"):
     * - String constructor para precisión exacta
     * - new BigDecimal(5000.00) → puede perder precisión
     * - "5000.00" → garantiza exactamente 5000.00
     */
    public CuentaService() {
        // Seed data: 3 cuentas de ejemplo
        // En producción: cargar desde DB, archivo, o servicio externo
        
        cuentas.put("1000000001", new Cuenta(
            "1000000001",           // Número cuenta
            "Juan Pérez",           // Titular
            new BigDecimal("5000.00"),  // Saldo (BigDecimal para precisión)
            "AHORRO"                // Tipo
        ));
        
        cuentas.put("1000000002", new Cuenta(
            "1000000002",
            "María López",
            new BigDecimal("12000.50"),
            "CORRIENTE"
        ));
        
        cuentas.put("1000000003", new Cuenta(
            "1000000003",
            "Carlos Ruiz",
            new BigDecimal("800.00"),
            "AHORRO"
        ));
        
        // Al terminar este constructor:
        // - Map tiene 3 entradas
        // - Service está listo para uso
        // - CDI puede inyectarlo en Resources
    }
    
    // ============================================
    // MÉTODOS DE NEGOCIO - CRUD
    // ============================================
    
    /**
     * Lista todas las cuentas existentes.
     * 
     * RETORNO:
     * - Nueva lista (ArrayList) con copia de valores
     * - NO retorna el Map directamente (encapsulación)
     * - Cliente puede modificar lista sin afectar Map interno
     * 
     * ¿Por qué new ArrayList<>(cuentas.values())?
     * 
     * OPCIÓN 1 (❌ MALA):
     * return cuentas.values();
     * Problema: Retorna Collection mutable vinculada al Map
     * Cliente podría modificarla indirectamente
     * 
     * OPCIÓN 2 (✅ BUENA - usada aquí):
     * return new ArrayList<>(cuentas.values());
     * Ventajas:
     * - Copia defensiva (defensive copy)
     * - Aislamiento del estado interno
     * - Cliente puede modificar lista sin afectar Map
     * 
     * OPCIÓN 3 (✅ MEJOR para inmutabilidad):
     * return List.copyOf(cuentas.values());
     * - Lista inmutable (Java 10+)
     * - Cliente NO puede modificarla
     * 
     * COMPLEJIDAD:
     * - Tiempo: O(n) - copia todos los elementos
     * - Espacio: O(n) - nueva lista
     * 
     * Con 1000 cuentas: overhead mínimo (~1ms)
     * Con 1M cuentas: considerar paginación
     * 
     * Analogía: Como entregar fotocopia de lista de clientes,
     * no el registro maestro original.
     * 
     * @return Lista con todas las cuentas (puede estar vacía, nunca null)
     */
    public List<Cuenta> listarTodas() {
        return new ArrayList<>(cuentas.values());
    }
    
    /**
     * Obtiene una cuenta específica por su número.
     * 
     * BÚSQUEDA:
     * - Map.get(key) → O(1) promedio (hash lookup)
     * - Muy eficiente, constante
     * 
     * RETORNO:
     * - Cuenta si existe
     * - null si no existe
     * 
     * NULL vs OPTIONAL:
     * Actual: return null (simple, tradicional)
     * 
     * Alternativa moderna:
     * public Optional<Cuenta> obtenerPorNumero(String numero) {
     *     return Optional.ofNullable(cuentas.get(numero));
     * }
     * 
     * Ventajas Optional:
     * - API explícita: "puede no existir"
     * - Evita NullPointerException
     * - Operaciones funcionales (.map, .filter, .orElse)
     * 
     * Uso con Optional:
     * service.obtenerPorNumero("123")
     *     .map(cuenta -> cuenta.getSaldo())
     *     .orElse(BigDecimal.ZERO);
     * 
     * VALIDACIÓN:
     * Actualmente no valida parámetro numero.
     * En producción:
     * if (numero == null || numero.isBlank()) {
     *     throw new IllegalArgumentException("Número inválido");
     * }
     * 
     * @param numero Identificador único de la cuenta
     * @return Cuenta si existe, null si no existe
     */
    public Cuenta obtenerPorNumero(String numero) {
        return cuentas.get(numero);
        
        // Map.get() internamente:
        // 1. Calcula hash del numero: int hash = numero.hashCode()
        // 2. Encuentra bucket: int index = hash & (table.length - 1)
        // 3. Busca en bucket (LinkedList o TreeNode)
        // 4. Compara con equals(): numero.equals(key)
        // 5. Retorna valor o null
    }
    
    /**
     * Crea una nueva cuenta en el sistema.
     * 
     * OPERACIÓN:
     * - Inserta en Map con número como clave
     * - Si ya existe: SOBREESCRIBE (⚠️ potencial bug)
     * - Retorna la misma instancia recibida
     * 
     * PROBLEMA ACTUAL:
     * No valida duplicados:
     * 
     * crear(new Cuenta("123", ...));  // OK
     * crear(new Cuenta("123", ...));  // ⚠️ Sobreescribe sin error
     * 
     * MEJORA RECOMENDADA:
     * public Cuenta crear(Cuenta cuenta) {
     *     if (cuentas.containsKey(cuenta.getNumero())) {
     *         throw new CuentaDuplicadaException(
     *             "Cuenta " + cuenta.getNumero() + " ya existe"
     *         );
     *     }
     *     cuentas.put(cuenta.getNumero(), cuenta);
     *     return cuenta;
     * }
     * 
     * VALIDACIONES FALTANTES (para producción):
     * - cuenta != null
     * - cuenta.getNumero() != null
     * - cuenta.getTitular() != null
     * - cuenta.getSaldo() >= 0
     * - tipoCuenta válido (AHORRO | CORRIENTE)
     * 
     * ATOMICIDAD:
     * Map.put() es atómico en ConcurrentHashMap
     * No necesita synchronized
     * 
     * EN PRODUCCIÓN (con DB):
     * @Transactional
     * public Cuenta crear(Cuenta cuenta) {
     *     // Validaciones...
     *     return repository.persist(cuenta);
     * }
     * 
     * @param cuenta Objeto con datos de la nueva cuenta
     * @return La cuenta creada (misma instancia)
     */
    public Cuenta crear(Cuenta cuenta) {
        // Futuro: validar duplicado
        // if (cuentas.containsKey(cuenta.getNumero())) {
        //     throw new DuplicateKeyException("Cuenta ya existe");
        // }
        
        cuentas.put(cuenta.getNumero(), cuenta);
        return cuenta;
        
        // Map.put() internamente (ConcurrentHashMap):
        // 1. Lock solo en el segmento afectado (no todo el Map)
        // 2. Calcula hash y ubica bucket
        // 3. Inserta o reemplaza
        // 4. Retorna valor previo (ignorado aquí)
    }
    
    /**
     * Actualiza una cuenta existente.
     * 
     * LÓGICA:
     * 1. Verifica si existe: containsKey(numero)
     * 2. Si existe:
     *    - Asegura que número no cambie (setNumero)
     *    - Reemplaza en Map
     *    - Retorna cuenta actualizada
     * 3. Si NO existe:
     *    - Retorna null
     * 
     * INMUTABILIDAD DE CLAVE:
     * cuentaActualizada.setNumero(numero);
     * 
     * Garantiza que la clave del Map no cambie.
     * Incluso si el cliente envió otro número en el body,
     * se fuerza el de la URL.
     * 
     * Ejemplo:
     * PUT /cuentas/123
     * Body: {"numero":"999", ...}
     * 
     * Resultado: numero se cambia a "123" (de la URL)
     * Map mantiene consistencia
     * 
     * RETORNO NULL:
     * Indica que la cuenta no existe.
     * Resource lo convierte en HTTP 404.
     * 
     * Alternativa con excepción:
     * if (!cuentas.containsKey(numero)) {
     *     throw new CuentaNoEncontradaException(numero);
     * }
     * 
     * ACTUALIZACIÓN PARCIAL vs COMPLETA:
     * Esto es PUT (reemplazo completo).
     * Para PATCH (parcial):
     * public Cuenta actualizarCampos(String numero, Map<String, Object> cambios) {
     *     Cuenta cuenta = obtenerPorNumero(numero);
     *     if (cuenta == null) return null;
     *     
     *     if (cambios.containsKey("saldo")) {
     *         cuenta.setSaldo(new BigDecimal(cambios.get("saldo").toString()));
     *     }
     *     if (cambios.containsKey("titular")) {
     *         cuenta.setTitular(cambios.get("titular").toString());
     *     }
     *     return cuenta;
     * }
     * 
     * @param numero Identificador de la cuenta a actualizar
     * @param cuentaActualizada Nuevos datos de la cuenta
     * @return Cuenta actualizada si existe, null si no existe
     */
    public Cuenta actualizar(String numero, Cuenta cuentaActualizada) {
        // Verifica existencia
        if (cuentas.containsKey(numero)) {
            // Asegura que número no cambie (inmutabilidad de clave)
            cuentaActualizada.setNumero(numero);
            
            // Reemplaza en Map
            cuentas.put(numero, cuentaActualizada);
            
            // Retorna cuenta actualizada
            return cuentaActualizada;
        }
        
        // No existe: retorna null
        return null;
    }
    
    /**
     * Elimina una cuenta del sistema.
     * 
     * OPERACIÓN:
     * - Map.remove(key) intenta eliminar
     * - Retorna valor eliminado o null si no existía
     * 
     * LÓGICA DE RETORNO:
     * return cuentas.remove(numero) != null;
     * 
     * Desglose:
     * 1. cuentas.remove(numero) → retorna Cuenta o null
     * 2. != null → true si eliminó, false si no existía
     * 3. Expresión elegante en una línea
     * 
     * Alternativa verbosa:
     * Cuenta eliminada = cuentas.remove(numero);
     * if (eliminada != null) {
     *     return true;
     * }
     * return false;
     * 
     * IDEMPOTENCIA:
     * 1er llamado: elimina, retorna true
     * 2do llamado: ya no existe, retorna false
     * 
     * Pero ambos dejan el sistema en el mismo estado:
     * cuenta no existe ✅
     * 
     * VALIDACIONES FALTANTES (para producción):
     * - Verificar permisos de usuario
     * - Validar saldo cero antes de eliminar
     * - Archivar cuenta en vez de eliminar (soft delete)
     * 
     * SOFT DELETE (alternativa):
     * En producción, rara vez se elimina físicamente:
     * cuenta.setActiva(false);
     * cuenta.setFechaEliminacion(LocalDateTime.now());
     * 
     * Permite auditoría y recuperación.
     * 
     * @param numero Identificador de la cuenta a eliminar
     * @return true si eliminó, false si no existía
     */
    public boolean eliminar(String numero) {
        // Intenta eliminar y verifica si había algo
        return cuentas.remove(numero) != null;
        
        // Map.remove() internamente (ConcurrentHashMap):
        // 1. Lock en segmento (no todo el Map)
        // 2. Busca clave por hash
        // 3. Si existe: elimina y retorna valor
        // 4. Si no existe: retorna null
        // 5. Operación atómica (thread-safe)
    }
}

/**
 * ═══════════════════════════════════════════════════════════════
 * CONCEPTOS CLAVE APLICADOS
 * ═══════════════════════════════════════════════════════════════
 * 
 * 1. SINGLETON con CDI (@ApplicationScoped)
 *    - Una instancia compartida
 *    - Inyectada automáticamente
 *    - Lifecycle gestionado por contenedor
 * 
 * 2. THREAD-SAFETY (ConcurrentHashMap)
 *    - Múltiples threads concurrentes
 *    - Sin sincronización manual
 *    - Operaciones atómicas garantizadas
 * 
 * 3. ENCAPSULACIÓN
 *    - Map privado
 *    - Acceso solo por métodos públicos
 *    - Defensive copy en listar()
 * 
 * 4. SEPARACIÓN DE RESPONSABILIDADES
 *    - Service: lógica de negocio
 *    - NO conoce HTTP/REST
 *    - Reutilizable desde cualquier capa
 * 
 * 5. INMUTABILIDAD DE CLAVES
 *    - Número de cuenta no cambia en actualizar()
 *    - Consistencia del Map garantizada
 * 
 * ═══════════════════════════════════════════════════════════════
 * COMPARACIÓN: MEMORIA vs BASE DE DATOS
 * ═══════════════════════════════════════════════════════════════
 * 
 * ACTUAL (Memoria - Map):
 * ┌─────────────────────┬────────────────────┐
 * │ Ventajas            │ Desventajas        │
 * ├─────────────────────┼────────────────────┤
 * │ ✅ Simple           │ ❌ No persistente  │
 * │ ✅ Rápido (O(1))    │ ❌ Límite memoria  │
 * │ ✅ Sin setup        │ ❌ Un solo server  │
 * │ ✅ Testing fácil    │ ❌ Sin queries     │
 * └─────────────────────┴────────────────────┘
 * 
 * FUTURO (Base de Datos - JPA):
 * ┌─────────────────────┬────────────────────┐
 * │ Ventajas            │ Desventajas        │
 * ├─────────────────────┼────────────────────┤
 * │ ✅ Persistente      │ ❌ Más complejo    │
 * │ ✅ Escalable        │ ❌ I/O más lento   │
 * │ ✅ ACID             │ ❌ Setup requerido │
 * │ ✅ Queries SQL      │ ❌ Dependencia ext │
 * └─────────────────────┴────────────────────┘
 * 
 * ═══════════════════════════════════════════════════════════════
 * EVOLUCIÓN FUTURA (Con Persistencia)
 * ═══════════════════════════════════════════════════════════════
 * 
 * Capítulo 4 - Hibernate ORM + Panache:
 * 
 * @ApplicationScoped
 * public class CuentaService {
 *     
 *     @Inject
 *     CuentaRepository repository;  // JPA Repository
 *     
 *     @Transactional
 *     public Cuenta crear(Cuenta cuenta) {
 *         validarDuplicado(cuenta.getNumero());
 *         return repository.persist(cuenta);
 *     }
 *     
 *     public List<Cuenta> listarTodas() {
 *         return repository.listAll();  // SELECT * FROM cuenta
 *     }
 *     
 *     public Cuenta obtenerPorNumero(String numero) {
 *         return repository.findById(numero);
 *     }
 * }
 * 
 * Cambios:
 * - Map → EntityManager/Repository
 * - ConcurrentHashMap → Database (PostgreSQL)
 * - Constructor → @PostConstruct o Flyway migrations
 * - Validaciones → Bean Validation (@NotNull, @Size)
 * - CRUD simple → Transacciones ACID
 * 
 * ═══════════════════════════════════════════════════════════════
 * BUENAS PRÁCTICAS APLICADAS
 * ═══════════════════════════════════════════════════════════════
 * 
 * ✅ Service como Singleton (@ApplicationScoped)
 * ✅ Thread-safe con ConcurrentHashMap
 * ✅ Defensive copy en listar()
 * ✅ Inmutabilidad de claves en actualizar()
 * ✅ Separación de capas (no conoce HTTP)
 * ✅ BigDecimal para valores monetarios
 * ✅ Seed data en constructor
 * ✅ API simple y consistente
 * 
 * ═══════════════════════════════════════════════════════════════
 * MEJORAS RECOMENDADAS (Ejercicio)
 * ═══════════════════════════════════════════════════════════════
 * 
 * 1. Validación de duplicados en crear()
 * 2. Usar Optional<Cuenta> en obtenerPorNumero()
 * 3. Validaciones de negocio (saldo >= 0)
 * 4. Excepción personalizada (CuentaNoEncontradaException)
 * 5. Logging de operaciones
 * 6. Métodos de búsqueda:
 *    - findByTitular(String titular)
 *    - findByTipo(String tipo)
 *    - findBySaldoMayorA(BigDecimal monto)
 * 7. Operaciones bancarias:
 *    - depositar(String numero, BigDecimal monto)
 *    - retirar(String numero, BigDecimal monto)
 *    - transferir(String origen, String destino, BigDecimal monto)
 * 
 * ═══════════════════════════════════════════════════════════════
 */