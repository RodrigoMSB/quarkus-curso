package pe.banco.tasacorp.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO (Data Transfer Object) para la respuesta de conversión de moneda.
 * 
 * 📋 PROPÓSITO:
 * Este objeto representa la respuesta completa de una operación de conversión
 * de tasas de cambio. Contiene toda la información que el cliente necesita
 * para entender el resultado de su conversión.
 * 
 * 🎯 CASO DE USO:
 * Cuando un usuario solicita: "Convertir 1000 PEN a USD"
 * Este DTO devuelve:
 * - El cálculo de la conversión (1000 * 3.75 = 3750 USD)
 * - La comisión aplicada (varía según perfil: 0%, 1.5%, 2.5%)
 * - El total final (conversión + comisión)
 * - Validación de límites transaccionales
 * 
 * 💡 ¿POR QUÉ @JsonProperty?
 * En Java usamos camelCase (montoOrigen), pero en JSON es estándar usar snake_case (monto_origen).
 * La anotación @JsonProperty hace la conversión automáticamente:
 * 
 * Java:  montoOrigen = 1000.0
 *   ↓
 * JSON:  "monto_origen": 1000.0
 * 
 * Esto mantiene las convenciones de nomenclatura de cada lenguaje.
 * 
 * 📊 EJEMPLO DE RESPUESTA JSON:
 * {
 *   "monto_origen": 1000.0,
 *   "moneda_origen": "PEN",
 *   "monto_convertido": 3750.0,
 *   "moneda_destino": "USD",
 *   "tasa_aplicada": 3.75,
 *   "comision": 93.75,
 *   "monto_total": 3843.75,
 *   "proveedor": "PremiumProvider",
 *   "limite_transaccional": 50000,
 *   "dentro_limite": true
 * }
 * 
 * 🔗 RELACIÓN CON CONFIGURACIÓN:
 * Los valores de este DTO dependen del perfil activo:
 * - DEV:  comision=0.0,    limite_transaccional=999999
 * - TEST: comision=56.25,  limite_transaccional=1000
 * - PROD: comision=93.75,  limite_transaccional=50000
 * 
 * @author Arquitectura TasaCorp
 * @version 1.0.0
 * @see TasaService Para la lógica que construye este DTO
 */
public class ConversionResponse {

    // ========================================================================
    // ATRIBUTOS - Información de la Conversión
    // ========================================================================

    /**
     * Monto original ingresado por el usuario.
     * Ejemplo: 1000.0 PEN
     */
    @JsonProperty("monto_origen")
    private Double montoOrigen;

    /**
     * Moneda del monto original.
     * Siempre es la moneda base configurada (PEN en TasaCorp).
     */
    @JsonProperty("moneda_origen")
    private String monedaOrigen;

    /**
     * Monto resultante de la conversión (ANTES de comisión).
     * Cálculo: montoOrigen × tasaAplicada
     * Ejemplo: 1000 × 3.75 = 3750.0 USD
     */
    @JsonProperty("monto_convertido")
    private Double montoConvertido;

    /**
     * Moneda de destino solicitada por el usuario.
     * Puede ser: USD, EUR, MXN (según tasacorp.currency.supported)
     */
    @JsonProperty("moneda_destino")
    private String monedaDestino;

    /**
     * Tasa de cambio utilizada para la conversión.
     * Proviene de la configuración en application.yaml
     * Ejemplo: tasacorp.exchange.rates.usd = 3.75
     */
    @JsonProperty("tasa_aplicada")
    private Double tasaAplicada;

    /**
     * Comisión cobrada por la operación.
     * Cálculo: montoConvertido × (tasacorp.commission.rate / 100)
     * 
     * VARÍA SEGÚN PERFIL:
     * - DEV:  0.0% → comision = 0.00 USD
     * - TEST: 1.5% → comision = 56.25 USD (sobre 3750)
     * - PROD: 2.5% → comision = 93.75 USD (sobre 3750)
     */
    @JsonProperty("comision")
    private Double comision;

    /**
     * Monto final que el usuario debe pagar/recibir.
     * Cálculo: montoConvertido + comision
     * Ejemplo: 3750.0 + 93.75 = 3843.75 USD
     */
    @JsonProperty("monto_total")
    private Double montoTotal;

    /**
     * Nombre del proveedor de tasas de cambio.
     * Proviene de: tasacorp.provider.name
     * 
     * VARÍA SEGÚN PERFIL:
     * - DEV:  MockProvider
     * - TEST: FreeCurrencyAPI
     * - PROD: PremiumProvider
     */
    @JsonProperty("proveedor")
    private String proveedor;

    /**
     * Límite transaccional configurado para el ambiente actual.
     * Proviene de: tasacorp.transaction.limit
     * 
     * VARÍA SEGÚN PERFIL:
     * - DEV:  999,999 (prácticamente ilimitado)
     * - TEST: 1,000 (bajo para pruebas)
     * - PROD: 50,000 (alto para producción)
     */
    @JsonProperty("limite_transaccional")
    private Integer limiteTransaccional;

    /**
     * Indica si la transacción está dentro del límite permitido.
     * 
     * true:  montoOrigen <= limiteTransaccional (transacción válida)
     * false: montoOrigen > limiteTransaccional (transacción excede límite)
     * 
     * EJEMPLO:
     * - En TEST (límite 1,000): monto 500 → true ✅
     * - En TEST (límite 1,000): monto 2,000 → false ❌
     * 
     * En un sistema real, cuando es false, la transacción podría rechazarse.
     */
    @JsonProperty("dentro_limite")
    private Boolean dentroLimite;

