package pe.banco.clientes.dto;

/**
 * Data Transfer Object (DTO) para recibir datos de clientes en peticiones HTTP.
 * <p>
 * Esta clase transporta información desde el cliente (frontend/API consumer) hacia
 * el servidor, típicamente en operaciones POST (crear) y PUT (actualizar). Los datos
 * llegan en <strong>texto plano</strong> y serán cifrados por el servicio antes de
 * persistir en la base de datos.
 * </p>
 * 
 * <p><strong>Flujo de seguridad:</strong></p>
 * <pre>{@code
 * 1. Cliente envía JSON → ClienteRequest (texto plano)
 * 2. ClienteService recibe → cifra numeroTarjeta y email
 * 3. Repository persiste → datos cifrados en BD
 * }</pre>
 * 
 * <p><strong>Ventajas del patrón DTO:</strong></p>
 * <ul>
 *   <li>Desacopla la API de la estructura interna de la entidad</li>
 *   <li>Previene mass assignment vulnerabilities</li>
 *   <li>Permite validaciones específicas de entrada</li>
 *   <li>Control granular sobre qué campos pueden ser modificados</li>
 *   <li>Facilita versionado de API (v1, v2) sin afectar el modelo</li>
 * </ul>
 * 
 * <p><strong>Ejemplo de payload JSON:</strong></p>
 * <pre>{@code
 * POST /api/v1/clientes
 * Content-Type: application/json
 * 
 * {
 *   "nombre": "Juan Pérez",
 *   "numeroTarjeta": "4532-1234-5678-9012",
 *   "email": "juan.perez@banco.com",
 *   "telefono": "+56912345678"
 * }
 * }</pre>
 * 
 * <p><strong>⚠️ Seguridad - Datos Sensibles en Tránsito:</strong></p>
 * <ul>
 *   <li><strong>OBLIGATORIO:</strong> Usar HTTPS/TLS 1.3+ en producción</li>
 *   <li>Datos viajan cifrados por TLS hasta el servidor</li>
 *   <li>Nunca enviar por HTTP sin cifrado (expone datos en red)</li>
 *   <li>Implementar rate limiting para prevenir ataques de fuerza bruta</li>
 *   <li>Validar formato antes de procesar (previene injection)</li>
 * </ul>
 * 
 * <p><strong>Campos que serán cifrados:</strong></p>
 * <ul>
 *   <li>{@code numeroTarjeta} → Cifrado con AES-256-GCM (PCI-DSS obligatorio)</li>
 *   <li>{@code email} → Cifrado con AES-256-GCM (GDPR recomendado)</li>
 * </ul>
 * 
 * <p><strong>Campos en texto plano:</strong></p>
 * <ul>
 *   <li>{@code nombre} → Necesario para búsquedas</li>
 *   <li>{@code telefono} → Menos sensible, útil para contacto</li>
 * </ul>
 * 
 * <p><strong>Nota:</strong> Los campos son públicos para facilitar la serialización/deserialización
 * automática con Jackson. En producción, considera agregar validaciones con
 * {@code jakarta.validation.constraints}.</p>
 * 
 * @author Curso Quarkus - Capítulo 4.2
 * @version 1.0
 * @see pe.banco.clientes.entity.Cliente
 * @see pe.banco.clientes.service.CryptoService
 */
public class ClienteRequest {

    /**
     * Nombre completo del cliente.
     * <p>
     * Este campo NO será cifrado en la base de datos para permitir búsquedas
     * y operaciones de consulta eficientes.
     * </p>
     * 
     * <p><strong>Validaciones recomendadas:</strong></p>
     * <ul>
     *   <li><strong>@NotBlank:</strong> No debe ser null, vacío ni solo espacios</li>
     *   <li><strong>@Size(min=2, max=255):</strong> Longitud razonable</li>
     *   <li><strong>@Pattern:</strong> Solo letras, espacios y acentos permitidos</li>
     * </ul>
     * 
     * <p><strong>Ejemplo válido:</strong> "María José González Pérez"</p>
     * <p><strong>Ejemplo inválido:</strong> "M" (muy corto), "Juan123" (números)</p>
     */
    public String nombre;

