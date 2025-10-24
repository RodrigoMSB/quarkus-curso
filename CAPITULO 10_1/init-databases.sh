#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE orders_db;
    CREATE DATABASE inventory_db;
    CREATE DATABASE payment_db;
EOSQL

echo "✅ Bases de datos creadas: orders_db, inventory_db, payment_db"
