-- ============================================================================
-- DATOS DE PRUEBA - SCORING SERVICE
-- ============================================================================
-- Histórico de scores calculados para diferentes clientes
-- Estos datos corresponden a clientes del customer-service
-- ============================================================================

-- Cliente ID 1: TechPeru S.A.C. (Technology)
-- Score excelente por alto ingreso y baja antigüedad
INSERT INTO score_history (id, customer_id, customer_ruc, customer_name, score, risk_level, strategy, requested_amount, loan_term_months, scoring_factors, recommendation, max_recommended_amount, suggested_interest_rate, calculated_at, requested_by, notes)
VALUES (1, 1, 'XXXXXXX3456', 'TechPeru S.A.C.', 820, 'EXCELLENT', 'BALANCED', 150000.00, 24, '{"incomeScore":255,"industryScore":237,"debtRatioScore":225,"companyAgeScore":103}', '✅ APROBACIÓN RECOMENDADA. Score: 820 (Excelente). Perfil excelente, ofrecer mejores condiciones.', 1500000.00, 8.5, '2024-10-15 10:30:00', 'admin-user', 'Primera solicitud del cliente');

-- Cliente ID 2: Retail Express (Retail)
-- Score bueno pero con advertencia por ratio deuda
INSERT INTO score_history (id, customer_id, customer_ruc, customer_name, score, risk_level, strategy, requested_amount, loan_term_months, scoring_factors, recommendation, max_recommended_amount, suggested_interest_rate, calculated_at, requested_by, notes)
VALUES (2, 2, 'XXXXXXX5678', 'Retail Express', 680, 'GOOD', 'BALANCED', 200000.00, 36, '{"incomeScore":210,"industryScore":175,"debtRatioScore":165,"companyAgeScore":130}', '✅ APROBACIÓN RECOMENDADA. Score: 680 (Bueno). Buen perfil crediticio, condiciones estándar. ⚠️ Ratio deuda/ingreso alto (40.0%).', 300000.00, 12.0, '2024-10-18 14:15:00', 'admin-user', 'Requiere análisis adicional del flujo de caja');

-- Cliente ID 3: Minera del Sur (Mining)
-- Score regular por industria de alto riesgo
INSERT INTO score_history (id, customer_id, customer_ruc, customer_name, score, risk_level, strategy, requested_amount, loan_term_months, scoring_factors, recommendation, max_recommended_amount, suggested_interest_rate, calculated_at, requested_by, notes)
VALUES (3, 3, 'XXXXXXX9012', 'Minera del Sur', 550, 'FAIR', 'CONSERVATIVE', 500000.00, 60, '{"incomeScore":270,"industryScore":125,"debtRatioScore":140,"companyAgeScore":155}', '✅ APROBACIÓN RECOMENDADA. Score: 550 (Regular). Perfil aceptable, monitorear de cerca.', 1500000.00, 18.0, '2024-10-20 09:00:00', 'credit-analyst', 'Industria volátil, requiere garantías adicionales');

-- Cliente ID 1: Segunda solicitud con estrategia agresiva
INSERT INTO score_history (id, customer_id, customer_ruc, customer_name, score, risk_level, strategy, requested_amount, loan_term_months, scoring_factors, recommendation, max_recommended_amount, suggested_interest_rate, calculated_at, requested_by, notes)
VALUES (4, 1, 'XXXXXXX3456', 'TechPeru S.A.C.', 943, 'EXCELLENT', 'AGGRESSIVE', 200000.00, 12, '{"incomeScore":255,"industryScore":237,"debtRatioScore":215,"companyAgeScore":236}', '✅ APROBACIÓN RECOMENDADA. Score: 943 (Excelente). Perfil excelente, ofrecer mejores condiciones.', 1500000.00, 8.5, '2024-10-22 16:45:00', 'admin-user', 'Ampliación de línea de crédito');

-- Cliente ID 4: Startup nueva (< 1 año)
-- Score bajo por ser empresa nueva sin historial
INSERT INTO score_history (id, customer_id, customer_ruc, customer_name, score, risk_level, strategy, requested_amount, loan_term_months, scoring_factors, recommendation, max_recommended_amount, suggested_interest_rate, calculated_at, requested_by, notes)
VALUES (5, 4, 'XXXXXXX3344', 'Startup Digital', 420, 'POOR', 'AGGRESSIVE', 50000.00, 18, '{"incomeScore":150,"industryScore":237,"debtRatioScore":180,"companyAgeScore":50}', '✅ APROBACIÓN RECOMENDADA. Score: 420 (Malo). Perfil aceptable, monitorear de cerca.', 45000.00, 25.0, '2024-10-23 11:20:00', 'fintech-analyst', 'Startup prometedora pero sin historial');

-- Cliente ID 5: Empresa constructora con alto riesgo
-- Score muy bajo, recomendación de rechazo
INSERT INTO score_history (id, customer_id, customer_ruc, customer_name, score, risk_level, strategy, requested_amount, loan_term_months, scoring_factors, recommendation, max_recommended_amount, suggested_interest_rate, calculated_at, requested_by, notes)
VALUES (6, 5, 'XXXXXXX5566', 'Constructora Beta', 320, 'VERY_POOR', 'CONSERVATIVE', 300000.00, 48, '{"incomeScore":120,"industryScore":162,"debtRatioScore":50,"companyAgeScore":65}', '❌ RECHAZO RECOMENDADO. Score insuficiente: 320 (Muy Malo). ⚠️ Ratio deuda/ingreso alto (85.7%).', 70000.00, 35.0, '2024-10-24 13:00:00', 'senior-analyst', 'Alto endeudamiento actual, rechazar o restructurar deuda primero');

-- Cliente ID 2: Recálculo después de mejorar ingresos
INSERT INTO score_history (id, customer_id, customer_ruc, customer_name, score, risk_level, strategy, requested_amount, loan_term_months, scoring_factors, recommendation, max_recommended_amount, suggested_interest_rate, calculated_at, requested_by, notes)
VALUES (7, 2, 'XXXXXXX5678', 'Retail Express', 750, 'GOOD', 'BALANCED', 150000.00, 24, '{"incomeScore":240,"industryScore":175,"debtRatioScore":205,"companyAgeScore":130}', '✅ APROBACIÓN RECOMENDADA. Score: 750 (Bueno). Buen perfil crediticio, condiciones estándar.', 450000.00, 12.0, '2024-10-25 15:30:00', 'admin-user', 'Cliente mejoró sus ingresos anuales');

-- Reiniciar secuencia
SELECT setval('score_history_id_seq', (SELECT MAX(id) FROM score_history));

-- ============================================================================
-- RESUMEN DE DATOS
-- ============================================================================
-- Total de scores: 7
-- Clientes únicos: 5
-- Estrategias usadas: CONSERVATIVE, BALANCED, AGGRESSIVE
-- Niveles de riesgo: EXCELLENT (2), GOOD (2), FAIR (1), POOR (1), VERY_POOR (1)
-- Aprobados: 6
-- Rechazados: 1
-- ============================================================================
