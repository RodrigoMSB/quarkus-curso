package pe.edu.creditcore.scoring.model;

/**
 * Tipos de empresa según su clasificación y límites de crédito.
 * 
 * Clasificación según ingresos anuales y estructura:
 * - PERSONA_NATURAL: Individuos, hasta S/ 150,000/año
 * - MICROEMPRESA: Hasta 10 empleados, hasta S/ 500,000/año
 * - PYME: Pequeña y Mediana Empresa, hasta S/ 10M/año
 * - CORPORATIVO: Grandes empresas, más de S/ 10M/año
 */
public enum CompanyType {
    PERSONA_NATURAL(
        "Persona Natural",
        0.0,
        150_000.0,
        50_000.0,
        0.85
    ),
    MICROEMPRESA(
        "Microempresa",
        150_000.0,
        500_000.0,
        100_000.0,
        0.90
    ),
    PYME(
        "PYME",
        500_000.0,
        10_000_000.0,
        500_000.0,
        0.95
    ),
    CORPORATIVO(
        "Corporativo",
        10_000_000.0,
        Double.MAX_VALUE,
        5_000_000.0,
        1.0
    );
    
    private final String displayName;
    private final double minRevenue;
    private final double maxRevenue;
    private final double maxCreditLimit;
    private final double scoreFactor;
    
    CompanyType(String displayName, double minRevenue, double maxRevenue, 
                double maxCreditLimit, double scoreFactor) {
        this.displayName = displayName;
        this.minRevenue = minRevenue;
        this.maxRevenue = maxRevenue;
        this.maxCreditLimit = maxCreditLimit;
        this.scoreFactor = scoreFactor;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public double getMinRevenue() {
        return minRevenue;
    }
    
    public double getMaxRevenue() {
        return maxRevenue;
    }
    
    public double getMaxCreditLimit() {
        return maxCreditLimit;
    }
    
    /**
     * Factor de score: empresas más grandes tienen mejor scoring base.
     */
    public double getScoreFactor() {
        return scoreFactor;
    }
    
    /**
     * Determina el tipo de empresa basado en ingresos anuales.
     */
    public static CompanyType fromRevenue(double annualRevenue) {
        for (CompanyType type : values()) {
            if (annualRevenue >= type.minRevenue && annualRevenue < type.maxRevenue) {
                return type;
            }
        }
        return CORPORATIVO; // Por defecto para ingresos muy altos
    }
}
