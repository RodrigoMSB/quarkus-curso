### Cómo importar en Postman:

1. Abrir Postman
2. **File → Import** (o Ctrl+O)
3. Arrastrar ambos archivos JSON
4. Seleccionar el environment **"Capitulo 3 - Local"** en la esquina superior derecha

---

### Contenido de la colección:

| # | Request | Método | Descripción |
|---|---------|--------|-------------|
| 01 | Listar todas las cuentas | GET | Lista completa |
| 02 | Obtener cuenta específica | GET | Por número |
| 03 | Crear nueva cuenta | POST | Ana Torres |
| 04 | Verificar cuenta creada | GET | Confirmar creación |
| 05 | Actualizar cuenta | PUT | Cambiar saldo/tipo |
| 06 | Verificar actualización | GET | Confirmar cambios |
| 07 | Eliminar cuenta | DELETE | Carlos Ruiz |
| 08 | Verificar eliminación | GET | 404 esperado |
| 09 | Cuenta inexistente | GET | 404 esperado |
| 10 | Actualizar inexistente | PUT | 404 esperado |
| 11 | Eliminar inexistente | DELETE | 404 esperado |
| 12 | Estado final | GET | Resumen |

---

### Variable de entorno:

| Variable | Valor |
|----------|-------|
| `API_URL` | `http://localhost:8080` |


