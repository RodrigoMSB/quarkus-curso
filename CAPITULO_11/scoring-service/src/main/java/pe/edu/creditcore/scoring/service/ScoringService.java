package pe.edu.creditcore.scoring.service;

import io.quarkus.cache.CacheResult;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import pe.edu.creditcore.scoring.dto.CustomerData;
import pe.edu.creditcore.scoring.dto.ScoreRequest;
import pe.edu.creditcore.scoring.dto.ScoreResult;
import pe.edu.creditcore.scoring.entity.ScoreHistory;
import pe.edu.creditcore.scoring.integration.CustomerServiceClient;
import pe.edu.creditcore.scoring.model.CompanyType;
import pe.edu.creditcore.scoring.model.RiskLevel;
import pe.edu.creditcore.scoring.model.ScoringStrategy;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Servicio de cálculo de score crediticio.
 * 
 * ALGORITMO ORIGINAL DE SCORING:
 * 
 * El score final (0-1000) se calcula mediante la suma ponderada de 4 factores:
 * 
 * 1. INCOME SCORE (30%): Basado en ingresos anuales
 *    - Evalúa la capacidad de pago base
 *    - Empresas con mayores ingresos → mejor score
 *    - Escala logarítmica para evitar dominio de grandes empresas
 * 
 * 2. INDUSTRY SCORE (25%): Riesgo por sector industrial
 *    - Cada industria tiene un factor de riesgo predefinido
 *    - Tecnología y salud: bajo riesgo
 *    - Construcción y minería: alto riesgo
 * 
 * 3. DEBT RATIO SCORE (25%): Relación monto solicitado / capacidad de pago
 *    - Evalúa si el monto es razonable para los ingresos
 *    - Ratio < 20%: excelente
 *    - Ratio > 40%: riesgoso
 * 
 * 4. COMPANY AGE SCORE (20%): Antigüedad de la empresa
 *    - Empresas más antiguas: más estables
 *    - < 1 año: startup, riesgo alto
 *    - > 10 años: establecida, riesgo bajo
 * 
 * El score se ajusta según la ESTRATEGIA:
 * - CONSERVATIVE: multiplica x 0.85 (más estricto)
 * - BALANCED: sin ajuste (x 1.0)
 * - AGGRESSIVE: multiplica x 1.15 (más permisivo)
 */
@ApplicationScoped
public class ScoringService {
    
    @Inject
    @RestClient
    CustomerServiceClient customerServiceClient;
    
    @Inject
    JsonWebToken jwt;
    
    // Pesos de cada factor (deben sumar 1.0)
    private static final double WEIGHT_INCOME = 0.30;
    private static final double WEIGHT_INDUSTRY = 0.25;
    private static final double WEIGHT_DEBT_RATIO = 0.25;
    private static final double WEIGHT_COMPANY_AGE = 0.20;
    
    // Constantes para cálculo
    private static final double MAX_DEBT_RATIO = 0.40; // 40% de los ingresos
    private static final int MAX_COMPANY_AGE = 20; // Máximo para scoring
    
    /**
     * Calcula el score crediticio de un cliente.
     * 
     * Proceso:
     * 1. Obtiene datos del cliente desde customer-service (reactivo)
     * 2. Calcula score basado en algoritmo multi-factor
     * 3. Determina nivel de riesgo y recomendación
     * 4. Guarda en histórico
     * 5. Retorna resultado
     * 
     * @param request Solicitud de scoring
     * @return Score crediticio calculado
     */
    public Uni<ScoreResult> calculateScore(ScoreRequest request) {
        Log.infof("📊 Calculando score para cliente ID: %d, monto: S/ %.2f", 
                 request.getCustomerId(), request.getRequestedAmount());
        
        return customerServiceClient.getCustomerById(request.getCustomerId())
            .onItem().transform(customer -> {
                Log.infof("✅ Datos del cliente obtenidos: %s (RUC: %s)", 
                         customer.getLegalName(), customer.getRucMasked());
                return customer;
            })
            .onItem().transformToUni(customer -> 
                performScoringCalculation(customer, request)
            )
            .onItem().transformToUni(result -> 
                saveToHistory(result, request)
            )
            .onFailure().invoke(err -> 
                Log.errorf(err, "❌ Error calculando score para cliente %d", 
                          request.getCustomerId())
            );
    }
    
