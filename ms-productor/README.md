# ms-guias-productor

Microservicio **productor** del sistema de Gestión de Pedidos y Generación de Guías de
Despacho (Sumativa 3 — Desarrollo Cloud Native).

## Qué hace

- CRUD completo de guías de despacho (`/api/dispatch-guides`): crear, consultar por
  transportista/fecha, obtener, actualizar, eliminar.
- Genera el PDF de la guía y lo sube a S3 (`/api/dispatch-guides/{id}/s3`).
- Permite descargar la guía con validación de permisos (código de acceso / rol JWT).
- Cada vez que se crea una guía, publica un mensaje en RabbitMQ (`dispatchGuideExchange`
  → `dispatchGuideQueue`, "cola 1"). El componente que consume esa cola es el
  microservicio hermano **ms-guias-consumidor** (código, no este mismo proceso).
- Seguridad con Spring Security + OAuth2 Resource Server (Azure AD B2C), con dos roles:
  `GUIDES_DOWNLOAD` (solo descargar) y `GUIDES_MANAGE` (todo el resto de endpoints).

## Ejecutar localmente

Este microservicio vive dentro del repo único (monorepo); el `docker-compose.yml`
del cluster RabbitMQ está en la raíz del repo, un nivel arriba de esta carpeta.

```bash
cp .env.example .env
# completa los valores (S3, RabbitMQ, Azure AD B2C)
cd ..                          # a la raíz del repo
docker compose up -d           # levanta el cluster RabbitMQ (rabbitmq1 + rabbitmq2)
cd ms-productor
mvn spring-boot:run
```

## Relación con ms-guias-consumidor

Ambos microservicios son componentes **independientes** (dos apps Spring Boot
distintas, dos repos), tal como exige el enunciado. Comparten:

- El mismo broker RabbitMQ (`docker-compose.yml`, red `rabbitmq_net`).
- La misma base de datos H2 embebida en archivo (`H2_DB_PATH`), montada como volumen
  compartido en `/data` en ambos contenedores: `dispatch_guides` la usa este productor;
  `dispatch_guide_queue_log` la usa el consumidor.

No comparten código ni se llaman entre sí por HTTP: la única comunicación entre ambos
es a través de la cola RabbitMQ.

## Despliegue (GitHub Actions → EC2)

El workflow `.github/workflows/deploy.yml` es idempotente y **siempre libera los
puertos que necesita antes de levantar contenedores**, aunque haya quedado un
contenedor detenido, "colgado" u ocupando el puerto de un run anterior:

- Antes de `docker compose up -d` (cluster RabbitMQ), elimina cualquier contenedor
  que publique los puertos `5672/15672/5673/15673`.
- Antes de `docker run` del microservicio, además del `docker stop/rm` por nombre,
  elimina cualquier contenedor que publique el puerto `8080`.

Esto hace que cada ejecución del Action deje la EC2 en un estado limpio sin
intervención manual, sin importar si los contenedores anteriores quedaron
corriendo, detenidos o con otro nombre.
