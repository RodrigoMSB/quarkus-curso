package pe.banco.tasacorp.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import pe.banco.tasacorp.config.TasaCorpConfig;
import pe.banco.tasacorp.model.ConversionResponse;
import pe.banco.tasacorp.model.TasaResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Servicio de tasas de cambio - Lógica de negocio principal.
 * 
 * 📋 PROPÓSITO:
 * Este servicio centraliza toda la lógica de negocio relacionada con
 * tasas de cambio y conversiones de moneda. Es el "cerebro" de la aplicación.
 * 
 * 🏗️ ARQUITECTURA:
 * 
 * Flujo de una solicitud:
 * 
 * Cliente HTTP
 *     ↓
 * TasaResource (REST)
 *     ↓
 * TasaService (LÓGICA) ← Estamos aquí
 *     ↓
 * TasaCorpConfig (CONFIGURACIÓN)
 *     ↓
 * DTOs (ConversionResponse / TasaResponse)
 *     ↓
 * Cliente recibe JSON
 * 
 * 💡 @ApplicationScoped:
 * 
 * Esta anotación indica que:
 * - Se crea UNA SOLA instancia de este servicio
 * - Vive durante toda la vida de la aplicación
 * - Se comparte entre todos los requests
 * 
 * VENTAJAS:
 * ✅ Eficiente: No se crea/destruye en cada request
 * ✅ Stateless: No guarda estado entre requests (thread-safe)
 * ✅ Inyectable: Puede inyectarse en cualquier componente
 * 
 * ANALOGÍA:
 * Es como tener UN calculador de tasas en el banco que atiende
 * a todos los clientes, en lugar de crear uno nuevo por cliente.
 * 
 * 🎯 RESPONSABILIDADES:
 * 
 * 1. Consultar tasas de cambio
 *    → obtenerTasa(String moneda)
 * 
 * 2. Realizar conversiones completas
 *    → convertirMoneda(String moneda, Double monto)
 * 
 * 3. Exponer configuración actual
 *    → obtenerConfiguracion()
 * 
 * 4. Validar monedas soportadas
 *    → Lanza excepción si la moneda no existe
 * 
 * 5. Validar límites transaccionales
 *    → Marca si la transacción excede el límite
 * 
 * 🔐 SEGURIDAD:
 * - No expone el API key en las respuestas
 * - Valida todas las entradas
 * - Registra operaciones en logs
 * 
 * 📊 CONFIGURACIÓN DEPENDIENTE DE PERFIL:
 * 
 * Todos los cálculos varían según el perfil activo:
 * 
 * DEV:
 *   - Comisión: 0.0%
 *   - Límite: 999,999
 *   - Proveedor: MockProvider
 * 
 * TEST:
 *   - Comisión: 1.5%
 *   - Límite: 1,000
 *   - Proveedor: FreeCurrencyAPI
 * 
 * PROD:
 *   - Comisión: 2.5%
 *   - Límite: 50,000
 *   - Proveedor: PremiumProvider
 * 
 * @author Arquitectura TasaCorp
 * @version 1.0.0
 * @see TasaCorpConfig Para la configuración inyectada
 * @see TasaResource Para los endpoints REST que usan este servicio
 */
@ApplicationScoped
public class TasaService {

    // ========================================================================
    // CONSTANTES Y LOGGER
    // ========================================================================

    /**
     * Logger para registrar operaciones del servicio.
     * 
     * 💡 ¿POR QUÉ LOGGER?
     * 
     * Los logs son cruciales para:
     * - Debugging: Ver qué está pasando en runtime
     * - Auditoría: Registrar operaciones importantes
     * - Monitoreo: Detectar problemas en producción
     * - Troubleshooting: Investigar errores pasados
     * 
     * NIVELES DE LOG USADOS:
     * - INFO:  Operaciones normales (obtener tasa, convertir)
     * - WARN:  Situaciones atípicas pero manejables (límite excedido)
     * - ERROR: Errores graves (no se usan aquí, pero existirían)
     * 
     * EJEMPLO DE LOG:
     * INFO  [pe.ban.tas.ser.TasaService] Obteniendo tasa para USD en perfil: prod
     * WARN  [pe.ban.tas.ser.TasaService] Monto 60000.00 excede el límite transaccional de 50000
     */
    private static final Logger LOG = Logger.getLogger(TasaService.class);

