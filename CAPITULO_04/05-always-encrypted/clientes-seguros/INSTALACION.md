# Guía de Instalación - Proyecto Clientes Seguros

**Proyecto:** Sistema de gestión de clientes con Always Encrypted  
**Stack:** Quarkus 3.28.3 + SQL Server 2022 + Docker

---

## 📋 Tabla de Contenidos

1. [Requisitos Previos](#requisitos-previos)
2. [Instalación de Docker](#instalación-de-docker)
3. [Levantar SQL Server](#levantar-sql-server)
4. [Crear Base de Datos](#crear-base-de-datos)
5. [Crear Proyecto Quarkus](#crear-proyecto-quarkus)
6. [Configuración](#configuración)
7. [Crear Estructura del Proyecto](#crear-estructura-del-proyecto)
8. [Ejecutar el Proyecto](#ejecutar-el-proyecto)
9. [Probar la API](#probar-la-api)
10. [Comandos Útiles de Docker](#comandos-útiles-de-docker)

---

## 1. Requisitos Previos

- **Java 21** (verificar: `java -version`)
- **Maven 3.8+** (verificar: `mvn -version`)
- **Docker** (lo instalaremos en el siguiente paso)
- **cURL** (incluido en Windows 10+, macOS, Linux)

---

## 2. Instalación de Docker

### ¿Qué es Docker?

Docker permite ejecutar aplicaciones en **contenedores**: paquetes aislados que incluyen todo lo necesario para correr (código, runtime, librerías, etc.).

**Analogía:** Es como una "mini máquina virtual" pero más ligera y rápida.

**¿Por qué lo usamos?**
- ✅ SQL Server listo en 1 comando (sin instalar nada en tu sistema)
- ✅ Fácil de eliminar cuando ya no lo necesites
- ✅ Mismo ambiente para todos los alumnos

---

### Instalación según tu Sistema Operativo

#### macOS

1. Descargar: https://www.docker.com/products/docker-desktop/
2. Abrir el archivo `.dmg` descargado
3. Arrastrar Docker a la carpeta **Aplicaciones**
4. Abrir **Docker Desktop** desde Aplicaciones
5. Esperar a que diga "Docker Desktop is running" (ícono de ballena arriba)

**Nota:** Te pedirá crear una cuenta, pero puedes hacer clic en **"Skip"** o **"Continue without signing in"**

#### Windows

1. Descargar: https://www.docker.com/products/docker-desktop/
2. Ejecutar el instalador
3. Reiniciar si lo solicita
4. Abrir **Docker Desktop**
5. Esperar a que esté listo

#### Linux

```bash
sudo apt-get update
sudo apt-get install docker.io
sudo systemctl start docker
sudo systemctl enable docker
```

---

### Verificar Instalación

```bash
docker --version
```

**Salida esperada:**
```
Docker version 28.5.1, build e180ab8
```

Si ves la versión, ✅ Docker está instalado correctamente.

---

## 3. Levantar SQL Server

### Comando para Crear el Contenedor

```bash
docker run -e 'ACCEPT_EULA=Y' -e 'SA_PASSWORD=Pass123!Admin' \
  -p 1433:1433 --name sqlserver \
  -d mcr.microsoft.com/mssql/server:2022-latest
```

**¿Qué hace este comando?**

| Parámetro | Descripción |
|-----------|-------------|
| `docker run` | Crea y arranca un contenedor |
| `-e 'ACCEPT_EULA=Y'` | Acepta la licencia de SQL Server |
| `-e 'SA_PASSWORD=...'` | Contraseña del usuario administrador `sa` |
| `-p 1433:1433` | Mapea puerto 1433 del contenedor al puerto 1433 de tu máquina |
| `--name sqlserver` | Nombre del contenedor (para referenciarlo fácilmente) |
| `-d` | Modo detached (corre en segundo plano) |
| `mcr.microsoft.com/...` | Imagen oficial de SQL Server 2022 de Microsoft |

**Nota para Mac M1/M2/M3:** Verás un warning sobre emulación. Es normal y no afecta el funcionamiento para desarrollo.

---

### Verificar que SQL Server Está Corriendo

```bash
# Ver contenedores activos
docker ps
```

**Salida esperada:**
```
CONTAINER ID   IMAGE                                        STATUS         PORTS                    NAMES
8fa6c681d0af   mcr.microsoft.com/mssql/server:2022-latest   Up 2 minutes   0.0.0.0:1433->1433/tcp   sqlserver
```

Si ves `STATUS: Up`, ✅ SQL Server está corriendo.

---

### Verificar que SQL Server Está Listo para Conexiones

```bash
docker logs sqlserver
```

**Buscar esta línea:**
```
SQL Server is now ready for client connections.
```

Si la ves, ✅ SQL Server está completamente iniciado.

**Nota:** SQL Server demora ~30 segundos en estar listo después de arrancar el contenedor.

---

## 4. Crear Base de Datos

SQL Server está corriendo pero necesitamos crear la base de datos `BancoDB`:

```bash
docker exec sqlserver /opt/mssql-tools18/bin/sqlcmd \
  -S localhost -U sa -P 'Pass123!Admin' -C \
  -Q "CREATE DATABASE BancoDB"
```

**¿Qué hace?**
- Ejecuta un comando SQL dentro del contenedor
- Crea la base de datos `BancoDB`

**Si no sale nada = éxito.** ✅

---

## 5. Crear Proyecto Quarkus

### Ubicarse en la Carpeta Correcta

```bash
# Ir a la carpeta del capítulo
cd CAPITULO_4_2
```

---

### Crear Proyecto con Maven

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.28.3:create \
    -DprojectGroupId=pe.banco \
    -DprojectArtifactId=clientes-seguros \
    -DprojectVersion=1.0.0-SNAPSHOT \
    -Dextensions="resteasy-reactive-jackson,hibernate-orm-panache,jdbc-mssql"
```

**Extensiones incluidas:**
- `resteasy-reactive-jackson`: REST API reactivo con soporte JSON
- `hibernate-orm-panache`: ORM simplificado con Panache
- `jdbc-mssql`: Driver JDBC para SQL Server

**Maven creará automáticamente la carpeta `clientes-seguros`.**

---

### Entrar al Proyecto

```bash
cd clientes-seguros
```

---

## 6. Configuración

### application.properties

```bash
cat > src/main/resources/application.properties << 'EOF'
# SQL Server
quarkus.datasource.db-kind=mssql
quarkus.datasource.username=sa
quarkus.datasource.password=Pass123!Admin
quarkus.datasource.jdbc.url=jdbc:sqlserver://localhost:1433;databaseName=BancoDB;encrypt=false;trustServerCertificate=true

# Hibernate
quarkus.hibernate-orm.database.generation=drop-and-create
quarkus.hibernate-orm.log.sql=true

# HTTP
quarkus.http.port=8080
EOF
```

**Configuración explicada:**

| Propiedad | Valor | Descripción |
|-----------|-------|-------------|
| `db-kind` | `mssql` | Tipo de base de datos |
| `username` | `sa` | Usuario administrador de SQL Server |
| `password` | `Pass123!Admin` | Contraseña del usuario sa |
| `jdbc.url` | `jdbc:sqlserver://...` | URL de conexión |
| `database.generation` | `drop-and-create` | Recrea las tablas cada vez (solo desarrollo) |
| `log.sql` | `true` | Muestra las queries SQL en logs |
| `http.port` | `8080` | Puerto donde corre la API |

---

## 7. Crear Estructura del Proyecto

### 7.1 Crear Directorios

```bash
mkdir -p src/main/java/pe/banco/clientes/entity
mkdir -p src/main/java/pe/banco/clientes/repository
mkdir -p src/main/java/pe/banco/clientes/resource
mkdir -p src/main/java/pe/banco/clientes/dto
```

---

### 7.2 Entidad Cliente

```bash
cat > src/main/java/pe/banco/clientes/entity/Cliente.java << 'EOF'
package pe.banco.clientes.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;

@Entity
public class Cliente extends PanacheEntity {

    @Column(nullable = false)
    public String nombre;

    @Column(name = "numero_tarjeta", nullable = false)
    public String numeroTarjeta;  // Esta columna será CIFRADA con Always Encrypted

    @Column(nullable = false)
    public String email;  // Esta columna será CIFRADA con Always Encrypted

    @Column(nullable = false)
    public String telefono;  // Sin cifrar

    public Cliente() {
    }

    public Cliente(String nombre, String numeroTarjeta, String email, String telefono) {
        this.nombre = nombre;
        this.numeroTarjeta = numeroTarjeta;
        this.email = email;
        this.telefono = telefono;
    }
}
EOF
```

---

### 7.3 Repository

```bash
cat > src/main/java/pe/banco/clientes/repository/ClienteRepository.java << 'EOF'
package pe.banco.clientes.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import pe.banco.clientes.entity.Cliente;

import java.util.List;

@ApplicationScoped
public class ClienteRepository implements PanacheRepositoryBase<Cliente, Long> {

    public List<Cliente> buscarPorTarjeta(String numeroTarjeta) {
        return list("numeroTarjeta", numeroTarjeta);
    }
}
EOF
```

---

### 7.4 DTO

```bash
cat > src/main/java/pe/banco/clientes/dto/ClienteRequest.java << 'EOF'
package pe.banco.clientes.dto;

public class ClienteRequest {
    public String nombre;
    public String numeroTarjeta;
    public String email;
    public String telefono;

    public ClienteRequest() {
    }
}
EOF
```

---

### 7.5 Resource (REST API)

```bash
cat > src/main/java/pe/banco/clientes/resource/ClienteResource.java << 'EOF'
package pe.banco.clientes.resource;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pe.banco.clientes.dto.ClienteRequest;
import pe.banco.clientes.entity.Cliente;
import pe.banco.clientes.repository.ClienteRepository;

import java.net.URI;
import java.util.List;

@Path("/api/v1/clientes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClienteResource {

    @Inject
    ClienteRepository repository;

    @GET
    public List<Cliente> listarTodos() {
        return repository.listAll();
    }

    @GET
    @Path("/{id}")
    public Response buscarPorId(@PathParam("id") Long id) {
        Cliente cliente = repository.findById(id);
        if (cliente == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(cliente).build();
    }

    @POST
    @Transactional
    public Response crear(ClienteRequest request) {
        Cliente cliente = new Cliente(
            request.nombre,
            request.numeroTarjeta,
            request.email,
            request.telefono
        );
        repository.persist(cliente);
        return Response.created(URI.create("/api/v1/clientes/" + cliente.id))
                .entity(cliente)
                .build();
    }

    @GET
    @Path("/tarjeta/{numero}")
    public List<Cliente> buscarPorTarjeta(@PathParam("numero") String numero) {
        return repository.buscarPorTarjeta(numero);
    }
}
EOF
```

---

### 7.6 Datos Iniciales

```bash
cat > src/main/resources/import.sql << 'EOF'
INSERT INTO Cliente (id, nombre, numero_tarjeta, email, telefono) VALUES (1, 'Juan Pérez', '4532-1234-5678-9012', 'juan.perez@banco.com', '+56912345678');
INSERT INTO Cliente (id, nombre, numero_tarjeta, email, telefono) VALUES (2, 'María González', '5412-9876-5432-1098', 'maria.gonzalez@banco.com', '+56987654321');
INSERT INTO Cliente (id, nombre, numero_tarjeta, email, telefono) VALUES (3, 'Carlos López', '4716-5555-4444-3333', 'carlos.lopez@banco.com', '+56922334455');
EOF
```

---

## 8. Ejecutar el Proyecto

### Modo Desarrollo (con Live Reload)

```bash
./mvnw quarkus:dev
```

**Salida esperada:**
```
__  ____  __  _____   ___  __ ____  ______ 
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
...
INFO  [io.quarkus] clientes-seguros 1.0.0-SNAPSHOT on JVM started in 2.774s. 
Listening on: http://localhost:8080
```

**Warnings sobre "drop table Cliente":** Son normales la primera vez. Hibernate intenta borrar tablas que aún no existen.

---

## 9. Probar la API

### 9.1 Listar Todos los Clientes

```bash
curl http://localhost:8080/api/v1/clientes
```

**Respuesta esperada:**
```json
[
  {
    "id": 1,
    "nombre": "Juan Pérez",
    "numeroTarjeta": "4532-1234-5678-9012",
    "email": "juan.perez@banco.com",
    "telefono": "+56912345678"
  },
  {
    "id": 2,
    "nombre": "María González",
    "numeroTarjeta": "5412-9876-5432-1098",
    "email": "maria.gonzalez@banco.com",
    "telefono": "+56987654321"
  },
  {
    "id": 3,
    "nombre": "Carlos López",
    "numeroTarjeta": "4716-5555-4444-3333",
    "email": "carlos.lopez@banco.com",
    "telefono": "+56922334455"
  }
]
```

---

### 9.2 Buscar Cliente por ID

```bash
curl http://localhost:8080/api/v1/clientes/1
```

---

### 9.3 Crear Nuevo Cliente

```bash
curl -X POST http://localhost:8080/api/v1/clientes \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Ana Torres",
    "numeroTarjeta": "6011-1111-2222-3333",
    "email": "ana.torres@banco.com",
    "telefono": "+56933445566"
  }'
```

---

### 9.4 Buscar por Número de Tarjeta

```bash
curl http://localhost:8080/api/v1/clientes/tarjeta/4532-1234-5678-9012
```

---

## 10. Comandos Útiles de Docker

### Ver Contenedores Activos

```bash
docker ps
```

---

### Ver Todos los Contenedores (activos e inactivos)

```bash
docker ps -a
```

---

### Ver Logs del Contenedor

```bash
docker logs sqlserver

# Ver logs en tiempo real
docker logs -f sqlserver
```

---

### Detener SQL Server

```bash
docker stop sqlserver
```

---

### Iniciar SQL Server (si está detenido)

```bash
docker start sqlserver
```

---

### Reiniciar SQL Server

```bash
docker restart sqlserver
```

---

### Eliminar el Contenedor

```bash
# Primero detenerlo
docker stop sqlserver

# Luego eliminarlo
docker rm sqlserver
```

**Nota:** Esto NO elimina la imagen de SQL Server. Para volver a crear el contenedor, ejecuta nuevamente el comando `docker run` del paso 3.

---

### Conectarse a SQL Server desde Terminal

```bash
docker exec -it sqlserver /opt/mssql-tools18/bin/sqlcmd \
  -S localhost -U sa -P 'Pass123!Admin' -C
```

Una vez dentro:
```sql
-- Ver bases de datos
SELECT name FROM sys.databases;
GO

-- Usar BancoDB
USE BancoDB;
GO

-- Ver tablas
SELECT * FROM INFORMATION_SCHEMA.TABLES;
GO

-- Ver datos de clientes
SELECT * FROM Cliente;
GO

-- Salir
EXIT
```

---

### Eliminar la Imagen de SQL Server (liberar espacio)

```bash
# Primero eliminar el contenedor
docker stop sqlserver
docker rm sqlserver

# Luego eliminar la imagen
docker rmi mcr.microsoft.com/mssql/server:2022-latest
```

---

## 🎯 Resumen de lo que Logramos

✅ **Docker instalado y funcionando**  
✅ **SQL Server 2022 corriendo en contenedor**  
✅ **Base de datos BancoDB creada**  
✅ **Proyecto Quarkus configurado**  
✅ **API REST operativa con 4 endpoints**  
✅ **Datos de prueba cargados**  

---

## 📚 Próximos Pasos

En la siguiente sesión implementaremos:
- **Always Encrypted**: Cifrado transparente de columnas sensibles
- **Column Master Key (CMK)**: Gestión de claves maestras
- **Column Encryption Key (CEK)**: Claves de cifrado de columnas
- **Configuración del driver JDBC** para soportar cifrado

---

## 🐛 Solución de Problemas

### Error: "Cannot open database BancoDB"

**Causa:** La base de datos no existe.

**Solución:**
```bash
docker exec sqlserver /opt/mssql-tools18/bin/sqlcmd \
  -S localhost -U sa -P 'Pass123!Admin' -C \
  -Q "CREATE DATABASE BancoDB"
```

---

### Error: "docker: command not found"

**Causa:** Docker no está instalado o no está en el PATH.

**Solución:** Instalar Docker Desktop (ver paso 2).

---

### Error: "port 1433 already in use"

**Causa:** Otro servicio usa el puerto 1433.

**Solución:**
```bash
# Ver qué usa el puerto
lsof -i :1433

# Cambiar el puerto en docker run:
docker run ... -p 1444:1433 ... # Mapea a puerto 1444 local

# Y actualizar application.properties:
# jdbc:sqlserver://localhost:1444;...
```

---

### SQL Server no arranca (Mac ARM)

**Causa:** Emulación puede fallar en algunos casos.

**Solución:** Usar imagen específica para ARM:
```bash
docker run -e 'ACCEPT_EULA=Y' -e 'SA_PASSWORD=Pass123!Admin' \
  -p 1433:1433 --name sqlserver \
  -d --platform linux/amd64 \
  mcr.microsoft.com/azure-sql-edge
```

---

## 📖 Recursos Adicionales

- [Docker Documentation](https://docs.docker.com/)
- [SQL Server on Docker](https://hub.docker.com/_/microsoft-mssql-server)
- [Quarkus Guides](https://quarkus.io/guides/)
- [Hibernate Panache](https://quarkus.io/guides/hibernate-orm-panache)

---

**Autor:** Material didáctico - Curso Quarkus - NETEC
**Capítulo:** 4.2 - Always Encrypted con SQL Server  
**Fecha:** Octubre 2025  
**Versión:** 1.0.0