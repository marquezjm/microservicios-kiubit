# Checklist de QA, Seguridad y Documentación para Auth Service

---

## 1. QA Funcional (Swagger/Postman)

**Endpoints a probar y sus casos:**

| Endpoint                 | Caso principal        | Casos de error/edge                                     |
|--------------------------|----------------------|---------------------------------------------------------|
| POST /auth/register      | Registro exitoso     | Email duplicado, datos inválidos, campos vacíos         |
| POST /auth/login         | Login exitoso        | Contraseña mala, usuario inactivo/no existe             |
| POST /auth/refresh       | Refresh válido       | Token revocado/expirado, deviceId incorrecto/faltante   |
| POST /auth/logout        | Logout OK            | Token/session ya revocado, deviceId incorrecto          |
| POST /auth/logout-all    | Revoca todos OK      | userId incorrecto/ausente, sin sesión activa            |

**En cada endpoint valida:**
- Códigos de respuesta (`200`, `400`, etc.)
- Formato y mensaje de error en JSON
- Cookies set/eliminadas correctamente al login/refresh/logout
- Cambios en la base de datos (`refresh_token`, creación de AudithLog)
- Headers de respuesta (CORS, cookies, no-cache, etc.)

> Consejo: Haz pruebas de doble login/dispositivo usando incógnito o navegador diferente.

---

## 2. Checklist de Seguridad

- [ ] Contraseñas siempre en hash (BCrypt)
- [ ] Cookies de tokens:
    - `HttpOnly=true` para JWT y refresh (no para deviceId)
    - `Secure=true` (solo via HTTPS)
    - `SameSite=Strict` o `Lax` según frontend
- [ ] Endpoints públicos:
    - Solo `/auth/login`, `/auth/register`, `/auth/refresh`, `/v3/api-docs`, `/swagger-ui*` deben ser públicos
- [ ] CORS: solo permite origenes deseados
- [ ] Errores: Sin detalles internos (solo { "error": "mensaje" })
- [ ] Health check (`/actuator/health`) solo para monitoreo, no expone info sensible

---

## 3. Plantilla README/documentación básica

```
# Auth Service

Microservicio de autenticación (Spring Boot, Java 17, JWT, Cookies).

## Requisitos

- Java 17+
- Maven 3.8+
- MySQL (o usa H2 para pruebas)

## Instalación y ejecución

./mvnw clean package
./mvnw spring-boot:run

## Configuración

- `spring.datasource.*` (DB)
- `jwt.secret`, `jwt.expiration-ms`
- `spring.profiles.active` (`dev`, `prod`...)

## Endpoints principales

| Método | Ruta                | Descripción                             |
|--------|---------------------|-----------------------------------------|
| POST   | /auth/register      | Crear usuario                           |
| POST   | /auth/login         | Login, genera JWT y refresh (cookies)   |
| POST   | /auth/refresh       | Renovar accessToken vía refreshToken    |
| POST   | /auth/logout        | Cerrar sesión en un dispositivo         |
| POST   | /auth/logout-all    | Cerrar sesión en todos los dispositivos |

Consulta `/swagger-ui.html` para la API completa.

## Pruebas

./mvnw test

Pruebas unitarias y de integración (H2).
```

---

## 4. Health checks y externalización

- [ ] `/actuator/health` accesible para monitoreo (devops, Docker, K8s)
- [ ] Variables sensibles (`jwt.secret`, credenciales, etc.) por entorno/config, no en código
- [ ] Todo lo productivo/externalizable está fuera del repo o parametrizado
- [ ] Health check responde solo `{ "status": "UP" }` y no expone detalles internos

---

**Este documento sirve para QA, revisión de seguridad y handoff de tu Auth Service.**
