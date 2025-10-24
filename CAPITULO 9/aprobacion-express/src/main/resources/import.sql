-- ============================================================================
-- DATOS DE PRUEBA - SISTEMA DE PRE-APROBACIÓN CREDITICIA
-- ============================================================================
-- Estos datos se cargan automáticamente al iniciar la aplicación
-- Incluyen casos variados para demostrar todas las reglas de negocio
-- ============================================================================

-- ----------------------------------------------------------------------------
-- CASO 1: Cliente EXCELENTE - Pre-aprobado con mejores condiciones
-- ----------------------------------------------------------------------------
INSERT INTO solicitudes_credito (
    id, numero_documento, tipo_documento, nombre_completo,
    ingreso_mensual, monto_solicitado, deuda_actual,
    antiguedad_laboral_anios, edad, tiene_garantia, tipo_garantia,
    fecha_solicitud, estado, score_calculado, monto_aprobado,
    tasa_interes, plazo_maximo_meses, tiempo_evaluacion_ms, fecha_evaluacion
) VALUES (
    1, '45678901', 'DNI', 'María Elena Fernández Torres',
    8500.00, 50000.00, 5000.00,
    12, 38, true, 'HIPOTECARIA',
    '2025-10-20 10:30:00', 'APROBADO', 820, 50000.00,
    10.00, 84, 145, '2025-10-20 10:30:00'
);

-- ----------------------------------------------------------------------------
-- CASO 2: Cliente BUENO - Aprobado con condiciones normales
-- ----------------------------------------------------------------------------
INSERT INTO solicitudes_credito (
    id, numero_documento, tipo_documento, nombre_completo,
    ingreso_mensual, monto_solicitado, deuda_actual,
    antiguedad_laboral_anios, edad, tiene_garantia, tipo_garantia,
    fecha_solicitud, estado, score_calculado, monto_aprobado,
    tasa_interes, plazo_maximo_meses, tiempo_evaluacion_ms, fecha_evaluacion
) VALUES (
    2, '56789012', 'DNI', 'Carlos Alberto Mendoza Ríos',
    5000.00, 30000.00, 8000.00,
    6, 42, true, 'VEHICULAR',
    '2025-10-21 14:15:00', 'APROBADO', 685, 30000.00,
    12.50, 60, 152, '2025-10-21 14:15:00'
);

-- ----------------------------------------------------------------------------
-- CASO 3: Cliente REGULAR - Aprobado pero con tasa alta y plazo corto
-- ----------------------------------------------------------------------------
INSERT INTO solicitudes_credito (
    id, numero_documento, tipo_documento, nombre_completo,
    ingreso_mensual, monto_solicitado, deuda_actual,
    antiguedad_laboral_anios, edad, tiene_garantia, tipo_garantia,
    fecha_solicitud, estado, score_calculado, monto_aprobado,
    tasa_interes, plazo_maximo_meses, tiempo_evaluacion_ms, fecha_evaluacion
) VALUES (
    3, '22222222', 'DNI', 'Pedro José Ramírez Gómez',
    3500.00, 15000.00, 7000.00,
    3, 35, false, null,
    '2025-10-22 09:45:00', 'APROBADO', 625, 15000.00,
    15.00, 36, 178, '2025-10-22 09:45:00'
);

-- ----------------------------------------------------------------------------
-- CASO 4: Cliente RECHAZADO - Deuda muy alta
-- ----------------------------------------------------------------------------
INSERT INTO solicitudes_credito (
    id, numero_documento, tipo_documento, nombre_completo,
    ingreso_mensual, monto_solicitado, deuda_actual,
    antiguedad_laboral_anios, edad, tiene_garantia, tipo_garantia,
    fecha_solicitud, estado, score_calculado, monto_aprobado,
    tasa_interes, plazo_maximo_meses, tiempo_evaluacion_ms, fecha_evaluacion
) VALUES (
    4, '67890123', 'DNI', 'Ana Patricia Silva Montes',
    2500.00, 20000.00, 12000.00,
    2, 28, false, null,
    '2025-10-22 11:20:00', 'RECHAZADO', 485, null,
    null, null, 163, '2025-10-22 11:20:00'
);

-- ----------------------------------------------------------------------------
-- CASO 5: Cliente RECHAZADO - Lista negra del bureau
-- ----------------------------------------------------------------------------
INSERT INTO solicitudes_credito (
    id, numero_documento, tipo_documento, nombre_completo,
    ingreso_mensual, monto_solicitado, deuda_actual,
    antiguedad_laboral_anios, edad, tiene_garantia, tipo_garantia,
    fecha_solicitud, estado, score_calculado, monto_aprobado,
    tasa_interes, plazo_maximo_meses, tiempo_evaluacion_ms, fecha_evaluacion
) VALUES (
    5, '12345678', 'DNI', 'Roberto Carlos Delgado Pérez',
    4000.00, 25000.00, 3000.00,
    5, 40, true, 'PRENDARIA',
    '2025-10-22 16:00:00', 'RECHAZADO', 320, null,
    null, null, 141, '2025-10-22 16:00:00'
);

