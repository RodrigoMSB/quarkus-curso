package pe.banco.tasacorp.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO (Data Transfer Object) para la respuesta de consulta de tasa de cambio.
 * 
 * 📋 PROPÓSITO:
 * Este objeto representa la respuesta de una consulta simple de tasa de cambio,
 * SIN realizar ninguna conversión. Solo informa cuál es la tasa actual disponible.
 * 
 * 🎯 DIFERENCIA CON ConversionResponse:
 * - TasaResponse: "¿Cuál es la tasa USD?" → Solo devuelve la tasa (3.75)
 * - ConversionResponse: "Convertir 1000 PEN a USD" → Hace cálculos y devuelve montos
 * 
 * 🎯 CASO DE USO:
 * Endpoint: GET /api/tasas/USD
 * 
 * El usuario pregunta: "¿A cuánto está el dólar hoy?"
 * Este DTO responde con:
 * - Tasa actual: 3.75 PEN por 1 USD
 * - Comisión que se aplicaría: 2.5%
 * - Proveedor de la información: PremiumProvider
 * - Ambiente desde donde se consulta: producción
 * 
 * 💡 USO REAL:
 * Un cliente puede consultar la tasa ANTES de hacer una conversión para decidir
 * si le conviene operar o esperar a una mejor tasa.
 * 
 * 📊 EJEMPLO DE RESPUESTA JSON:
 * {
 *   "moneda_origen": "PEN",
 *   "moneda_destino": "USD",
 *   "tasa_cambio": 3.75,
 *   "comision_porcentaje": 2.5,
 *   "proveedor": "PremiumProvider",
 *   "ambiente": "producción"
 * }
 * 
 * 🔗 RELACIÓN CON CONFIGURACIÓN:
 * Los valores dependen del perfil activo:
 * 
 * DEV:
 *   - tasa_cambio: 3.75 (desde application.yaml)
 *   - comision_porcentaje: 0.0
 *   - proveedor: MockProvider
 *   - ambiente: desarrollo
 * 
 * TEST:
 *   - tasa_cambio: 3.75 (desde application.yaml)
 *   - comision_porcentaje: 1.5
 *   - proveedor: FreeCurrencyAPI
 *   - ambiente: testing
 * 
 * PROD:
 *   - tasa_cambio: 3.75 (desde application.yaml)
 *   - comision_porcentaje: 2.5
 *   - proveedor: PremiumProvider
 *   - ambiente: producción
 * 
 * @author Arquitectura TasaCorp
 * @version 1.0.0
 * @see TasaService#obtenerTasa(String) Método que construye este DTO
 * @see ConversionResponse Para conversiones completas con cálculos
 */
public class TasaResponse {

    // ========================================================================
    // ATRIBUTOS - Información de la Tasa
    // ========================================================================

    /**
     * Moneda de origen (base).
     * En TasaCorp siempre es PEN (Nuevo Sol Peruano).
     * 
     * Este valor viene de: tasacorp.currency.base
     */
    @JsonProperty("moneda_origen")
    private String monedaOrigen;

    /**
     * Moneda de destino consultada.
     * Puede ser: USD, EUR, MXN (según tasacorp.currency.supported)
     * 
     * EJEMPLO:
     * Si el usuario consulta GET /api/tasas/USD
     * Entonces: monedaDestino = "USD"
     */
    @JsonProperty("moneda_destino")
    private String monedaDestino;

    /**
     * Tasa de cambio actual para esta moneda.
     * 
     * SIGNIFICADO: Cuántos PEN necesitas para obtener 1 unidad de la moneda destino.
     * 
     * EJEMPLO:
     * tasaCambio = 3.75 significa:
     * - 1 USD = 3.75 PEN
     * - Para comprar 1 dólar necesitas 3.75 soles
     * 
     * Este valor proviene de la configuración:
     * - application.yaml → tasacorp.exchange.rates.usd = 3.75
     */
    @JsonProperty("tasa_cambio")
    private Double tasaCambio;

    /**
     * Porcentaje de comisión que se aplicaría en una conversión.
     * 
     * IMPORTANTE: Este DTO NO cobra comisión (es solo consulta),
     * pero INFORMA cuál sería la comisión si el usuario decidiera convertir.
     * 
     * VARÍA SEGÚN PERFIL:
     * - DEV:  0.0% (gratis para desarrollo)
     * - TEST: 1.5% (moderado para pruebas)
     * - PROD: 2.5% (completo para producción)
     * 
     * Proviene de: tasacorp.commission.rate
     */
    @JsonProperty("comision_porcentaje")
    private Double comisionPorcentaje;

    /**
     * Nombre del proveedor de tasas de cambio.
     * 
     * Indica de dónde proviene la información de tasas.
     * 
     * VARÍA SEGÚN PERFIL:
     * - DEV:  MockProvider (simulado para desarrollo)
     * - TEST: FreeCurrencyAPI (API gratuita de pruebas)
     * - PROD: PremiumProvider (API premium de producción)
     * 
     * Proviene de: tasacorp.provider.name
     */
    @JsonProperty("proveedor")
    private String proveedor;

