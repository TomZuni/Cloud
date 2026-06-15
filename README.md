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

## Autenticacion con Azure/Entra ID y AWS API Gateway

La autenticacion principal se realiza en AWS API Gateway mediante un autorizador JWT:

- Identity source: `$request.header.Authorization`
- Issuer: `https://login.microsoftonline.com/{tenant-id}/v2.0`
- Audience: el `aud` real del access token, normalmente el Client ID de la API o su Application ID URI.

En Postman, el token debe enviarse como:

```http
Authorization: Bearer {access_token}
```

Para Client Credentials en Entra ID, usa el endpoint del tenant, no `common`:

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
5. Si `JWT_VALIDATION_ENABLED=true`, Spring tambien debe tener el mismo issuer y audience.

Por defecto, Spring no aplica una segunda validacion JWT para evitar el `401` por Basic Auth cuando la proteccion ya esta en API Gateway. Si se quiere doble validacion en backend, configurar:

```env
JWT_VALIDATION_ENABLED=true
AZURE_AD_ISSUER_URI=https://login.microsoftonline.com/{tenant-id}/v2.0
AZURE_AD_ALLOWED_AUDIENCES=api://{api-client-id},{api-client-id}
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
JWT_VALIDATION_ENABLED=false
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
- `JWT_VALIDATION_ENABLED` opcional, recomendado `false` si API Gateway valida el JWT
- `AZURE_AD_ISSUER_URI` opcional, requerido si `JWT_VALIDATION_ENABLED=true`
- `AZURE_AD_ALLOWED_AUDIENCES` opcional, requerido si `JWT_VALIDATION_ENABLED=true`

## Modulo opcional de guias de despacho

Las guias de despacho pertenecen a otro alcance. Para activarlas manualmente:

```env
SPRING_PROFILES_ACTIVE=online,dispatch-guides
AWS_S3_GUIDE_PREFIX=guias
EFS_MOUNT_PATH=/mnt/efs
```

En la entrega de Semana 4 se debe mantener solamente `SPRING_PROFILES_ACTIVE=online`.