    // ========================================================================
    // DEPENDENCIAS INYECTADAS
    // ========================================================================

    /**
     * Configuración completa de TasaCorp.
     * 
     * 💉 @Inject:
     * Quarkus inyecta automáticamente la configuración al crear el servicio.
     * 
     * 🎯 USO:
     * Permite acceder a toda la configuración de forma type-safe:
     * 
     * <pre>
     * String base = config.currency().base();              // "PEN"
     * List<String> supported = config.currency().supported(); // [USD, EUR, MXN]
     * Double rate = config.commission().rate();             // 2.5 (varía por perfil)
     * </pre>
     * 
     * 🔄 REACTIVIDAD:
     * Esta configuración es INMUTABLE después del arranque.
     * Para cambiar valores, hay que reiniciar la aplicación.
     * 
     * VENTAJAS VS @ConfigProperty:
     * ✅ Acceso jerárquico organizado
     * ✅ Autocompletado en IDE
     * ✅ Type-safe (errores en compilación)
     * ✅ Menos verboso
     */
    @Inject
    TasaCorpConfig config;

    /**
     * Nombre de la aplicación.
     * 
     * 💉 @ConfigProperty:
     * Para valores INDIVIDUALES simples, se puede usar esta anotación.
     * 
     * Mapea: app.name=TasaCorp API (desde application.properties)
     * 
     * 🎯 USO:
     * Se incluye en el endpoint /api/tasas/config para identificar la app.
     * 
     * 💡 CUÁNDO USAR @ConfigProperty VS @ConfigMapping:
     * 
     * @ConfigProperty: Para 1-3 propiedades sueltas
     * @ConfigMapping:  Para grupos relacionados de propiedades
     */
    @ConfigProperty(name = "app.name")
    String appName;

    /**
     * Perfil activo de Quarkus.
     * 
     * 💉 @ConfigProperty:
     * Inyecta el perfil actualmente en ejecución.
     * 
     * Posibles valores:
     * - "dev"  (desarrollo)
     * - "test" (testing)
     * - "prod" (producción)
     * 
     * 🎯 USO:
     * - Logging: Registrar en qué perfil se ejecutan operaciones
     * - Debugging: Identificar comportamientos específicos de perfil
     * - Responses: Incluir el perfil en respuestas (opcional)
     * 
     * 💡 AUTOMÁTICO:
     * Quarkus lo establece según cómo arranques:
     * - ./mvnw quarkus:dev → "dev"
     * - mvn test → "test"
     * - java -jar app.jar → "prod"
     * - java -jar app.jar -Dquarkus.profile=X → "X"
     */
    @ConfigProperty(name = "quarkus.profile")
    String activeProfile;

    // ========================================================================
    // MÉTODOS PÚBLICOS - Lógica de Negocio
    // ========================================================================

    /**
     * Obtiene la tasa de cambio para una moneda específica.
     * 
     * 📋 FUNCIONALIDAD:
     * Consulta la tasa actual para convertir de PEN a la moneda destino.
     * NO realiza ninguna conversión, solo informa la tasa.
     * 
     * 🎯 CASO DE USO:
     * Un cliente quiere saber: "¿A cuánto está el dólar hoy?"
     * Este método responde con la tasa y la comisión que se aplicaría.
     * 
     * 📊 FLUJO:
     * 1. Loguear la operación
     * 2. Validar que la moneda esté soportada
     * 3. Obtener la tasa desde configuración
     * 4. Construir y devolver TasaResponse
     * 
     * 🔍 VALIDACIÓN:
     * Si la moneda no está en config.currency().supported(),
     * lanza IllegalArgumentException que se convierte en HTTP 400.
     * 
     * 📝 EJEMPLO:
     * 
     * Input:  "USD"
     * Output: TasaResponse {
     *           moneda_origen: "PEN",
     *           moneda_destino: "USD",
     *           tasa_cambio: 3.75,
     *           comision_porcentaje: 2.5 (varía por perfil),
     *           proveedor: "PremiumProvider" (varía por perfil),
     *           ambiente: "producción" (varía por perfil)
     *         }
     * 
     * @param monedaDestino Código de la moneda destino (USD, EUR, MXN)
     * @return TasaResponse con la tasa e información contextual
     * @throws IllegalArgumentException Si la moneda no está soportada
     */
    public TasaResponse obtenerTasa(String monedaDestino) {
        // Loguear la operación con el perfil activo
        LOG.infof("Obteniendo tasa para %s en perfil: %s", monedaDestino, activeProfile);

        // VALIDACIÓN 1: Moneda soportada
        // Verifica que la moneda esté en la lista de soportadas
        if (!config.currency().supported().contains(monedaDestino)) {
            throw new IllegalArgumentException("Moneda no soportada: " + monedaDestino);
        }

        // Obtener tasa según la moneda desde configuración
        Double tasa = obtenerTasaPorMoneda(monedaDestino);

        // Construir respuesta con toda la información contextual
        return new TasaResponse(
            config.currency().base(),           // PEN
            monedaDestino,                      // USD, EUR, o MXN
            tasa,                               // 3.75, 4.10, o 0.22
            config.commission().rate(),         // 0.0, 1.5, o 2.5 (según perfil)
            config.provider().name(),           // MockProvider, FreeCurrencyAPI, o PremiumProvider
            config.metadata().environment()     // desarrollo, testing, o producción
        );
    }

