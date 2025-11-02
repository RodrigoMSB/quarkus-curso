-- ===========================================
-- Datos de prueba para Customer Service
-- Ejecutado automáticamente en perfil DEV
-- ===========================================

-- NOTA: Los RUCs aquí están en texto plano, pero en producción
-- el servicio los cifrará con Google Tink antes de almacenarlos

INSERT INTO customers (
    id, ruc, legal_name, trade_name, industry, founded_date,
    annual_revenue, contact_email, contact_phone, address, city,
    status, credit_score, risk_category, sunat_validated,
    created_at, updated_at, created_by
) VALUES
-- Cliente Premium - Tecnología
(1, '20123456789', 'Tech Innovations S.A.C.', 'TechInnov', 'TECHNOLOGY',
 '2010-03-15', 15000000.00, 'contacto@techinno.pe', '+51987654321',
 'Av. Innovación 123', 'Lima', 'ACTIVE', 850, 'AAA', true,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system'),

-- Cliente Medio - Retail
(2, '20987654321', 'Comercial del Sur S.A.', 'ComSur', 'RETAIL',
 '2015-07-20', 8000000.00, 'ventas@comsur.pe', '+51912345678',
 'Jr. Comercio 456', 'Arequipa', 'ACTIVE', 650, 'A', true,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system'),

-- Cliente de Riesgo - Manufactura
(3, '20555666777', 'Industrias Perú S.R.L.', 'IndPeru', 'MANUFACTURING',
 '2018-11-10', 3500000.00, 'admin@indperu.pe', '+51998877665',
 'Av. Industrial 789', 'Callao', 'ACTIVE', 450, 'BB', true,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system'),

-- Cliente Alto Riesgo - Servicios
(4, '20111222333', 'Servicios Generales E.I.R.L.', 'ServiGen', 'SERVICES',
 '2020-01-05', 1200000.00, 'info@servigen.pe', '+51955443322',
 'Calle Principal 321', 'Cusco', 'ACTIVE', 320, 'C', false,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system'),

-- Cliente Suspendido
(5, '20888999000', 'Construcciones Rápidas S.A.', 'ConRapid', 'CONSTRUCTION',
 '2012-05-30', 5000000.00, 'contacto@conrapid.pe', '+51977788899',
 'Av. Construcción 654', 'Trujillo', 'SUSPENDED', 280, 'C', true,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system');

-- Resetear secuencia de IDs
ALTER SEQUENCE customers_seq RESTART WITH 6;
