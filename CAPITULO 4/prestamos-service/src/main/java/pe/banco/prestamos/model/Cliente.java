package pe.banco.prestamos.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Entidad JPA que representa un cliente bancario.
 * 
 * PATRÓN: Active Record (extiende PanacheEntity)
 * 
 * Esta clase combina:
 * - DATOS: Información del cliente (nombre, DNI, email, teléfono)
 * - PERSISTENCIA: Métodos heredados de PanacheEntity para CRUD
 * 
 * Relación con otras entidades:
 * - Un cliente puede tener MUCHOS préstamos (1:N)
 * 
 * Mapeo ORM:
 * ┌──────────────────────────────────────┐
 * │ Clase Java: Cliente                  │
 * │                                      │
 * │ - id: Long (heredado)                │
 * │ - nombre: String                     │
 * │ - dni: String (UNIQUE)               │
 * │ - email: String (UNIQUE)             │
 * │ - telefono: String                   │
 * │ - prestamos: List<Prestamo>          │
 * └──────────────────────────────────────┘
 *                  ↓
 * ┌──────────────────────────────────────┐
 * │ Tabla SQL: clientes                  │
 * │                                      │
 * │ - id BIGINT PRIMARY KEY              │
 * │ - nombre VARCHAR(255) NOT NULL       │
 * │ - dni VARCHAR(8) UNIQUE NOT NULL     │
 * │ - email VARCHAR(255) UNIQUE NOT NULL │
 * │ - telefono VARCHAR(255) NOT NULL     │
 * └──────────────────────────────────────┘
 * 
 * Analogía: Como una ficha de cliente en un banco tradicional,
 * pero con superpoderes para guardarse y buscarse a sí misma.
 */

// ============================================
// ANOTACIONES DE ENTIDAD JPA
// ============================================

/**
 * @Entity
 * Marca esta clase como una ENTIDAD JPA.
 * 
 * Le dice a Hibernate:
 * "Esta clase se mapea a una tabla de base de datos"
 * 
 * Hibernate hará:
 * 1. Escanear esta clase al arrancar
 * 2. Crear/validar tabla 'clientes' en BD
 * 3. Generar SQL automáticamente (INSERT, UPDATE, DELETE, SELECT)
 * 4. Mapear ResultSet ↔ Objeto Java
 * 
 * Sin @Entity, Hibernate ignora esta clase completamente.
 */
@Entity

/**
 * @Table(name = "clientes")
 * Especifica el nombre exacto de la tabla en la base de datos.
 * 
 * OPCIONAL: Si se omite, Hibernate usa el nombre de la clase.
 * - Con @Table: "clientes" (plural, snake_case)
 * - Sin @Table: "Cliente" (nombre clase tal cual)
 * 
 * Best Practice: Siempre especificar name explícitamente
 * - Tablas en plural: clientes, prestamos, cuotas
 * - Clases en singular: Cliente, Prestamo, Cuota
 * 
 * Opciones adicionales de @Table:
 * @Table(
 *     name = "clientes",
 *     schema = "finanzas",                    // Esquema de BD
 *     uniqueConstraints = {
 *         @UniqueConstraint(columnNames = {"dni", "sucursal"})
 *     },
 *     indexes = {
 *         @Index(name = "idx_dni", columnList = "dni")
 *     }
 * )
 */
@Table(name = "clientes")

