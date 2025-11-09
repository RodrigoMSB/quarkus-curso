-- ============================================================================
-- DATOS INICIALES - INVENTORY SERVICE
-- ============================================================================
-- Este archivo se ejecuta automáticamente cuando el servicio arranca
-- (Quarkus ejecuta import.sql en modo dev si hibernate.hbm2ddl.auto=drop-and-create)
--
-- Productos de prueba para el sistema de e-commerce
-- ============================================================================

INSERT INTO products (productcode, name, stock, reservedstock, price, created_at, updated_at) 
VALUES 
  ('LAPTOP-001', 'Laptop HP Pavilion 15', 50, 0, 899.99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('MOUSE-001', 'Mouse Logitech MX Master', 100, 0, 99.99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('KEYBOARD-001', 'Teclado Mecánico Corsair', 80, 0, 79.99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Nota: ON CONFLICT no funciona en H2 (dev), pero sí en PostgreSQL (prod)
-- Si usas PostgreSQL en dev, considera agregar: ON CONFLICT DO NOTHING