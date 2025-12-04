#!/bin/bash
set -e

# ============================================================================
# SCRIPT DE INICIALIZACION DE BASES DE DATOS
# ============================================================================
# Este script se ejecuta automaticamente cuando Docker crea el contenedor
# de PostgreSQL por primera vez.
#
# CREA:
#   - orders_db     (para Order Service)
#   - inventory_db  (para Inventory Service)
#   - payment_db    (para Payment Service)
#
# NOTA: Los datos de prueba se insertan automaticamente cuando cada servicio
#       arranca por primera vez (via import.sql en cada microservicio)
# ============================================================================

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE orders_db;
    CREATE DATABASE inventory_db;
    CREATE DATABASE payment_db;
EOSQL

echo "Bases de datos creadas: orders_db, inventory_db, payment_db"