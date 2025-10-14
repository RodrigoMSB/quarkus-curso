package pe.banco.prestamos.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import pe.banco.prestamos.model.Cliente;

import java.util.Optional;

/**
 * Repositorio para acceso a datos de clientes.
 * 
 * PATRÓN: Repository Pattern
 * 
 * Responsabilidades:
 * - Encapsular lógica de acceso a datos
 * - Abstraer queries de la capa de negocio
 * - Proveer métodos de búsqueda específicos del dominio
 * - Separar persistencia de lógica de negocio
 * 
 * REPOSITORY PATTERN vs ACTIVE RECORD:
 * 
 * ┌─────────────────────────────────────────────────────┐
 * │ ACTIVE RECORD (Cliente extends PanacheEntity)      │
 * │                                                     │
 * │ Cliente.findById(1L)         ← Métodos estáticos   │
 * │ Cliente.listAll()                                   │
 * │ cliente.persist()            ← Entidad se persiste │
 * │                                                     │
 * │ ✅ Simple, conciso                                  │
 * │ ❌ Entidad sabe de persistencia (rompe SRP)        │
 * │ ❌ Difícil de testear (mockear statics)            │
 * └─────────────────────────────────────────────────────┘
 * 
 * ┌─────────────────────────────────────────────────────┐
 * │ REPOSITORY PATTERN (esta clase)                     │
 * │                                                     │
 * │ @Inject ClienteRepository repo;                     │
 * │ repo.findById(1L)            ← Métodos de instancia│
 * │ repo.listAll()                                      │
 * │ repo.persist(cliente)        ← Repository persiste │
 * │                                                     │
 * │ ✅ Separación de responsabilidades (SRP)           │
 * │ ✅ Fácil de testear (inyectar mock)                │
 * │ ✅ Queries organizadas                             │
 * │ ⚠️ Más código (clase adicional)                    │
 * └─────────────────────────────────────────────────────┘
 * 
 * ¿POR QUÉ USAMOS REPOSITORY AQUÍ?
 * 
 * 1. SEPARACIÓN DE RESPONSABILIDADES:
 *    - Cliente = solo datos
 *    - ClienteRepository = acceso a datos
 * 
 * 2. TESTING:
 *    @InjectMock
 *    ClienteRepository mockRepo;
 *    // Fácil mockear en tests
 * 
 * 3. QUERIES COMPLEJAS:
 *    - findByDni(), existsByEmail() centralizados
 *    - Fácil agregar nuevas búsquedas
 * 
 * 4. DOMAIN-DRIVEN DESIGN (DDD):
 *    - Repository es un concepto de DDD
 *    - Abstrae infraestructura (BD)
 * 
 * Analogía: Como un bibliotecario que sabe exactamente
 * dónde encontrar cada libro. No necesitas saber cómo
 * están organizados los estantes, solo pides al bibliotecario.
 */

// ============================================
// ANOTACIONES DE CDI
// ============================================

/**
 * @ApplicationScoped
 * Define el scope del bean CDI como APPLICATION.
 * 
 * SINGLETON PATTERN automático:
 * - UNA SOLA instancia en toda la aplicación
 * - Creada LAZY (al primer uso)
 * - Compartida por todos los que la inyecten
 * - Destruida al cerrar aplicación
 * 
 * SCOPES DISPONIBLES:
 * 
 * @ApplicationScoped (usado aquí):
 * - 1 instancia por aplicación
 * - Vive durante toda la ejecución
 * - Ideal para repositories, services, DAOs
 * - DEBE ser thread-safe (stateless)
 * 
 * @RequestScoped:
 * - 1 instancia por request HTTP
 * - Vive solo durante el request
 * - Ideal para datos específicos del request
 * 
 * @Dependent (default):
 * - Nueva instancia cada inyección
 * - No es proxy
 * - Útil para objetos livianos
 * 
 * @Singleton (Quarkus específico):
 * - Similar a ApplicationScoped
 * - Inicialización EAGER (al arrancar)
 * 
 * THREAD-SAFETY:
 * Como es ApplicationScoped (singleton):
 * - Múltiples threads acceden simultáneamente
 * - NO tener estado mutable (campos variables)
 * - Solo métodos (stateless) ✅
 * 
 * Esta clase ES thread-safe porque:
 * - No tiene campos mutables
 * - Solo métodos que usan PanacheRepository
 * - PanacheRepository es thread-safe
 * 
 * INYECCIÓN:
 * @Inject
 * ClienteRepository clienteRepository;
 * 
 * CDI automáticamente:
 * 1. Detecta @Inject
 * 2. Busca bean compatible (@ApplicationScoped ClienteRepository)
 * 3. Crea instancia (si no existe)
 * 4. Inyecta referencia
 * 
 * Ventajas vs new:
 * ✅ Reutiliza instancia (no crea cada vez)
 * ✅ Lazy creation (solo si se usa)
 * ✅ Mockeable en tests
 * ✅ Lifecycle gestionado
 * 
 * Ciclo de vida:
 * 1. Aplicación arranca
 * 2. CDI escanea clases
 * 3. Registra ClienteRepository como bean disponible
 * 4. Primer @Inject → crea instancia
 * 5. Siguientes @Inject → reutilizan misma instancia
 * 6. Aplicación cierra → destruye instancia
 */
