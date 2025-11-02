# 🏦 TasaCorp API - Configuración y Perfiles en Quarkus

## Capítulo 7: Externalización de Configuraciones y Perfiles de Entorno

---

## 📖 Descripción

**TasaCorp** es un sistema bancario para consulta y conversión de tasas de cambio de divisas. Este ejercicio práctico está diseñado para dominar la **configuración y gestión de perfiles** en Quarkus, cubriendo desde conceptos básicos hasta integración con HashiCorp Vault.

**Contexto Bancario:**
- 🏦 Banco peruano: TasaCorp
- 💱 Operaciones: Compra/venta de USD, EUR, MXN
- 🌍 Ambientes: Desarrollo, Testing, Producción
- 🔐 Seguridad: Secrets protegidos con Vault

---

## 🎯 Objetivos de Aprendizaje

✅ Externalizar configuraciones con `application.properties` y `application.yaml`  
✅ Entender prioridades de carga (System Props > ENV > Files)  
✅ Usar perfiles de entorno (`%dev`, `%test`, `%prod`)  
✅ Proteger información sensible con HashiCorp Vault  
✅ Aplicar mejores prácticas de configuración en producción  

---

## 📚 Documentación del Ejercicio

### 🛠️ Guías Prácticas (Paso a Paso)

| Documento | Duración | Descripción |
|-----------|----------|-------------|
| **[README-PARTE1.md](README-PARTE1.md)** | 30 min | **Externalización de Configuraciones**<br>Properties, YAML, inyección, prioridades de carga |
| **[README-PARTE2.md](README-PARTE2.md)** | 30 min | **Perfiles y Configuración Sensible**<br>%dev, %test, %prod, integración con Vault |

### 📖 Teoría Profunda

| Documento | Contenido |
|-----------|-----------|
| **[TEORIA-PARTE1.md](TEORIA-PARTE1.md)** | **Fundamentos de Configuración**<br>Historia, MicroProfile Config, tipos de datos, patrones, mejores prácticas |
| **[TEORIA-PARTE2.md](TEORIA-PARTE2.md)** | **Perfiles y Seguridad**<br>Arquitectura de perfiles, HashiCorp Vault, gestión de secretos, casos reales |

---

## 🚀 Inicio Rápido

### Prerequisitos

```bash
# Java 17+
java -version

# Maven
mvn -version

# Docker Desktop (para Vault)
docker --version
```

### Crear Proyecto

#### Windows
```powershell
mvn io.quarkus.platform:quarkus-maven-plugin:3.17.5:create `
    -DprojectGroupId=pe.banco `
    -DprojectArtifactId=tasacorp-api `
    -Dextensions="resteasy-reactive-jackson,config-yaml,vault"

cd tasacorp-api
```

#### macOS/Linux
```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.17.5:create \
    -DprojectGroupId=pe.banco \
    -DprojectArtifactId=tasacorp-api \
    -Dextensions="resteasy-reactive-jackson,config-yaml,vault"

cd tasacorp-api
```

### Arrancar

#### Windows
```powershell
.\mvnw.cmd quarkus:dev
```

#### macOS/Linux
```bash
./mvnw quarkus:dev
```

Abre: http://localhost:8080/api/tasas/config

---

## 📁 Estructura del Proyecto

```
tasacorp-api/
├── README.md                    ← Estás aquí
├── README-PARTE1.md             ← Guía: Externalización
├── README-PARTE2.md             ← Guía: Perfiles + Vault
├── TEORIA-PARTE1.md             ← Teoría: Configuración
├── TEORIA-PARTE2.md             ← Teoría: Seguridad
├── test-part1-config.sh         ← Script de pruebas Parte 1
├── test-part2-profiles.sh       ← Script de pruebas Parte 2
├── docker-compose.yml           ← Vault para Parte 2
├── pom.xml
└── src/
    └── main/
        ├── java/pe/banco/tasacorp/
        │   ├── config/              ← @ConfigMapping
        │   ├── model/               ← DTOs
        │   ├── service/             ← Lógica de negocio
        │   └── resource/            ← REST endpoints
        └── resources/
            ├── application.properties
            └── application.yaml