    /**
     * Realiza una conversión completa de moneda con todos los cálculos.
     * 
     * 📋 FUNCIONALIDAD:
     * Convierte un monto de PEN a la moneda destino, aplicando:
     * 1. Tasa de cambio
     * 2. Comisión (varía por perfil)
     * 3. Validación de límite transaccional
     * 
     * 🎯 CASO DE USO:
     * Un cliente quiere: "Convertir 1000 PEN a USD"
     * Este método calcula y devuelve:
     * - Cuántos USD obtienes
     * - Cuánto de comisión pagas
     * - El total final
     * - Si estás dentro del límite
     * 
     * 📊 FLUJO DE CÁLCULO:
     * 
     * PASO 1: Validar moneda soportada
     * PASO 2: Obtener tasa desde config
     * PASO 3: Calcular conversión
     *         montoConvertido = monto × tasa
     *         Ejemplo: 1000 × 3.75 = 3750 USD
     * 
     * PASO 4: Calcular comisión
     *         comision = montoConvertido × (rate / 100)
     *         Ejemplo (PROD): 3750 × (2.5 / 100) = 93.75 USD
     * 
     * PASO 5: Calcular total
     *         montoTotal = montoConvertido + comision
     *         Ejemplo: 3750 + 93.75 = 3843.75 USD
     * 
     * PASO 6: Validar límite transaccional
     *         dentroLimite = monto <= limit
     *         Ejemplo (PROD): 1000 <= 50000 → true ✅
     * 
     * 🔍 VALIDACIONES:
     * 
     * 1. Moneda no soportada → IllegalArgumentException (HTTP 400)
     * 2. Límite excedido → Log warning + dentroLimite=false
     *    (NO se rechaza, solo se marca)
     * 
     * 💡 NOTA SOBRE LÍMITES:
     * En este ejercicio, si se excede el límite solo se MARCA en la respuesta.
     * En producción real, probablemente se rechazaría la transacción completa.
     * 
     * 📝 EJEMPLO COMPLETO (PROD):
     * 
     * Input:  monedaDestino="USD", monto=1000.0
     * 
     * Cálculos:
     * - tasa = 3.75 (config)
     * - convertido = 1000 × 3.75 = 3750.0
     * - comision = 3750 × 0.025 = 93.75
     * - total = 3750 + 93.75 = 3843.75
     * - limite = 50000 (config)
     * - dentro = 1000 <= 50000 = true
     * 
     * Output: ConversionResponse {
     *           monto_origen: 1000.0,
     *           moneda_origen: "PEN",
     *           monto_convertido: 3750.0,
     *           moneda_destino: "USD",
     *           tasa_aplicada: 3.75,
     *           comision: 93.75,
     *           monto_total: 3843.75,
     *           proveedor: "PremiumProvider",
     *           limite_transaccional: 50000,
     *           dentro_limite: true
     *         }
     * 
     * @param monedaDestino Código de la moneda destino (USD, EUR, MXN)
     * @param monto Cantidad en PEN a convertir
     * @return ConversionResponse con todos los detalles de la conversión
     * @throws IllegalArgumentException Si la moneda no está soportada
     */
    public ConversionResponse convertirMoneda(String monedaDestino, Double monto) {
        // Loguear la operación detalladamente
        LOG.infof("Convirtiendo %.2f %s a %s", monto, config.currency().base(), monedaDestino);

        // VALIDACIÓN 1: Moneda soportada
        if (!config.currency().supported().contains(monedaDestino)) {
            throw new IllegalArgumentException("Moneda no soportada: " + monedaDestino);
        }

        // PASO 1: Obtener tasa de cambio
        Double tasa = obtenerTasaPorMoneda(monedaDestino);

        // PASO 2: Calcular conversión básica
        // Fórmula: montoConvertido = montoOriginal × tasaCambio
        Double montoConvertido = monto * tasa;

        // PASO 3: Calcular comisión
        // Fórmula: comision = montoConvertido × (porcentaje / 100)
        // 
        // Ejemplo en PROD (rate=2.5):
        // comision = 3750 × (2.5 / 100) = 3750 × 0.025 = 93.75
        //
        // Ejemplo en DEV (rate=0.0):
        // comision = 3750 × (0.0 / 100) = 0.0
        Double comision = montoConvertido * (config.commission().rate() / 100);

        // PASO 4: Calcular monto total
        // Fórmula: total = convertido + comisión
        // El usuario recibe/paga: montoConvertido
        // El banco cobra: comision
        // Total de la operación: montoTotal
        Double montoTotal = montoConvertido + comision;

        // PASO 5: Validar límite transaccional
        // Compara el monto ORIGINAL (en PEN) contra el límite configurado
        Boolean dentroLimite = monto <= config.transaction().limit();

        // Si excede el límite, registrar una advertencia
        if (!dentroLimite) {
            LOG.warnf("Monto %.2f excede el límite transaccional de %d", 
                     monto, config.transaction().limit());
        }

        // PASO 6: Construir respuesta completa
        return new ConversionResponse(
            monto,                          // Monto original en PEN
            config.currency().base(),       // PEN
            montoConvertido,                // Resultado de la conversión
            monedaDestino,                  // USD, EUR, o MXN
            tasa,                           // Tasa utilizada
            comision,                       // Comisión calculada
            montoTotal,                     // Total (conversión + comisión)
            config.provider().name(),       // Proveedor según perfil
            config.transaction().limit(),   // Límite según perfil
            dentroLimite                    // Si cumple el límite
        );
    }

