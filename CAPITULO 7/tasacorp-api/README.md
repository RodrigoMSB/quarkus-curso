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

**Al finalizar dominarás:**
- Perfiles de entorno
- Configuración específica por ambiente
- Protección de secretos con Vault

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
| **Límite Trans.** | Ilimitado | $1,000 | $50,000 |
| **Cache** | ❌ | ✅ | ✅ |
| **Logs** | DEBUG | INFO | ERROR |
| **API Key** | Hardcoded | Hardcoded | 🔐 Vault |

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

---

## 📝 Licencia

Material educativo para el **Curso de Quarkus - Capítulo 7**

---

**¿Listo para empezar? Comienza con [README-PARTE1.md](README-PARTE1.md)** 🚀