/**
 * extends PanacheEntity
 * 
 * ACTIVE RECORD PATTERN - Panache simplifica JPA
 * 
 * PanacheEntity proporciona AUTOMÁTICAMENTE:
 * 
 * 1. ID AUTO-GENERADO:
 *    public Long id;  // No necesitas declararlo
 * 
 * 2. MÉTODOS DE PERSISTENCIA (estáticos):
 *    Cliente.persist(cliente);        // Guardar
 *    Cliente.findById(1L);            // Buscar por ID
 *    Cliente.listAll();               // Listar todos
 *    Cliente.find("dni", "12345678"); // Buscar por campo
 *    Cliente.count();                 // Contar registros
 *    Cliente.deleteById(1L);          // Eliminar
 * 
 * 3. MÉTODOS DE INSTANCIA:
 *    cliente.persist();               // Se guarda a sí mismo
 *    cliente.delete();                // Se elimina a sí mismo
 *    cliente.isPersistent();          // ¿Está en BD?
 * 
 * VENTAJAS vs JPA Tradicional:
 * ✅ No necesitas EntityManager
 * ✅ No necesitas crear Repository
 * ✅ Código más limpio y conciso
 * ✅ Menos boilerplate (50+ líneas menos)
 * 
 * COMPARACIÓN:
 * 
 * JPA Tradicional:
 * @PersistenceContext
 * EntityManager em;
 * 
 * public void guardar(Cliente c) {
 *     em.persist(c);
 * }
 * 
 * public Cliente buscar(Long id) {
 *     return em.find(Cliente.class, id);
 * }
 * 
 * Panache (Active Record):
 * Cliente c = new Cliente();
 * c.persist();  // ✅ Se guarda solo
 * 
 * Cliente encontrado = Cliente.findById(1L);  // ✅ Método estático
 * 
 * Analogía: PanacheEntity es como darle a tu entidad
 * un asistente personal que sabe cómo guardarse,
 * buscarse y eliminarse sin ayuda externa.
 */
public class Cliente extends PanacheEntity {
    
    // ============================================
    // CAMPOS DE LA ENTIDAD (DATOS DEL CLIENTE)
    // ============================================
    
    /**
     * Nombre completo del cliente.
     * 
     * @Column(nullable = false)
     * - nullable = false → NOT NULL en SQL
     * - Hibernate valida antes de INSERT
     * - Si es null, lanza ConstraintViolationException
     * 
     * Campo PÚBLICO (estilo Panache):
     * - Panache genera getters/setters en BYTECODE
     * - En tiempo de ejecución: cliente.nombre llama al getter generado
     * - Hibernate detecta y usa estos getters/setters
     * - NO hay diferencia de performance vs privado
     * 
     * Mapeo SQL:
     * nombre VARCHAR(255) NOT NULL
     * 
     * Por defecto:
     * - String → VARCHAR(255)
     * - Si quieres más: @Column(length = 500)
     */
    @Column(nullable = false)
    public String nombre;
    
    /**
     * Documento Nacional de Identidad (DNI).
     * 
     * @Column(nullable = false, unique = true, length = 8)
     * 
     * RESTRICCIONES:
     * - nullable = false → Obligatorio
     * - unique = true → UNIQUE constraint en BD
     * - length = 8 → VARCHAR(8) (DNI peruano)
     * 
     * Hibernate genera SQL:
     * CREATE TABLE clientes (
     *     ...
     *     dni VARCHAR(8) NOT NULL,
     *     CONSTRAINT uk_dni UNIQUE (dni)
     * );
     * 
     * VALIDACIÓN EN APLICACIÓN (ClienteResource):
     * - Antes de persist(), verificamos con Repository.existsByDni()
     * - Evita intentar INSERT duplicado (más rápido que catch exception)
     * 
     * Si intentas INSERT duplicado:
     * org.hibernate.exception.ConstraintViolationException:
     * could not execute statement... UNIQUE constraint
     * 
     * Best Practice:
     * - Validar duplicados en capa de servicio ANTES de persist()
     * - Retornar HTTP 409 Conflict al cliente
     * 
     * Analogía: Como el número de cédula - único por persona,
     * no puede haber dos clientes con el mismo DNI.
     */
    @Column(nullable = false, unique = true, length = 8)
    public String dni;
    
    /**
     * Correo electrónico del cliente.
     * 
     * @Column(nullable = false, unique = true)
     * - unique = true → No puede repetirse
     * - Usado para notificaciones, login (futuro)
     * 
     * Sin especificar length:
     * - Default: VARCHAR(255)
     * - Suficiente para emails normales
     * 
     * IMPORTANTE:
     * En producción, este campo debería estar CIFRADO
     * por ser dato personal sensible (GDPR, LGPD).
     * 
     * Cifrado (tema avanzado - Cap 4 extra):
     * @Convert(converter = EmailEncryptionConverter.class)
     * public String email;
     * 
     * Validación adicional recomendada:
     * @Email  // Bean Validation (Cap 5)
     * 
     * Mapeo SQL:
     * email VARCHAR(255) UNIQUE NOT NULL
     */
    @Column(nullable = false, unique = true)
    public String email;
    
