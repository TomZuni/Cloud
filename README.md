# Course Enrollment API

Microservicio Spring Boot para la actividad formativa de Semana 4: sistema de inscripcion de cursos virtuales, generacion de resumen fisico, administracion del resumen en AWS S3, exposicion mediante AWS API Gateway y autenticacion con JWT emitido por Azure/Entra ID.

## Alcance de esta entrega

Esta rama queda enfocada en el caso educativo solicitado en la pauta:

- Consultar cursos disponibles.
- Crear cursos.
- Inscribir estudiantes en uno o mas cursos.
- Generar un archivo fisico con el resumen de la inscripcion.
- Subir el resumen generado a S3 en una carpeta con el numero de inscripcion.
- Reemplazar, descargar y borrar el resumen almacenado en S3.
- Exponer los endpoints por AWS API Gateway.
- Proteger las rutas del API Gateway con un autorizador JWT de Azure/Entra ID.

El modulo de guias de despacho queda separado en el perfil Spring `dispatch-guides`, por lo que no se publica en la entrega normal de Semana 4.

## Endpoints principales

### Cursos

```http
GET /api/courses
```

```http
POST /api/courses
Content-Type: application/json
```

```json
{
  "name": "Cloud Computing",
  "instructor": "Luis Rojas",
  "durationHours": 24,
  "cost": 120000
}
```

### Inscripciones

```http
POST /api/enrollments
Content-Type: application/json
```

```json
{
  "studentName": "Camila Perez",
  "studentEmail": "camila@example.com",
  "courseIds": [1, 2]
}
```

### Descargar resumen generado

Genera el archivo fisico para guardarlo desde Postman o navegador.

```http
GET /api/enrollments/{enrollmentId}/summary
```

Archivo generado:

```text
resumen-inscripcion-{enrollmentId}.txt
```

### Subir resumen generado a S3

```http
POST /api/enrollments/{enrollmentId}/summary/s3
```

Ruta del objeto en S3:

```text
inscripciones/{enrollmentId}/resumen-inscripcion-{enrollmentId}.txt
```

### Reemplazar resumen en S3

En Postman usar `Body > form-data`, key `file`, tipo `File`.

```http
PUT /api/enrollments/{enrollmentId}/summary/s3
Content-Type: multipart/form-data
```

### Descargar resumen desde S3

```http
GET /api/enrollments/{enrollmentId}/summary/s3
```

### Eliminar resumen desde S3

```http
DELETE /api/enrollments/{enrollmentId}/summary/s3
```

## Autenticacion con Azure AD B2C, Entra ID y AWS API Gateway

La autenticacion principal se realiza en AWS API Gateway mediante un autorizador JWT:

- Identity source: `$request.header.Authorization`
- Issuer: el valor exacto del claim `iss` del token.
- Audience: el valor exacto del claim `aud` del token, normalmente el Client ID de la API o su Application ID URI.

Para el flujo nuevo de Azure AD B2C, el issuer debe usar `b2clogin.com`:

```text
https://{tenant-name}.b2clogin.com/{tenant-id}/v2.0/
```

El metadata endpoint de cada flujo de usuario se obtiene con el tenant y la policy:

```text
https://{tenant-name}.b2clogin.com/{tenant-name}.onmicrosoft.com/{policy}/v2.0/.well-known/openid-configuration
```

Desde ese JSON se toma el `jwks_uri`, por ejemplo:

```text
https://{tenant-name}.b2clogin.com/{tenant-name}.onmicrosoft.com/{policy}/discovery/v2.0/keys
```

En Postman, el token debe enviarse como:

```http
Authorization: Bearer {access_token}
```

Para Entra ID clasico con Client Credentials, usa el endpoint del tenant, no `common`:

```text
https://login.microsoftonline.com/{tenant-id}/oauth2/v2.0/token
```

Y un scope con `/.default`, por ejemplo:

```text
api://{api-client-id}/.default
```

o:

```text
{application-id-uri}/.default
```

Si `/api/courses` responde `401` con token, revisar en este orden:

