package pe.banco.tasacorp.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

import java.util.List;

/**
 * Configuración principal de TasaCorp usando @ConfigMapping.
 * 
 * 📋 PROPÓSITO:
 * Esta interfaz mapea automáticamente las propiedades de configuración desde
 * application.properties y application.yaml a un objeto Java type-safe.
 * 
 * 💡 ¿POR QUÉ UNA INTERFAZ Y NO UNA CLASE?
 * @ConfigMapping trabaja con interfaces porque:
 * 
 * 1. INMUTABILIDAD: Las interfaces garantizan que la configuración no cambie
 *    en runtime (no hay setters, solo getters).
 * 
 * 2. MENOS CÓDIGO: No necesitas escribir implementaciones, Quarkus las genera
 *    automáticamente en tiempo de compilación.
 * 
 * 3. TYPE-SAFE: Si una propiedad falta o tiene el tipo incorrecto, el error
 *    aparece al ARRANCAR la aplicación, no en runtime.
 * 
 * 🎯 VENTAJAS VS @ConfigProperty:
 * 
 * @ConfigProperty (individual):
 * <pre>
 * @ConfigProperty(name = "tasacorp.currency.base")
 * String base;
 * 
 * @ConfigProperty(name = "tasacorp.currency.supported")
 * List<String> supported;
 * </pre>
 * 
 * @ConfigMapping (agrupado):
 * <pre>
 * @Inject
 * TasaCorpConfig config;
 * 
 * String base = config.currency().base();
 * List<String> supported = config.currency().supported();
 * </pre>
 * 
 * Beneficios:
 * ✅ Organización jerárquica clara
 * ✅ Navegación tipo IDE (autocompletado)
 * ✅ Validación en tiempo de compilación
 * ✅ Menos repetición de prefijos
 * 
 * 🔗 MAPEO CON ARCHIVOS DE CONFIGURACIÓN:
 * 
 * Esta interfaz con prefix = "tasacorp" mapea propiedades como:
 * 
 * application.properties:
 * tasacorp.currency.base=PEN
 * tasacorp.currency.supported=USD,EUR,MXN
 * tasacorp.transaction.limit=1000
 * 
 * application.yaml:
 * tasacorp:
 *   currency:
 *     base: PEN
 *     supported:
 *       - USD
 *       - EUR
 *       - MXN
 *   transaction:
 *     limit: 1000
 * 
 * 📊 ESTRUCTURA JERÁRQUICA:
 * 
 * TasaCorpConfig (raíz: tasacorp.*)
 * ├── Currency (tasacorp.currency.*)
 * │   ├── base
 * │   └── supported
 * ├── Transaction (tasacorp.transaction.*)
 * │   └── limit
 * ├── Provider (tasacorp.provider.*)
 * │   ├── name
 * │   ├── url
 * │   └── apikey
 * ├── Commission (tasacorp.commission.*)
 * │   └── rate
 * ├── Exchange (tasacorp.exchange.*)
 * │   └── Rates (tasacorp.exchange.rates.*)
 * │       ├── usd
 * │       ├── eur
 * │       └── mxn
 * ├── Features (tasacorp.features.*)
 * │   ├── cache-enabled
 * │   ├── rate-refresh-minutes
 * │   └── audit-enabled
 * └── Metadata (tasacorp.metadata.*)
 *     ├── created-by
 *     ├── environment
 *     └── supported-profiles
 * 
 * 🎭 VALORES POR PERFIL:
 * Las propiedades pueden variar según el perfil activo:
 * 
 * DEV:
 *   transaction.limit = 999999
 *   commission.rate = 0.0
 *   provider.name = MockProvider
 *   features.cache-enabled = false
 * 
 * TEST:
 *   transaction.limit = 1000
 *   commission.rate = 1.5
 *   provider.name = FreeCurrencyAPI
 *   features.cache-enabled = true
 * 
 * PROD:
 *   transaction.limit = 50000
 *   commission.rate = 2.5
 *   provider.name = PremiumProvider
 *   features.cache-enabled = true
 * 
 * @author Arquitectura TasaCorp
 * @version 1.0.0
 * @see TasaService Para el uso de esta configuración
 */
