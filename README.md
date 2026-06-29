# Sistema de Gestion de Guias de Despacho

Microservicio Spring Boot para la Sumativa 2 de Desarrollo Cloud Native. La solucion permite crear, consultar, actualizar, descargar y eliminar guias de despacho, generar documentos PDF reales, almacenar archivos temporalmente en EFS y administrarlos en AWS S3. El acceso queda protegido con JWT emitidos por Azure AD B2C/IDaaS y expuesto por AWS API Gateway.

## Alcance de la entrega

- Backend Spring Boot securitizado con Spring Security y JWT.
- API Gateway con una ruta por endpoint del microservicio.
- Integracion con Azure AD B2C/IDaaS para emision de tokens JWT.
- Dos permisos de acceso:
  - descarga: `guides.download` o rol `GUIDES_DOWNLOAD`.
  - administracion: `guides.manage` o rol `GUIDES_MANAGE`.
- Generacion de guias de despacho en PDF.
- Almacenamiento temporal en EFS.
- Subida, descarga, actualizacion y eliminacion de documentos en S3.
- Despliegue con Docker, Docker Hub, GitHub Actions y EC2.

Los controladores antiguos de cursos e inscripciones quedan desactivados cuando se despliega con el perfil `dispatch-guides`, para mantener separada esta sumativa de actividades formativas anteriores.

## Perfil de ejecucion

Para esta entrega se debe usar:

```env
SPRING_PROFILES_ACTIVE=online,dispatch-guides
```

El workflow de GitHub Actions ya despliega el contenedor con ese perfil.

## Endpoints de guias

Todas las llamadas del video deben hacerse contra la URL publica del API Gateway.

| Funcion | Metodo y ruta |
| --- | --- |
| Crear guia | `POST /api/dispatch-guides` |
| Consultar/listar por transportista y fecha | `GET /api/dispatch-guides?carrierName={carrierName}&dispatchDate={yyyy-MM-dd}` |
| Obtener detalle | `GET /api/dispatch-guides/{guideId}` |
| Actualizar guia | `PUT /api/dispatch-guides/{guideId}` |
| Descargar guia temporal/EFS | `GET /api/dispatch-guides/{guideId}/efs` |
| Subir guia a S3 | `POST /api/dispatch-guides/{guideId}/s3` |
| Descargar guia desde S3 | `GET /api/dispatch-guides/{guideId}/s3?accessCode={accessCode}` |
| Eliminar guia | `DELETE /api/dispatch-guides/{guideId}` |

## Ejemplo de creacion

```http
POST /api/dispatch-guides
Content-Type: application/json
Authorization: Bearer {access_token}
```

```json
{
  "orderNumber": "PED-2026-0001",
  "carrierName": "Transportes Andina",
  "carrierRut": "76.123.456-7",
  "recipientName": "Comercial Los Aromos",
  "originAddress": "Av. Apoquindo 4501, Las Condes",
  "destinationAddress": "Camino Industrial 1200, Rancagua",
  "packageDescription": "12 cajas selladas con repuestos mecanicos, peso aproximado 180 kg.",
  "dispatchDate": "2026-06-29"
}
```

La respuesta entrega el `id` de la guia y el `accessCode` requerido para descargar desde S3.

## Configuracion JWT

Variables principales:

```env
JWT_VALIDATION_ENABLED=true
AZURE_AD_ISSUER_URI=https://your-tenant.b2clogin.com/your-tenant-id/v2.0/
AZURE_AD_JWK_SET_URI=https://your-tenant.b2clogin.com/your-tenant.onmicrosoft.com/B2C_1_your_flow/discovery/v2.0/keys
AZURE_AD_ALLOWED_AUDIENCES=your-api-client-id
AZURE_AD_ALLOWED_POLICIES=B2C_1_your_flow
APP_SECURITY_DOWNLOAD_AUTHORITIES=SCOPE_guides.download,ROLE_GUIDES_DOWNLOAD
APP_SECURITY_MANAGE_AUTHORITIES=SCOPE_guides.manage,ROLE_GUIDES_MANAGE
```

El token puede traer permisos como scopes (`scp`/`scope`) o roles (`roles`). Spring acepta ambos formatos.

## Configuracion AWS

```env
AWS_REGION=us-east-1
AWS_S3_BUCKET=your_bucket_name
AWS_S3_GUIDE_PREFIX=guias
EFS_MOUNT_PATH=/mnt/efs
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
AWS_SESSION_TOKEN=your_session_token_if_using_academy
```

Los objetos S3 se guardan con esta estructura:

```text
guias/{fecha-despacho}/{transportista}/guia-despacho-{id}.pdf
```
