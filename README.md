# Course Enrollment API

Microservicio Spring Boot para administrar cursos virtuales, inscripciones y archivos de resumen almacenados en AWS S3.

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

## Endpoints

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
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
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
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`

La instancia EC2 debe tener Docker instalado y permitir trafico entrante al puerto `8080`.
