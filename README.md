# Course Enrollment API

Microservicio Spring Boot para administrar cursos virtuales, inscripciones y archivos de resumen almacenados en AWS S3.
Tambien incluye un modulo de gestion de pedidos y generacion de guias de despacho para una solucion Cloud Native.

## Funcionalidades

- Consulta de cursos disponibles con nombre, instructor, duracion y costo.
- Creacion de nuevos cursos con persistencia en Oracle Cloud.
- Inscripcion de estudiantes en uno o mas cursos.
- Resumen de inscripcion con cursos seleccionados, costo por curso y total a pagar.
- Generacion del resumen como archivo descargable.
- Carga del resumen generado a un bucket S3.
- Reemplazo, descarga y eliminacion del resumen almacenado en S3.
- Imagen Docker generada y publicada automaticamente en Docker Hub al hacer push a `main`.
- Despliegue automatico de la imagen en una instancia EC2.
- Creacion de guias de despacho en PDF.
- Almacenamiento temporal de guias en una ruta montable como EFS.
- Subida, descarga, actualizacion, eliminacion y busqueda historica de guias en AWS S3.

## Endpoints de guias de despacho

### Crear guia de despacho

Genera el registro y crea el PDF temporal en la ruta EFS configurada.

```http
POST /api/dispatch-guides
Content-Type: application/json
```

```json
{
  "orderNumber": "PED-1001",
  "carrierName": "Transportista Tomas",
  "carrierRut": "12.345.678-9",
  "recipientName": "Camila Perez",
  "originAddress": "Bodega Central, Santiago",
  "destinationAddress": "Av. Siempre Viva 742, Valparaiso",
  "packageDescription": "3 cajas con materiales de oficina",
  "dispatchDate": "2026-06-08"
}
```

La respuesta incluye `accessCode`, requerido para descargar desde S3.

### Consultar guias

```http
GET /api/dispatch-guides
GET /api/dispatch-guides?carrierName=Transportista&dispatchDate=2026-06-08
GET /api/dispatch-guides/{guideId}
```

### Descargar guia temporal desde EFS

```http
GET /api/dispatch-guides/{guideId}/efs
```

### Subir guia generada a S3

```http
POST /api/dispatch-guides/{guideId}/s3
```

Las guias quedan organizadas por fecha y transportista:

```text
guias/2026-06-08/transportista-tomas/guia-despacho-1.pdf
```

### Descargar guia desde S3 con validacion

```http
GET /api/dispatch-guides/{guideId}/s3?accessCode={accessCode}
```

### Actualizar guia

Regenera el PDF temporal. Si la guia ya estaba subida a S3, tambien reemplaza el objeto almacenado.

```http
PUT /api/dispatch-guides/{guideId}
Content-Type: application/json
```

```json
{
  "orderNumber": "PED-1001-A",
  "carrierName": "Transportista Tomas",
  "carrierRut": "12.345.678-9",
  "recipientName": "Camila Perez",
  "originAddress": "Bodega Central, Santiago",
  "destinationAddress": "Av. Siempre Viva 742, Valparaiso",
  "packageDescription": "3 cajas y 1 sobre con documentos",
  "dispatchDate": "2026-06-08"
}
```

### Eliminar guia

Elimina el registro, el archivo temporal y el objeto S3 si existe.

```http
DELETE /api/dispatch-guides/{guideId}
```

## Endpoints de cursos e inscripciones

### Listar cursos

```http
GET /api/courses
```

### Crear curso

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

### Inscribir estudiante

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

Genera el archivo fisico del resumen para guardarlo desde Postman o el navegador.

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

El archivo queda guardado con esta jerarquia:

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

## Configuracion local

Variables de entorno:

```bash
PORT=8080
DB_URL=jdbc:oracle:thin:@your-oracle-host:1521/your-service
DB_USERNAME=your_user
DB_PASSWORD=your_password
DDL_AUTO=update
AWS_REGION=us-east-1
AWS_S3_BUCKET=your_bucket_name
AWS_S3_SUMMARY_PREFIX=inscripciones
AWS_S3_GUIDE_PREFIX=guias
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
AWS_SESSION_TOKEN=your_session_token_if_using_academy_or_voclabs
EFS_MOUNT_PATH=/mnt/efs
```

Para Oracle Autonomous Database con wallet, use una URL como:

```bash
DB_URL=jdbc:oracle:thin:@mydb_high?TNS_ADMIN=/opt/oracle/wallet
```

En ese caso, monte el wallet en el contenedor en `/opt/oracle/wallet`.

## Ejecutar con Docker

```bash
docker build -t course-enrollment-api .
docker run --rm -p 8080:8080 --env-file .env course-enrollment-api
```

## CI/CD

El workflow `.github/workflows/deploy.yml` se ejecuta con cada push a `main`.

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
- `AWS_S3_GUIDE_PREFIX`
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_SESSION_TOKEN` si usas credenciales temporales de AWS Academy o VocLabs
- `EFS_MOUNT_PATH` opcional, usa `/mnt/efs` en Docker

El workflow monta `~/course-enrollment-api/efs` dentro del contenedor como `/mnt/efs`.
Para usar EFS real, monta el filesystem EFS en esa ruta de la instancia EC2 antes del despliegue.

La instancia EC2 debe tener Docker instalado y permitir trafico entrante al puerto `8080`.