@ApplicationScoped

/**
 * implements PanacheRepository<Cliente>
 * 
 * PANACHE REPOSITORY PATTERN
 * 
 * PanacheRepository<T> es una interfaz genérica que provee:
 * 
 * MÉTODOS CRUD BÁSICOS (heredados automáticamente):
 * 
 * // CREATE
 * persist(cliente)              // Guardar uno
 * persist(cliente1, cliente2)   // Guardar varios
 * persist(Stream<Cliente>)      // Guardar stream
 * 
 * // READ
 * findById(1L)                  // Buscar por ID
 * findByIdOptional(1L)          // Optional<Cliente>
 * listAll()                     // Listar todos
 * find("nombre", "Juan")        // Buscar por campo
 * findAll()                     // PanacheQuery para paginación
 * streamAll()                   // Stream<Cliente>
 * count()                       // Contar todos
 * 
 * // UPDATE
 * update("nombre = ?1 WHERE id = ?2", "Juan", 1L)
 * 
 * // DELETE
 * delete("id", 1L)              // Eliminar por campo
 * deleteById(1L)                // Eliminar por ID
 * deleteAll()                   // Eliminar todos
 * 
 * QUERIES PERSONALIZADAS:
 * find(query, params)           // HQL/JPQL
 * find(query, Sort)             // Con ordenamiento
 * find(query, Parameters)       // Named parameters
 * 
 * PAGINACIÓN:
 * findAll().page(0, 10)         // Página 0, 10 items
 * findAll().pageCount()         // Total de páginas
 * 
 * ¿POR QUÉ INTERFAZ Y NO CLASE ABSTRACTA?
 * - Panache usa bytecode enhancement
 * - Genera implementación en compile-time
 * - No necesitas escribir código boilerplate
 * 
 * COMPARACIÓN:
 * 
 * JPA Tradicional (50+ líneas):
 * @Stateless
 * public class ClienteDAO {
 *     @PersistenceContext
 *     EntityManager em;
 *     
 *     public Cliente findById(Long id) {
 *         return em.find(Cliente.class, id);
 *     }
 *     
 *     public List<Cliente> listAll() {
 *         return em.createQuery("SELECT c FROM Cliente c", Cliente.class)
 *                  .getResultList();
 *     }
 *     
 *     public void persist(Cliente c) {
 *         em.persist(c);
 *     }
 *     // ... más métodos
 * }
 * 
 * Panache Repository (3 líneas):
 * @ApplicationScoped
 * public class ClienteRepository implements PanacheRepository<Cliente> {
 *     // ¡Métodos heredados automáticamente! ✅
 * }
 * 
 * MÉTODOS CUSTOM:
 * Solo agregas los métodos específicos que necesites:
 * - findByDni()
 * - existsByEmail()
 * - etc.
 * 
 * Los métodos CRUD ya vienen incluidos.
 */
public class ClienteRepository implements PanacheRepository<Cliente> {
    
    // ============================================
    // MÉTODOS DE BÚSQUEDA PERSONALIZADOS
    // ============================================
    
