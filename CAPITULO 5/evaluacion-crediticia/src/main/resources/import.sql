-- Datos de prueba con DNI peruano

INSERT INTO solicitudes_credito (id, dni, nombreCompleto, email, edad, ingresosMensuales, deudasActuales, montoSolicitado, mesesEnEmpleoActual, scoreCrediticio, aprobada, razonEvaluacion, estado, fechaCreacion, fechaActualizacion) 
VALUES (1, '12345678', 'Juan Pérez González', 'juan.perez@email.pe', 35, 2500000.00, 300000.00, 5000000.00, 48, 850, true, 'Aprobado: Excelente perfil crediticio. Felicitaciones.', 'APROBADA', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO solicitudes_credito (id, dni, nombreCompleto, email, edad, ingresosMensuales, deudasActuales, montoSolicitado, mesesEnEmpleoActual, scoreCrediticio, aprobada, razonEvaluacion, estado, fechaCreacion, fechaActualizacion) 
VALUES (2, '23456789', 'María Silva Torres', 'maria.silva@email.pe', 28, 1800000.00, 400000.00, 3000000.00, 24, 720, true, 'Aprobado: Perfil crediticio cumple con los requisitos del banco.', 'APROBADA', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO solicitudes_credito (id, dni, nombreCompleto, email, edad, ingresosMensuales, deudasActuales, montoSolicitado, mesesEnEmpleoActual, scoreCrediticio, aprobada, razonEvaluacion, estado, fechaCreacion, fechaActualizacion) 
VALUES (3, '34567890', 'Carlos Rojas Vega', 'carlos.rojas@email.pe', 42, 1500000.00, 900000.00, 4000000.00, 12, 420, false, 'Rechazado: Ratio deuda/ingreso (60.00%) supera el límite permitido (50%).', 'RECHAZADA', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO solicitudes_credito (id, dni, nombreCompleto, email, edad, ingresosMensuales, deudasActuales, montoSolicitado, mesesEnEmpleoActual, scoreCrediticio, aprobada, razonEvaluacion, estado, fechaCreacion, fechaActualizacion) 
VALUES (4, '45678901', 'Ana López Muñoz', 'ana.lopez@email.pe', 23, 1200000.00, 150000.00, 2000000.00, 2, 480, false, 'Rechazado: Inestabilidad laboral. Se requiere mínimo 3 meses en empleo actual.', 'RECHAZADA', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO solicitudes_credito (id, dni, nombreCompleto, email, edad, ingresosMensuales, deudasActuales, montoSolicitado, mesesEnEmpleoActual, estado, fechaCreacion, fechaActualizacion) 
VALUES (5, '56789012', 'Pedro Soto Castro', 'pedro.soto@email.pe', 50, 3000000.00, 500000.00, 8000000.00, 60, 'PENDIENTE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

ALTER SEQUENCE solicitudes_credito_SEQ RESTART WITH 6;
