# Etapa 1: BUILD - Compilar la aplicación
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copiar archivos de configuración Maven
COPY pom.xml .

# Descargar dependencias (esta capa se cachea)
RUN mvn dependency:go-offline

# Copiar código fuente
COPY src ./src

# Compilar la aplicación
RUN mvn clean package -DskipTests

# Etapa 2: RUNTIME - Ejecutar la aplicación
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiar el JAR compilado desde la etapa anterior
COPY --from=builder /build/target/quarkus-app/ ./

# Exponer el puerto
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/api/bureau/health || exit 1

# Comando para ejecutar la aplicación
CMD ["java", "-jar", "quarkus-run.jar"]
