package pe.banco.ejemplo;

// ============================================================================
// IMPORTS - Especificación JAX-RS (Jakarta RESTful Web Services)
// ============================================================================

// @GET: Anotación que marca un método como manejador de peticiones HTTP GET.
// Parte de: jakarta.ws.rs (JAX-RS 3.1+)
// Equivalente a: @GetMapping en Spring
// HTTP GET se usa para obtener/consultar recursos (operaciones de solo lectura)
import jakarta.ws.rs.GET;

// @Path: Define la ruta (URL) del recurso REST.
// Puede usarse en clase (ruta base) o en método (sub-ruta)
// Ejemplos: @Path("/usuarios") → http://localhost:8080/usuarios
//           @Path("/usuarios/{id}") → path con parámetro
import jakarta.ws.rs.Path;

// @Produces: Especifica el tipo de contenido que el endpoint DEVUELVE
// Se traduce en el header HTTP: Content-Type
// Valores comunes: TEXT_PLAIN, APPLICATION_JSON, APPLICATION_XML
import jakarta.ws.rs.Produces;

// MediaType: Clase de constantes para tipos MIME estándar
// Evita errores de tipeo: MediaType.TEXT_PLAIN en vez de "text/plain"
// Constantes: TEXT_PLAIN, APPLICATION_JSON, APPLICATION_XML, TEXT_HTML, etc.
import jakarta.ws.rs.core.MediaType;

// ============================================================================
// CLASE RESOURCE - Controlador REST
// ============================================================================

/**
 * HelloResource - Endpoint REST básico
 * 
 * Un "Resource" en JAX-RS es equivalente a un "Controller" en Spring.
 * Es una clase que expone endpoints HTTP (APIs REST).
 * 
 * Características:
 * - Clase pública con @Path que define la ruta base
 * - Métodos públicos anotados con @GET, @POST, @PUT, @DELETE
 * - Quarkus la detecta automáticamente (no necesita @Controller)
 * 
 * Convenciones de nombre:
 * - *Resource: endpoints REST (UsuarioResource, ProductoResource)
 * - *Service: lógica de negocio (UsuarioService)  
 * - *Repository: acceso a datos (UsuarioRepository)
 * 
 * @Path("/hello") define que todos los endpoints empiezan con /hello
 * URL completa: http://localhost:8080/hello
 */
@Path("/hello")
public class HelloResource {