    /**
     * Número de teléfono del cliente.
     * 
     * @Column(nullable = false)
     * - Obligatorio para contacto
     * - NO es unique (pueden compartir teléfono familiar)
     * 
     * Mapeo SQL:
     * telefono VARCHAR(255) NOT NULL
     * 
     * MEJORAS FUTURAS:
     * - Validar formato: @Pattern(regexp = "\\d{9}")
     * - Separar código país: +51 987654321
     * - Almacenar como String (no Integer) para preservar 0s
     *   Ejemplo: "012345678" vs 12345678 (pierde el 0)
     */
    @Column(nullable = false)
    public String telefono;
    
    // ============================================
    // RELACIONES JPA
    // ============================================
    
    /**
     * Lista de préstamos asociados a este cliente.
     * 
     * RELACIÓN: One-to-Many (1:N)
     * - Un cliente puede tener MUCHOS préstamos
     * - Un préstamo pertenece a UN solo cliente
     * 
     * @JsonIgnore
     * CRÍTICO para evitar dos problemas:
     * 
     * PROBLEMA 1: Referencias circulares infinitas
     * Cliente → Prestamos → Cliente → Prestamos → ...
     * Jackson entra en loop infinito al serializar JSON
     * 
     * PROBLEMA 2: LazyInitializationException
     * - prestamos es LAZY por defecto
     * - Al serializar JSON, sesión Hibernate ya cerró
     * - Intentar acceder → "no session" exception
     * 
     * @JsonIgnore dice:
     * "No incluyas 'prestamos' en JSON de Cliente"
     * 
     * JSON resultante:
     * {
     *   "id": 1,
     *   "nombre": "Juan",
     *   "dni": "12345678",
     *   "email": "juan@example.com",
     *   "telefono": "987654321"
     *   // prestamos NO aparece
     * }
     * 
     * Si necesitas ver préstamos:
     * GET /prestamos/cliente/{clienteId}
     * Endpoint específico que carga préstamos explícitamente
     * 
     * Alternativa (sin @JsonIgnore):
     * - @JsonManagedReference / @JsonBackReference
     * - DTOs separados (ClienteDTO sin prestamos)
     * - fetch = EAGER (carga siempre, más pesado)
     * 
     * @OneToMany(mappedBy = "cliente", ...)
     * 
     * LADO INVERSO de la relación bidireccional
     * 
     * - mappedBy = "cliente" → Campo en Prestamo que apunta aquí
     *   Prestamo.cliente es el DUEÑO (tiene FK)
     *   Cliente.prestamos es INVERSO (solo lectura de relación)
     * 
     * - cascade = CascadeType.ALL
     *   Propaga TODAS las operaciones a préstamos:
     * 
     *   cliente.persist();
     *   → Automáticamente persiste todos sus prestamos
     * 
     *   cliente.delete();
     *   → Automáticamente elimina todos sus prestamos
     * 
     *   Tipos de cascade:
     *   - PERSIST: Solo al guardar
     *   - MERGE: Solo al actualizar
     *   - REMOVE: Solo al eliminar
     *   - REFRESH: Recargar desde BD
     *   - ALL: Todas las anteriores
     * 
     * - orphanRemoval = true
     *   Elimina préstamos "huérfanos" (sin cliente)
     * 
     *   Ejemplo:
     *   cliente.prestamos.remove(prestamo);
     *   → Hibernate ejecuta DELETE del préstamo
     * 
     *   Sin orphanRemoval:
     *   → Solo pone prestamo.cliente_id = NULL (queda huérfano)
     * 
     * FETCH TYPE (por defecto):
     * @OneToMany default = LAZY (carga perezosa)
     * 
     * Carga perezosa significa:
     * Cliente c = Cliente.findById(1L);
     * // SQL: SELECT * FROM clientes WHERE id = 1
     * // NO carga prestamos
     * 
     * System.out.println(c.prestamos.size());
     * // AHORA ejecuta:
     * // SQL: SELECT * FROM prestamos WHERE cliente_id = 1
     * 
     * VENTAJA: No carga datos innecesarios
     * DESVENTAJA: Problema N+1
     * 
     * List<Cliente> clientes = Cliente.listAll();
     * for (Cliente c : clientes) {
     *     System.out.println(c.prestamos.size());
     *     // 1 query extra POR CADA cliente
     * }
     * 
     * Solución N+1:
     * List<Cliente> clientes = Cliente.find(
     *     "SELECT c FROM Cliente c JOIN FETCH c.prestamos"
     * ).list();
     * // 1 sola query con JOIN
     * 
     * Mapeo SQL:
     * No genera columna en tabla 'clientes'.
     * La relación vive en tabla 'prestamos':
     * 
     * CREATE TABLE prestamos (
     *     ...
     *     cliente_id BIGINT NOT NULL,
     *     FOREIGN KEY (cliente_id) REFERENCES clientes(id)
     * );
     * 
     * Analogía: Como la lista de cuentas bancarias de un cliente.
     * El cliente "sabe" que tiene cuentas, pero las cuentas
     * guardan la referencia al cliente (cliente_id).
     */
    @JsonIgnore
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<Prestamo> prestamos;
    