@ConfigMapping(prefix = "tasacorp")
public interface TasaCorpConfig {

    // ========================================================================
    // MÉTODOS RAÍZ - Acceso a Sub-Configuraciones
    // ========================================================================
    
    /**
     * Configuración de monedas.
     * 
     * Mapea: tasacorp.currency.*
     * 
     * CONTIENE:
     * - Moneda base del sistema (PEN)
     * - Lista de monedas soportadas (USD, EUR, MXN)
     * 
     * @return La configuración de monedas
     */
    Currency currency();
    
    /**
     * Configuración de transacciones.
     * 
     * Mapea: tasacorp.transaction.*
     * 
     * CONTIENE:
     * - Límite máximo transaccional
     * 
     * VARÍA POR PERFIL:
     * - DEV: 999,999 (ilimitado para desarrollo)
     * - TEST: 1,000 (bajo para pruebas)
     * - PROD: 50,000 (alto para producción)
     * 
     * @return La configuración de transacciones
     */
    Transaction transaction();
    
    /**
     * Configuración del proveedor de tasas.
     * 
     * Mapea: tasacorp.provider.*
     * 
     * CONTIENE:
     * - Nombre del proveedor
     * - URL del servicio
     * - API Key (en PROD viene desde Vault)
     * 
     * VARÍA POR PERFIL:
     * - DEV: MockProvider + localhost
     * - TEST: FreeCurrencyAPI + API de pruebas
     * - PROD: PremiumProvider + API real + Vault
     * 
     * @return La configuración del proveedor
     */
    Provider provider();
    
    /**
     * Configuración de comisiones.
     * 
     * Mapea: tasacorp.commission.*
     * 
     * CONTIENE:
     * - Porcentaje de comisión por operación
     * 
     * VARÍA POR PERFIL:
     * - DEV: 0.0% (gratis para desarrollo)
     * - TEST: 1.5% (moderado para pruebas)
     * - PROD: 2.5% (completo para producción)
     * 
     * @return La configuración de comisiones
     */
    Commission commission();
    
    /**
     * Configuración de tasas de cambio.
     * 
     * Mapea: tasacorp.exchange.*
     * 
     * CONTIENE:
     * - Tasas de cambio para cada moneda soportada
     * 
     * NOTA: Estas tasas son HARDCODED en application.yaml para el ejercicio.
     * En producción real, vendrían de un servicio externo en tiempo real.
     * 
     * @return La configuración de tasas de cambio
     */
    Exchange exchange();
    
    /**
     * Configuración de características (features).
     * 
     * Mapea: tasacorp.features.*
     * 
     * CONTIENE:
     * - Flags de funcionalidades activadas/desactivadas
     * - Configuraciones de comportamiento del sistema
     * 
     * VARÍA POR PERFIL para optimizar cada ambiente.
     * 
     * @return La configuración de características
     */
    Features features();
    
    /**
     * Metadatos de la aplicación.
     * 
     * Mapea: tasacorp.metadata.*
     * 
     * CONTIENE:
     * - Información descriptiva del sistema
     * - Ambiente de ejecución
     * - Perfiles soportados
     * 
     * @return Los metadatos de la aplicación
     */
    Metadata metadata();

    // ========================================================================
    // SUB-INTERFACES - Configuraciones Específicas
    // ========================================================================

    /**
     * Configuración de monedas del sistema.
     * 
     * 📋 PROPÓSITO:
     * Define qué monedas maneja el sistema y cuál es la base.
     * 
     * 🔗 MAPEO:
     * tasacorp.currency.base → base()
     * tasacorp.currency.supported → supported()
     * 
     * 📊 EJEMPLO EN YAML:
     * <pre>
     * tasacorp:
     *   currency:
     *     base: PEN
     *     supported:
     *       - USD
     *       - EUR
     *       - MXN
     * </pre>
     */
    interface Currency {
        /**
         * Moneda base del sistema.
         * 
         * En TasaCorp: PEN (Nuevo Sol Peruano)
         * 
         * Esta es la moneda desde la cual se hacen las conversiones.
         * 
         * @return El código de la moneda base (ISO 4217)
         */
        String base();
        