    /**
     * Endpoint GET /hello
     * 
     * Retorna un saludo en texto plano.
     * 
     * ANOTACIONES:
     * - @GET: Este método responde a peticiones HTTP GET
     * - @Produces(MediaType.TEXT_PLAIN): Devuelve texto plano (Content-Type: text/plain)
     * 
     * FLUJO DE EJECUCIÓN:
     * 1. Cliente hace: GET http://localhost:8080/hello
     * 2. Quarkus (Vert.x) recibe la petición
     * 3. JAX-RS encuentra este método (por @GET + @Path en clase)
     * 4. Ejecuta hello()
     * 5. Toma el String retornado
     * 6. Crea respuesta HTTP:
     *    - Status: 200 OK
     *    - Header: Content-Type: text/plain
     *    - Body: "Hola mundo desde Quarkus"
     * 7. Envía respuesta al cliente
     * 
     * MÉTODO:
     * - public: obligatorio para endpoints
     * - String: tipo de retorno (será el body de la respuesta)
     * - hello(): nombre (puede ser cualquiera, routing es por anotaciones)
     * - sin parámetros en este caso
     * 
     * @return Mensaje de saludo como String
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hola mundo desde Quarkus";
    }
    
    // ========================================================================
    // EJEMPLOS DE OTROS ENDPOINTS (comentados para referencia)
    // ========================================================================
    
    // EJEMPLO 1: Endpoint que devuelve JSON
    // @GET
    // @Path("/json")
    // @Produces(MediaType.APPLICATION_JSON)
    // public Map<String, String> helloJson() {
    //     return Map.of("mensaje", "Hola desde JSON", "framework", "Quarkus");
    // }
    
    // EJEMPLO 2: Endpoint con Path Parameter
    // @GET
    // @Path("/{nombre}")
    // @Produces(MediaType.TEXT_PLAIN)
    // public String saludarPersona(@PathParam("nombre") String nombre) {
    //     return "Hola " + nombre + " desde Quarkus";
    // }
    // URL: GET /hello/Juan → "Hola Juan desde Quarkus"
    
    // EJEMPLO 3: Endpoint con Query Parameter  
    // @GET
    // @Path("/saludar")
    // @Produces(MediaType.TEXT_PLAIN)
    // public String saludarConQuery(@QueryParam("nombre") String nombre) {
    //     return "Hola " + (nombre != null ? nombre : "invitado");
    // }
    // URL: GET /hello/saludar?nombre=Maria → "Hola Maria"
    
    // EJEMPLO 4: POST que recibe y devuelve JSON
    // @POST
    // @Consumes(MediaType.APPLICATION_JSON)
    // @Produces(MediaType.APPLICATION_JSON)
    // public Response crear(Usuario usuario) {
    //     // Lógica para crear usuario
    //     return Response.status(201).entity(usuario).build();
    // }
}

// ============================================================================
// NOTAS TÉCNICAS IMPORTANTES
// ============================================================================

// 1. JAKARTA vs JAVAX:
//    - jakarta.ws.rs → Versión moderna (Jakarta EE 9+) ← USAR ESTA
//    - javax.ws.rs → Versión antigua (Java EE 8-)
//    - Quarkus 3.x REQUIERE jakarta
//    - NO mezclar ambas, causan conflictos

// 2. INYECCIÓN DE DEPENDENCIAS:
//    - Esta clase NO tiene @ApplicationScoped porque los Resources JAX-RS
//      son manejados especialmente por Quarkus
//    - Si necesitas inyectar servicios:
//      @ApplicationScoped
//      public class HelloResource {
//          @Inject
//          MiServicio servicio;
//      }

// 3. RUTEO AUTOMÁTICO:
//    - Quarkus escanea en build-time todas las clases con @Path
//    - No necesitas registrar endpoints manualmente
//    - Motor de ruteo: Vert.x (extremadamente rápido)

// 4. CÓDIGOS HTTP MÁS USADOS:
//    - 200 OK: éxito (por defecto)
//    - 201 Created: recurso creado (POST)
//    - 204 No Content: éxito sin body (DELETE)
//    - 400 Bad Request: datos inválidos
//    - 404 Not Found: recurso no existe
//    - 500 Internal Server Error: error del servidor

// 5. CONTROL DE RESPUESTA HTTP:
//    return Response.status(201).entity(objeto).build();
//    return Response.ok(objeto).build();
//    return Response.status(404).entity("No encontrado").build();

// 6. CONTENT NEGOTIATION:
//    Si defines múltiples tipos:
//    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
//    JAX-RS elegirá según header "Accept" del cliente

// 7. EXTENSIÓN REQUERIDA:
//    Este código necesita: quarkus-rest
//    Agregar con: ./mvnw quarkus:add-extension -Dextensions="rest"
//    O en pom.xml:
//    <dependency>
//        <groupId>io.quarkus</groupId>
//        <artifactId>quarkus-rest</artifactId>
//    </dependency>

// 8. TESTING DEL ENDPOINT:
//    @QuarkusTest
//    public class HelloResourceTest {
//        @Test
//        public void testHello() {
//            given()
//                .when().get("/hello")
//                .then()
//                .statusCode(200)
//                .body(is("Hola mundo desde Quarkus"));
//        }
//    }

// ============================================================================
// ANALOGÍA: EL RESTAURANTE
// ============================================================================

// Este código funciona como un RESTAURANTE:
//
// @Path("/hello")              → Dirección: Calle Hello #1
// public class HelloResource   → El restaurante en sí
// @GET                         → Pedir comida (consultar, no modificar)
// @Produces(TEXT_PLAIN)        → Menú: "Servimos comida casera" (texto)
// public String hello()        → El chef que prepara el plato
// return "Hola mundo..."       → El plato que te sirven
//
// Flujo del cliente:
// 1. Llega a la dirección: /hello
// 2. Pide con método GET (como "para llevar")
// 3. Chef prepara: "Hola mundo desde Quarkus"
// 4. Se sirve en formato texto plano
// 5. Cliente recibe con código 200 (todo OK)