```

---

## 🎓 Ruta de Aprendizaje

### Parte 1: Externalización (30 min)

1. **Leer:** [TEORIA-PARTE1.md](TEORIA-PARTE1.md) (10 min)
2. **Practicar:** [README-PARTE1.md](README-PARTE1.md) (20 min)
   - Crear proyecto
   - Configurar properties y yaml
   - Probar prioridades de carga
3. **Ejecutar pruebas:**
   ```bash
   ./test-part1-config.sh
   ```
   📄 Genera: `test-part1-config-YYYY-MM-DD_HH-MM-SS.txt`

**Al finalizar dominarás:**
- application.properties vs application.yaml
- @ConfigProperty vs @ConfigMapping
- Prioridades: System Properties > ENV > Files

### Parte 2: Perfiles y Vault (30 min)

1. **Leer:** [TEORIA-PARTE2.md](TEORIA-PARTE2.md) (10 min)
2. **Practicar:** [README-PARTE2.md](README-PARTE2.md) (20 min)
   - Configurar perfiles (dev, test, prod)
   - Levantar Vault con Docker
   - Integrar Vault con Quarkus
3. **Ejecutar pruebas:**
   ```bash
   ./test-part2-profiles.sh
   ```
   📄 Genera: `test-part2-profiles-YYYY-MM-DD_HH-MM-SS.txt`

**Al finalizar dominarás:**
- Perfiles de entorno
- Configuración específica por ambiente
- Protección de secretos con Vault

---

## 🧪 Scripts de Prueba Automatizados

El proyecto incluye scripts interactivos que prueban todas las funcionalidades y **generan logs automáticamente**.

### Script Parte 1: test-part1-config.sh

**Pruebas incluidas (7):**
- ✅ Configuración Base desde Properties
- ✅ Inyección de Configuración (@ConfigProperty y @ConfigMapping)
- ✅ Conversión usando Configuración Base
- ✅ Variables de Entorno (explicación)
- ✅ System Properties como Máxima Prioridad
- ✅ Properties vs YAML
- ✅ Tasas desde Configuración YAML

**Cómo ejecutar:**
```bash
# Terminal 1: Arrancar la aplicación
./mvnw quarkus:dev

# Terminal 2: Ejecutar pruebas
./test-part1-config.sh
```

**Resultado:**
- 🖥️ Output interactivo con colores en terminal
- 📄 Archivo de log generado automáticamente: `test-part1-config-2025-11-01_19-36-33.txt`

### Script Parte 2: test-part2-profiles.sh

**Pruebas incluidas (10):**

**Perfil DEV (3 pruebas):**
- ✅ Configuración del Perfil DEV
- ✅ Conversión SIN Comisión
- ✅ Límite Ilimitado

**Perfil TEST (3 pruebas):**
- ✅ Configuración del Perfil TEST
- ✅ Conversión CON Comisión (1.5%)
- ✅ Límite Transaccional EXCEDIDO

**Perfil PROD (4 pruebas):**
- ✅ Configuración PROD + Vault
- ✅ Conversión en PROD (2.5% comisión)
- ✅ Límite Alto en Producción
- ✅ Exceder Límite en PROD

**Cómo ejecutar:**
```bash
# El script te guiará para probar los 3 perfiles

# Reinicia la app con cada perfil:
./mvnw quarkus:dev                          # DEV
./mvnw quarkus:dev -Dquarkus.profile=test   # TEST
./mvnw quarkus:dev -Dquarkus.profile=prod   # PROD

