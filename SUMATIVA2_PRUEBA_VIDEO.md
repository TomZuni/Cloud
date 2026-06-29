# Sumativa 2 - checklist de prueba y video

Usar esta lista durante la grabacion. La regla principal del profesor es: todo debe probarse en vivo, desde Postman apuntando a la URL publica de API Gateway, nunca a localhost ni a la IP publica/elastica del EC2.

## Antes de grabar

- Despliegue activo con `SPRING_PROFILES_ACTIVE=online,dispatch-guides`.
- API Gateway configurado solo con rutas del sistema de guias de despacho.
- Todas las rutas del API Gateway apuntan a la integracion del microservicio desplegado en EC2.
- Todas las rutas tienen autorizacion JWT activa.
- Bucket S3 visible en consola AWS para mostrar cambios antes y despues de las llamadas.
- Postman configurado con OAuth 2.0 para obtener JWT desde el IDaaS.
- Tener dos tokens/permisos definidos en IDaaS:
  - permiso de descarga: solo descargar guias, tanto desde EFS como desde S3. En Spring puede llegar como `SCOPE_guides.download` o `ROLE_GUIDES_DOWNLOAD`.
  - permiso de administracion: crear, subir, modificar, eliminar y consultar guias. En Spring puede llegar como `SCOPE_guides.manage` o `ROLE_GUIDES_MANAGE`.
- Tener abierto un bloc con los valores no sensibles:
  - URL publica de API Gateway.
  - nombre del bucket S3.
  - prefijo S3, por ejemplo `guias`.
  - tenant ID / issuer / audience del token.

## Rutas que deben existir en API Gateway

Crear una ruta por cada endpoint que se vaya a demostrar. Para este proyecto son:

| Funcion | Metodo y ruta |
| --- | --- |
| Crear guia | `POST /api/dispatch-guides` |
| Consultar/listar por transportista y fecha | `GET /api/dispatch-guides` |
| Obtener detalle de una guia | `GET /api/dispatch-guides/{guideId}` |
| Actualizar guia | `PUT /api/dispatch-guides/{guideId}` |
| Descargar guia temporal/EFS | `GET /api/dispatch-guides/{guideId}/efs` |
| Subir guia generada a S3 | `POST /api/dispatch-guides/{guideId}/s3` |
| Descargar guia desde S3 con codigo de acceso | `GET /api/dispatch-guides/{guideId}/s3?accessCode={accessCode}` |
| Eliminar guia | `DELETE /api/dispatch-guides/{guideId}` |

En la pestana `Integrations`, mostrar todas estas rutas. En la pestana `Authorization`, mostrar que todas tienen la marca azul `JWT Auth`.

## Payloads para Postman

Crear guia:

```json
{
  "orderNumber": "PED-2026-0001",
  "carrierName": "Transportes Andina",
  "carrierRut": "76.123.456-7",
  "recipientName": "Comercial Los Aromos",
  "originAddress": "Av. Apoquindo 4501, Las Condes",
  "destinationAddress": "Camino Industrial 1200, Rancagua",
  "packageDescription": "12 cajas selladas con repuestos mecanicos, peso aproximado 180 kg, retiro contra orden PED-2026-0001.",
  "dispatchDate": "2026-06-29"
}
```

Actualizar guia:

```json
{
  "orderNumber": "PED-2026-0001-ACT",
  "carrierName": "Transportes Cordillera",
  "carrierRut": "76.123.456-7",
  "recipientName": "Comercial Los Aromos",
  "originAddress": "Av. Apoquindo 4501, Las Condes",
  "destinationAddress": "Ruta 5 Sur Km 85, Rancagua",
  "packageDescription": "12 cajas selladas con repuestos mecanicos y una caja adicional de herramientas, peso aproximado 210 kg.",
  "dispatchDate": "2026-06-29"
}
```

Consultar filtrado:

```http
GET {{base_url}}/api/dispatch-guides?carrierName=Transportes%20Cordillera&dispatchDate=2026-06-29
```

Guardar desde la respuesta de creacion:

- `id`: usarlo como `guideId`.
- `accessCode`: usarlo para descargar desde S3.
- `s3Key`: debe aparecer despues de subir a S3.

## Orden recomendado para grabar

