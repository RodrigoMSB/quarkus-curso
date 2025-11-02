# 🔐 VaultCorp API - Ejercicio Completo de Seguridad en Quarkus

Ejercicio práctico que cubre **tres métodos de autenticación y autorización** en aplicaciones Quarkus.

---

## 📚 Guías Prácticas

### [README-PARTE1.md](README-PARTE1.md)
Implementación paso a paso de **Basic Authentication** para administradores. Incluye configuración de usuarios, roles y endpoints protegidos.

### [README-PARTE2.md](README-PARTE2.md)
Implementación completa de **JWT (JSON Web Tokens)** para empleados internos. Incluye generación de claves RSA, endpoint de login y validación de tokens.

### [README-PARTE3.md](README-PARTE3.md)
Integración con **Keycloak usando OIDC** para clientes externos. Incluye configuración completa de Keycloak, realms, clients, usuarios y roles.

---

## 📖 Material Teórico

### [TEORIA-PARTE1.md](TEORIA-PARTE1.md)
Conceptos fundamentales: Autenticación vs Autorización, RBAC, códigos HTTP 401/403, stateful vs stateless, y buenas prácticas de seguridad.

### [TEORIA-PARTE2.md](TEORIA-PARTE2.md)
Teoría profunda de JWT: anatomía de tokens, claims, firma criptográfica (RSA), validación, expiración y diferencias con otros métodos.

### [TEORIA-PARTE3.md](TEORIA-PARTE3.md)
OpenID Connect y federación de identidades: OIDC vs OAuth 2.0, Identity Providers, flujos de autenticación, SSO y Keycloak en detalle.

---

## 🐳 Docker

### [DOCKER-SETUP.md](DOCKER-SETUP.md)
Guía completa de Docker: conceptos básicos (contenedores, imágenes, volúmenes, redes), docker-compose.yml explicado línea por línea, comandos útiles y troubleshooting.

---

## 🚀 Quick Start
```bash
# Levantar Keycloak + PostgreSQL
docker-compose up -d

# Levantar la aplicación
./mvnw quarkus:dev
```

---

## 🎯 Arquitectura
```
/api/admin/*     → Basic Auth     → Admins/Auditores
/api/internal/*  → JWT Propio     → Empleados
/api/external/*  → OIDC Keycloak  → Clientes
```