    // ============================================
    // CONSTRUCTORES
    // ============================================
    
    /**
     * Constructor vacío (sin parámetros).
     * 
     * OBLIGATORIO PARA JPA/HIBERNATE
     * 
     * ¿Por qué?
     * Hibernate necesita crear instancias desde ResultSet:
     * 
     * ResultSet rs = statement.executeQuery("SELECT * FROM clientes");
     * while (rs.next()) {
     *     Cliente c = new Cliente();  // ← Usa constructor vacío
     *     c.id = rs.getLong("id");
     *     c.nombre = rs.getString("nombre");
     *     // Setea campos con reflection
     * }
     * 
     * Sin constructor vacío:
     * org.hibernate.InstantiationException:
     * No default constructor for entity
     * 
     * También necesario para:
     * - JAX-RS deserializar JSON → Objeto
     * - Frameworks que usan reflection
     * 
     * Best Practice:
     * - Siempre público
     * - Siempre presente, incluso si tienes otros constructores
     */
    public Cliente() {
        // Constructor vacío para JPA
        // No hace nada, solo permite a Hibernate crear la instancia
    }
    
    /**
     * Constructor con todos los datos de negocio.
     * 
     * ÚTIL PARA:
     * - Crear instancias en código: new Cliente("Juan", "123", "juan@", "987")
     * - Tests: fácil crear datos de prueba
     * - Seed data: inicializar base de datos
     * 
     * NO incluye 'id' porque:
     * - ID es auto-generado por base de datos
     * - No lo conocemos hasta después de persist()
     * 
     * Ejemplo de uso:
     * Cliente cliente = new Cliente(
     *     "María González",
     *     "12345678",
     *     "maria@example.com",
     *     "987654321"
     * );
     * cliente.persist();
     * // Ahora cliente.id tiene valor (ej: 1)
     * 
     * Alternativa: Builder Pattern
     * Cliente cliente = Cliente.builder()
     *     .nombre("María González")
     *     .dni("12345678")
     *     .email("maria@example.com")
     *     .telefono("987654321")
     *     .build();
     * 
     * (Requiere Lombok o implementación manual)
     * 
     * @param nombre Nombre completo del cliente
     * @param dni Documento de identidad (8 dígitos)
     * @param email Correo electrónico único
     * @param telefono Número de contacto
     */
    public Cliente(String nombre, String dni, String email, String telefono) {
        this.nombre = nombre;
        this.dni = dni;
        this.email = email;
        this.telefono = telefono;
        // prestamos inicia null (se setea después si es necesario)
    }
    
    // ============================================
    // MÉTODOS ÚTILES (Opcionales pero recomendados)
    // ============================================
    
    /**
     * Representación en String del cliente.
     * Útil para debugging y logs.
     * 
     * @return String con datos principales del cliente
     */
    @Override
    public String toString() {
        return "Cliente{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", dni='" + dni + '\'' +
                ", email='" + email + '\'' +
                ", telefono='" + telefono + '\'' +
                '}';
    }
}