1. Presentar objetivo en 20 segundos: sistema cloud native para gestion de guias de despacho, protegido con API Gateway e IDaaS, integrado con S3 y desplegado en Docker/EC2.
2. Mostrar API Gateway.
   - Abrir `Integrations`.
   - Mostrar todas las rutas de guias.
   - Abrir `Authorization`.
   - Mostrar `JWT Auth` en todas las rutas.
   - Mostrar la URL publica del API Gateway y decir que esa sera usada en Postman.
3. Mostrar IDaaS/Azure AD B2C.
   - Tenant creado.
   - App creada.
   - Secreto creado, sin revelar el valor completo.
   - API expuesta.
   - Permisos/roles: descarga y administracion.
4. Mostrar Postman OAuth 2.0.
   - Grant type configurado.
   - Token URL, client ID, scope/audience segun el IDaaS.
   - Presionar `Get New Access Token` en vivo.
   - Confirmar que Postman recibio el JWT.
5. Probar seguridad negativa primero.
   - Llamar un endpoint sin token o con un caracter borrado del JWT.
   - Mostrar respuesta `401 Unauthorized`.
6. Probar creacion de guia.
   - `POST /api/dispatch-guides`.
   - Mostrar respuesta `201`.
   - Guardar `id` y `accessCode`.
   - Mostrar que se genero una guia real, no archivo vacio ni "hola mundo".
7. Probar subida a S3.
   - Antes del POST, mostrar en S3 que el objeto aun no existe o mostrar el estado actual del prefijo.
   - `POST /api/dispatch-guides/{guideId}/s3`.
   - Volver a S3 y refrescar para mostrar el PDF creado en `guias/{fecha}/{transportista}/`.
8. Probar descarga desde S3.
   - `GET /api/dispatch-guides/{guideId}/s3?accessCode={accessCode}`.
   - Mostrar descarga correcta del PDF.
   - Si se demuestra permisos, usar el token/permiso de descarga para este endpoint.
   - Con ese mismo token de descarga, intentar crear o modificar una guia y mostrar que falla por falta de permisos.
9. Probar consulta filtrada.
   - `GET /api/dispatch-guides?carrierName=...&dispatchDate=...`.
   - Mostrar que retorna la guia creada.
10. Probar actualizacion.
   - `PUT /api/dispatch-guides/{guideId}` con el payload actualizado.
   - Volver a S3, refrescar y mostrar que el objeto cambio de ruta o contenido si cambio transportista/fecha.
   - Descargar nuevamente para verificar que la guia refleja los datos nuevos.
11. Probar detalle individual.
   - `GET /api/dispatch-guides/{guideId}`.
   - Mostrar `uploadedToS3=true` y `s3Key`.
12. Probar eliminacion.
   - `DELETE /api/dispatch-guides/{guideId}`.
   - Mostrar respuesta `204`.
   - Volver a S3 y refrescar para evidenciar que el documento fue eliminado.
   - Intentar consultar o descargar la guia eliminada y mostrar `404`.

## Checklist de evaluacion

| Criterio de pauta | Evidencia concreta en video |
| --- | --- |
| API Gateway, 15 pts | Rutas creadas, integraciones visibles, URL publica usada en Postman, JWT Auth en todas las rutas |
| IDaaS/JWT, 15 pts | Tenant, app, secreto, API expuesta, permisos/roles y obtencion de JWT |
| Integracion API Gateway + IDaaS, 15 pts | Llamadas con JWT exitosas y llamada sin JWT/token alterado con `401` |
| Endpoints Spring Boot, 15 pts | Crear, subir, descargar, actualizar, eliminar, consultar por transportista/fecha y detalle |
| S3, 15 pts | Mostrar el bucket antes/despues de subir, modificar y eliminar |
| Docker, 15 pts | Mostrar despliegue en EC2/Docker o workflow GitHub Actions exitoso |
| Video, 10 pts | Grabacion entre 5 y 10 minutos, ordenada, todo en vivo y usando API Gateway |

## Errores que no deben aparecer

- Probar contra `localhost`, `127.0.0.1` o la IP del EC2.
- Mostrar solo respuestas antiguas de Postman sin ejecutar la llamada en vivo.
- Usar archivos vacios o textos de prueba tipo "hola mundo".
- Olvidar la consulta por transportista y fecha.
- Modificar una guia sin volver a mostrar el cambio en S3.
- Mostrar rutas antiguas de cursos/inscripciones como si fueran parte de esta sumativa.
- Mostrar el valor completo del client secret en la grabacion.
