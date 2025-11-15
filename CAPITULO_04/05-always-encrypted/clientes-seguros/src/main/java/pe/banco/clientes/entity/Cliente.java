package pe.banco.clientes.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;

/**
 * Entidad JPA que representa un cliente bancario con datos sensibles.
 * <p>
 * Esta entidad almacena información personal y financiera de clientes, 
 * incluyendo datos que requieren cifrado por regulaciones de seguridad
 * (PCI-DSS, GDPR).
 * </p>
 * 
 * <p><strong>Campos cifrados:</strong></p>
 * <ul>
 *   <li><strong>numeroTarjeta:</strong> Cifrado con Google Tink (AES-256-GCM) - PCI-DSS obligatorio</li>
 *   <li><strong>email:</strong> Cifrado con Google Tink (AES-256-GCM) - GDPR recomendado</li>
 * </ul>
 * 
 * <p><strong>Campos en texto plano:</strong></p>
 * <ul>
 *   <li><strong>nombre:</strong> Necesario para búsquedas y reportes</li>
 *   <li><strong>telefono:</strong> Menos sensible, útil para contacto directo</li>
 * </ul>
 * 
 * <p><strong>Flujo de cifrado/descifrado:</strong></p>
 * <pre>{@code
 * // Al guardar (ClienteService)
 * cliente.numeroTarjeta = cryptoService.cifrar("4532-1234-5678-9012");
 * // BD almacena: "AebqJ3oc/tkB8ryE+6YZ4i3oWlS/SBhcyPul"
 * 
 * // Al leer (ClienteService)
 * String numeroReal = cryptoService.descifrar(cliente.numeroTarjeta);
 * // API devuelve: "4532-1234-5678-9012"
 * }</pre>
 * 
 * <p><strong>⚠️ Advertencias de seguridad:</strong></p>
 * <ul>
 *   <li>Los datos cifrados en BD son ilegibles sin la clave de cifrado</li>
 *   <li>Perder la clave = perder acceso permanente a los datos</li>
 *   <li>NO hacer queries SQL directas sobre campos cifrados (usar servicios)</li>
 *   <li>Los logs NO deben mostrar datos sensibles descifrados</li>
 * </ul>
 * 
 * <p><strong>Compliance:</strong></p>
 * <ul>
 *   <li><strong>PCI-DSS Req 3.4:</strong> Números de tarjeta deben estar cifrados</li>
 *   <li><strong>GDPR Art. 32:</strong> Datos personales requieren medidas técnicas apropiadas</li>
 * </ul>
 * 
 * @author Curso Quarkus - Capítulo 4.2
 * @version 1.0
 * @see pe.banco.clientes.service.CryptoService
 * @see PanacheEntity
 */
@Entity
public class Cliente extends PanacheEntity {

    /**
     * Nombre completo del cliente.
     * <p>
     * Almacenado en <strong>texto plano</strong> para facilitar búsquedas y reportes.
     * Considerado dato personal bajo GDPR pero no requiere cifrado obligatorio
     * por su naturaleza pública.
     * </p>
     * 
     * <p><strong>Validaciones recomendadas:</strong></p>
     * <ul>
     *   <li>No debe ser null ni vacío</li>
     *   <li>Longitud máxima: 255 caracteres</li>
     *   <li>Solo caracteres alfabéticos y espacios</li>
     * </ul>
     * 
     * <p><strong>Ejemplo:</strong> "Juan Pérez González"</p>
     */
    @Column(nullable = false)
    public String nombre;

    /**
     * Número de tarjeta de crédito o débito del cliente.
     * <p>
     * ⚠️ <strong>DATO SENSIBLE - ALMACENADO CIFRADO</strong>
     * </p>
     * 
     * <p><strong>Seguridad:</strong></p>
     * <ul>
     *   <li>Cifrado con AES-256-GCM antes de persistir</li>
     *   <li>Almacenado como Base64 en la columna {@code numero_tarjeta}</li>
     *   <li>Descifrado solo cuando un usuario autorizado lo solicita</li>
     * </ul>
     * 
     * <p><strong>Compliance:</strong></p>
     * <ul>
     *   <li><strong>PCI-DSS Requisito 3.4:</strong> Obligatorio cifrar PAN (Primary Account Number)</li>
     *   <li>Multas por incumplimiento: $5,000 - $100,000 por mes</li>
     * </ul>
     * 
     * <p><strong>Ejemplo en BD:</strong></p>
     * <pre>{@code
     * Valor real:    "4532-1234-5678-9012"
     * En BD (cifrado): "AebqJ3oc/tkB8ryE+6YZ4i3oWlS/SBhcyPul"
     * }</pre>
     * 
     * <p><strong>⚠️ NUNCA:</strong></p>
     * <ul>
     *   <li>Mostrar en logs sin enmascarar</li>
     *   <li>Enviar por email sin cifrado adicional</li>
     *   <li>Exponer en URLs o query params</li>
     *   <li>Cachear sin protección adicional</li>
     * </ul>
     */
    @Column(name = "numero_tarjeta", nullable = false)
    public String numeroTarjeta;