        /**
         * Lista de monedas soportadas para conversión.
         * 
         * En TasaCorp: USD, EUR, MXN
         * 
         * Solo estas monedas pueden ser destino de conversión.
         * Si un usuario pide otra moneda, se rechaza con error.
         * 
         * @return Lista de códigos de monedas soportadas
         */
        List<String> supported();
    }

    /**
     * Configuración de límites transaccionales.
     * 
     * 📋 PROPÓSITO:
     * Define el monto máximo permitido por transacción.
     * 
     * 🔗 MAPEO:
     * tasacorp.transaction.limit → limit()
     * 
     * ⚠️ VARÍA POR PERFIL:
     * %dev.tasacorp.transaction.limit=999999
     * %test.tasacorp.transaction.limit=1000
     * %prod.tasacorp.transaction.limit=50000
     * 
     * 💡 USO:
     * En el servicio se valida:
     * if (monto > config.transaction().limit()) {
     *     // Marcar como fuera de límite
     * }
     */
    interface Transaction {
        /**
         * Límite máximo por transacción.
         * 
         * INTERPRETACIÓN:
         * - Valor en la moneda base (PEN)
         * - Transacciones mayores a este valor se marcan como "fuera de límite"
         * 
         * VALORES SEGÚN PERFIL:
         * - DEV: 999,999 (sin restricciones para desarrollo)
         * - TEST: 1,000 (bajo para facilitar pruebas de límites)
         * - PROD: 50,000 (alto pero controlado para producción)
         * 
         * @return El límite transaccional en PEN
         */
        Integer limit();
    }

    /**
     * Configuración del proveedor de tasas de cambio.
     * 
     * 📋 PROPÓSITO:
     * Define qué servicio externo proporciona las tasas de cambio.
     * 
     * 🔗 MAPEO:
     * tasacorp.provider.name → name()
     * tasacorp.provider.url → url()
     * tasacorp.provider.apikey → apikey()
     * 
     * 🔐 SEGURIDAD:
     * En PROD, el apikey NO está en properties, sino que se obtiene desde Vault:
     * %prod.tasacorp.provider.apikey=${api-key}
     * 
     * El valor ${api-key} se resuelve automáticamente desde HashiCorp Vault.
     */
    interface Provider {
        /**
         * Nombre del proveedor de tasas.
         * 
         * VALORES SEGÚN PERFIL:
         * - DEV: MockProvider (simulado)
         * - TEST: FreeCurrencyAPI (API gratuita)
         * - PROD: PremiumProvider (API de pago)
         * 
         * @return El nombre del proveedor
         */
        String name();
        
        /**
         * URL del servicio del proveedor.
         * 
         * VALORES SEGÚN PERFIL:
         * - DEV: http://localhost:8080/mock
         * - TEST: https://api.freecurrencyapi.com/v1
         * - PROD: https://api.currencylayer.com/live
         * 
         * @return La URL del servicio
         */
        String url();
        
        /**
         * API Key para autenticación con el proveedor.
         * 
         * 🔐 SEGURIDAD CRÍTICA:
         * 
         * DEV/TEST: Valor hardcoded (no importa, es ambiente de pruebas)
         * %dev.tasacorp.provider.apikey=DEV_NO_API_KEY_NEEDED
         * 
         * PROD: Valor desde Vault (NUNCA en properties)
         * %prod.tasacorp.provider.apikey=${api-key}
         * 
         * Quarkus lee ${api-key} desde Vault automáticamente usando:
         * %prod.quarkus.vault.secret-config-kv-path=tasacorp
         * 
         * @return La API key del proveedor
         */
        String apikey();
    }

