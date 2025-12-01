### Contenido de la colección:

**📁 Cuentas Válidas (3 tests)**

| # | Test | Cuenta | Esperado |
|---|------|--------|----------|
| 01 | Cuenta válida estándar | 1234567890 | true |
| 02 | Con ceros al inicio | 0000000123 | true |
| 03 | Todo nueves | 9999999999 | true |

**📁 Cuentas Inválidas (7 tests)**

| # | Test | Cuenta | Esperado |
|---|------|--------|----------|
| 04 | Muy corta (9 dígitos) | 123456789 | false |
| 05 | Muy corta (5 dígitos) | 12345 | false |
| 06 | Muy larga (11 dígitos) | 12345678901 | false |
| 07 | Con letras | 123ABC7890 | false |
| 08 | Todo letras | ABCDEFGHIJ | false |
| 09 | Con guiones | 1234-56789 | false |
| 10 | Con arroba | 1234@67890 | false |

---

### Bonus: Tests automáticos incluidos 🧪

Cada request tiene tests de Postman para validar automáticamente:

```javascript
pm.test('Status 200', function () {
    pm.response.to.have.status(200);
});

pm.test('Cuenta valida/invalida', function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.valido).to.eql(true/false);
});
```

Puedes ejecutar toda la colección con **Run Collection** y ver los resultados automáticamente. 