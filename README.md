# Course Enrollment API

Microservicio Spring Boot para administrar la oferta de cursos virtuales e inscripciones de estudiantes.

## Funcionalidades

- Consulta de cursos disponibles con nombre, instructor, duración y costo.
- Creación de nuevos cursos con persistencia en Oracle Cloud.
- Inscripción de estudiantes en uno o más cursos.
- Resumen de inscripción con cursos seleccionados, costo por curso y total a pagar.
- Imagen Docker generada y publicada automáticamente en Docker Hub al hacer push a `main`.
- Despliegue automático de la imagen en una instancia EC2.

## Endpoints

### Listar cursos

```http
GET /api/courses
```

Respuesta:

```json
[
  {
    "id": 1,
    "name": "Spring Boot",
    "instructor": "Ana Soto",
    "durationHours": 32,
    "cost": 150000.00
  }
]
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

Respuesta:

```json
{
  "enrollmentId": 10,
  "studentName": "Camila Perez",
  "studentEmail": "camila@example.com",
  "courses": [
    {
      "courseId": 1,
      "name": "Spring Boot",
      "cost": 150000.00
    },
    {
      "courseId": 2,
      "name": "Cloud Computing",
      "cost": 120000.00
    }
  ],
  "totalCost": 270000.00,
  "createdAt": "2026-05-25T12:00:00Z"
}
```

## Configuración local

El servicio toma la conexión a Oracle desde variables de entorno:

```bash
DB_URL=jdbc:oracle:thin:@your-oracle-host:1521/your-service
DB_USERNAME=your_user
DB_PASSWORD=your_password
DDL_AUTO=update
PORT=8080
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

La instancia EC2 debe tener Docker instalado y permitir tráfico entrante al puerto `8080`.
