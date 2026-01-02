---
type: requerimientos
applyTo: '**'
opencodeRequirements: true
---
 Kiubit Cursos Matemáticos – Microservicios
 Descripción General
**Propósito:**  
Crear una plataforma para cursos en línea de matemáticas organizada bajo arquitectura de microservicios y con foco inicial en estructura de base de datos y entidades principales. El sistema debe permitir la inscripción, avance y gestión de progreso de los usuarios en diversos cursos, garantizando autenticación segura y correcta jerarquización de cursos, secciones y temas.
**Alcance:**  
- Usuarios pueden registrarse, autentificarse y suscribirse a cursos de matemáticas.
- Gestión integral de cursos: creación, consulta, avance, contenidos descargables y gestión de progreso.
- Enfoque en la definición y modelado de la base de datos para futuras entidades y relaciones entre usuarios, cursos, secciones, temas y archivos descargables.
- Arquitectura enfocada en microservicios con especial atención a la separación de dominios de Usuario, Curso, Progreso y Notificaciones (para eventos futuros).
**Público objetivo:**  
Estudiantes de matemáticas, profesores y personas interesadas en aprendizaje estructurado y gestionado por microservicios.
---
 Requerimientos Funcionales
* **RF1: Registro y Autenticación de Usuarios**
  - El sistema debe permitir alta (registro) de usuarios con nombre, edad, correo electrónico y contraseña.
  - Las contraseñas deben almacenarse hasheadas.
  - Los usuarios podrán iniciar sesión (login) con su correo y contraseña.
  - El acceso a servicios estará protegido mediante JWT (JSON Web Tokens).
* **RF2: Consulta y Gestión de Cursos**
  - Los usuarios autenticados podrán consultar la lista de cursos disponibles.
  - Cada curso contará con título, descripción y duración (calculada por la suma de las secciones).
  - Las secciones de un curso agrupan temas, y ambos pueden tener contenidos descargables independientes.
  - Cada tema puede tener uno o más archivos descargables y un contenido principal (video, PDF o examen), además de su duración independiente.
* **RF3: Suscripción y Progreso en Cursos**
  - Un usuario podrá suscribirse a cualquier curso y el sistema debe persistir esa relación.
  - El sistema debe registrar y actualizar el avance individual del usuario por curso, sección y tema, incluyendo estado de completado.
  - Los usuarios podrán registrar "marcas de tiempo" personalizadas con notas o dudas dentro de cada curso.
* **RF4: Comentarios y Notas**
  - Permitirá registrar dudas/comentarios generales sobre un curso, y notas puntuales asociadas a marcas de tiempo dentro del contenido.
* **RF5: Operaciones Futuras (a diseñar posteriormente)**
  - Notificación por email al suscribirse a un curso y al finalizar un curso (entrega de certificado).
  - Integración con sistemas de cobro para cursos premium.
---
 Requerimientos No Funcionales
* **Rendimiento:**  
  - El sistema debe permitir concurrencia de múltiples usuarios sin degradación sensible al consultar o actualizar progreso.
* **Seguridad:**  
  - Contraseñas hasheadas y autenticación mediante JWT.
  - Acceso encriptado a través de HTTPS.
* **Escalabilidad y Modularidad:**  
  - Arquitectura de microservicios, permitiendo desplegar componentes de Usuarios, Cursos, Progreso y Notificaciones de forma independiente.
* **Compatibilidad:**  
  - Debe ser fácilmente extensible para futuras integraciones de métodos de pago, notificaciones y certificados.
---
 Tecnologías y Stack
* **Backend (Microservicios):**  Spring Boot (Java)
* **Base de Datos:**  MySQL  
* **Seguridad:**  JWT con estrategia de autenticación estándar (Spring Security recomendado)
* **Comunicacion entre microservicios** Para la comunicacion entre los servicios se puede utilizar Feign o Kafka dependiendo la necesidad de la comunicacion
* **(Opcional, futuro) Email y pagos:** Servicios externos vía microservicio dedicado (Mailgun, Stripe, etc.)
---
 Diagramas o Wireframes (Opcional)

## Auth Service

### Tabla: auth_audit_log

| Field | Type | Null | Key | Default | Extra |
|-------|------|------|-----|---------|-------|
| id | bigint | NO | PRI | NULL | auto_increment |
| auth_user_id | bigint | YES | | NULL | |
| event_type | varchar(50) | YES | | NULL | |
| ip_address | varchar(50) | YES | | NULL | |
| created_at | timestamp | YES | | CURRENT_TIMESTAMP | DEFAULT_GENERATED |

### Tabla: auth_user

| Field | Type | Null | Key | Default | Extra |
|-------|------|------|-----|---------|-------|
| id | bigint | NO | PRI | NULL | auto_increment |
| email | varchar(255) | NO | UNI | NULL | |
| password_hash | varchar(255) | NO | | NULL | |
| status | varchar(30) | NO | | NULL | |
| created_at | timestamp | YES | | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| updated_at | timestamp | YES | | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |

### Tabla: auth_user_role

| Field | Type | Null | Key | Default | Extra |
|-------|------|------|-----|---------|-------|
| auth_user_id | bigint | NO | PRI | NULL | |
| role_id | bigint | NO | PRI | NULL | |

### Tabla: refresh_token

| Field | Type | Null | Key | Default | Extra |
|-------|------|------|-----|---------|-------|
| id | bigint | NO | PRI | NULL | auto_increment |
| auth_user_id | bigint | NO | MUL | NULL | |
| token | varchar(512) | NO | UNI | NULL | |
| expires_at | timestamp | NO | | NULL | |
| revoked | tinyint(1) | YES | | 0 | |
| created_at | timestamp | YES | | CURRENT_TIMESTAMP | DEFAULT_GENERATED |

### Tabla: role

| Field | Type | Null | Key | Default | Extra |
|-------|------|------|-----|---------|-------|
| id | bigint | NO | PRI | NULL | auto_increment |
| name | varchar(50) | NO | UNI | NULL | |

---
 Criterios de Aceptación
* **CA1 (RF1):** Un usuario puede registrarse con información válida y es autenticado por JWT.
* **CA2 (RF2):** El listado de cursos muestra estructura jerárquica completa (curso->sección->tema->contenido).
* **CA3 (RF3):** El avance del usuario se actualiza correctamente y persiste en la base de datos.
* **CA4 (RF4):** Un usuario puede dejar notas y comentarios asociados a marcas de tiempo.
* **CA5:** Todas las operaciones protegidas requieren JWT válido.
* **CA6:** La base de datos refleja adecuadamente todas las relaciones especificadas.

---
 Licencia
MIT (o la que determine el proyecto principal).
---
*Nota*: Antes de desplegar asegúrate de adaptar este documento a la versión específica de microservicios y stack finalmente seleccionados.