    /**
     * Realiza el cálculo del score.
     */
    private Uni<ScoreResult> performScoringCalculation(CustomerData customer, ScoreRequest request) {
        return Uni.createFrom().item(() -> {
            // 1. Calcular cada factor del score
            int incomeScore = calculateIncomeScore(customer.getAnnualRevenue());
            int industryScore = calculateIndustryScore(customer.getIndustry());
            int debtRatioScore = calculateDebtRatioScore(
                request.getRequestedAmount(), 
                customer.getAnnualRevenue()
            );
            int companyAgeScore = calculateCompanyAgeScore(customer.getCompanyAgeInYears());
            
            // 2. Score base (suma ponderada)
            int baseScore = (int) (
                incomeScore * WEIGHT_INCOME +
                industryScore * WEIGHT_INDUSTRY +
                debtRatioScore * WEIGHT_DEBT_RATIO +
                companyAgeScore * WEIGHT_COMPANY_AGE
            );
            
            // 3. Ajustar por estrategia
            ScoringStrategy strategy = request.getStrategyOrDefault();
            int finalScore = (int) (baseScore * strategy.getScoreMultiplier());
            finalScore = Math.min(1000, Math.max(0, finalScore)); // Clamp 0-1000
            
            // 4. Determinar nivel de riesgo
            RiskLevel riskLevel = RiskLevel.fromScore(finalScore);
            
            // 5. Generar recomendación
            boolean approved = finalScore >= strategy.getMinimumApprovalScore();
            String recommendation = generateRecommendation(
                finalScore, riskLevel, request.getRequestedAmount(), 
                customer.getAnnualRevenue(), approved
            );
            
            // 6. Calcular monto máximo y tasa sugerida
            double maxAmount = calculateMaxRecommendedAmount(
                customer.getAnnualRevenue(), 
                CompanyType.fromRevenue(customer.getAnnualRevenue())
            );
            double interestRate = calculateSuggestedRate(riskLevel);
            
            // 7. Construir factores para logging
            Map<String, Object> factors = new LinkedHashMap<>();
            factors.put("incomeScore", incomeScore);
            factors.put("industryScore", industryScore);
            factors.put("debtRatioScore", debtRatioScore);
            factors.put("companyAgeScore", companyAgeScore);
            factors.put("baseScore", baseScore);
            factors.put("strategyMultiplier", strategy.getScoreMultiplier());
            factors.put("annualRevenue", customer.getAnnualRevenue());
            factors.put("companyAge", customer.getCompanyAgeInYears());
            factors.put("industry", customer.getIndustry().name());
            factors.put("debtRatio", request.getRequestedAmount() / customer.getAnnualRevenue());
            
            Log.infof("📈 Score calculado: %d (base: %d, estrategia: %s)", 
                     finalScore, baseScore, strategy.name());
            Log.debugf("Factores: %s", factors);
            
            // 8. Construir resultado
            return ScoreResult.builder()
                .customerId(customer.getId())
                .customerRuc(customer.getRucMasked())
                .customerName(customer.getDisplayName())
                .score(finalScore)
                .riskLevel(riskLevel)
                .strategy(strategy)
                .requestedAmount(request.getRequestedAmount())
                .loanTermMonths(request.getLoanTermMonths())
                .scoringFactors(factors)
                .approved(approved)
                .recommendation(recommendation)
                .maxRecommendedAmount(maxAmount)
                .suggestedInterestRate(interestRate)
                .calculatedAt(LocalDateTime.now())
                .notes(request.getNotes())
                .fromCache(false)
                .build();
        });
    }
    