# Ejecutar pruebas
./test-part2-profiles.sh
```

**Resultado:**
- 🖥️ Output interactivo con colores en terminal
- 📄 Archivo de log generado automáticamente: `test-part2-profiles-2025-11-01_23-10-15.txt`
- 📊 Incluye tabla comparativa de los 3 perfiles

### 📄 Archivos de Log Generados

Los scripts generan archivos `.txt` con timestamp que contienen:

✅ Header con fecha, hora, y URL de la API  
✅ Todas las pruebas ejecutadas con sus resultados  
✅ Respuestas HTTP completas (JSON formateado)  
✅ Validaciones y mensajes de éxito/error  
✅ Resumen final de conceptos demostrados  
✅ Footer con ubicación del archivo  

**Beneficios:**
- 📚 Logs permanentes para estudio offline
- 📤 Fácil de compartir con instructores
- 📝 Documentación automática de pruebas
- 🔍 Comparación entre múltiples ejecuciones

---

## 🧪 Endpoints Disponibles

| Endpoint | Descripción | Ejemplo |
|----------|-------------|---------|
| `GET /api/tasas/config` | Ver configuración actual | Ver ambiente activo |
| `GET /api/tasas/{moneda}` | Consultar tasa | `/api/tasas/USD` |
| `GET /api/tasas/convertir/{moneda}?monto=X` | Convertir monto | `/api/tasas/convertir/USD?monto=1000` |
| `GET /api/tasas/health` | Health check | Estado del servicio |

---

## 📊 Comparativa de Perfiles

| Característica | DEV | TEST | PROD |
|----------------|-----|------|------|
| **Comisión** | 0.0% | 1.5% | 2.5% |
| **Límite Trans.** | 999,999 | $1,000 | $50,000 |
| **Cache** | ❌ | ✅ (30 min) | ✅ (15 min) |
| **Logs** | DEBUG | INFO | ERROR |
| **API Key** | Hardcoded | Hardcoded | 🔐 Vault |
| **Ambiente** | desarrollo | testing | producción |

---

## 🔧 Tecnologías

- **Quarkus** 3.17.5+
- **Java** 17+
- **Maven** 3.8+
- **Docker** (para Vault)
- **HashiCorp Vault** 1.15.2

---

## 📖 Recursos Adicionales

- [Quarkus Configuration Guide](https://quarkus.io/guides/config)
- [Quarkus Vault Extension](https://quarkus.io/guides/vault)
- [MicroProfile Config](https://github.com/eclipse/microprofile-config)
- [HashiCorp Vault](https://www.vaultproject.io/)
- [12-Factor App](https://12factor.net/)

---

## 👨‍🏫 Para el Instructor

Este ejercicio está diseñado para clases de **alto nivel técnico** con énfasis en:

✅ **Práctica sobre teoría** (60/40)  
✅ **Casos reales bancarios** (no ejemplos genéricos)  
✅ **Configuración production-ready** (no solo "hello world")  
✅ **Seguridad desde el día 1** (Vault integrado)  
✅ **Scripts de prueba automatizados** (con generación de logs)  

**Duración total:** 60 minutos (30 min cada parte)

---

## ✅ Verificación Rápida

Antes de dar por completado el ejercicio, verifica:

- [ ] Proyecto creado y compila sin errores
- [ ] Entiendes properties vs yaml
- [ ] Probaste las 3 prioridades de carga
- [ ] Los 3 perfiles funcionan (dev, test, prod)
- [ ] Vault está corriendo y conectado
- [ ] Secretos se leen desde Vault en PROD
- [ ] **Ejecutaste los scripts de prueba y revisaste los logs generados**

---

## 📝 Notas Importantes

### 🔥 Sobre los Scripts de Prueba

Los scripts `test-part1-config.sh` y `test-part2-profiles.sh`:

✅ **Generan archivos .txt automáticamente** con timestamp  
✅ **Son interactivos** - te guían paso a paso  
✅ **Tienen colores** en terminal para mejor legibilidad  
✅ **Documentan todo** - respuestas HTTP completas, validaciones, conceptos  
✅ **Siguen el patrón** establecido en capítulos anteriores del curso  

**No son solo para demostración**, son herramientas educativas completas que:
- Te ayudan a entender cada concepto
- Generan evidencia de tu aprendizaje
- Pueden compartirse con instructores
- Sirven como referencia futura

---

## 📝 Licencia

Material educativo para el **Curso de Quarkus - Capítulo 7**

---

**¿Listo para empezar? Comienza con [README-PARTE1.md](README-PARTE1.md)** 🚀