/**
 * ═══════════════════════════════════════════════════════════════
 * EJEMPLOS DE USO DE ESTA ENTIDAD
 * ═══════════════════════════════════════════════════════════════
 * 
 * 1. CREAR Y GUARDAR (Active Record):
 * 
 *    Cliente cliente = new Cliente(
 *        "Juan Pérez",
 *        "12345678",
 *        "juan@example.com",
 *        "987654321"
 *    );
 *    cliente.persist();  // Se guarda solo
 *    System.out.println(cliente.id);  // Ahora tiene ID
 * 
 * 2. BUSCAR POR ID:
 * 
 *    Cliente cliente = Cliente.findById(1L);
 *    if (cliente != null) {
 *        System.out.println(cliente.nombre);
 *    }
 * 
 * 3. LISTAR TODOS:
 * 
 *    List<Cliente> clientes = Cliente.listAll();
 *    clientes.forEach(c -> System.out.println(c.nombre));
 * 
 * 4. BUSCAR POR CAMPO:
 * 
 *    List<Cliente> clientes = Cliente.find("nombre", "Juan").list();
 *    
 *    // Con query más compleja:
 *    Cliente cliente = Cliente.find("dni = ?1", "12345678")
 *                             .firstResult();
 * 
 * 5. ACTUALIZAR:
 * 
 *    Cliente cliente = Cliente.findById(1L);
 *    cliente.telefono = "999888777";
 *    // Auto-persistido por Hibernate (dentro de @Transactional)
 * 
 * 6. ELIMINAR:
 * 
 *    Cliente.deleteById(1L);
 *    
 *    // O desde instancia:
 *    Cliente cliente = Cliente.findById(1L);
 *    cliente.delete();
 * 
 * 7. CONTAR:
 * 
 *    long total = Cliente.count();
 *    System.out.println("Total clientes: " + total);
 * 
 * 8. VERIFICAR EXISTENCIA:
 * 
 *    boolean existe = Cliente.count("dni", "12345678") > 0;
 * 
 * ═══════════════════════════════════════════════════════════════
 * FLUJO COMPLETO: CREAR CLIENTE CON PRÉSTAMO
 * ═══════════════════════════════════════════════════════════════
 * 
 * @POST
 * @Transactional
 * public Response crearClienteConPrestamo(RequestDTO dto) {
 *     // 1. Crear cliente
 *     Cliente cliente = new Cliente(
 *         dto.nombre,
 *         dto.dni,
 *         dto.email,
 *         dto.telefono
 *     );
 *     
 *     // 2. Crear préstamo
 *     Prestamo prestamo = new Prestamo();
 *     prestamo.cliente = cliente;
 *     prestamo.monto = new BigDecimal("10000");
 *     prestamo.plazoMeses = 12;
 *     
 *     // 3. Agregar préstamo a cliente
 *     cliente.prestamos = List.of(prestamo);
 *     
 *     // 4. Guardar (cascade ALL persiste préstamo también)
 *     cliente.persist();
 *     
 *     return Response.status(201).entity(cliente).build();
 * }
 * 
 * SQL generado por Hibernate:
 * INSERT INTO clientes (nombre, dni, email, telefono) 
 * VALUES ('Juan', '123', 'juan@', '987');
 * 
 * INSERT INTO prestamos (cliente_id, monto, plazo_meses) 
 * VALUES (1, 10000, 12);
 * 
 * ═══════════════════════════════════════════════════════════════
 * EVOLUCIÓN FUTURA DE ESTA ENTIDAD
 * ═══════════════════════════════════════════════════════════════
 * 
 * Capítulo 5 - Bean Validation:
 *   @NotNull(message = "Nombre es obligatorio")
 *   @Size(min = 3, max = 100)
 *   public String nombre;
 *   
 *   @Pattern(regexp = "\\d{8}", message = "DNI debe tener 8 dígitos")
 *   public String dni;
 *   
 *   @Email(message = "Email inválido")
 *   public String email;
 * 
 * Capítulo 6 - Auditoría:
 *   @CreationTimestamp
 *   public LocalDateTime fechaCreacion;
 *   
 *   @UpdateTimestamp
 *   public LocalDateTime fechaActualizacion;
 * 
 * Capítulo 7 - Soft Delete:
 *   @SQLDelete(sql = "UPDATE clientes SET activo = false WHERE id = ?")
 *   @Where(clause = "activo = true")
 *   public class Cliente {
 *       public Boolean activo = true;
 *   }
 * 
 * Capítulo 8 - Cifrado:
 *   @Convert(converter = EmailEncryptionConverter.class)
 *   public String email;
 *   
 *   @Convert(converter = DNIEncryptionConverter.class)
 *   public String dni;
 * 
 * ═══════════════════════════════════════════════════════════════
 */