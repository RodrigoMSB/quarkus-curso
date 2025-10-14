# 🧠 INTELLIJ-IDEA.md - Guía Completa para Quarkus

Guía definitiva para trabajar con proyectos Quarkus en IntelliJ IDEA Community Edition.

---

## 📖 Índice

1. [¿Por qué IntelliJ IDEA?](#1-por-qué-intellij-idea)
2. [Instalación](#2-instalación)
3. [Abrir Proyecto Quarkus](#3-abrir-proyecto-quarkus)
4. [Configurar JDK](#4-configurar-jdk)
5. [Ejecutar Proyecto](#5-ejecutar-proyecto)
6. [Navegación y Código](#6-navegación-y-código)
7. [Hot Reload](#7-hot-reload)
8. [Debugging](#8-debugging)
9. [Shortcuts Esenciales](#9-shortcuts-esenciales)
10. [Comparación con VSCode](#10-comparación-con-vscode)

---

## 1. ¿Por qué IntelliJ IDEA?

### Ventajas para Quarkus

✅ **Reconoce código generado** automáticamente  
✅ **Autocompletado superior** (más inteligente)  
✅ **Refactoring poderoso** (renombrar, mover, extraer)  
✅ **Maven integrado** perfectamente  
✅ **Sin falsos errores** en código generado  
✅ **Debugging avanzado** out-of-the-box  
✅ **Performance excelente** con proyectos grandes  

### Community vs Ultimate

| Característica | Community (GRATIS) | Ultimate (PAGO) |
|----------------|-------------------|-----------------|
| **Java** | ✅ | ✅ |
| **Maven** | ✅ | ✅ |
| **Quarkus** | ✅ | ✅ |
| **Spring Boot** | ⚠️ Básico | ✅ Completo |
| **Bases de datos** | ⚠️ Básico | ✅ Avanzado |
| **JavaScript/React** | ⚠️ Básico | ✅ Completo |
| **Precio** | 🆓 GRATIS | 💰 ~$150/año |

**Para Quarkus: Community Edition es más que suficiente** ✅

### Analogía

**VSCode** es como una **bicicleta deportiva**: ligera, rápida, pero básica.  
**IntelliJ** es como una **motocicleta**: más pesada, pero con motor potente y todas las funcionalidades integradas.

---

## 2. Instalación

### 🍎 macOS

**Opción 1: Homebrew (Recomendado)**

```bash
brew install --cask intellij-idea-ce
```

**Verificar instalación:**
```bash
# Debería abrir IntelliJ
open /Applications/IntelliJ\ IDEA\ CE.app
```

**Opción 2: Descarga Manual**

1. Ir a: https://www.jetbrains.com/idea/download/
2. Descargar **Community Edition** (botón de la derecha)
3. Abrir el `.dmg` descargado
4. Arrastrar IntelliJ IDEA a `Applications`

---

### 🪟 Windows

**Opción 1: Chocolatey**

```powershell
# PowerShell como Administrador
choco install intellijidea-community
```

**Opción 2: Scoop**

```powershell
scoop bucket add extras
scoop install intellij-idea-community
```

**Opción 3: Descarga Manual**

1. Ir a: https://www.jetbrains.com/idea/download/
2. Descargar **Community Edition** (Windows)
3. Ejecutar instalador `.exe`
4. Seguir wizard de instalación

---

### 🐧 Linux

**Con Snap:**

```bash
sudo snap install intellij-idea-community --classic
```

**Con tar.gz:**

```bash
# Descargar desde sitio oficial
wget https://download.jetbrains.com/idea/ideaIC-2025.2.3.tar.gz
tar -xzf ideaIC-2025.2.3.tar.gz
cd idea-IC-*/bin
./idea.sh
```

---

### Primera Ejecución

Al abrir IntelliJ por primera vez:

1. **Tema:** Elige Light o Dark (recomiendo Dark)
2. **Plugins:** Puedes saltar esto (ya viene con lo necesario)
3. **Keyboard Shortcuts:** Elige tu preferencia (Default está bien)

---

## 3. Abrir Proyecto Quarkus

### Desde Pantalla de Bienvenida

```
Welcome to IntelliJ IDEA
│
├─ New Project
├─ Open                    ← Click aquí
└─ Get from VCS
```

**Pasos:**

1. Click en **"Open"** (ícono de carpeta 📂)
2. Navegar hasta la carpeta del proyecto
3. Seleccionar la carpeta raíz (donde está `pom.xml`)
4. Click en **"Open"**

### Diálogo de Confianza

```
Trust and Open Project 'nombre-proyecto'?
│
├─ Don't Open
├─ Preview in Safe Mode
└─ Trust Project           ← Click aquí
```

**Siempre:** "Trust Project" (es tu código)

### Sincronización Maven

Después de abrir, IntelliJ detecta que es proyecto Maven:

```
Maven project detected

Do you want to import it?
│
└─ Import Changes          ← Click aquí
```

O aparecerá un banner arriba a la derecha:

```
[i] Load Maven Project?   [Load]   [Dismiss]
                           ↑
                    Click aquí
```

**Espera:** IntelliJ descargará dependencias (puede tardar 1-2 min la primera vez)

---

## 4. Configurar JDK

### Detectar JDK Automáticamente

IntelliJ usualmente detecta JDKs instalados automáticamente.

Si te pregunta por el JDK:

```
Project JDK is not defined

Setup SDK
│
├─ Download JDK...
├─ Add JDK from disk...
└─ Detected SDKs:
   ├─ OpenJDK 21
   ├─ OpenJDK 24         ← Selecciona este (o 21)
   └─ Eclipse Temurin 21
```

### Verificar JDK del Proyecto

**Menu:** `File → Project Structure` (Cmd+; en macOS)

```
Project Structure
│
└─ Project
   ├─ Name: nombre-proyecto
   ├─ SDK: [21]                    ← Debe ser 17, 21 o superior
   └─ Language level: [21]
```

### Cambiar JDK si es Necesario

1. `File → Project Structure`
2. Pestaña **"Project"**
3. Click en dropdown **"SDK"**
4. Seleccionar JDK correcto
5. Click **"OK"**

---

## 5. Ejecutar Proyecto

### 5.1 Desde Maven Panel (Recomendado)

#### Abrir Maven Panel

**Opción 1:** Click en pestaña **"Maven"** (lado derecho)

**Opción 2:** `View → Tool Windows → Maven`

**Opción 3:** Doble tap `Shift` → escribir "Maven" → Enter

#### Estructura del Panel Maven

```
Maven
│
└─ nombre-proyecto
   ├─ Lifecycle
   │  ├─ clean
   │  ├─ validate
   │  ├─ compile
   │  ├─ test
   │  └─ package
   │
   └─ Plugins
      ├─ compiler
      ├─ failsafe
      ├─ quarkus           ← Expandir este
      │  ├─ quarkus:build
      │  ├─ quarkus:dev    ← Doble click aquí ⭐
      │  └─ quarkus:test
      └─ surefire
```

#### Ejecutar Dev Mode

1. Expandir **"Plugins"**
2. Expandir **"quarkus"**
3. **Doble click** en **"quarkus:dev"**

**Resultado:**
- Se abre pestaña **"Run"** abajo
- Muestra logs de Quarkus
- Arranca en `http://localhost:8080`

#### Detener Servidor

**Botón rojo STOP** (cuadrado) en la consola de abajo  
O **Cmd+F2** (macOS) / **Ctrl+F2** (Windows/Linux)

---

### 5.2 Desde Terminal Integrada

#### Abrir Terminal

**Opción 1:** Click en pestaña **"Terminal"** (abajo)

**Opción 2:** `View → Tool Windows → Terminal`

**Opción 3:** **Option+F12** (macOS) / **Alt+F12** (Windows/Linux)

#### Ejecutar Comando

```bash
./mvnw quarkus:dev
```

**Windows:**
```cmd
mvnw.cmd quarkus:dev
```

#### Ventajas Terminal vs Maven Panel

| Característica | Maven Panel | Terminal |
|----------------|-------------|----------|
| **Clicks** | 2 clicks | Escribir comando |
| **Visual** | ✅ Organizado | Texto plano |
| **Múltiples tareas** | ❌ Una a la vez | ✅ Múltiples pestañas |
| **Personalización** | ❌ Limitada | ✅ Total |

---

### 5.3 Crear Configuración Run Permanente

Para no hacer doble click siempre:

1. Ir a: `Run → Edit Configurations...`
2. Click en **"+"** (arriba izquierda)
3. Seleccionar **"Maven"**
4. Configurar:
   - **Name:** `Quarkus Dev`
   - **Command line:** `quarkus:dev`
   - **Working directory:** Ruta del proyecto
5. Click **"OK"**

**Ahora puedes:**
- Click en dropdown arriba → "Quarkus Dev" → Play ▶️
- O **Ctrl+R** (macOS) / **Shift+F10** (Windows/Linux)

---

## 6. Navegación y Código

### 6.1 Reconocimiento de Código Generado

IntelliJ automáticamente reconoce:

```
target/
└─ generated-sources/
   └─ open-api/
      └─ pe/banco/capitulo2/
         ├─ api/
         │  └─ DefaultApi.java      ✅ Reconocido
         └─ model/
            └─ ValidacionResponse.java ✅ Reconocido
```

**Sin configuración adicional** (a diferencia de VSCode)

### 6.2 Navegar a Clase/Archivo

**Buscar cualquier clase:**

**Cmd+O** (macOS) / **Ctrl+N** (Windows/Linux)

```
[Buscar clase]
> ValidacionResponse    ← Escribe esto
  
Resultados:
├─ ValidacionResponse.java (pe.banco.capitulo2.model)  ← Enter aquí
└─ ...
```

**Buscar cualquier archivo:**

**Cmd+Shift+O** (macOS) / **Ctrl+Shift+N** (Windows/Linux)

### 6.3 Ir a Definición

**Cmd+Click** (macOS) / **Ctrl+Click** (Windows/Linux)

Ejemplo:
```java
public ValidacionResponse validarGet(String numero) {
    // Cmd+Click en "ValidacionResponse" te lleva a la clase
    ValidacionResponse response = new ValidacionResponse();
}
```

O **Cmd+B** / **Ctrl+B** con cursor sobre el símbolo

### 6.4 Ver Implementaciones

Sobre una interfaz:

**Cmd+Option+B** (macOS) / **Ctrl+Alt+B** (Windows/Linux)

```java
public interface DefaultApi {
    ValidacionResponse validarGet(String numero);
}

// Cmd+Option+B sobre "DefaultApi" muestra:
// → ValidadorResource.java (implementación)
```

### 6.5 Ver Estructura del Archivo

**Cmd+F12** (macOS) / **Ctrl+F12** (Windows/Linux)

Muestra:
- Todos los métodos
- Variables
- Constructores
- En orden

### 6.6 Autocompletado Inteligente

IntelliJ ofrece autocompletado **contextual**:

```java
response.  ← Al escribir punto, muestra:
           setValido()
           setNumeroCuenta()
           setMensaje()
           getValido()
           ...
```

**Ctrl+Space:** Forzar autocompletado

**Ctrl+Shift+Space:** Autocompletado inteligente (sugiere basándose en tipo esperado)

---

## 7. Hot Reload

### Cómo Funciona en IntelliJ

Con `quarkus:dev` corriendo:

```
1. Modificas código Java
2. Guardas (Cmd+S / Ctrl+S)
3. IntelliJ compila automáticamente
4. Quarkus detecta cambio
5. Recarga en ~1 segundo
6. Refrescas navegador
7. ¡Ves cambios!
```

### Compilación Automática

**Por defecto:** IntelliJ compila al guardar

**Verificar:**
1. `Settings → Build, Execution, Deployment → Compiler`
2. ✅ **"Build project automatically"**

### Probar Hot Reload

**Ejemplo:**

1. Abrir `ValidadorResource.java`
2. Modificar mensaje:
```java
response.setMensaje(esValido 
    ? "✅ APROBADA" 
    : "❌ RECHAZADA");
```
3. Guardar (**Cmd+S**)
4. Refrescar navegador
5. **Ver cambio instantáneo** 🔥

### Sin Reiniciar

**NO necesitas:**
- ❌ Detener servidor
- ❌ Recompilar manualmente
- ❌ Reiniciar nada

**Solo:**
- ✅ Modificar
- ✅ Guardar
- ✅ Refrescar navegador

---

## 8. Debugging

### 8.1 Iniciar en Modo Debug

**Desde Maven Panel:**

1. Click **derecho** en `quarkus:dev`
2. Seleccionar **"Debug 'validador-banco [quarkus:dev]'"**

O:

**Shift+F9** si ya tienes configuración Run

### 8.2 Colocar Breakpoints

Click en el **margen izquierdo** del editor (número de línea):

```java
public ValidacionResponse validarGet(String numero) {
●   ValidacionResponse response = new ValidacionResponse();  ← Breakpoint aquí
    response.setNumeroCuenta(numero);
    ...
}
```

**Círculo rojo = Breakpoint activo**

### 8.3 Ejecutar y Pausar

1. Hacer request al endpoint: `http://localhost:8080/validar/123`
2. **IntelliJ pausa** en el breakpoint
3. Panel de Debug muestra:
   - **Variables:** valores actuales
   - **Frames:** stack de llamadas
   - **Console:** logs

### 8.4 Controles de Debug

| Tecla | Acción | Descripción |
|-------|--------|-------------|
| **F8** | Step Over | Ejecuta línea actual, pasa a siguiente |
| **F7** | Step Into | Entra en el método |
| **Shift+F8** | Step Out | Sale del método actual |
| **F9** | Resume | Continúa hasta siguiente breakpoint |
| **Cmd+F8** | Toggle Breakpoint | Agregar/quitar breakpoint |

### 8.5 Evaluar Expresiones

Con el debugger pausado:

1. Seleccionar una variable o expresión
2. **Option+F8** (macOS) / **Alt+F8** (Windows/Linux)
3. Ver resultado

O:

**Click derecho** en variable → **"Evaluate Expression"**

### 8.6 Watches (Observadores)

Panel de Debug → Pestaña **"Watches"**

Click **"+"** y agregar:
- Variables
- Expresiones
- Métodos

Se actualizan en cada paso del debug.

---

## 9. Shortcuts Esenciales

### Navegación

| Shortcut | Acción |
|----------|--------|
| **Cmd+O** / **Ctrl+N** | Buscar clase |
| **Cmd+Shift+O** / **Ctrl+Shift+N** | Buscar archivo |
| **Cmd+B** / **Ctrl+B** | Ir a definición |
| **Cmd+[** / **Ctrl+Alt+←** | Volver atrás |
| **Cmd+]** / **Ctrl+Alt+→** | Ir adelante |
| **Cmd+F12** / **Ctrl+F12** | Estructura del archivo |
| **Cmd+E** / **Ctrl+E** | Archivos recientes |

### Edición

| Shortcut | Acción |
|----------|--------|
| **Cmd+D** / **Ctrl+D** | Duplicar línea |
| **Cmd+Y** / **Ctrl+Y** | Eliminar línea |
| **Cmd+/** / **Ctrl+/** | Comentar/descomentar |
| **Option+↑/↓** / **Ctrl+W** | Expandir selección |
| **Cmd+Shift+↑/↓** / **Ctrl+Shift+↑/↓** | Mover línea |
| **Cmd+Option+L** / **Ctrl+Alt+L** | Formatear código |

### Refactoring

| Shortcut | Acción |
|----------|--------|
| **Shift+F6** | Renombrar |
| **Cmd+Option+M** / **Ctrl+Alt+M** | Extraer método |
| **Cmd+Option+V** / **Ctrl+Alt+V** | Extraer variable |
| **F6** | Mover clase/método |

### Ejecución

| Shortcut | Acción |
|----------|--------|
| **Ctrl+R** / **Shift+F10** | Run |
| **Ctrl+D** / **Shift+F9** | Debug |
| **Cmd+F2** / **Ctrl+F2** | Stop |

### Búsqueda

| Shortcut | Acción |
|----------|--------|
| **Cmd+F** / **Ctrl+F** | Buscar en archivo |
| **Cmd+R** / **Ctrl+R** | Reemplazar en archivo |
| **Cmd+Shift+F** / **Ctrl+Shift+F** | Buscar en proyecto |
| **Shift Shift** | Buscar cualquier cosa |

### General

| Shortcut | Acción |
|----------|--------|
| **Cmd+,** / **Ctrl+Alt+S** | Settings |
| **Cmd+;** / **Ctrl+Alt+Shift+S** | Project Structure |
| **Cmd+1** / **Alt+1** | Panel Project |
| **Option+F12** / **Alt+F12** | Terminal |

---

## 10. Comparación con VSCode

### Tabla Comparativa

| Característica | IntelliJ IDEA | VSCode |
|----------------|---------------|--------|
| **Peso (RAM)** | ~1-2 GB | ~300-500 MB |
| **Inicio** | ~5-10s | ~2-3s |
| **Código generado** | ✅ Reconoce automático | ❌ Problemas |
| **Autocompletado** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| **Refactoring** | ⭐⭐⭐⭐⭐ | ⭐⭐ |
| **Maven integrado** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| **Debugging** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Curva aprendizaje** | Media | Baja |
| **Plugins** | ~5,000 | ~30,000 |
| **Multiplataforma** | ✅ | ✅ |
| **Precio** | 🆓 Community | 🆓 |

### Cuándo Usar Cada Uno

#### Usa IntelliJ IDEA cuando:
- ✅ Desarrollas proyectos Java/Quarkus serios
- ✅ Necesitas refactoring potente
- ✅ Trabajas con código generado
- ✅ Quieres debugging avanzado
- ✅ Priorizas productividad sobre velocidad

#### Usa VSCode cuando:
- ✅ Proyectos pequeños/prototipos
- ✅ Múltiples lenguajes (JS, Python, Go)
- ✅ Computadora con poca RAM
- ✅ Prefieres personalización extrema
- ✅ Desarrollo web frontend

### Mi Recomendación

**Para Quarkus:**
1. **IntelliJ IDEA Community** (desarrollo serio)
2. VSCode (scripts rápidos o si RAM es limitada)

**Para proyectos fullstack (Java + React):**
- IntelliJ IDEA Ultimate (si tienes licencia)
- O: IntelliJ Community (backend) + VSCode (frontend)

---

## 11. Tips y Trucos

### 11.1 Productividad

**Generar código:**
- **Cmd+N** / **Alt+Insert** dentro de clase
- Opciones: Constructor, Getters, Setters, toString, etc.

**Importar automáticamente:**
- `Settings → Editor → General → Auto Import`
- ✅ "Add unambiguous imports on the fly"
- ✅ "Optimize imports on the fly"

**Live Templates:**
- Escribir `psvm` + Tab → `public static void main`
- Escribir `sout` + Tab → `System.out.println()`
- Escribir `fori` + Tab → `for (int i = 0; i < ; i++)`

### 11.2 Personalización

**Tema:**
- `Settings → Appearance & Behavior → Appearance`
- Elegir **Darcula** (dark) o **IntelliJ Light**

**Font:**
- `Settings → Editor → Font`
- Recomendado: **JetBrains Mono** (incluida), size 14-16

**Keymap:**
- `Settings → Keymap`
- Predeterminado está bien, pero puedes cambiarlo

### 11.3 Plugins Útiles

`Settings → Plugins → Marketplace`

**Recomendados para Quarkus:**
- **Rainbow Brackets:** Colorea paréntesis
- **Key Promoter X:** Aprende shortcuts
- **GitToolBox:** Mejora integración Git
- **.ignore:** Soporte para .gitignore

**NO necesitas:**
- ❌ Plugin de Quarkus (ya incluido en Community)
- ❌ Plugin de Maven (ya incluido)

---

## 12. Solución de Problemas

### Problema: "Project JDK is not defined"

**Solución:**
1. `File → Project Structure`
2. Pestaña **"Project"**
3. SDK → Seleccionar JDK 17, 21 o superior
4. OK

### Problema: Maven no sincroniza

**Solución:**
1. Panel Maven (lado derecho)
2. Click en ícono **"Reload All Maven Projects"** (círculo con flechas)
3. Esperar sincronización

### Problema: Código generado no se reconoce

**Solución:**
1. `./mvnw clean compile` (desde terminal)
2. Panel Maven → Reload
3. `File → Invalidate Caches → Invalidate and Restart`

### Problema: IntelliJ muy lento

**Solución:**
1. Aumentar memoria: `Help → Change Memory Settings`
2. Subir a 2048 MB o más
3. Reiniciar IntelliJ

### Problema: Conflictos de shortcuts

**Solución:**
1. `Settings → Keymap`
2. Buscar el shortcut
3. Remover conflicto o cambiar

---

## 13. Recursos Adicionales

### Documentación Oficial

- [IntelliJ IDEA Docs](https://www.jetbrains.com/idea/documentation/)
- [Quarkus Tools](https://plugins.jetbrains.com/plugin/13234-quarkus)
- [IntelliJ Shortcuts PDF](https://resources.jetbrains.com/storage/products/intellij-idea/docs/IntelliJIDEA_ReferenceCard.pdf)

### Tutoriales

- [IntelliJ IDEA for Java Developers](https://www.jetbrains.com/guide/java/)
- [IntelliJ IDEA Tips & Tricks](https://www.youtube.com/c/intellijidea)

### Comunidad

- [IntelliJ IDEA Forum](https://intellij-support.jetbrains.com/hc/en-us/community/topics)
- [Reddit r/IntelliJIDEA](https://www.reddit.com/r/IntelliJIDEA/)

---

## ✅ Checklist de Dominio

Después de leer esta guía, deberías poder:

- [ ] Instalar IntelliJ IDEA Community Edition
- [ ] Abrir proyecto Quarkus existente
- [ ] Configurar JDK correctamente
- [ ] Ejecutar proyecto desde Maven panel
- [ ] Ejecutar proyecto desde Terminal
- [ ] Navegar entre clases generadas
- [ ] Usar autocompletado efectivamente
- [ ] Aprovechar hot reload
- [ ] Hacer debugging básico
- [ ] Usar shortcuts principales
- [ ] Decidir cuándo usar IntelliJ vs VSCode

---

**🎉 ¡Ahora eres productivo en IntelliJ IDEA!**
