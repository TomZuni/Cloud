# ms-guias-consumidor

Microservicio **consumidor** del sistema de Gestión de Pedidos y Generación de Guías de
Despacho (Sumativa 3 — Desarrollo Cloud Native).

## Qué hace

- Escucha `dispatchGuideQueue` ("cola 1") con acknowledgment manual
  (`DispatchGuideQueueConsumer`). Por cada mensaje: genera el PDF de la guía, lo sube a
  S3 y guarda el resultado en la tabla H2 `dispatch_guide_queue_log` (distinta a la
  tabla `dispatch_guides` que usa el productor, ambas en el mismo archivo H2
  compartido vía `H2_DB_PATH`).
- Si el procesamiento falla (incluido un error forzado de prueba), rechaza el mensaje
  (`nack`, sin requeue) y RabbitMQ lo enruta automáticamente a la DLQ
  `dispatchGuideErrorQueue` ("cola 2") vía Dead Letter Exchange.
  - Para forzar el error en el video: crea una guía con `orderNumber` que contenga
    `ERROR_TEST` (ej: `"PED-ERROR_TEST-01"`).
- Endpoint adicional `GET /api/dispatch-guides/queue-log` (con filtro opcional
  `?status=PROCESSED|ERROR`): expone lo que este consumidor ha ido guardando desde la
  cola 1.
- Administración de RabbitMQ vía `AmqpAdmin` y control del listener vía
  `RabbitListenerEndpointRegistry`, bajo `/api/rabbit-admin/**`:
  - `POST /api/rabbit-admin/colas/{nombre}` / `DELETE ...` — crear/eliminar colas.
  - `POST /api/rabbit-admin/exchanges/{nombre}` / `DELETE ...` — crear/eliminar exchanges.
  - `POST /api/rabbit-admin/bindings` — crear bindings.
  - `POST /api/rabbit-admin/listener/pausar` / `reanudar` / `status` — controlar el
    listener del consumidor en tiempo real.
- Seguridad con Spring Security + OAuth2 Resource Server (Azure AD B2C), mismos roles
  que el productor: `GUIDES_DOWNLOAD` (solo consultar `queue-log`) y `GUIDES_MANAGE`
  (endpoints de administración RabbitMQ).

## Ejecutar localmente

Este microservicio vive dentro del repo único (monorepo); el `docker-compose.yml`
del cluster RabbitMQ está en la raíz del repo, un nivel arriba de esta carpeta.

```bash
cp .env.example .env
# completa los valores (S3, RabbitMQ, Azure AD B2C)
cd ..                          # a la raíz del repo
docker compose up -d           # levanta el cluster RabbitMQ (rabbitmq1 + rabbitmq2)
cd ms-consumidor
mvn spring-boot:run
```

## Relación con ms-guias-productor

Componente **independiente** del productor (dos apps Spring Boot distintas, dos repos).
Comparten el mismo broker RabbitMQ y la misma base de datos H2 embebida en archivo
(volumen montado en `/data`). No se llaman entre sí por HTTP: la única comunicación
entre ambos es a través de la cola RabbitMQ.

## Despliegue (GitHub Actions → EC2)

Igual que el productor, `.github/workflows/deploy.yml` libera los puertos
`5672/15672/5673/15673` (cluster RabbitMQ) y `8081` (este microservicio) antes de
levantar contenedores, eliminando cualquier contenedor previo que los ocupe, esté
corriendo, detenido o con otro nombre — así el Action nunca falla por "puerto en uso".