    /**
     * Dirección de correo electrónico del cliente.
     * <p>
     * ⚠️ <strong>DATO SENSIBLE - ALMACENADO CIFRADO</strong>
     * </p>
     * 
     * <p><strong>Seguridad:</strong></p>
     * <ul>
     *   <li>Cifrado con AES-256-GCM antes de persistir</li>
     *   <li>Protege contra fugas de datos en brechas de seguridad</li>
     *   <li>Previene spam/phishing si la BD es comprometida</li>
     * </ul>
     * 
     * <p><strong>Compliance:</strong></p>
     * <ul>
     *   <li><strong>GDPR Art. 32:</strong> Recomendado cifrar datos personales</li>
     *   <li>Si está cifrado, NO requiere notificación a usuarios en caso de brecha</li>
     * </ul>
     * 
     * <p><strong>Alternativa sin cifrado:</strong> Usar hash (SHA-256) para búsquedas
     * y mantener el email original cifrado separadamente.</p>
     * 
     * <p><strong>Ejemplo en BD:</strong></p>
     * <pre>{@code
     * Valor real:    "juan.perez@banco.com"
     * En BD (cifrado): "Xm8kL!pQ3@zR7vN..."
     * }</pre>
     * 
     * <p><strong>Consideración:</strong> Si necesitas buscar por email, considera
     * agregar una columna adicional con hash del email para índices.</p>
     */
    @Column(nullable = false)
    public String email;

    /**
     * Número de teléfono del cliente.
     * <p>
     * Almacenado en <strong>texto plano</strong> por considerarse dato menos sensible
     * y por necesidad de contacto directo frecuente.
     * </p>
     * 
     * <p><strong>Justificación de no cifrar:</strong></p>
     * <ul>
     *   <li>Usado frecuentemente para verificación de identidad (2FA)</li>
     *   <li>Necesario para búsquedas rápidas en atención al cliente</li>
     *   <li>Menor riesgo comparado con datos financieros</li>
     *   <li>Regulaciones no lo requieren obligatoriamente</li>
     * </ul>
     * 
     * <p><strong>Formato esperado:</strong> +56912345678 (con código de país)</p>
     * 
     * <p><strong>Validaciones recomendadas:</strong></p>
     * <ul>
     *   <li>Formato E.164 internacional</li>
     *   <li>Longitud: 10-15 dígitos</li>
     *   <li>Incluir código de país (+)</li>
     * </ul>
     * 
     * <p><strong>Nota:</strong> En sistemas de máxima seguridad (banca privada, gobierno),
     * considerar cifrar también este campo.</p>
     */
    @Column(nullable = false)
    public String telefono;

    /**
     * Constructor por defecto requerido por JPA.
     * <p>
     * Hibernate necesita este constructor para instanciar la entidad
     * al recuperarla de la base de datos mediante reflection.
     * </p>
     */
    public Cliente() {
    }

    /**
     * Constructor con parámetros para inicializar un cliente completo.
     * <p>
     * ⚠️ <strong>IMPORTANTE:</strong> Los valores de {@code numeroTarjeta} y {@code email}
     * deben pasarse <strong>YA CIFRADOS</strong> si este constructor se usa desde
     * la capa de persistencia. Si se usa desde la capa de servicio, pasar en texto
     * plano y el servicio se encargará del cifrado.
     * </p>
     * 
     * <p><strong>Uso típico desde ClienteService:</strong></p>
     * <pre>{@code
     * // El servicio cifra antes de crear el cliente
     * String numeroTarjetaCifrado = cryptoService.cifrar(request.numeroTarjeta);
     * String emailCifrado = cryptoService.cifrar(request.email);
     * 
     * Cliente cliente = new Cliente(
     *     request.nombre,
     *     numeroTarjetaCifrado,  // YA cifrado
     *     emailCifrado,          // YA cifrado
     *     request.telefono
     * );
     * }</pre>
     * 
     * @param nombre Nombre completo del cliente (texto plano)
     * @param numeroTarjeta Número de tarjeta (debe estar cifrado si viene de BD)
     * @param email Correo electrónico (debe estar cifrado si viene de BD)
     * @param telefono Número de teléfono con código de país (texto plano)
     */
    public Cliente(String nombre, String numeroTarjeta, String email, String telefono) {
        this.nombre = nombre;
        this.numeroTarjeta = numeroTarjeta;
        this.email = email;
        this.telefono = telefono;
    }
}