    /**
     * Obtiene un mapa con toda la configuración actual del sistema.
     * 
     * 📋 FUNCIONALIDAD:
     * Expone la configuración completa en un formato JSON amigable
     * para debugging y verificación de ambiente.
     * 
     * 🎯 CASO DE USO:
     * - Verificar en qué perfil está corriendo la app
     * - Debugging: Ver qué configuración está activa
     * - Monitoreo: Endpoints de health/info incluyen esta data
     * - Scripts de prueba: Validar configuración automáticamente
     * 
     * 🔐 SEGURIDAD:
     * NOTA: Este método NO expone el API key por seguridad.
     * Solo muestra la URL del proveedor, pero no las credenciales.
     * 
     * 📊 ESTRUCTURA DE RESPUESTA:
     * 
     * {
     *   "aplicacion": "TasaCorp API",
     *   "perfil_activo": "prod",
     *   "ambiente": "producción",
     *   "proveedor": "PremiumProvider",
     *   "proveedor_url": "https://api.currencylayer.com/live",
     *   "moneda_base": "PEN",
     *   "monedas_soportadas": ["USD", "EUR", "MXN"],
     *   "limite_transaccional": 50000,
     *   "comision_porcentaje": 2.5,
     *   "cache_habilitado": true,
     *   "auditoria_habilitada": true,
     *   "refresh_minutos": 15
     * }
     * 
     * 💡 USO EN SCRIPTS DE PRUEBA:
     * Los scripts test-part1-config.sh y test-part2-profiles.sh
     * consultan este endpoint para validar que la configuración
     * sea la correcta según el perfil activo.
     * 
     * @return Map con toda la configuración visible del sistema
     */
    public Map<String, Object> obtenerConfiguracion() {
        // Loguear la consulta con el perfil actual
        LOG.infof("Obteniendo configuración para perfil: %s", activeProfile);

        // Crear mapa para almacenar toda la configuración
        Map<String, Object> configMap = new HashMap<>();
        
        // INFORMACIÓN GENERAL
        configMap.put("aplicacion", appName);
        configMap.put("perfil_activo", activeProfile);
        configMap.put("ambiente", config.metadata().environment());
        
        // CONFIGURACIÓN DEL PROVEEDOR
        configMap.put("proveedor", config.provider().name());
        configMap.put("proveedor_url", config.provider().url());
        // NOTA: NO incluimos el API key por seguridad
        
        // CONFIGURACIÓN DE MONEDAS
        configMap.put("moneda_base", config.currency().base());
        configMap.put("monedas_soportadas", config.currency().supported());
        
        // CONFIGURACIÓN DE TRANSACCIONES Y COMISIONES
        configMap.put("limite_transaccional", config.transaction().limit());
        configMap.put("comision_porcentaje", config.commission().rate());
        
        // CONFIGURACIÓN DE CARACTERÍSTICAS
        configMap.put("cache_habilitado", config.features().cacheEnabled());
        configMap.put("auditoria_habilitada", config.features().auditEnabled());
        configMap.put("refresh_minutos", config.features().rateRefreshMinutes());
        
        return configMap;
    }