    /**
     * FACTOR 1: Income Score (0-300 puntos)
     * Escala logarítmica para balancear empresas de diferentes tamaños.
     */
    private int calculateIncomeScore(Double annualRevenue) {
        if (annualRevenue == null || annualRevenue <= 0) {
            return 0;
        }
        
        // Logaritmo base 10 normalizado
        // S/ 10,000 → ~120 puntos
        // S/ 100,000 → ~150 puntos
        // S/ 1,000,000 → ~180 puntos
        // S/ 10,000,000 → ~210 puntos
        double logScore = Math.log10(annualRevenue) * 30;
        return (int) Math.min(300, Math.max(0, logScore));
    }
    
    /**
     * FACTOR 2: Industry Score (0-250 puntos)
     * Basado en el factor de riesgo de la industria.
     */
    private int calculateIndustryScore(pe.edu.creditcore.scoring.model.Industry industry) {
        if (industry == null) {
            return 175; // Score promedio si no se conoce
        }
        return (int) (industry.getRiskFactor() * 250);
    }
    
    /**
     * FACTOR 3: Debt Ratio Score (0-250 puntos)
     * Evalúa si el monto solicitado es razonable para los ingresos.
     */
    private int calculateDebtRatioScore(Double requestedAmount, Double annualRevenue) {
        if (annualRevenue == null || annualRevenue <= 0) {
            return 0;
        }
        
        double ratio = requestedAmount / annualRevenue;
        
        // Ratio excelente: < 10% → 250 puntos
        if (ratio < 0.10) {
            return 250;
        }
        // Ratio bueno: 10-20% → 200-250 puntos
        else if (ratio < 0.20) {
            return (int) (250 - ((ratio - 0.10) * 500));
        }
        // Ratio aceptable: 20-30% → 150-200 puntos
        else if (ratio < 0.30) {
            return (int) (200 - ((ratio - 0.20) * 500));
        }
        // Ratio alto: 30-40% → 50-150 puntos
        else if (ratio < MAX_DEBT_RATIO) {
            return (int) (150 - ((ratio - 0.30) * 1000));
        }
        // Ratio muy alto: > 40% → 0-50 puntos
        else {
            return Math.max(0, (int) (50 - ((ratio - MAX_DEBT_RATIO) * 500)));
        }
    }
    
    /**
     * FACTOR 4: Company Age Score (0-200 puntos)
     * Empresas más antiguas son más estables.
     */
    private int calculateCompanyAgeScore(int ageInYears) {
        // < 1 año: startup, 50 puntos
        if (ageInYears < 1) {
            return 50;
        }
        // 1-3 años: en crecimiento, 100-120 puntos
        else if (ageInYears < 3) {
            return 100 + (ageInYears * 10);
        }
        // 3-10 años: establecida, 120-180 puntos
        else if (ageInYears < 10) {
            return 120 + ((ageInYears - 3) * 9);
        }
        // > 10 años: muy establecida, 180-200 puntos
        else {
            return Math.min(200, 180 + (Math.min(ageInYears, MAX_COMPANY_AGE) - 10) * 2);
        }
    }
    
    /**
     * Calcula el monto máximo recomendado basado en ingresos y tipo de empresa.
     */
    private double calculateMaxRecommendedAmount(Double annualRevenue, CompanyType companyType) {
        if (annualRevenue == null || annualRevenue <= 0) {
            return 0.0;
        }
        
        // 30% de los ingresos anuales como máximo
        double maxByIncome = annualRevenue * 0.30;
        
        // Limitado por el tope del tipo de empresa
        return Math.min(maxByIncome, companyType.getMaxCreditLimit());
    }
    