    /**
     * Ambiente desde el cual se está consultando.
     * 
     * Ayuda al cliente a entender en qué contexto está operando.
     * 
     * VALORES POSIBLES:
     * - "desarrollo" (perfil DEV)
     * - "testing" (perfil TEST)
     * - "producción" (perfil PROD)
     * 
     * Proviene de: tasacorp.metadata.environment
     * 
     * 💡 UTILIDAD:
     * Si un cliente ve "desarrollo", sabe que está en un ambiente de pruebas
     * y que las tasas/comisiones no son reales.
     */
    @JsonProperty("ambiente")
    private String ambiente;

    // ========================================================================
    // CONSTRUCTORES
    // ========================================================================

    /**
     * Constructor vacío (sin argumentos).
     * 
     * 📌 REQUERIDO POR JACKSON:
     * Necesario para que Jackson pueda deserializar JSON a objetos Java.
     * 
     * Aunque en este caso solo DEVOLVEMOS JSON (no recibimos), es buena
     * práctica incluirlo para mantener compatibilidad bidireccional.
     */
    public TasaResponse() {
    }

    /**
     * Constructor completo con todos los parámetros.
     * 
     * 📌 USO PRINCIPAL:
     * Se utiliza en TasaService.obtenerTasa(String moneda) para crear
     * la respuesta después de obtener la tasa desde la configuración.
     * 
     * EJEMPLO DE USO EN EL SERVICIO:
     * <pre>
     * return new TasaResponse(
     *     config.currency().base(),           // "PEN"
     *     monedaDestino,                      // "USD"
     *     obtenerTasaPorMoneda(monedaDestino), // 3.75
     *     config.commission().rate(),          // 2.5
     *     config.provider().name(),            // "PremiumProvider"
     *     config.metadata().environment()      // "producción"
     * );
     * </pre>
     * 
     * @param monedaOrigen        Moneda base (PEN)
     * @param monedaDestino       Moneda destino consultada (USD, EUR, MXN)
     * @param tasaCambio          Tasa de cambio actual
     * @param comisionPorcentaje  Porcentaje de comisión informativo
     * @param proveedor           Nombre del proveedor de tasas
     * @param ambiente            Ambiente de ejecución
     */
    public TasaResponse(String monedaOrigen, String monedaDestino, Double tasaCambio, 
                       Double comisionPorcentaje, String proveedor, String ambiente) {
        this.monedaOrigen = monedaOrigen;
        this.monedaDestino = monedaDestino;
        this.tasaCambio = tasaCambio;
        this.comisionPorcentaje = comisionPorcentaje;
        this.proveedor = proveedor;
        this.ambiente = ambiente;
    }

    // ========================================================================
    // GETTERS Y SETTERS
    // ========================================================================

    /**
     * 💡 ¿POR QUÉ GETTERS Y SETTERS?
     * 
     * SERIALIZACIÓN (Java → JSON):
     * Cuando Quarkus devuelve este objeto como respuesta REST,
     * Jackson llama a los getters para construir el JSON.
     * 
     * FLUJO:
     * 1. TasaService crea: TasaResponse response = new TasaResponse(...)
     * 2. TasaResource devuelve: return Response.ok(response).build()
     * 3. Jackson serializa:
     *    - Llama a getMonedaOrigen() → "moneda_origen": "PEN"
     *    - Llama a getTasaCambio() → "tasa_cambio": 3.75
     *    - Etc.
     * 4. Cliente recibe el JSON completo
     */

    /**
     * @return La moneda de origen (PEN)
     */
    public String getMonedaOrigen() {
        return monedaOrigen;
    }

    /**
     * @param monedaOrigen La moneda origen a establecer
     */
    public void setMonedaOrigen(String monedaOrigen) {
        this.monedaOrigen = monedaOrigen;
    }

    /**
     * @return La moneda de destino consultada (USD, EUR, MXN)
     */
    public String getMonedaDestino() {
        return monedaDestino;
    }

    /**
     * @param monedaDestino La moneda destino a establecer
     */
    public void setMonedaDestino(String monedaDestino) {
        this.monedaDestino = monedaDestino;
    }

    /**
     * @return La tasa de cambio actual
     */
    public Double getTasaCambio() {
        return tasaCambio;
    }

    /**
     * @param tasaCambio La tasa de cambio a establecer
     */
    public void setTasaCambio(Double tasaCambio) {
        this.tasaCambio = tasaCambio;
    }

    /**
     * @return El porcentaje de comisión (informativo)
     */
    public Double getComisionPorcentaje() {
        return comisionPorcentaje;
    }

    /**
     * @param comisionPorcentaje El porcentaje de comisión a establecer
     */
    public void setComisionPorcentaje(Double comisionPorcentaje) {
        this.comisionPorcentaje = comisionPorcentaje;
    }

    /**
     * @return El nombre del proveedor de tasas
     */
    public String getProveedor() {
        return proveedor;
    }

    /**
     * @param proveedor El proveedor a establecer
     */
    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    /**
     * @return El ambiente de ejecución (desarrollo, testing, producción)
     */
    public String getAmbiente() {
        return ambiente;
    }

    /**
     * @param ambiente El ambiente a establecer
     */
    public void setAmbiente(String ambiente) {
        this.ambiente = ambiente;
    }
}