    // ========================================================================
    // MÉTODOS PRIVADOS - Utilidades Internas
    // ========================================================================

    /**
     * Método privado para obtener la tasa según la moneda.
     * 
     * 📋 FUNCIONALIDAD:
     * Resuelve qué tasa corresponde a cada moneda desde la configuración.
     * 
     * 💡 SWITCH EXPRESSION (Java 14+):
     * Este es un switch moderno que DEVUELVE un valor directamente.
     * 
     * Sintaxis clásica (Java < 14):
     * <pre>
     * Double tasa;
     * switch (moneda) {
     *     case "USD":
     *         tasa = config.exchange().rates().usd();
     *         break;
     *     case "EUR":
     *         tasa = config.exchange().rates().eur();
     *         break;
     *     case "MXN":
     *         tasa = config.exchange().rates().mxn();
     *         break;
     *     default:
     *         throw new IllegalArgumentException("...");
     * }
     * return tasa;
     * </pre>
     * 
     * Sintaxis moderna (Java 14+):
     * <pre>
     * return switch (moneda) {
     *     case "USD" -> config.exchange().rates().usd();
     *     case "EUR" -> config.exchange().rates().eur();
     *     case "MXN" -> config.exchange().rates().mxn();
     *     default -> throw new IllegalArgumentException("...");
     * };
     * </pre>
     * 
     * VENTAJAS:
     * ✅ Más conciso
     * ✅ Retorno directo
     * ✅ No hay break (automático)
     * ✅ Exhaustividad verificada por compilador
     * 
     * 🔗 MAPEO DE TASAS:
     * 
     * Moneda → Propiedad de configuración
     * ----------------------------------------
     * "USD"  → tasacorp.exchange.rates.usd (3.75)
     * "EUR"  → tasacorp.exchange.rates.eur (4.10)
     * "MXN"  → tasacorp.exchange.rates.mxn (0.22)
     * 
     * 📝 EJEMPLO:
     * 
     * Input:  "USD"
     * Output: 3.75
     * 
     * Input:  "EUR"
     * Output: 4.10
     * 
     * Input:  "JPY"
     * Output: IllegalArgumentException (no configurada)
     * 
     * @param moneda Código de la moneda (USD, EUR, MXN)
     * @return La tasa de cambio configurada para esa moneda
     * @throws IllegalArgumentException Si la moneda no está configurada
     */
    private Double obtenerTasaPorMoneda(String moneda) {
        return switch (moneda) {
            case "USD" -> config.exchange().rates().usd();
            case "EUR" -> config.exchange().rates().eur();
            case "MXN" -> config.exchange().rates().mxn();
            default -> throw new IllegalArgumentException("Moneda no configurada: " + moneda);
        };
    }
}