-- ----------------------------------------------------------------------------
-- CASO 6: Cliente RECHAZADO - Antigüedad laboral insuficiente
-- ----------------------------------------------------------------------------
INSERT INTO solicitudes_credito (
    id, numero_documento, tipo_documento, nombre_completo,
    ingreso_mensual, monto_solicitado, deuda_actual,
    antiguedad_laboral_anios, edad, tiene_garantia, tipo_garantia,
    fecha_solicitud, estado, score_calculado, monto_aprobado,
    tasa_interes, plazo_maximo_meses, tiempo_evaluacion_ms, fecha_evaluacion
) VALUES (
    6, '78901234', 'DNI', 'Luis Fernando Castro López',
    6000.00, 40000.00, 2000.00,
    0, 25, false, null,
    '2025-10-23 08:30:00', 'RECHAZADO', 540, null,
    null, null, 155, '2025-10-23 08:30:00'
);

-- ----------------------------------------------------------------------------
-- CASO 7: Solicitud PENDIENTE (para que el alumno pueda evaluar)
-- ----------------------------------------------------------------------------
INSERT INTO solicitudes_credito (
    id, numero_documento, tipo_documento, nombre_completo,
    ingreso_mensual, monto_solicitado, deuda_actual,
    antiguedad_laboral_anios, edad, tiene_garantia, tipo_garantia,
    fecha_solicitud, estado, score_calculado, monto_aprobado,
    tasa_interes, plazo_maximo_meses, tiempo_evaluacion_ms, fecha_evaluacion
) VALUES (
    7, '89012345', 'DNI', 'Carmen Rosa Vega Sánchez',
    7000.00, 60000.00, 10000.00,
    8, 45, true, 'HIPOTECARIA',
    '2025-10-23 10:00:00', 'PENDIENTE', null, null,
    null, null, null, null
);

-- ----------------------------------------------------------------------------
-- CASO 8: Cliente joven con buen perfil - Aprobado
-- ----------------------------------------------------------------------------
INSERT INTO solicitudes_credito (
    id, numero_documento, tipo_documento, nombre_completo,
    ingreso_mensual, monto_solicitado, deuda_actual,
    antiguedad_laboral_anios, edad, tiene_garantia, tipo_garantia,
    fecha_solicitud, estado, score_calculado, monto_aprobado,
    tasa_interes, plazo_maximo_meses, tiempo_evaluacion_ms, fecha_evaluacion
) VALUES (
    8, '90123456', 'DNI', 'Diego Alejandro Rojas Martínez',
    4500.00, 18000.00, 1500.00,
    4, 32, true, 'VEHICULAR',
    '2025-10-23 12:30:00', 'APROBADO', 710, 18000.00,
    11.50, 60, 148, '2025-10-23 12:30:00'
);

-- ----------------------------------------------------------------------------
-- CASO 9: Cliente con historial regular en bureau - Aprobado límite
-- ----------------------------------------------------------------------------
INSERT INTO solicitudes_credito (
    id, numero_documento, tipo_documento, nombre_completo,
    ingreso_mensual, monto_solicitado, deuda_actual,
    antiguedad_laboral_anios, edad, tiene_garantia, tipo_garantia,
    fecha_solicitud, estado, score_calculado, monto_aprobado,
    tasa_interes, plazo_maximo_meses, tiempo_evaluacion_ms, fecha_evaluacion
) VALUES (
    9, '33333333', 'DNI', 'Sofía Isabel Paredes Flores',
    3800.00, 12000.00, 6000.00,
    5, 29, false, null,
    '2025-10-23 14:00:00', 'APROBADO', 615, 12000.00,
    14.50, 36, 172, '2025-10-23 14:00:00'
);

-- ----------------------------------------------------------------------------
-- CASO 10: Extranjero con CE - Aprobado
-- ----------------------------------------------------------------------------
INSERT INTO solicitudes_credito (
    id, numero_documento, tipo_documento, nombre_completo,
    ingreso_mensual, monto_solicitado, deuda_actual,
    antiguedad_laboral_anios, edad, tiene_garantia, tipo_garantia,
    fecha_solicitud, estado, score_calculado, monto_aprobado,
    tasa_interes, plazo_maximo_meses, tiempo_evaluacion_ms, fecha_evaluacion
) VALUES (
    10, '001234567', 'CE', 'Andrés Felipe Gutiérrez Moreno',
    9000.00, 70000.00, 8000.00,
    10, 40, true, 'HIPOTECARIA',
    '2025-10-23 15:30:00', 'APROBADO', 765, 70000.00,
    10.50, 84, 139, '2025-10-23 15:30:00'
);

-- Configurar la secuencia para el próximo ID
ALTER SEQUENCE solicitudes_credito_seq RESTART WITH 11;

-- ============================================================================
-- RESUMEN DE CASOS DE PRUEBA
-- ============================================================================
-- Total registros: 10
-- - Aprobados: 6 (IDs: 1, 2, 3, 8, 9, 10)
-- - Rechazados: 3 (IDs: 4, 5, 6)
-- - Pendientes: 1 (ID: 7)
--
-- Casos especiales incluidos:
-- ✓ Cliente con garantía hipotecaria (mejor tasa)
-- ✓ Cliente en lista negra del bureau (auto-rechazo)
-- ✓ Cliente con deuda muy alta (rechazo por ratio)
-- ✓ Cliente sin antigüedad laboral (rechazo por estabilidad)
-- ✓ Cliente con historial regular en bureau (límite aprobación)
-- ✓ Extranjero con CE
-- ✓ Solicitud pendiente de evaluación
-- ============================================================================