    /**
     * Número de tarjeta de crédito o débito.
     * <p>
     * ⚠️ <strong>DATO ALTAMENTE SENSIBLE</strong> - Será cifrado antes de persistir.
     * </p>
     * 
     * <p><strong>Seguridad:</strong></p>
     * <ul>
     *   <li>Recibido en texto plano desde el cliente (sobre TLS)</li>
     *   <li>Cifrado inmediatamente por {@code CryptoService.cifrar()}</li>
     *   <li>Almacenado cifrado en BD (AES-256-GCM)</li>
     *   <li>Nunca logueado sin enmascarar</li>
     * </ul>
     * 
     * <p><strong>Validaciones recomendadas:</strong></p>
     * <ul>
     *   <li><strong>@NotBlank:</strong> Obligatorio</li>
     *   <li><strong>@CreditCardNumber:</strong> Algoritmo de Luhn válido</li>
     *   <li><strong>@Size(min=13, max=19):</strong> Longitud de tarjetas estándar</li>
     *   <li><strong>@Pattern:</strong> Formato con o sin guiones (####-####-####-####)</li>
     * </ul>
     * 
     * <p><strong>Formatos aceptados:</strong></p>
     * <ul>
     *   <li>Con guiones: "4532-1234-5678-9012"</li>
     *   <li>Con espacios: "4532 1234 5678 9012"</li>
     *   <li>Sin separadores: "4532123456789012"</li>
     * </ul>
     * 
     * <p><strong>Tipos de tarjeta soportados:</strong></p>
     * <ul>
     *   <li>Visa: 16 dígitos, empieza con 4</li>
     *   <li>MasterCard: 16 dígitos, empieza con 51-55</li>
     *   <li>American Express: 15 dígitos, empieza con 34 o 37</li>
     * </ul>
     * 
     * <p><strong>Compliance PCI-DSS:</strong></p>
     * <ul>
     *   <li>Requisito 3.4: Debe cifrarse en almacenamiento</li>
     *   <li>Requisito 3.3: Enmascarar al mostrar (mostrar solo últimos 4 dígitos)</li>
     *   <li>Requisito 4.1: Cifrar durante transmisión (TLS)</li>
     * </ul>
     * 
     * <p><strong>⚠️ NUNCA:</strong></p>
     * <ul>
     *   <li>Guardar en logs sin enmascarar</li>
     *   <li>Mostrar completo en pantalla (solo **** **** **** 9012)</li>
     *   <li>Enviar por email sin cifrado adicional</li>
     *   <li>Incluir en URLs o query parameters</li>
     * </ul>
     * 
     * <p><strong>Ejemplo válido:</strong> "4532-1234-5678-9012"</p>
     * <p><strong>Ejemplo inválido:</strong> "1234" (muy corto), "abcd-efgh-ijkl-mnop" (letras)</p>
     */
    public String numeroTarjeta;