    /**
     * Busca un cliente por su DNI.
     * 
     * MÉTODO CUSTOM del dominio bancario.
     * DNI es identificador único de personas en Perú.
     * 
     * RETORNO: Optional<Cliente>
     * 
     * ¿Por qué Optional?
     * - Manejo explícito de ausencia (no hay cliente)
     * - Evita NullPointerException
     * - API funcional moderna (Java 8+)
     * 
     * Optional vs null:
     * 
     * CON NULL (tradicional):
     * Cliente c = repo.findByDniNullable("12345678");
     * if (c != null) {
     *     // usar c
     * } else {
     *     // manejar ausencia
     * }
     * 
     * CON OPTIONAL (moderno):
     * Optional<Cliente> opt = repo.findByDni("12345678");
     * opt.ifPresent(c -> System.out.println(c.nombre));
     * 
     * Cliente c = opt.orElse(clienteDefault);
     * Cliente c = opt.orElseThrow(() -> new NotFoundException());
     * 
     * IMPLEMENTACIÓN:
     * 
     * find("dni", dni)
     * - Método heredado de PanacheRepository
     * - Genera query: SELECT c FROM Cliente c WHERE c.dni = ?1
     * - Parámetro posicional: dni
     * 
     * Alternativas de sintaxis:
     * find("dni = ?1", dni)              // Explícito
     * find("dni = :dni", Parameters.with("dni", dni))  // Named
     * 
     * .firstResultOptional()
     * - Retorna Optional<Cliente>
     * - Optional.empty() si no encuentra
     * - Optional.of(cliente) si encuentra
     * 
     * SQL generado por Hibernate:
     * SELECT c.id, c.nombre, c.dni, c.email, c.telefono
     * FROM clientes c
     * WHERE c.dni = ?
     * 
     * PERFORMANCE:
     * - Índice UNIQUE en columna dni (creado por JPA)
     * - Búsqueda O(1) por índice
     * - Muy rápido
     * 
     * USO EN SERVICIO:
     * Optional<Cliente> opt = clienteRepository.findByDni("12345678");
     * 
     * if (opt.isPresent()) {
     *     Cliente cliente = opt.get();
     *     // procesar
     * } else {
     *     // DNI no encontrado
     * }
     * 
     * O con programación funcional:
     * clienteRepository.findByDni("12345678")
     *     .map(c -> c.nombre)
     *     .orElse("Desconocido");
     * 
     * VALIDACIÓN ADICIONAL (futura):
     * if (!dni.matches("\\d{8}")) {
     *     throw new IllegalArgumentException("DNI inválido");
     * }
     * 
     * @param dni Documento Nacional de Identidad (8 dígitos)
     * @return Optional con cliente si existe, empty si no
     */
    public Optional<Cliente> findByDni(String dni) {
        return find("dni", dni).firstResultOptional();
    }
    
