#!/bin/bash

echo "=========================================="
echo "PRUEBAS API REACTIVA - QUARKUS"
echo "=========================================="
echo ""

echo "1️⃣  Listar todos los productos:"
curl -s http://localhost:8080/api/v1/productos/reactivo | jq
echo ""

echo "2️⃣  Buscar producto por ID (ID=1):"
curl -s http://localhost:8080/api/v1/productos/reactivo/1 | jq
echo ""

echo "3️⃣  Crear nuevo producto:"
curl -s -X POST http://localhost:8080/api/v1/productos/reactivo \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Auriculares Sony",
    "descripcion": "Auriculares inalámbricos con cancelación de ruido",
    "precio": 299.99,
    "stock": 25
  }' | jq
echo ""

echo "4️⃣  Actualizar producto (ID=1):"
curl -s -X PUT http://localhost:8080/api/v1/productos/reactivo/1 \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Laptop Dell XPS 15 Pro",
    "descripcion": "Laptop profesional actualizada",
    "precio": 1800.00,
    "stock": 20
  }' | jq
echo ""

echo "5️⃣  Buscar productos con stock bajo (umbral < 20):"
curl -s http://localhost:8080/api/v1/productos/reactivo/stock-bajo/20 | jq
echo ""

echo "6️⃣  Carga masiva (crear 100 productos):"
time curl -s -X POST http://localhost:8080/api/v1/productos/reactivo/carga-masiva/100 | jq
echo ""

echo "7️⃣  Eliminar producto (ID=3):"
curl -s -X DELETE http://localhost:8080/api/v1/productos/reactivo/3 -w "\nHTTP Status: %{http_code}\n"
echo ""

echo "8️⃣  Verificar que el producto fue eliminado (debe dar 404):"
curl -s http://localhost:8080/api/v1/productos/reactivo/3 -w "\nHTTP Status: %{http_code}\n"
echo ""

echo "=========================================="
echo "✅ Pruebas completadas exitosamente!"
echo "=========================================="