1. El route exacto `GET /api/courses` tiene asociado el JWT authorizer.
2. El audience configurado en API Gateway coincide exactamente con el claim `aud` del token.
3. El issuer configurado en API Gateway coincide exactamente con el claim `iss`.
4. Postman envia el token en `Request Headers` con prefix `Bearer`.
5. Si usas Azure AD B2C, el metadata endpoint y el `jwks_uri` corresponden al mismo flujo de usuario.
6. Si `JWT_VALIDATION_ENABLED=true`, Spring tambien debe tener el mismo issuer, audience y policy.

Para cumplir la Semana 5, el despliegue deja la validacion JWT de Spring Security activada por defecto desde GitHub Actions. Configura estos secrets:

```env
JWT_VALIDATION_ENABLED=true
AZURE_AD_ISSUER_URI=https://login.microsoftonline.com/{tenant-id}/v2.0
AZURE_AD_ALLOWED_AUDIENCES=api://{api-client-id},{api-client-id}
```

Para Azure AD B2C con flujo de usuario:

```env
JWT_VALIDATION_ENABLED=true
AZURE_AD_ISSUER_URI=https://{tenant-name}.b2clogin.com/{tenant-id}/v2.0/
AZURE_AD_JWK_SET_URI=https://{tenant-name}.b2clogin.com/{tenant-name}.onmicrosoft.com/{policy}/discovery/v2.0/keys
AZURE_AD_ALLOWED_AUDIENCES={api-client-id}
AZURE_AD_ALLOWED_POLICIES={policy}
```

## Configuracion local

Variables de entorno:

```env
PORT=8080
DB_URL=jdbc:oracle:thin:@your-oracle-host:1521/your-service
DB_USERNAME=your_user
DB_PASSWORD=your_password
DDL_AUTO=update
AWS_REGION=us-east-1
AWS_S3_BUCKET=your_bucket_name
AWS_S3_SUMMARY_PREFIX=inscripciones
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
AWS_SESSION_TOKEN=your_session_token_if_using_academy_or_voclabs
JWT_VALIDATION_ENABLED=true
AZURE_AD_ISSUER_URI=https://your-issuer
AZURE_AD_JWK_SET_URI=https://your-jwks-uri
AZURE_AD_ALLOWED_AUDIENCES=your-api-client-id
AZURE_AD_ALLOWED_POLICIES=B2C_1_your_flow
```

Para Oracle Autonomous Database con wallet, usar una URL como:

```env
DB_URL=jdbc:oracle:thin:@mydb_high?TNS_ADMIN=/opt/oracle/wallet
```

En ese caso, montar el wallet en el contenedor en `/opt/oracle/wallet`.

## Ejecutar con Docker

```bash
docker build -t course-enrollment-api .
docker run --rm -p 8080:8080 --env-file .env course-enrollment-api
```

## CI/CD

El workflow `.github/workflows/deploy.yml` ejecuta pruebas, genera la imagen Docker, la publica en Docker Hub y la despliega en EC2.

Secrets requeridos en GitHub:

- `DOCKERHUB_USERNAME`
- `DOCKERHUB_TOKEN`
- `EC2_HOST`
- `EC2_PORT` opcional, usa `22` si no se define
- `EC2_USER`
- `EC2_SSH_KEY`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `AWS_REGION`
- `AWS_S3_BUCKET`
- `AWS_S3_SUMMARY_PREFIX`
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_SESSION_TOKEN` si usas credenciales temporales
- `JWT_VALIDATION_ENABLED` opcional, por defecto `true` en el workflow para cumplir Semana 5
- `AZURE_AD_ISSUER_URI` requerido si `JWT_VALIDATION_ENABLED=true`
- `AZURE_AD_JWK_SET_URI` opcional para Entra ID clasico; recomendado para Azure AD B2C
- `AZURE_AD_ALLOWED_AUDIENCES` requerido si `JWT_VALIDATION_ENABLED=true`
- `AZURE_AD_ALLOWED_POLICIES` opcional; recomendado para validar el flujo de usuario B2C

## Modulo opcional de guias de despacho

Las guias de despacho pertenecen a otro alcance. Para activarlas manualmente:

```env
SPRING_PROFILES_ACTIVE=online,dispatch-guides
AWS_S3_GUIDE_PREFIX=guias
EFS_MOUNT_PATH=/mnt/efs
```

En la entrega de Semana 4 se debe mantener solamente `SPRING_PROFILES_ACTIVE=online`.
