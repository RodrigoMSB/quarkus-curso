-- Insertar productos iniciales para pruebas
INSERT INTO products (productcode, name, stock, reservedstock, price, created_at, updated_at) 
VALUES 
  ('LAPTOP-001', 'Laptop HP Pavilion 15', 50, 0, 899.99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('MOUSE-001', 'Mouse Logitech MX Master', 100, 0, 99.99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('KEYBOARD-001', 'Teclado Mecánico', 80, 0, 79.99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('MONITOR-001', 'Monitor LG 27 pulgadas', 30, 0, 349.99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('MOUSE-PAD-001', 'Mouse Pad Gaming XL', 200, 0, 19.99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