    /**
     * Busca un cliente por su email.
     * 
     * EMAIL como identificador alternativo.
     * Útil para login, notificaciones, recuperación de cuenta.
     * 
     * RETORNO: Optional<Cliente>
     * Mismo patrón que findByDni() para consistencia.
     * 
     * IMPLEMENTACIÓN:
     * 
     * find("email", email)
     * - Query: SELECT c FROM Cliente c WHERE c.email = ?1
     * - Email es UNIQUE (constraint en BD)
     * - Retorna máximo 1 resultado
     * 
     * .firstResultOptional()
     * - Si encuentra: Optional.of(cliente)
     * - Si no encuentra: Optional.empty()
     * 
     * SQL generado:
     * SELECT c.id, c.nombre, c.dni, c.email, c.telefono
     * FROM clientes c
     * WHERE c.email = ?
     * 
     * CASE SENSITIVITY:
     * Email comparación es case-sensitive por defecto.
     * 
     * Para case-insensitive:
     * find("LOWER(email) = LOWER(?1)", email).firstResultOptional()
     * 
     * O normalizar antes:
     * find("email", email.toLowerCase())
     * 
     * PERFORMANCE:
     * - Índice UNIQUE en email
     * - Búsqueda rápida O(1)
     * 
     * VALIDACIÓN (recomendada):
     * if (!email.contains("@")) {
     *     throw new IllegalArgumentException("Email inválido");
     * }
     * 
     * USO TÍPICO:
     * // Verificar si email ya registrado
     * Optional<Cliente> existente = repo.findByEmail("juan@example.com");
     * if (existente.isPresent()) {
     *     throw new EmailDuplicadoException();
     * }
     * 
     * // Login por email
     * Cliente cliente = repo.findByEmail(loginRequest.email)
     *     .orElseThrow(() -> new CredencialesInvalidasException());
     * 
     * SEGURIDAD:
     * En producción, email debería estar cifrado:
     * - Almacenar hash o cifrado
     * - Buscar por hash del email
     * - Prevenir filtración de datos
     * 
     * @param email Correo electrónico único del cliente
     * @return Optional con cliente si existe, empty si no
     */
    public Optional<Cliente> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }
    
    /**
     * Verifica si existe un cliente con el DNI dado.
     * 
     * MÉTODO DE VALIDACIÓN
     * Usado ANTES de crear cliente para evitar duplicados.
     * 
     * RETORNO: boolean
     * - true: DNI ya registrado
     * - false: DNI disponible
     * 
     * ¿Por qué no Optional?
     * - Solo nos interesa existencia (sí/no)
     * - No necesitamos el objeto Cliente
     * - Más eficiente que findByDni()
     * 
     * IMPLEMENTACIÓN:
     * 
     * count("dni", dni)
     * - Método heredado de PanacheRepository
     * - Genera: SELECT COUNT(c) FROM Cliente c WHERE c.dni = ?1
     * - Retorna long (cantidad de coincidencias)
     * 
     * > 0
     * - Convierte long a boolean
     * - count >= 1 → true (existe)
     * - count == 0 → false (no existe)
     * 
     * SQL generado:
     * SELECT COUNT(c.id)
     * FROM clientes c
     * WHERE c.dni = ?
     * 
     * PERFORMANCE:
     * - COUNT es más rápido que SELECT *
     * - Solo cuenta, no trae datos
     * - Usa índice UNIQUE en dni
     * - Óptimo para validaciones
     * 
     * ALTERNATIVAS:
     * 
     * Opción 1 (actual - más eficiente):
     * return count("dni", dni) > 0;
     * 
     * Opción 2 (menos eficiente):
     * return findByDni(dni).isPresent();
     * // Trae objeto completo solo para verificar existencia
     * 
     * Opción 3 (JPA puro):
     * return em.createQuery("SELECT COUNT(c) FROM Cliente c WHERE c.dni = ?1", Long.class)
     *          .setParameter(1, dni)
     *          .getSingleResult() > 0;
     * 
     * USO EN RESOURCE:
     * @POST
     * @Transactional
     * public Response crear(Cliente cliente) {
     *     // Validar DNI duplicado
     *     if (clienteRepository.existsByDni(cliente.dni)) {
     *         return Response.status(409)
     *             .entity("DNI ya registrado")
     *             .build();
     *     }
     *     
     *     clienteRepository.persist(cliente);
     *     return Response.status(201).entity(cliente).build();
     * }
     * 
     * VENTAJAS de validar ANTES de persist():
     * - Evita intentar INSERT duplicado
     * - Más rápido que catch exception
     * - Control explícito del error
     * - Mejor UX (mensaje claro al usuario)
     * 
     * Sin validación previa:
     * try {
     *     repo.persist(cliente);  // Falla si DNI duplicado
     * } catch (PersistenceException e) {
     *     // Manejar error de constraint
     * }
     * 
     * Con validación (mejor):
     * if (repo.existsByDni(dni)) {
     *     return 409 Conflict;
     * }
     * repo.persist(cliente);  // Safe
     * 
     * @param dni Documento Nacional de Identidad a verificar
     * @return true si DNI ya existe, false si está disponible
     */
    public boolean existsByDni(String dni) {
        return count("dni", dni) > 0;
    }
    
    /**
     * Verifica si existe un cliente con el email dado.
     * 
     * MÉTODO DE VALIDACIÓN
     * Mismo patrón que existsByDni() para consistencia.
     * 
     * RETORNO: boolean
     * - true: email ya registrado
     * - false: email disponible
     * 
     * IMPLEMENTACIÓN:
     * count("email", email) > 0
     * - SELECT COUNT(c) FROM Cliente c WHERE c.email = ?1
     * - Retorna true si count >= 1
     * 
     * SQL generado:
     * SELECT COUNT(c.id)
     * FROM clientes c
     * WHERE c.email = ?
     * 
     * PERFORMANCE:
     * - COUNT sobre índice UNIQUE
     * - No trae datos, solo cuenta
     * - Muy eficiente
     * 
     * CASE SENSITIVITY:
     * Para case-insensitive:
     * return count("LOWER(email) = LOWER(?1)", email) > 0;
     * 
     * USO EN VALIDACIÓN:
     * @POST
     * public Response crear(Cliente cliente) {
     *     if (repo.existsByDni(cliente.dni)) {
     *         return Response.status(409)
     *             .entity("DNI ya registrado")
     *             .build();
     *     }
     *     
     *     if (repo.existsByEmail(cliente.email)) {
     *         return Response.status(409)
     *             .entity("Email ya registrado")
     *             .build();
     *     }
     *     
     *     repo.persist(cliente);
     *     return Response.status(201).entity(cliente).build();
     * }
     * 
     * MEJORA FUTURA:
     * Validar ambos en una sola query:
     * return count("dni = ?1 OR email = ?2", dni, email) > 0;
     * 
     * O retornar qué campo está duplicado:
     * public String validarDuplicados(String dni, String email) {
     *     if (existsByDni(dni)) return "DNI duplicado";
     *     if (existsByEmail(email)) return "Email duplicado";
     *     return null;  // OK
     * }
     * 
     * @param email Correo electrónico a verificar
     * @return true si email ya existe, false si está disponible
     */
    public boolean existsByEmail(String email) {
        return count("email", email) > 0;
    }
    
    // ============================================
    // MÉTODOS ADICIONALES (Ejemplos comentados)
    // ============================================
    
    /**
     * Ejemplo: Buscar clientes por nombre (like)
     */
    /*
    public List<Cliente> findByNombreLike(String nombre) {
        return find("LOWER(nombre) LIKE LOWER(?1)", "%" + nombre + "%").list();
    }
    */
    
    /**
     * Ejemplo: Listar clientes con préstamos activos
     */
    /*
    public List<Cliente> findConPrestamosActivos() {
        return find(
            "SELECT DISTINCT c FROM Cliente c " +
            "JOIN c.prestamos p " +
            "WHERE p.estado = ?1",
            Prestamo.EstadoPrestamo.ACTIVO
        ).list();
    }
    */
    
    /**
     * Ejemplo: Contar clientes por rango de edad
     * (requiere campo edad en Cliente)
     */
    /*
    public long countByEdadRange(int min, int max) {
        return count("edad BETWEEN ?1 AND ?2", min, max);
    }
    */
    
    /**
     * Ejemplo: Paginación
     */
    /*
    public List<Cliente> findAllPaginated(int page, int size) {
        return findAll()
            .page(page, size)
            .list();
    }
    */
    
    /**
     * Ejemplo: Ordenamiento
     */
    /*
    public List<Cliente> findAllOrdered() {
        return listAll(Sort.by("nombre").ascending());
    }
    */
}

