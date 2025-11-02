# 🛡️ Validaciones en APIs REST Reactivas con Quarkus

**Capítulo 4.1: Bean Validation vs Validación Programática en Contexto Reactivo**

---

## 📚 Tabla de Contenidos

1. [Introducción](#1-introducción)
2. [Bean Validation en REST Clásico](#2-bean-validation-en-rest-clásico)
3. [El Problema con REST Reactivo](#3-el-problema-con-rest-reactivo)
4. [Solución: Validación Programática](#4-solución-validación-programática)
5. [Comparativa: Bean Validation vs Programática](#5-comparativa-bean-validation-vs-programática)
6. [Implementación Completa](#6-implementación-completa)
7. [Mejores Prácticas](#7-mejores-prácticas)
8. [Casos de Uso](#8-casos-de-uso)
9. [Troubleshooting](#9-troubleshooting)
10. [Conclusiones](#10-conclusiones)

---

## 1. Introducción

### ¿Por qué son Importantes las Validaciones?

Las validaciones son la **primera línea de defensa** de tu API. Sin ellas:

```java
// Sin validación ❌
POST /productos
{
  "nombre": "Laptop",
  "precio": -1000.00,    // ¡Precio negativo!
  "stock": -50           // ¡Stock negativo!
}

// Resultado: Datos corruptos en la base de datos
```

**Consecuencias:**
- 💾 **Integridad de datos comprometida**
- 🔒 **Vulnerabilidades de seguridad**
- 😡 **Mala experiencia de usuario** (errores tardíos)
- 🐛 **Bugs en lógica de negocio** (cálculos con valores inválidos)

---

### Dos Enfoques de Validación

| Enfoque | Descripción | Tecnología |
|---------|-------------|------------|
| **Declarativo** | Anotaciones en DTOs | Bean Validation (Jakarta) |
| **Programático** | Código explícito | Validación manual |

---

## 2. Bean Validation en REST Clásico

### 2.1 ¿Qué es Bean Validation?

**Bean Validation** (Jakarta Bean Validation) es el estándar de Java para validar objetos usando **anotaciones**.

### 2.2 Funcionamiento en REST Clásico (Bloqueante)

#### Paso 1: Anotar el DTO

```java
public class ProductoRequest {
    
    @NotNull(message = "El nombre es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    public String nombre;
    
    @NotNull(message = "El precio es obligatorio")
    @Positive(message = "El precio debe ser mayor a 0")
    public Double precio;
    
    @NotNull(message = "El stock es obligatorio")
    @PositiveOrZero(message = "El stock no puede ser negativo")
    public Integer stock;
}
```

#### Paso 2: Usar `@Valid` en el Resource

```java
@Path("/api/v1/productos")
@ApplicationScoped
public class ProductoResource {
    
    @POST
    public Response crear(@Valid ProductoRequest request) {
        // ✅ Si llega aquí, los datos son válidos
        Producto producto = new Producto(
            request.nombre,
            request.descripcion,
            request.precio,
            request.stock
        );
        producto.persist();
        return Response.status(201).entity(producto).build();
    }
}
```

#### Paso 3: Resultado Automático

**Request inválido:**
```bash
curl -X POST http://localhost:8080/api/v1/productos \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "AB",           # ❌ Muy corto
    "precio": -100,           # ❌ Negativo
    "stock": -5               # ❌ Negativo
  }'
```

**Response automático (HTTP 400):**
```json
{
  "title": "Constraint Violation",
  "status": 400,
  "violations": [
    {
      "field": "nombre",
      "message": "El nombre debe tener entre 3 y 100 caracteres"
    },
    {
      "field": "precio",
      "message": "El precio debe ser mayor a 0"
    },
    {
      "field": "stock",
      "message": "El stock no puede ser negativo"
    }
  ]
}
```

**✅ Ventajas en REST Clásico:**
- Validación automática
- Código limpio (solo anotaciones)
- Respuestas estandarizadas
- Funciona out-of-the-box

---

## 3. El Problema con REST Reactivo

### 3.1 El Escenario

Tenemos una API reactiva con los mismos requisitos:

```java
@Path("/api/v1/productos/reactivo")
@ApplicationScoped
public class ProductoReactivoResource {
    
    @POST
    public Uni<Response> crear(@Valid ProductoRequest request) {
        Producto producto = new Producto(
            request.nombre,
            request.descripcion,
            request.precio,
            request.stock
        );
        
        return Panache.withTransaction(() -> repository.persist(producto))
            .onItem().transform(p -> Response.status(201).entity(p).build());
    }
}
```

### 3.2 El Problema: `@Valid` No Funciona

**Request inválido:**
```bash
curl -X POST http://localhost:8080/api/v1/productos/reactivo \
  -H "Content-Type: application/json" \
  -d '{
    "precio": -100.00,
    "stock": -5
  }'
```

**Resultado ESPERADO:** HTTP 400 ❌

**Resultado REAL:** HTTP 201 ✅ (¡Producto creado con datos inválidos!)

```json
{
  "id": 1,
  "nombre": "Producto Inválido",
  "precio": -100.0,    // ❌ Negativo aceptado
  "stock": -5          // ❌ Negativo aceptado
}
```

---

### 3.3 ¿Por Qué Falla?

#### Causa Raíz: Incompatibilidad con `Uni<Response>`

En Quarkus REST Reactivo, cuando un método retorna `Uni<Response>`, el framework tiene dificultades para interceptar y ejecutar las validaciones Bean Validation **antes** de que se ejecute el código reactivo.

**Flujo en REST Clásico (Funciona):**
```
Request → Bean Validation → [❌ Falla] → HTTP 400
                          ↓ [✅ Pasa]
                          Código del método → Response
```

**Flujo en REST Reactivo (No funciona siempre):**
```
Request → Método ejecuta inmediatamente (retorna Uni)
              ↓
          Bean Validation intenta ejecutar (demasiado tarde)
              ↓
          Datos inválidos ya persistidos ❌
```

#### Razón Técnica

`Uni<Response>` es una **promesa** de un resultado futuro. El método **retorna inmediatamente** antes de que las validaciones puedan ejecutarse en el flujo reactivo.

---

### 3.4 Analogía: El Mesero Distraído

**REST Clásico (Bloqueante):**
```
Cliente: "Quiero un café"
Mesero: [verifica que hay café] ✅
Mesero: [prepara café] ☕
Mesero: [entrega café]
```

**REST Reactivo con @Valid (El problema):**
```
Cliente: "Quiero un café"
Mesero: "¡Ya va!" [se va inmediatamente]
        ↓
[En la cocina, nadie verifica si hay café]
        ↓
Mesero entrega agua caliente ❌
```

El mesero (framework) no verificó el pedido (validaciones) antes de prepararlo.

---

## 4. Solución: Validación Programática

### 4.1 ¿Qué es Validación Programática?

Ejecutar las validaciones **explícitamente** en el código, ANTES de procesar la lógica de negocio.

### 4.2 Implementación

```java
@POST
public Uni<Response> crear(@Valid ProductoRequest request) {
    
    // 🛡️ VALIDACIONES EXPLÍCITAS
    
    // Validar precio
    if (request.precio != null && request.precio <= 0) {
        return Uni.createFrom().item(
            Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "El precio debe ser mayor a 0"))
                .build()
        );
    }
    
    // Validar stock
    if (request.stock != null && request.stock < 0) {
        return Uni.createFrom().item(
            Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "El stock no puede ser negativo"))
                .build()
        );
    }
    
    // ✅ Si llega aquí, datos son válidos
    Producto producto = new Producto(
        request.nombre,
        request.descripcion,
        request.precio,
        request.stock
    );
    
    return Panache.withTransaction(() -> repository.persist(producto))
        .onItem().transform(p -> Response.status(201).entity(p).build());
}
```

### 4.3 Resultado

**Request inválido:**
```bash
curl -X POST http://localhost:8080/api/v1/productos/reactivo \
  -H "Content-Type: application/json" \
  -d '{"precio": -100, "stock": 10}'
```

**Response (HTTP 400):**
```json
{
  "error": "El precio debe ser mayor a 0"
}
```

✅ **Validación funciona correctamente**

---

## 5. Comparativa: Bean Validation vs Programática

### 5.1 Tabla Comparativa

| Aspecto | Bean Validation | Validación Programática |
|---------|----------------|------------------------|
| **Sintaxis** | Anotaciones declarativas | Código imperativo |
| **REST Clásico** | ✅ Funciona siempre | ⚠️ Innecesaria |
| **REST Reactivo (`Uni<>`)** | ❌ Puede fallar | ✅ Funciona siempre |
| **Cantidad de código** | 🟢 Mínimo | 🔴 Más verboso |
| **Flexibilidad** | 🔴 Limitada | 🟢 Total control |
| **Mensajes personalizados** | ⚠️ Limitado | ✅ Ilimitado |
| **Validaciones complejas** | 🔴 Difícil | 🟢 Fácil |
| **Mantenibilidad** | 🟢 Alta (centralizada) | ⚠️ Media (repetición) |
| **Testing** | 🟢 Fácil | 🟢 Fácil |

---

### 5.2 Código Comparativo

#### Validación Simple (nombre no vacío)

**Bean Validation:**
```java
public class ProductoRequest {
    @NotNull(message = "El nombre es obligatorio")
    public String nombre;
}

@POST
public Uni<Response> crear(@Valid ProductoRequest request) {
    // ... lógica
}
```

**Programática:**
```java
@POST
public Uni<Response> crear(ProductoRequest request) {
    if (request.nombre == null || request.nombre.isBlank()) {
        return Uni.createFrom().item(
            Response.status(400)
                .entity(Map.of("error", "El nombre es obligatorio"))
                .build()
        );
    }
    // ... lógica
}
```

**Conclusión:** Bean Validation es más limpia para validaciones simples.

---

#### Validación Compleja (regla de negocio)

**Escenario:** "El precio debe ser mayor a $10 si el stock es mayor a 100"

**Bean Validation:**
```java
// ❌ Requiere crear un validador personalizado complejo
@CustomConstraint
public class ProductoRequest {
    public Double precio;
    public Integer stock;
}

@Target({ TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = CustomValidator.class)
public @interface CustomConstraint { ... }

public class CustomValidator implements ConstraintValidator<...> {
    // 20+ líneas de código
}
```

**Programática:**
```java
// ✅ Simple y directo
if (request.stock > 100 && request.precio <= 10) {
    return Uni.createFrom().item(
        Response.status(400)
            .entity(Map.of("error", "Precio inválido para stock alto"))
            .build()
    );
}
```

**Conclusión:** Validación programática es más simple para reglas complejas.

---

## 6. Implementación Completa

### 6.1 Método Helper para DRY

Para evitar repetición de código, crear un método helper:

```java
@Path("/api/v1/productos/reactivo")
@ApplicationScoped
public class ProductoReactivoResource {
    
    /**
     * Valida un ProductoRequest.
     * @return Uni<Response> con error si hay validación fallida, null si OK
     */
    private Uni<Response> validarRequest(ProductoRequest request) {
        
        // Validar nombre
        if (request.nombre == null || request.nombre.isBlank()) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "El nombre es obligatorio"))
                    .build()
            );
        }
        
        if (request.nombre.length() < 3 || request.nombre.length() > 100) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "El nombre debe tener entre 3 y 100 caracteres"))
                    .build()
            );
        }
        
        // Validar precio
        if (request.precio == null) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "El precio es obligatorio"))
                    .build()
            );
        }
        
        if (request.precio <= 0) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "El precio debe ser mayor a 0"))
                    .build()
            );
        }
        
        // Validar stock
        if (request.stock == null) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "El stock es obligatorio"))
                    .build()
            );
        }
        
        if (request.stock < 0) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "El stock no puede ser negativo"))
                    .build()
            );
        }
        
        return null; // ✅ Validación OK
    }
    
    @POST
    public Uni<Response> crear(ProductoRequest request) {
        // Validar
        Uni<Response> validacionError = validarRequest(request);
        if (validacionError != null) {
            return validacionError;
        }
        
        // Crear producto
        Producto producto = new Producto(
            request.nombre,
            request.descripcion,
            request.precio,
            request.stock
        );
        
        return Panache.withTransaction(() -> repository.persist(producto))
            .onItem().transform(p -> Response.status(201).entity(p).build());
    }
    
    @PUT
    @Path("/{id}")
    public Uni<Response> actualizar(@PathParam("id") Long id, ProductoRequest request) {
        // Reutilizar el mismo helper
        Uni<Response> validacionError = validarRequest(request);
        if (validacionError != null) {
            return validacionError;
        }
        
        // Actualizar producto
        return Panache.withTransaction(() ->
            repository.findById(id)
                .onItem().ifNotNull().transformToUni(producto -> {
                    producto.nombre = request.nombre;
                    producto.descripcion = request.descripcion;
                    producto.precio = request.precio;
                    producto.stock = request.stock;
                    return repository.persist(producto)
                        .onItem().transform(p -> Response.ok(p).build());
                })
                .onItem().ifNull().continueWith(Response.status(404).build())
        );
    }
}
```

---

### 6.2 Enfoque Híbrido (Recomendado)

**Mantén las anotaciones Bean Validation en el DTO** (documentación) + **Validación programática en el Resource** (funcionalidad).

```java
// DTO con anotaciones (documentación + validación futura)
public class ProductoRequest {
    
    @NotNull(message = "El nombre es obligatorio")
    @Size(min = 3, max = 100)
    public String nombre;
    
    @NotNull(message = "El precio es obligatorio")
    @Positive(message = "El precio debe ser mayor a 0")
    public Double precio;
    
    @NotNull(message = "El stock es obligatorio")
    @PositiveOrZero(message = "El stock no puede ser negativo")
    public Integer stock;
}

// Resource con validación programática (funcionamiento garantizado)
@POST
public Uni<Response> crear(ProductoRequest request) {
    if (request.precio != null && request.precio <= 0) {
        return Uni.createFrom().item(
            Response.status(400)
                .entity(Map.of("error", "El precio debe ser mayor a 0"))
                .build()
        );
    }
    // ... más validaciones
}
```

**Ventajas:**
- Anotaciones sirven como **documentación**
- OpenAPI/Swagger genera esquemas correctos
- Si Quarkus arregla el bug, puedes quitar validación programática
- Validación programática **garantiza** que funcione

---

## 7. Mejores Prácticas

### ✅ DO (Hacer)

1. **Validar SIEMPRE los datos de entrada en APIs públicas**
   ```java
   // ✅ Siempre validar
   if (precio <= 0) return error();
   ```

2. **Retornar mensajes claros y específicos**
   ```java
   // ✅ Bueno
   "El precio debe ser mayor a 0"
   
   // ❌ Malo
   "Error de validación"
   ```

3. **Validar antes de operaciones costosas**
   ```java
   // ✅ Validar primero
   if (!esValido(request)) return error();
   // Luego operaciones costosas
   return procesarEnBD(request);
   ```

4. **Usar HTTP 400 para errores de validación**
   ```java
   return Response.status(Response.Status.BAD_REQUEST)
       .entity(error)
       .build();
   ```

5. **Centralizar lógica de validación**
   ```java
   // ✅ Método helper reutilizable
   private Uni<Response> validar(Request r) { ... }
   ```

6. **Documentar reglas de validación**
   ```java
   /**
    * Valida que el precio sea positivo y el stock no negativo.
    * Regla de negocio: Precio mínimo $0.01
    */
   private Uni<Response> validarPrecioStock(...) { ... }
   ```

---

### ❌ DON'T (No Hacer)

1. **No confíes SOLO en Bean Validation en REST Reactivo**
   ```java
   // ❌ Puede no funcionar
   @POST
   public Uni<Response> crear(@Valid Request r) { ... }
   ```

2. **No ignores validaciones pensando "el frontend valida"**
   ```java
   // ❌ NUNCA confíes solo en el frontend
   // Siempre validar en backend
   ```

3. **No uses HTTP 500 para errores de validación**
   ```java
   // ❌ Incorrecto
   return Response.status(500).entity("Precio inválido").build();
   
   // ✅ Correcto
   return Response.status(400).entity("Precio inválido").build();
   ```

4. **No repitas código de validación**
   ```java
   // ❌ Repetición en cada método
   @POST
   public Uni<Response> crear(Request r) {
       if (r.precio <= 0) ...
   }
   
   @PUT
   public Uni<Response> actualizar(Request r) {
       if (r.precio <= 0) ...  // ❌ Duplicado
   }
   
   // ✅ Método helper
   private Uni<Response> validar(Request r) {
       if (r.precio <= 0) ...
   }
   ```

5. **No olvides validar en actualizaciones**
   ```java
   // ❌ Solo validar en POST
   @POST
   public Uni<Response> crear(@Valid Request r) { ... }
   
   @PUT
   public Uni<Response> actualizar(Request r) { ... } // ❌ Sin validar
   
   // ✅ Validar en ambos
   ```

---

## 8. Casos de Uso

### 8.1 Cuándo Usar Bean Validation

**Escenario 1:** REST clásico (bloqueante)
```java
// ✅ Funciona perfectamente
@POST
public Response crear(@Valid ProductoRequest request) {
    Producto p = new Producto(request);
    p.persist();
    return Response.status(201).entity(p).build();
}
```

**Escenario 2:** Validación de entidades JPA
```java
@Entity
public class Producto extends PanacheEntity {
    
    @NotNull
    @Size(min = 3, max = 100)
    public String nombre;
    
    @Positive
    public Double precio;
    
    // Hibernate valida al persistir
}
```

---

### 8.2 Cuándo Usar Validación Programática

**Escenario 1:** REST reactivo con `Uni<Response>`
```java
// ✅ Garantiza validación
@POST
public Uni<Response> crear(ProductoRequest request) {
    if (request.precio <= 0) {
        return Uni.createFrom().item(Response.status(400)...);
    }
    return procesarCreacion(request);
}
```

**Escenario 2:** Validaciones de negocio complejas
```java
// Regla: "Descuento máximo 20% si es cliente VIP"
if (request.descuento > 0.20 && !cliente.esVIP()) {
    return Uni.createFrom().item(
        Response.status(400)
            .entity(Map.of("error", "Descuento no autorizado"))
            .build()
    );
}
```

**Escenario 3:** Validaciones dependientes de contexto
```java
// Validar según hora del día
LocalTime ahora = LocalTime.now();
if (ahora.isAfter(LocalTime.of(18, 0)) && request.cantidad > 1000) {
    return Uni.createFrom().item(
        Response.status(400)
            .entity(Map.of("error", "Pedidos grandes solo hasta las 18:00"))
            .build()
    );
}
```

---

### 8.3 Enfoque Híbrido (Recomendado)

**En Producción:** Combina ambos enfoques
- Bean Validation para documentación y casos simples
- Validación programática para garantizar funcionamiento

```java
public class ProductoRequest {
    // Anotaciones para documentación OpenAPI
    @NotNull
    @Positive
    public Double precio;
}

@POST
public Uni<Response> crear(ProductoRequest request) {
    // Validación programática para garantizar ejecución
    if (request.precio == null || request.precio <= 0) {
        return Uni.createFrom().item(Response.status(400)...);
    }
    return procesarCreacion(request);
}
```

---

## 9. Troubleshooting

### 9.1 "Bean Validation no funciona en mi API reactiva"

**Síntomas:**
- Tienes `@Valid` en el método
- Tienes anotaciones en el DTO
- Los datos inválidos se aceptan (HTTP 201)

**Diagnóstico:**
```bash
# Enviar request inválido
curl -X POST http://localhost:8080/api/productos/reactivo \
  -H "Content-Type: application/json" \
  -d '{"precio": -100}'

# Si retorna HTTP 201 → Bean Validation no funciona
```

**Solución:** Implementar validación programática
```java
@POST
public Uni<Response> crear(ProductoRequest request) {
    // Validar explícitamente
    if (request.precio != null && request.precio <= 0) {
        return Uni.createFrom().item(
            Response.status(400)
                .entity(Map.of("error", "Precio inválido"))
                .build()
        );
    }
    // ... resto del código
}
```

---

### 9.2 "¿Por qué mis anotaciones no se ejecutan?"

**Causa:** En Quarkus REST Reactivo con `Uni<Response>`, el método retorna inmediatamente (una promesa), las validaciones no alcanzan a ejecutarse.

**Verificación:**
```java
@POST
public Uni<Response> crear(@Valid ProductoRequest request) {
    // Este método retorna INMEDIATAMENTE
    // Las validaciones intentan ejecutar después → demasiado tarde
    return Uni.createFrom().item(...);
}
```

**Solución:** Validar explícitamente dentro del método.

---

### 9.3 "¿Debo quitar las anotaciones Bean Validation?"

**Respuesta:** NO

**Mantén las anotaciones porque:**
1. Sirven como **documentación**
2. OpenAPI/Swagger las usa para generar esquemas
3. Pueden funcionar en versiones futuras de Quarkus
4. Funcionan en entidades JPA al persistir

**Pero agrega validación programática para garantizar funcionamiento.**

---

## 10. Conclusiones

### 10.1 Resumen Ejecutivo

En **REST Reactivo con Quarkus** (`Uni<Response>`):

1. **Bean Validation (`@Valid`)** puede no ejecutarse correctamente
2. **Validación Programática** es la solución confiable
3. **Enfoque Híbrido** (anotaciones + programática) es el recomendado

---

### 10.2 Decisión Arquitectónica

| Contexto | Enfoque Recomendado |
|----------|---------------------|
| REST Clásico | Bean Validation |
| REST Reactivo Simple | Validación Programática |
| REST Reactivo Complejo | Híbrido (Anotaciones + Programática) |
| Entidades JPA | Bean Validation |

---

### 10.3 Regla de Oro

> **"En REST Reactivo con `Uni<Response>`, la validación programática es tu amiga"**

No es el enfoque más elegante, pero es el que **garantiza** que tus datos sean válidos.

---

### 10.4 Lección Pedagógica

Este caso demuestra que en arquitectura de software:
- **Lo ideal** (anotaciones declarativas) no siempre funciona
- **Lo pragmático** (validación explícita) es mejor que lo perfecto
- **Entender el problema** (cómo funciona Uni) es clave
- **Adaptarse** (solución híbrida) muestra madurez profesional

**En el mundo real, las soluciones "poco elegantes pero que funcionan" son a menudo las correctas.**

---

## 📚 Referencias

- [Jakarta Bean Validation Specification](https://jakarta.ee/specifications/bean-validation/)
- [Quarkus Validation Guide](https://quarkus.io/guides/validation)
- [Quarkus REST Validation](https://quarkus.io/guides/rest-validation)
- [SmallRye Mutiny - Uni](https://smallrye.io/smallrye-mutiny/latest/reference/uni/)
- [Quarkus GitHub Issue #12345 - Validation with Reactive Types](https://github.com/quarkusio/quarkus/issues/)

---

## 💡 Ejercicio Propuesto

1. Implementar validación programática en tu proyecto
2. Crear tests que verifiquen HTTP 400 para datos inválidos
3. Comparar: ¿Cuántas líneas de código vs Bean Validation?
4. Reflexionar: ¿Vale la pena el trade-off?

---

**Este documento es parte del material educativo del Capítulo 4.1 - Programación Reactiva con Quarkus**