    /**
     * Calcula la tasa de interés sugerida según el riesgo.
     * Tasas en % anual.
     */
    private double calculateSuggestedRate(RiskLevel riskLevel) {
        return switch (riskLevel) {
            case EXCELLENT -> 8.5;    // Mejor tasa
            case GOOD -> 12.0;        // Tasa competitiva
            case FAIR -> 18.0;        // Tasa estándar
            case POOR -> 25.0;        // Tasa alta
            case VERY_POOR -> 35.0;   // Tasa muy alta
        };
    }
    
    /**
     * Genera recomendación textual basada en el score.
     */
    private String generateRecommendation(int score, RiskLevel riskLevel, 
                                         double requestedAmount, double annualRevenue, 
                                         boolean approved) {
        double debtRatio = requestedAmount / annualRevenue;
        
        StringBuilder rec = new StringBuilder();
        
        if (approved) {
            rec.append("✅ APROBACIÓN RECOMENDADA. ");
            rec.append(String.format("Score: %d (%s). ", score, riskLevel.getDisplayName()));
            
            if (score >= 800) {
                rec.append("Perfil excelente, ofrecer mejores condiciones. ");
            } else if (score >= 650) {
                rec.append("Buen perfil crediticio, condiciones estándar. ");
            } else {
                rec.append("Perfil aceptable, monitorear de cerca. ");
            }
        } else {
            rec.append("❌ RECHAZO RECOMENDADO. ");
            rec.append(String.format("Score insuficiente: %d (%s). ", score, riskLevel.getDisplayName()));
        }
        
        // Advertencias específicas
        if (debtRatio > 0.35) {
            rec.append("⚠️ Ratio deuda/ingreso alto (")
               .append(String.format("%.1f%%", debtRatio * 100))
               .append("). ");
        }
        
        return rec.toString();
    }
    
    /**
     * Guarda el resultado en el histórico.
     */
    private Uni<ScoreResult> saveToHistory(ScoreResult result, ScoreRequest request) {
        return Panache.withTransaction(() -> {
            ScoreHistory history = ScoreHistory.builder()
                .customerId(result.getCustomerId())
                .customerRuc(result.getCustomerRuc())
                .customerName(result.getCustomerName())
                .score(result.getScore())
                .riskLevel(result.getRiskLevel())
                .strategy(result.getStrategy())
                .requestedAmount(result.getRequestedAmount())
                .loanTermMonths(result.getLoanTermMonths())
                .scoringFactors(result.getScoringFactors().toString())
                .recommendation(result.getRecommendation())
                .maxRecommendedAmount(result.getMaxRecommendedAmount())
                .suggestedInterestRate(result.getSuggestedInterestRate())
                .calculatedAt(result.getCalculatedAt())
                .requestedBy(getUsername())
                .notes(request.getNotes())
                .build();
            
            return history.persist()
                .onItem().transform(persisted -> {
                    result.setHistoryId(persisted.id);
                    Log.infof("💾 Score guardado en histórico con ID: %d", persisted.id);
                    return result;
                });
        });
    }
    
    /**
     * Obtiene el histórico de scores de un cliente.
     */
    public Uni<java.util.List<ScoreHistory>> getScoreHistory(Long customerId) {
        Log.infof("📜 Consultando histórico de scores para cliente: %d", customerId);
        return ScoreHistory.findByCustomerId(customerId);
    }
    
    /**
     * Obtiene el score más reciente de un cliente (con cache).
     */
    @CacheResult(cacheName = "latest-scores")
    public Uni<ScoreHistory> getLatestScore(Long customerId) {
        Log.infof("🔍 Buscando último score para cliente: %d", customerId);
        return ScoreHistory.findLatestByCustomerId(customerId);
    }
    
    /**
     * Obtiene username del JWT o "system" si no está disponible.
     */
    private String getUsername() {
        try {
            return jwt != null && jwt.getName() != null ? jwt.getName() : "system";
        } catch (Exception e) {
            return "system";
        }
    }
}