/**
 * ═══════════════════════════════════════════════════════════════
 * COMPARACIÓN: ACTIVE RECORD vs REPOSITORY
 * ═══════════════════════════════════════════════════════════════
 * 
 * ACTIVE RECORD (Cliente extends PanacheEntity):
 * 
 * // Crear
 * Cliente cliente = new Cliente(...);
 * cliente.persist();  // Entidad se guarda sola
 * 
 * // Buscar
 * Cliente c = Cliente.findById(1L);
 * List<Cliente> todos = Cliente.listAll();
 * 
 * // Eliminar
 * cliente.delete();
 * 
 * Ventajas:
 * ✅ Conciso, menos código
 * ✅ Intuitivo para CRUD simple
 * ✅ No necesita Repository
 * 
 * Desventajas:
 * ❌ Entidad conoce persistencia (rompe SRP)
 * ❌ Difícil mockear métodos estáticos en tests
 * ❌ Acoplamiento a Panache
 * 
 * ───────────────────────────────────────────────────────────────
 * 
 * REPOSITORY PATTERN (esta clase):
 * 
 * // Inyectar
 * @Inject
 * ClienteRepository repo;
 * 
 * // Crear
 * Cliente cliente = new Cliente(...);
 * repo.persist(cliente);  // Repository persiste
 * 
 * // Buscar
 * Cliente c = repo.findById(1L);
 * List<Cliente> todos = repo.listAll();
 * Optional<Cliente> opt = repo.findByDni("12345678");
 * 
 * // Eliminar
 * repo.delete(cliente);
 * 
 * Ventajas:
 * ✅ Separación de responsabilidades (SRP)
 * ✅ Fácil testear (inyectar mock)
 * ✅ Queries organizadas en un lugar
 * ✅ Patrón DDD
 * 
 * Desventajas:
 * ⚠️ Clase adicional por entidad
 * ⚠️ Ligeramente más verbose
 * 
 * ═══════════════════════════════════════════════════════════════
 * TESTING CON MOCKITO
 * ═══════════════════════════════════════════════════════════════
 * 
 * @QuarkusTest
 * public class ClienteResourceTest {
 *     
 *     @InjectMock
 *     ClienteRepository clienteRepository;
 *     
 *     @Test
 *     void testCrearClienteDniDuplicado() {
 *         // Mock: simular que DNI ya existe
 *         when(clienteRepository.existsByDni("12345678"))
 *             .thenReturn(true);
 *         
 *         Cliente cliente = new Cliente();
 *         cliente.dni = "12345678";
 *         
 *         // Llamar endpoint
 *         Response response = given()
 *             .contentType(MediaType.APPLICATION_JSON)
 *             .body(cliente)
 *             .post("/clientes")
 *             .then()
 *             .extract().response();
 *         
 *         // Verificar
 *         assertEquals(409, response.statusCode());
 *         assertTrue(response.body().asString().contains("DNI ya registrado"));
 *         
 *         // Verificar que se llamó el método
 *         verify(clienteRepository).existsByDni("12345678");
 *     }
 * }
 * 
 * Con Active Record, mockear Cliente.findById() es más complejo
 * (requiere PowerMock o similar).
 * 
 * ═══════════════════════════════════════════════════════════════
 * MÉTODOS HEREDADOS DE PANACHEREPOSITORY (Más usados)
 * ═══════════════════════════════════════════════════════════════
 * 
 * // PERSISTENCIA
 * persist(Cliente cliente)
 * persist(Cliente c1, Cliente c2, ...)
 * persist(Iterable<Cliente>)
 * persist(Stream<Cliente>)
 * 
 * // BÚSQUEDA
 * findById(Long id)                 → Cliente (puede ser null)
 * findByIdOptional(Long id)         → Optional<Cliente>
 * find(String query, Object... params) → PanacheQuery<Cliente>
 * find(String query, Sort sort, Object... params)
 * find(String query, Map<String, Object> params)
 * findAll()                         → PanacheQuery<Cliente>
 * listAll()                         → List<Cliente>
 * streamAll()                       → Stream<Cliente>
 * 
 * // CONTEO
 * count()                           → long (total)
 * count(String query, Object... params)
 * 
 * // ELIMINACIÓN
 * delete(String query, Object... params) → long (eliminados)
 * deleteById(Long id)               → boolean
 * deleteAll()                       → long
 * 
 * // ACTUALIZACIÓN
 * update(String query, Object... params) → int (actualizados)
 * 
 * // PAGINACIÓN
 * findAll().page(int pageIndex, int pageSize)
 * findAll().pageCount()
 * 
 * ═══════════════════════════════════════════════════════════════
 * BUENAS PRÁCTICAS
 * ═══════════════════════════════════════════════════════════════
 * 
 * ✅ Usar Optional para búsquedas que pueden no encontrar
 * ✅ Validar con exists() antes de persist() para evitar duplicados
 * ✅ Nombrar métodos según convención: findBy..., existsBy..., countBy...
 * ✅ No mezclar Active Record y Repository en misma entidad
 * ✅ Mantener Repository stateless (sin campos mutables)
 * ✅ Un Repository por entidad (ClienteRepository, PrestamoRepository)
 * ✅ Centralizar queries complejas en Repository
 * ✅ Inyectar Repository (no usar new)
 * 
 * ═══════════════════════════════════════════════════════════════
 */