    /**
     * Configuración de comisiones.
     * 
     * 📋 PROPÓSITO:
     * Define el porcentaje de comisión cobrado por cada conversión.
     * 
     * 🔗 MAPEO:
     * tasacorp.commission.rate → rate()
     * 
     * 💰 CÁLCULO:
     * comision = montoConvertido × (rate / 100)
     * 
     * Ejemplo con rate = 2.5:
     * - Convertido: 3750 USD
     * - Comisión: 3750 × 0.025 = 93.75 USD
     * - Total: 3843.75 USD
     */
    interface Commission {
        /**
         * Porcentaje de comisión.
         * 
         * VALORES SEGÚN PERFIL:
         * - DEV: 0.0% (gratis para no complicar desarrollo)
         * - TEST: 1.5% (moderado para pruebas realistas)
         * - PROD: 2.5% (comisión real de producción)
         * 
         * INTERPRETACIÓN:
         * - 2.5 significa 2.5% (no 250%)
         * - Se divide entre 100 al calcular
         * 
         * @return El porcentaje de comisión
         */
        Double rate();
    }

    /**
     * Configuración de tasas de cambio.
     * 
     * 📋 PROPÓSITO:
     * Define las tasas de conversión para cada moneda.
     * 
     * 🔗 MAPEO:
     * tasacorp.exchange.rates → rates()
     * 
     * ⚠️ NOTA IMPORTANTE:
     * Estas tasas están HARDCODED en application.yaml solo para el ejercicio.
     * En un sistema real de producción, se obtendrían de un servicio externo
     * en tiempo real (usando el Provider configurado).
     */
    interface Exchange {
        /**
         * Acceso a las tasas específicas por moneda.
         * 
         * @return La configuración de tasas
         */
        Rates rates();
        
        /**
         * Tasas de cambio específicas por moneda.
         * 
         * 📊 INTERPRETACIÓN DE LAS TASAS:
         * 
         * usd() = 3.75 significa:
         * - 1 USD = 3.75 PEN
         * - Para comprar 1 dólar necesitas 3.75 soles
         * 
         * eur() = 4.10 significa:
         * - 1 EUR = 4.10 PEN
         * - Para comprar 1 euro necesitas 4.10 soles
         * 
         * mxn() = 0.22 significa:
         * - 1 MXN = 0.22 PEN
         * - Para comprar 1 peso mexicano necesitas 0.22 soles
         * 
         * 🔗 MAPEO EN YAML:
         * <pre>
         * tasacorp:
         *   exchange:
         *     rates:
         *       usd: 3.75
         *       eur: 4.10
         *       mxn: 0.22
         * </pre>
         */
        interface Rates {
            /**
             * Tasa de cambio PEN → USD.
             * 
             * @WithDefault: Si no se configura, usa 3.75 por defecto.
             * 
             * @return Cuántos PEN equivalen a 1 USD
             */
            @WithDefault("3.75")
            Double usd();
            
            /**
             * Tasa de cambio PEN → EUR.
             * 
             * @WithDefault: Si no se configura, usa 4.10 por defecto.
             * 
             * @return Cuántos PEN equivalen a 1 EUR
             */
            @WithDefault("4.10")
            Double eur();
            
            /**
             * Tasa de cambio PEN → MXN.
             * 
             * @WithDefault: Si no se configura, usa 0.22 por defecto.
             * 
             * @return Cuántos PEN equivalen a 1 MXN
             */
            @WithDefault("0.22")
            Double mxn();
        }
    }

