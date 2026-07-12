# Sistema de Gestión de Pedidos y Generación de Guías de Despacho

Sumativa 3 — Desarrollo Cloud Native. Repositorio único (monorepo) que contiene los dos
microservicios independientes que exige el enunciado, más la infraestructura compartida
para desarrollo local.

```
.
├── docker-compose.yml         # Cluster RabbitMQ (rabbitmq1 + rabbitmq2), compartido
├── ms-productor/               # CRUD de guías + publica en la cola 1 al crear una guía
└── ms-consumidor/              # Consume la cola 1, genera PDF, sube a S3, guarda en H2,
                                 # enruta errores a la DLQ (cola 2), admin de RabbitMQ
```

Cada microservicio es un proyecto Maven independiente (su propio `pom.xml`, `Dockerfile`,
`.env.example` y `README.md` con el detalle de sus endpoints). No comparten código ni se
llaman entre sí por HTTP — la única comunicación entre ambos es a través de RabbitMQ, y
comparten la misma base de datos H2 en archivo (montada en `/data` en ambos contenedores).

## Despliegue (GitHub Actions)

Al ser un solo repositorio, los workflows están en la raíz, en `.github/workflows/`
(es la única ubicación que GitHub reconoce), uno por microservicio:

- `.github/workflows/deploy-productor.yml` — build, test, imagen Docker y despliegue en
  EC2 de `ms-productor`. Se dispara solo cuando cambia algo dentro de `ms-productor/**`
  (o `docker-compose.yml`, o el propio workflow).
- `.github/workflows/deploy-consumidor.yml` — lo mismo para `ms-consumidor`.

Cada workflow es independiente: un push que solo toca `ms-productor/` no vuelve a
desplegar `ms-consumidor`, y viceversa. Ambos son idempotentes y liberan los puertos que
necesitan en la EC2 en cada ejecución (ver el README de cada microservicio para el
detalle), así que también puedes disparar cualquiera de los dos manualmente
(`workflow_dispatch` o un push vacío) sin miedo a dejar contenedores duplicados u
ocupando el puerto.

Los Secrets de GitHub (Docker Hub, EC2, S3, Azure AD B2C) se configuran **una sola vez**
en este repositorio y los reutilizan ambos workflows.

## Ejecutar todo localmente

```bash
docker compose up -d          # cluster RabbitMQ (rabbitmq1 + rabbitmq2)
cd ms-productor && cp .env.example .env && mvn spring-boot:run &
cd ms-consumidor && cp .env.example .env && mvn spring-boot:run &
```

Revisa el `README.md` de cada microservicio para el detalle de endpoints, variables de
entorno y seguridad.