    /**
     * Dirección de correo electrónico del cliente.
     * <p>
     * ⚠️ <strong>DATO SENSIBLE</strong> - Será cifrado antes de persistir.
     * </p>
     * 
     * <p><strong>Seguridad:</strong></p>
     * <ul>
     *   <li>Recibido en texto plano desde el cliente (sobre TLS)</li>
     *   <li>Cifrado inmediatamente por {@code CryptoService.cifrar()}</li>
     *   <li>Almacenado cifrado en BD (AES-256-GCM)</li>
     *   <li>Protege contra spam/phishing si la BD es comprometida</li>
     * </ul>
     * 
     * <p><strong>Validaciones recomendadas:</strong></p>
     * <ul>
     *   <li><strong>@NotBlank:</strong> Obligatorio</li>
     *   <li><strong>@Email:</strong> Formato válido de email</li>
     *   <li><strong>@Size(max=255):</strong> Longitud estándar de emails</li>
     *   <li><strong>Normalización:</strong> Convertir a lowercase antes de procesar</li>
     * </ul>
     * 
     * <p><strong>Formatos aceptados:</strong></p>
     * <ul>
     *   <li>Estándar: usuario@dominio.com</li>
     *   <li>Con subdominios: usuario@mail.empresa.com</li>
     *   <li>Con plus: usuario+tag@dominio.com</li>
     * </ul>
     * 
     * <p><strong>Compliance GDPR:</strong></p>
     * <ul>
     *   <li>Artículo 32: Recomendado cifrar datos personales</li>
     *   <li>Si está cifrado: NO requiere notificación en caso de brecha</li>
     *   <li>Derecho al olvido: Facilita eliminación definitiva</li>
     * </ul>
     * 
     * <p><strong>Consideraciones de búsqueda:</strong></p>
     * <p>
     * Si necesitas buscar clientes por email, considera agregar una columna
     * adicional con hash (SHA-256) del email para búsquedas rápidas mientras
     * mantienes el email original cifrado.
     * </p>
     * 
     * <p><strong>Ejemplo válido:</strong> "juan.perez@banco.com"</p>
     * <p><strong>Ejemplo inválido:</strong> "juan.perez" (sin dominio), "juan@" (incompleto)</p>
     */
    public String email;

    /**
     * Número de teléfono del cliente con código de país.
     * <p>
     * Este campo NO será cifrado en la base de datos por considerarse menos
     * sensible y por necesidad de búsquedas y contacto frecuente.
     * </p>
     * 
     * <p><strong>Justificación de no cifrar:</strong></p>
     * <ul>
     *   <li>Usado frecuentemente para verificación 2FA/OTP</li>
     *   <li>Necesario para búsquedas rápidas en call center</li>
     *   <li>Menor impacto de seguridad comparado con datos financieros</li>
     *   <li>Regulaciones no lo requieren obligatoriamente</li>
     * </ul>
     * 
     * <p><strong>Validaciones recomendadas:</strong></p>
     * <ul>
     *   <li><strong>@NotBlank:</strong> Obligatorio</li>
     *   <li><strong>@Pattern:</strong> Formato E.164 internacional</li>
     *   <li><strong>@Size(min=8, max=15):</strong> Longitud típica con código país</li>
     * </ul>
     * 
     * <p><strong>Formato esperado:</strong></p>
     * <pre>{@code
     * Formato E.164: +[código_país][número]
     * Ejemplos:
     * - Chile: +56912345678 (9 dígitos después de +56)
     * - México: +525512345678 (10 dígitos después de +52)
     * - USA: +15551234567 (10 dígitos después de +1)
     * }</pre>
     * 
     * <p><strong>Normalización:</strong></p>
     * <ul>
     *   <li>Remover espacios, guiones, paréntesis antes de persistir</li>
     *   <li>Validar que empiece con + y código de país válido</li>
     *   <li>Almacenar siempre en formato E.164</li>
     * </ul>
     * 
     * <p><strong>Ejemplo válido:</strong> "+56912345678"</p>
     * <p><strong>Ejemplo inválido:</strong> "912345678" (sin código país), "56912345678" (sin +)</p>
     */
    public String telefono;

    /**
     * Constructor por defecto requerido para deserialización JSON.
     * <p>
     * Jackson (biblioteca de serialización de Quarkus RESTEasy) requiere un
     * constructor sin argumentos para crear instancias al convertir JSON a
     * objetos Java.
     * </p>
     * 
     * <p><strong>Proceso de deserialización:</strong></p>
     * <pre>{@code
     * 1. Jackson recibe JSON del cliente
     * 2. Crea instancia con new ClienteRequest()
     * 3. Usa setters o acceso directo a campos para asignar valores
     * 4. Resultado: ClienteRequest con datos del JSON
     * }</pre>
     */
    public ClienteRequest() {
    }
}