    // ========================================================================
    // CONSTRUCTORES
    // ========================================================================

    /**
     * Constructor vacío (sin argumentos).
     * 
     * 📌 OBLIGATORIO para Jackson (librería de JSON):
     * Jackson necesita este constructor para deserializar JSON a objetos Java.
     * 
     * Aunque no lo usamos directamente en nuestro código, frameworks como
     * Quarkus/Jackson lo requieren para crear instancias desde JSON.
     */
    public ConversionResponse() {
    }

    /**
     * Constructor completo con todos los parámetros.
     * 
     * 📌 USO PRINCIPAL:
     * Este constructor se usa en TasaService.convertirMoneda() para crear
     * la respuesta después de hacer todos los cálculos.
     * 
     * @param montoOrigen          Monto inicial ingresado por el usuario
     * @param monedaOrigen         Moneda del monto original (PEN)
     * @param montoConvertido      Resultado de la conversión (antes de comisión)
     * @param monedaDestino        Moneda destino solicitada (USD, EUR, MXN)
     * @param tasaAplicada         Tasa de cambio utilizada
     * @param comision             Comisión calculada según perfil
     * @param montoTotal           Total final (convertido + comisión)
     * @param proveedor            Nombre del proveedor de tasas
     * @param limiteTransaccional  Límite configurado para el ambiente
     * @param dentroLimite         Si la transacción cumple el límite
     */
    public ConversionResponse(Double montoOrigen, String monedaOrigen, Double montoConvertido,
                            String monedaDestino, Double tasaAplicada, Double comision,
                            Double montoTotal, String proveedor, Integer limiteTransaccional,
                            Boolean dentroLimite) {
        this.montoOrigen = montoOrigen;
        this.monedaOrigen = monedaOrigen;
        this.montoConvertido = montoConvertido;
        this.monedaDestino = monedaDestino;
        this.tasaAplicada = tasaAplicada;
        this.comision = comision;
        this.montoTotal = montoTotal;
        this.proveedor = proveedor;
        this.limiteTransaccional = limiteTransaccional;
        this.dentroLimite = dentroLimite;
    }

    // ========================================================================
    // GETTERS Y SETTERS
    // ========================================================================
    
    /**
     * 💡 ¿POR QUÉ GETTERS Y SETTERS?
     * 
     * Encapsulamiento: Los atributos son privados, solo se acceden vía métodos públicos.
     * 
     * Jackson los necesita para:
     * - Getters: Convertir el objeto Java a JSON (serialización)
     * - Setters: Convertir JSON a objeto Java (deserialización)
     * 
     * EJEMPLO DE SERIALIZACIÓN (Java → JSON):
     * 
     * ConversionResponse response = new ConversionResponse(...);
     * response.setMontoOrigen(1000.0);  // Java
     *    ↓ Jackson llama getMontoOrigen()
     * "monto_origen": 1000.0            // JSON
     */

    /**
     * @return El monto original en moneda base (PEN)
     */
    public Double getMontoOrigen() {
        return montoOrigen;
    }

    /**
     * @param montoOrigen El monto original a establecer
     */
    public void setMontoOrigen(Double montoOrigen) {
        this.montoOrigen = montoOrigen;
    }

    /**
     * @return La moneda del monto original (PEN)
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
     * @return El monto convertido ANTES de comisión
     */
    public Double getMontoConvertido() {
        return montoConvertido;
    }

    /**
     * @param montoConvertido El monto convertido a establecer
     */
    public void setMontoConvertido(Double montoConvertido) {
        this.montoConvertido = montoConvertido;
    }

    /**
     * @return La moneda de destino (USD, EUR, MXN)
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
     * @return La tasa de cambio aplicada
     */
    public Double getTasaAplicada() {
        return tasaAplicada;
    }

    /**
     * @param tasaAplicada La tasa a establecer
     */
    public void setTasaAplicada(Double tasaAplicada) {
        this.tasaAplicada = tasaAplicada;
    }

    /**
     * @return La comisión cobrada (varía según perfil)
     */
    public Double getComision() {
        return comision;
    }

    /**
     * @param comision La comisión a establecer
     */
    public void setComision(Double comision) {
        this.comision = comision;
    }

    /**
     * @return El monto total final (convertido + comisión)
     */
    public Double getMontoTotal() {
        return montoTotal;
    }

    /**
     * @param montoTotal El monto total a establecer
     */
    public void setMontoTotal(Double montoTotal) {
        this.montoTotal = montoTotal;
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
     * @return El límite transaccional del ambiente actual
     */
    public Integer getLimiteTransaccional() {
        return limiteTransaccional;
    }

    /**
     * @param limiteTransaccional El límite a establecer
     */
    public void setLimiteTransaccional(Integer limiteTransaccional) {
        this.limiteTransaccional = limiteTransaccional;
    }

    /**
     * @return true si está dentro del límite, false si lo excede
     */
    public Boolean getDentroLimite() {
        return dentroLimite;
    }

    /**
     * @param dentroLimite El estado del límite a establecer
     */
    public void setDentroLimite(Boolean dentroLimite) {
        this.dentroLimite = dentroLimite;
    }
}