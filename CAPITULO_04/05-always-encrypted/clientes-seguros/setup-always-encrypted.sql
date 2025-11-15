-- ================================================
-- Script: Configurar Always Encrypted
-- Plataforma: Windows (Certificate Store)
-- ================================================

USE BancoDB;
GO

-- ================================================
-- PASO 1: Crear Column Master Key (CMK)
-- ================================================
-- IMPORTANTE: Reemplazar el thumbprint con el certificado real de Windows
-- Para obtenerlo en Windows PowerShell:
--   Get-ChildItem -Path Cert:\CurrentUser\My | Where-Object {$_.Subject -like "*AlwaysEncrypted*"}

CREATE COLUMN MASTER KEY [CMK_AlwaysEncrypted]
WITH (
    KEY_STORE_PROVIDER_NAME = 'MSSQL_CERTIFICATE_STORE',
    KEY_PATH = 'CurrentUser/My/THUMBPRINT_AQUI'
);
GO

-- ================================================
-- PASO 2: Crear Column Encryption Key (CEK)
-- ================================================
CREATE COLUMN ENCRYPTION KEY [CEK_Clientes]
WITH VALUES (
    COLUMN_MASTER_KEY = [CMK_AlwaysEncrypted],
    ALGORITHM = 'RSA_OAEP'
);
GO

-- ================================================
-- PASO 3: Recrear tabla con columnas cifradas
-- ================================================

-- Renombrar tabla existente
EXEC sp_rename 'Cliente', 'Cliente_Old';
GO

-- Crear nueva tabla con columnas cifradas
CREATE TABLE Cliente (
    id BIGINT NOT NULL PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    
    -- CIFRADA: Deterministic (permite búsquedas por igualdad)
    numero_tarjeta VARCHAR(255) 
        COLLATE Latin1_General_BIN2
        ENCRYPTED WITH (
            COLUMN_ENCRYPTION_KEY = [CEK_Clientes],
            ENCRYPTION_TYPE = Deterministic,
            ALGORITHM = 'AEAD_AES_256_CBC_HMAC_SHA_256'
        ) NOT NULL,
    
    -- CIFRADA: Randomized (no permite búsquedas, más seguro)
    email VARCHAR(255)
        COLLATE Latin1_General_BIN2
        ENCRYPTED WITH (
            COLUMN_ENCRYPTION_KEY = [CEK_Clientes],
            ENCRYPTION_TYPE = Randomized,
            ALGORITHM = 'AEAD_AES_256_CBC_HMAC_SHA_256'
        ) NOT NULL,
    
    -- SIN CIFRAR
    telefono VARCHAR(255) NOT NULL
);
GO

-- ================================================
-- PASO 4: Copiar datos (se cifrarán automáticamente)
-- ================================================
SET IDENTITY_INSERT Cliente ON;
GO

INSERT INTO Cliente (id, nombre, numero_tarjeta, email, telefono)
SELECT id, nombre, numero_tarjeta, email, telefono
FROM Cliente_Old;
GO

SET IDENTITY_INSERT Cliente OFF;
GO

-- ================================================
-- PASO 5: Eliminar tabla vieja
-- ================================================
DROP TABLE Cliente_Old;
GO

-- ================================================
-- VERIFICACIÓN
-- ================================================
PRINT 'Verificando configuración...';
GO

-- Ver Column Master Keys
SELECT * FROM sys.column_master_keys;
GO

-- Ver Column Encryption Keys
SELECT * FROM sys.column_encryption_keys;
GO

-- Ver columnas cifradas
SELECT 
    c.name AS ColumnName,
    t.name AS TableName,
    cek.name AS EncryptionKeyName,
    CASE ce.encryption_type
        WHEN 1 THEN 'Deterministic'
        WHEN 2 THEN 'Randomized'
    END AS EncryptionType
FROM sys.columns c
INNER JOIN sys.tables t ON c.object_id = t.object_id
INNER JOIN sys.column_encryption_keys cek 
    ON c.column_encryption_key_id = cek.column_encryption_key_id
INNER JOIN sys.column_encryption_key_values ce
    ON cek.column_encryption_key_id = ce.column_encryption_key_id
WHERE t.name = 'Cliente';
GO

PRINT 'Configuración completada.';
GO
