# CAPÍTULO 5: TESTING Y VALIDACIÓN EN QUARKUS

## PREREQUISITOS

1. Docker Desktop corriendo
2. Java 21 instalado
3. Terminal (zsh en Mac, Git Bash en Windows)

---

## SECUENCIA 1: Tests Automatizados (JUnit)

```bash
cd evaluacion-crediticia
./mvnw test
```

**¿Qué pasa?**
- Dev Services levanta PostgreSQL en Docker automáticamente
- Ejecuta todos los tests
- Destruye el contenedor al terminar

**Resultado esperado:** `BUILD SUCCESS`

---

## SECUENCIA 2: Aplicación + Script Interactivo

**Terminal 1:**
```bash
./mvnw quarkus:dev
```
Esperar: `Listening on: http://localhost:8080`

**Terminal 2:**
```bash
./test-evaluacion-crediticia.sh
```

Detener: `Ctrl+C`

---

## SECUENCIA 3: Binario Nativo

### 3.1: Levantar PostgreSQL

**Si es primera vez:**
```bash
docker run --name postgres-native -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=evaluacion_db -p 5432:5432 -d postgres:16-alpine
```

**Si ya existe el contenedor:**
```bash
docker start postgres-native
```

**Si da error de nombre en uso:**
```bash
docker rm -f postgres-native
docker run --name postgres-native -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=evaluacion_db -p 5432:5432 -d postgres:16-alpine
```

### 3.2: Compilar (5-10 min)
```bash
./mvnw clean package -Pnative -DskipTests
```

### 3.3: Ejecutar binario
```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/evaluacion_db
export DATABASE_USER=postgres
export DATABASE_PASSWORD=postgres
export QUARKUS_HIBERNATE_ORM_DATABASE_GENERATION=drop-and-create

./target/evaluacion-crediticia-1.0.0-SNAPSHOT-runner
```

### 3.4: Probar (otra terminal)
```bash
./test-evaluacion-crediticia.sh
```

### 3.5: Limpiar
```bash
# Ctrl+C en el runner
docker stop postgres-native && docker rm postgres-native
```

---

## SECUENCIA 4: Tests JUnit contra Binario Nativo

### 4.1: Detener el binario si está corriendo
```
Ctrl+C
```

### 4.2: Asegurar PostgreSQL arriba
```bash
docker start postgres-native
```

### 4.3: Ejecutar tests (sin recompilar)
```bash
./mvnw failsafe:integration-test -Pnative
```

---

## ORDEN PARA CLASE

| # | Comando | Tiempo | Qué muestra |
|---|---------|--------|-------------|
| 1 | `./mvnw test` | 30s | Dev Services (PostgreSQL automático) |
| 2 | `./mvnw quarkus:dev` + script | 15min | App en vivo + validaciones |
| 3 | Binario nativo + script | 10min | Arranque en milisegundos |
| 4 | Tests nativos | 5min | Tests contra binario compilado |

---

## CONCEPTOS CLAVE

| Concepto | Qué es |
|----------|--------|
| `@QuarkusTest` | Arranca Quarkus + Dev Services + CDI |
| Dev Services | PostgreSQL automático sin configurar nada |
| Given-When-Then | Estructura para tests legibles |
| Bean Validation | `@NotNull`, `@Email`, `@Min`, `@Max` |
| `@DniValido` | Validador custom (8 dígitos peruanos) |
| Exception Mapper | Convierte excepciones en JSON amigable |
| `@QuarkusIntegrationTest` | Tests contra binario nativo |

---

## COMPARATIVA JVM vs NATIVO

| Métrica | JVM (`quarkus:dev`) | Nativo (runner) |
|---------|---------------------|-----------------|
| Arranque | ~2 segundos | ~20 milisegundos |
| Memoria | ~200 MB | ~50 MB |
| Uso | Desarrollo | Producción/Serverless |

---

## TROUBLESHOOTING

**Error: "Connection refused port 5432"**
```bash
docker start postgres-native
```

**Error: "missing table solicitudes_credito"**
```bash
export QUARKUS_HIBERNATE_ORM_DATABASE_GENERATION=drop-and-create
```

**Error: "container name already in use"**
```bash
docker rm -f postgres-native
```

**PostgreSQL no arranca (Docker apagado):**
1. Abrir Docker Desktop
2. Esperar que diga "running"
3. `docker start postgres-native`