    /**
     * Configuración de características del sistema.
     * 
     * 📋 PROPÓSITO:
     * Feature flags y configuraciones de comportamiento que pueden
     * activarse/desactivarse según el ambiente.
     * 
     * 🎯 ESTRATEGIA:
     * Cada ambiente optimiza estas características para su propósito:
     * - DEV: Máxima velocidad de desarrollo
     * - TEST: Balance entre realismo y control
     * - PROD: Máximo rendimiento y seguridad
     */
    interface Features {
        /**
         * Indica si el caché está habilitado.
         * 
         * 💡 ¿QUÉ HACE EL CACHÉ?
         * Almacena temporalmente las tasas de cambio para no consultar
         * el proveedor externo en cada request.
         * 
         * VALORES SEGÚN PERFIL:
         * - DEV: false (cambios inmediatos al desarrollar)
         * - TEST: true (simular comportamiento real)
         * - PROD: true (reducir latencia y costos de API)
         * 
         * 📌 @WithName("cache-enabled"):
         * En properties se escribe: tasacorp.features.cache-enabled
         * En Java se accede: cacheEnabled()
         * 
         * La anotación permite usar kebab-case en config y camelCase en Java.
         * 
         * @return true si el caché está activo, false si no
         */
        @WithName("cache-enabled")
        @WithDefault("false")
        Boolean cacheEnabled();
        
        /**
         * Minutos de duración del caché de tasas.
         * 
         * 💡 SIGNIFICADO:
         * Cada cuántos minutos se refresca la tasa desde el proveedor.
         * 
         * VALORES SEGÚN PERFIL:
         * - DEV: 60 min (default, pero cache desactivado)
         * - TEST: 30 min (refresco moderado)
         * - PROD: 15 min (tasas más actualizadas)
         * 
         * EJEMPLO:
         * Si es 15 minutos:
         * - 10:00 AM → Consulta al proveedor (3.75)
         * - 10:05 AM → Devuelve 3.75 del caché
         * - 10:10 AM → Devuelve 3.75 del caché
         * - 10:15 AM → Refresca desde proveedor (podría ser 3.76)
         * 
         * @return Minutos de vigencia del caché
         */
        @WithName("rate-refresh-minutes")
        @WithDefault("60")
        Integer rateRefreshMinutes();
        
        /**
         * Indica si la auditoría está habilitada.
         * 
         * 💡 ¿QUÉ HACE LA AUDITORÍA?
         * Registra todas las operaciones realizadas para:
         * - Compliance regulatorio
         * - Trazabilidad de transacciones
         * - Detección de fraudes
         * - Análisis posterior
         * 
         * VALORES SEGÚN PERFIL:
         * - DEV: false (no contaminar logs con datos de prueba)
         * - TEST: true (validar que funciona)
         * - PROD: true (obligatorio para cumplimiento)
         * 
         * @return true si debe auditar operaciones, false si no
         */
        @WithName("audit-enabled")
        @WithDefault("true")
        Boolean auditEnabled();
    }

    /**
     * Metadatos descriptivos de la aplicación.
     * 
     * 📋 PROPÓSITO:
     * Información descriptiva que ayuda a identificar el ambiente
     * y contexto de ejecución.
     * 
     * 💡 UTILIDAD:
     * - Debugging: Saber en qué ambiente estás
     * - Logs: Identificar el origen de los logs
     * - Monitoreo: Agrupar métricas por ambiente
     */
    interface Metadata {
        /**
         * Identifica quién creó/mantiene esta configuración.
         * 
         * @WithDefault: Si no se configura, usa "TasaCorp".
         * 
         * @return El nombre del equipo o proyecto
         */
        @WithName("created-by")
        @WithDefault("TasaCorp")
        String createdBy();
        
        /**
         * Describe el ambiente de ejecución actual.
         * 
         * VALORES SEGÚN PERFIL:
         * - DEV: "desarrollo"
         * - TEST: "testing"
         * - PROD: "producción"
         * 
         * 💡 USO:
         * Se incluye en los DTOs de respuesta para que el cliente
         * sepa en qué ambiente está operando.
         * 
         * @return El nombre del ambiente
         */
        @WithName("environment")
        @WithDefault("unknown")
        String environment();
        
        /**
         * Lista de perfiles soportados por la aplicación.
         * 
         * @WithDefault: Si no se configura, usa "dev,test,prod".
         * 
         * NOTA: Esto es informativo, no limita qué perfiles pueden usarse.
         * 
         * @return Lista de nombres de perfiles soportados
         */
        @WithName("supported-profiles")
        @WithDefault("dev,test,prod")
        List<String> supportedProfiles();
    }
}