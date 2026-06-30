# Sumativa 2 - Paso a paso de implementacion

Proyecto: Sistema de Gestion de Pedidos y Guias de Despacho  
Asignatura: Desarrollo Cloud Native, Semana 6  
Backend: Spring Boot + Docker + GitHub Actions  
Cloud: AWS Academy, API Gateway HTTP API, EC2, S3  
IDaaS: Azure AD B2C / Microsoft Entra External ID con JWT

> Nota importante 2026: Microsoft informa que Azure AD B2C dejo de estar disponible para compra de nuevos clientes desde el 1 de mayo de 2025. Si en la cuenta estudiante no aparece "Azure AD B2C", usar el tenant de clientes de Microsoft Entra External ID equivalente o confirmar con el docente. Para la entrega, mantener el nombre "Azure AD B2C" si ese es el requisito de la pauta.

## 1. Objetivo de la entrega

Implementar un backend Cloud Native que permita:

- Crear guias de despacho.
- Generar un PDF real de la guia.
- Subir guias generadas a AWS S3.
- Descargar guias con validacion de permisos.
- Modificar o actualizar guias.
- Eliminar guias especificas.
- Consultar guias por transportista y fecha.
- Proteger los endpoints con Azure AD B2C y Spring Security.
- Exponer todos los endpoints por AWS API Gateway.
- Desplegar la aplicacion en Docker usando GitHub Actions.

## 2. Arquitectura

```text
Postman
  |
  | Authorization: Bearer <JWT Azure AD B2C>
  v
AWS API Gateway HTTP API
  |  valida issuer/audience del JWT
  v
EC2 con Docker
  |  Spring Boot + Spring Security valida JWT y permisos
  |  genera PDF y guarda archivo temporal
  v
AWS S3 bucket
  |  almacena guias PDF
```

El video debe llamar siempre a la URL publica de API Gateway. No usar `localhost` ni la IP publica del EC2 en la demostracion final.

## 3. Repositorio y backend

### 3.1 Archivos principales

- `src/main/java/cl/education/enrollment/controller/DispatchGuideController.java`: expone los endpoints REST.
- `src/main/java/cl/education/enrollment/service/DispatchGuideService.java`: reglas de negocio, EFS temporal y S3.
- `src/main/java/cl/education/enrollment/service/DispatchGuidePdfService.java`: genera PDFs reales con PDFBox.
- `src/main/java/cl/education/enrollment/config/SecurityConfig.java`: valida JWT, scopes y roles.
- `src/main/resources/application.yml`: variables de AWS, Azure AD B2C y seguridad.
- `src/main/resources/application-online.yml`: usa H2 persistente en `/data` para despliegue estudiante.
- `.github/workflows/deploy.yml`: pipeline de test, build, push Docker y despliegue en EC2.
- `postman/SUMATIVA2_dispatch_guides.postman_collection.json`: coleccion de pruebas.

### 3.2 Perfil que se debe usar

En EC2 y en GitHub Actions usar:

```env
SPRING_PROFILES_ACTIVE=online,dispatch-guides
```

`online` deja una base H2 persistente en `/data/course_enrollment`.  
`dispatch-guides` activa los controladores y servicios de guias de despacho.

### 3.3 Endpoints del backend

| Funcion | Metodo y ruta |
| --- | --- |
| Crear guia | `POST /api/dispatch-guides` |
| Consultar por transportista y fecha | `GET /api/dispatch-guides?carrierName={carrierName}&dispatchDate={yyyy-MM-dd}` |
| Obtener detalle | `GET /api/dispatch-guides/{guideId}` |
| Actualizar guia | `PUT /api/dispatch-guides/{guideId}` |
| Descargar guia temporal | `GET /api/dispatch-guides/{guideId}/efs` |
| Subir guia a S3 | `POST /api/dispatch-guides/{guideId}/s3` |
| Descargar guia desde S3 | `GET /api/dispatch-guides/{guideId}/s3?accessCode={accessCode}` |
| Eliminar guia | `DELETE /api/dispatch-guides/{guideId}` |

## 4. Desarrollo local rapido

Ejecutar pruebas:

```powershell
mvn -B test
```

Construir el jar:

```powershell
mvn -B package
```

Ejecutar localmente sin JWT para validar el backend:

```powershell
$env:SPRING_PROFILES_ACTIVE="online,dispatch-guides"
$env:JWT_VALIDATION_ENABLED="false"
$env:EFS_MOUNT_PATH="./target/local-efs"
$env:AWS_REGION="us-east-1"
$env:AWS_S3_BUCKET="bucket-de-prueba"
java -jar target/course-enrollment-api-0.0.1-SNAPSHOT.jar
```

En la entrega final, volver a `JWT_VALIDATION_ENABLED=true`.

## 5. AWS Academy - EC2

### 5.1 Crear instancia

1. Entrar a AWS Academy Learner Lab o VocLabs.
2. Abrir AWS Console en `us-east-1`.
3. Ir a EC2.
4. Crear una instancia con Amazon Linux 2023 o Ubuntu.
5. Tipo recomendado estudiante: `t2.micro` o `t3.micro`, segun disponibilidad.
6. Crear o seleccionar un key pair.
7. En Security Group permitir:
   - SSH `22` desde tu IP.
   - HTTP backend `8080` temporalmente para API Gateway.
8. Lanzar instancia.
9. Copiar DNS publico o IP publica, por ejemplo:

```text
ec2-xx-xx-xx-xx.compute-1.amazonaws.com
```

### 5.2 Instalar Docker en EC2

Amazon Linux:

```bash
sudo dnf update -y
sudo dnf install -y docker
sudo systemctl enable docker
sudo systemctl start docker
sudo usermod -aG docker ec2-user
```

Ubuntu:

```bash
sudo apt-get update
sudo apt-get install -y docker.io
sudo systemctl enable docker
sudo systemctl start docker
sudo usermod -aG docker ubuntu
```

Cerrar sesion SSH y volver a entrar para que el grupo `docker` quede aplicado.

### 5.3 Carpetas persistentes en EC2

El workflow crea estas carpetas automaticamente:

```bash
~/course-enrollment-api/data
~/course-enrollment-api/efs
```

El contenedor las monta asi:

```text
~/course-enrollment-api/data -> /data
~/course-enrollment-api/efs  -> /mnt/efs
```

## 6. AWS Academy - S3

1. Ir a S3.
2. Crear bucket en `us-east-1`.
3. Nombre sugerido:

```text
sumativa2-guias-<tu-nombre-o-iniciales>
```

4. Dejar bloqueo de acceso publico activado. Las guias no se publican directo por S3, se descargan por el backend.
5. Copiar el nombre del bucket para `AWS_S3_BUCKET`.
6. En el video mostrar el bucket vacio antes de subir una guia.
7. Despues de llamar `POST /api/dispatch-guides/{guideId}/s3`, refrescar S3 y mostrar el objeto creado.

Ruta esperada de los objetos:

```text
guias/{fecha-despacho}/{transportista}/guia-despacho-{id}.pdf
```

Ejemplo:

```text
guias/2026-06-29/transportes-andina/guia-despacho-1.pdf
```

## 7. Azure AD B2C / IDaaS

### 7.1 Crear o seleccionar tenant

1. Entrar al portal de Azure con la cuenta estudiante.
2. Buscar `Azure AD B2C`.
3. Crear un tenant B2C o seleccionar uno existente.
4. Si aparece Microsoft Entra External ID en vez de B2C, usar el tenant de clientes equivalente y dejar evidencia en el documento.
5. Copiar:
   - nombre del tenant, ejemplo `miempresa`.
   - dominio, ejemplo `miempresa.onmicrosoft.com`.
   - tenant id.

### 7.2 Crear flujo de usuario

1. En Azure AD B2C, entrar a `User flows`.
2. Crear flujo `Sign up and sign in`.
3. Nombre sugerido:

```text
B2C_1_susi
```

4. Habilitar email como metodo de identidad.
5. Guardar el flujo.

### 7.3 Registrar API protegida

1. Ir a `App registrations`.
2. Crear nueva app:

```text
sumativa2-dispatch-guides-api
```

3. Sin redirect URI.
4. Copiar `Application (client) ID`; se usara como audiencia.
5. Entrar a `Expose an API`.
6. Definir `Application ID URI`, por ejemplo:

```text
api://<client-id-api>
```

7. Agregar scopes:

| Scope | Uso |
| --- | --- |
| `guides.download` | permite descargar guias |
| `guides.manage` | permite crear, consultar, actualizar, subir a S3 y eliminar |

Si el docente pide explicitamente "roles", agregar tambien `App roles` en el manifest:

```json
[
  {
    "allowedMemberTypes": ["User", "Application"],
    "description": "Permite descargar guias de despacho.",
    "displayName": "GUIDES_DOWNLOAD",
    "id": "GENERAR-GUID-AQUI",
    "isEnabled": true,
    "value": "GUIDES_DOWNLOAD"
  },
  {
    "allowedMemberTypes": ["User", "Application"],
    "description": "Permite administrar guias de despacho.",
    "displayName": "GUIDES_MANAGE",
    "id": "GENERAR-GUID-AQUI",
    "isEnabled": true,
    "value": "GUIDES_MANAGE"
  }
]
```

Generar un GUID distinto para cada rol:

```powershell
[guid]::NewGuid()
```

### 7.4 Registrar cliente para Postman

1. Ir a `App registrations`.
2. Crear nueva app:

```text
sumativa2-postman-client
```

3. Redirect URI para Postman:

```text
https://oauth.pstmn.io/v1/callback
```

4. Crear un client secret en `Certificates & secrets`.
5. Copiar el valor del secreto apenas se crea.
6. En `API permissions`, agregar permisos de la API creada:
   - `guides.download`
   - `guides.manage`
7. Presionar `Grant admin consent`.

Para demostrar dos permisos separados, crear opcionalmente dos clientes:

| Cliente | Permiso |
| --- | --- |
| `sumativa2-postman-download` | solo `guides.download` |
| `sumativa2-postman-manage` | `guides.manage` |

### 7.5 Datos que se deben copiar

Issuer para API Gateway y Spring:

```text
https://<tenant>.b2clogin.com/<tenant-id>/v2.0/
```

JWK Set URI:

```text
https://<tenant>.b2clogin.com/<tenant>.onmicrosoft.com/B2C_1_susi/discovery/v2.0/keys
```

Metadata para revisar valores:

```text
https://<tenant>.b2clogin.com/<tenant>.onmicrosoft.com/B2C_1_susi/v2.0/.well-known/openid-configuration
```

Audiencia:

```text
<client-id-api>
```

Politica/flujo:

```text
B2C_1_susi
```

## 8. GitHub Actions

El workflow `.github/workflows/deploy.yml` hace:

1. Checkout del repositorio.
2. Configura Java 21.
3. Ejecuta `mvn -B test`.
4. Construye la imagen Docker.
5. Publica `latest` y `${{ github.sha }}` en Docker Hub.
6. Entra por SSH al EC2.
7. Crea `app.env`.
8. Descarga la imagen y reinicia el contenedor.

### 8.1 Secrets requeridos

En GitHub:

```text
Settings -> Secrets and variables -> Actions -> New repository secret
```

Crear estos secrets:

| Secret | Ejemplo / descripcion |
| --- | --- |
| `DOCKERHUB_USERNAME` | usuario Docker Hub |
| `DOCKERHUB_TOKEN` | access token de Docker Hub |
| `EC2_HOST` | DNS o IP publica del EC2 |
| `EC2_PORT` | `22` |
| `EC2_USER` | `ec2-user` en Amazon Linux, `ubuntu` en Ubuntu |
| `EC2_SSH_KEY` | contenido completo del archivo `.pem` |
| `AWS_REGION` | `us-east-1` |
| `AWS_S3_BUCKET` | bucket creado en S3 |
| `AWS_S3_GUIDE_PREFIX` | `guias` |
| `AWS_ACCESS_KEY_ID` | credencial de AWS Academy |
| `AWS_SECRET_ACCESS_KEY` | credencial de AWS Academy |
| `AWS_SESSION_TOKEN` | token temporal de AWS Academy |
| `JWT_VALIDATION_ENABLED` | `true` |
| `AZURE_AD_ISSUER_URI` | issuer copiado desde B2C |
| `AZURE_AD_JWK_SET_URI` | jwks_uri de B2C |
| `AZURE_AD_ALLOWED_AUDIENCES` | client id de la API |
| `AZURE_AD_ALLOWED_POLICIES` | `B2C_1_susi` |
| `APP_SECURITY_DOWNLOAD_AUTHORITIES` | `SCOPE_guides.download,ROLE_GUIDES_DOWNLOAD` |
| `APP_SECURITY_MANAGE_AUTHORITIES` | `SCOPE_guides.manage,SCOPE_access_as_user,SCOPE_user_impersonation,ROLE_GUIDES_MANAGE` |

En AWS Academy las credenciales expiran. Si el deploy empieza a fallar con S3, renovar `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` y `AWS_SESSION_TOKEN`.

### 8.2 Ejecutar deploy

1. Hacer commit y push a `main`.
2. Ir a GitHub `Actions`.
3. Abrir el workflow `Build and Deploy`.
4. Confirmar que `Test`, `Build and publish Docker image` y `Deploy on EC2` terminen en verde.
5. En EC2 validar:

```bash
docker ps
docker logs course-enrollment-api --tail 100
curl http://localhost:8080/actuator/health
```

Debe responder:

```json
{"status":"UP"}
```

## 9. AWS API Gateway

Usar API Gateway HTTP API.

### 9.1 Crear API

1. Ir a API Gateway en `us-east-1`.
2. Seleccionar `HTTP API`.
3. Crear API con nombre:

```text
sumativa2-dispatch-guides-api
```

4. Crear stage:

```text
prod
```

5. Activar `Auto-deploy`.

### 9.2 Crear rutas e integraciones

Crear una ruta por cada endpoint. Todas apuntan al EC2 por HTTP.

Base de integracion:

```text
http://<EC2_PUBLIC_DNS>:8080
```

| Route key | Integration URL |
| --- | --- |
| `POST /api/dispatch-guides` | `http://<EC2_PUBLIC_DNS>:8080/api/dispatch-guides` |
| `GET /api/dispatch-guides` | `http://<EC2_PUBLIC_DNS>:8080/api/dispatch-guides` |
| `GET /api/dispatch-guides/{guideId}` | `http://<EC2_PUBLIC_DNS>:8080/api/dispatch-guides/{guideId}` |
| `PUT /api/dispatch-guides/{guideId}` | `http://<EC2_PUBLIC_DNS>:8080/api/dispatch-guides/{guideId}` |
| `GET /api/dispatch-guides/{guideId}/efs` | `http://<EC2_PUBLIC_DNS>:8080/api/dispatch-guides/{guideId}/efs` |
| `POST /api/dispatch-guides/{guideId}/s3` | `http://<EC2_PUBLIC_DNS>:8080/api/dispatch-guides/{guideId}/s3` |
| `GET /api/dispatch-guides/{guideId}/s3` | `http://<EC2_PUBLIC_DNS>:8080/api/dispatch-guides/{guideId}/s3` |
| `DELETE /api/dispatch-guides/{guideId}` | `http://<EC2_PUBLIC_DNS>:8080/api/dispatch-guides/{guideId}` |

Primero probar sin authorizer, como recomienda el PPT:

```text
Postman -> API Gateway URL -> backend responde
```

### 9.3 Crear JWT authorizer

1. En la API HTTP, ir a `Authorization`.
2. Entrar a `Manage authorizers`.
3. Crear authorizer:

```text
Name: azure-b2c-jwt
Type: JWT
Identity source: $request.header.Authorization
Issuer URL: https://<tenant>.b2clogin.com/<tenant-id>/v2.0/
Audience: <client-id-api>
```

4. Guardar.

API Gateway valida firma, issuer, audiencia y expiracion del JWT. Si usas scopes de autorizacion en la ruta, tambien valida `scope` o `scp`.

### 9.4 Activar authorizer en rutas

En `Authorization`, asociar `azure-b2c-jwt` a todas las rutas.

Recomendacion:

| Rutas | Authorization scopes |
| --- | --- |
| Descarga `GET /api/dispatch-guides/{guideId}/efs` | `guides.download`, `guides.manage` |
| Descarga `GET /api/dispatch-guides/{guideId}/s3` | `guides.download`, `guides.manage` |
| Todas las demas rutas | `guides.manage` |

Si el token trae roles en `roles` pero no trae scopes `scp`, dejar `Authorization scopes` vacio en API Gateway y dejar que Spring Security valide los roles. Igual debe quedar la marca azul `JWT Auth` en todas las rutas.

### 9.5 URL publica

Copiar el Invoke URL del stage:

```text
https://<api-id>.execute-api.us-east-1.amazonaws.com/prod
```

Ese valor va en la variable `base_url` de Postman.

## 10. Postman

### 10.1 Configurar OAuth 2.0

En la coleccion Postman:

1. Ir a `Authorization`.
2. Type: `OAuth 2.0`.
3. Grant Type recomendado para esta entrega: `Authorization Code` con PKCE o `Client Credentials`, segun lo permita el tenant.

Client Credentials:

```text
Access Token URL:
https://<tenant>.b2clogin.com/<tenant>.onmicrosoft.com/B2C_1_susi/oauth2/v2.0/token

Client ID:
<client-id-postman>

Client Secret:
<secret-postman>

Scope:
api://<client-id-api>/.default
```

Authorization Code con PKCE:

```text
Auth URL:
https://<tenant>.b2clogin.com/<tenant>.onmicrosoft.com/B2C_1_susi/oauth2/v2.0/authorize

Access Token URL:
https://<tenant>.b2clogin.com/<tenant>.onmicrosoft.com/B2C_1_susi/oauth2/v2.0/token

Callback URL:
https://oauth.pstmn.io/v1/callback

Client ID:
<client-id-postman>

Client Secret:
<secret-postman si aplica>

Scope:
openid offline_access api://<client-id-api>/guides.manage api://<client-id-api>/guides.download
```

Presionar `Get New Access Token`, autenticar y usar `Use Token`.

### 10.2 Variables de coleccion

Editar:

```text
base_url=https://<api-id>.execute-api.us-east-1.amazonaws.com/prod
access_token=<token obtenido>
guide_id=
access_code=
```

La primera request guarda automaticamente `guide_id` y `access_code`.

### 10.3 Orden de pruebas para el video

1. Obtener token JWT desde Postman.
2. `01 - Crear guia`: debe responder `201` y devolver `id` + `accessCode`.
3. Ver PDF temporal con `07 - Descargar guia temporal EFS`.
4. Mostrar bucket S3 antes de subir.
5. `02 - Subir guia a S3`: debe responder `201`.
6. Refrescar S3 y mostrar el PDF creado.
7. `03 - Descargar guia desde S3`: debe descargar el PDF con `accessCode`.
8. `04 - Consultar por transportista y fecha`: debe listar la guia.
9. `05 - Actualizar guia`: debe cambiar datos y regenerar PDF.
10. `06 - Ver detalle de guia`: confirmar cambios.
11. `08 - Seguridad negativa sin JWT`: debe responder `401`.
12. `09 - Seguridad negativa JWT alterado`: debe responder `401`.
13. `10 - Eliminar guia`: debe responder `204`.
14. `11 - Validar guia eliminada`: debe responder `404`.

## 11. Evidencias que deben ir al Word

Capturar pantallas de:

- Repositorio GitHub con codigo.
- Workflow GitHub Actions en verde.
- Docker Hub con imagen publicada.
- EC2 corriendo el contenedor.
- `curl http://localhost:8080/actuator/health` en EC2 con `UP`.
- Bucket S3 creado.
- Objeto S3 creado despues del upload.
- Tenant Azure AD B2C o Entra External ID.
- App registration de la API.
- Scopes o roles creados.
- Client secret creado, ocultando el valor.
- User flow creado.
- API Gateway con rutas en `Integrations`.
- API Gateway con `JWT Auth` en `Authorization`.
- URL publica de API Gateway.
- Postman obteniendo token.
- Postman ejecutando cada endpoint requerido.
- Prueba sin JWT o con JWT alterado respondiendo `401`.

## 12. Guion recomendado para el video

Duracion objetivo: 7 a 9 minutos.

1. Presentar repositorio y explicar que el workflow despliega Docker en EC2.
2. Mostrar GitHub Actions en verde.
3. Mostrar EC2 con contenedor activo.
4. Mostrar S3 vacio o con prefijo de guias.
5. Mostrar API Gateway con rutas e integraciones.
6. Mostrar Azure AD B2C: tenant, app, secreto, API expuesta y scopes/roles.
7. En Postman, obtener JWT.
8. Ejecutar todos los endpoints usando solo API Gateway.
9. Mostrar que S3 cambia al subir/eliminar guia.
10. Quitar el JWT o alterar un caracter y demostrar `401`.
11. Cerrar explicando que se cumplen API Gateway, IDaaS, integracion, Spring Security, S3, Docker y video.

## 13. Mapeo con la pauta

| Criterio | Como se evidencia |
| --- | --- |
| API Gateway para endpoints | Rutas creadas y llamadas desde Postman a API Gateway |
| IDaaS JWT | Azure AD B2C emite token en Postman |
| API Gateway integrado con IDaaS | Rutas con `JWT Auth` y prueba `401` sin token |
| Endpoints Spring Boot | Crear, consultar, actualizar, descargar, subir S3 y eliminar |
| S3 | Bucket y objetos PDF cambiando en vivo |
| Docker | Contenedor corriendo en EC2, imagen en Docker Hub |
| Video | Demostracion completa en Teams, 5 a 10 minutos |

## 14. Troubleshooting rapido

### 401 en API Gateway

Revisar:

- `Issuer URL` debe coincidir exactamente con el `iss` del token.
- `Audience` debe coincidir con `aud` o `client_id`.
- El token no debe estar vencido.
- Si configuraste scopes en la ruta, el token debe traer `scp` o `scope`.

### 403 en Spring Boot

El JWT es valido, pero no trae permisos aceptados. Revisar:

- Para descarga: `guides.download`, `GUIDES_DOWNLOAD` o `guides.manage`.
- Para administracion: `guides.manage` o `GUIDES_MANAGE`.

### API Gateway 502

Revisar:

- EC2 encendido.
- Security Group permite puerto `8080`.
- Contenedor corriendo.
- Integration URL apunta a `http://<EC2_PUBLIC_DNS>:8080/...`.

### Error S3

Revisar:

- `AWS_S3_BUCKET` correcto.
- Region `us-east-1`.
- Credenciales AWS Academy renovadas.
- `AWS_SESSION_TOKEN` actualizado.

### GitHub Actions falla al conectar SSH

Revisar:

- `EC2_HOST`.
- `EC2_USER`.
- `EC2_SSH_KEY` completo, incluyendo `BEGIN` y `END`.
- Security Group permite SSH desde GitHub Actions. Si no se puede restringir por IP, usar `0.0.0.0/0` solo durante la entrega y luego cerrarlo.

## 15. Fuentes oficiales revisadas

- AWS API Gateway JWT authorizers: https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-jwt-authorizer.html
- AWS API Gateway troubleshooting JWT: https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-troubleshooting-jwt.html
- AWS API Gateway HTTP API routes: https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-develop-routes.html
- Microsoft Azure AD B2C updates: https://learn.microsoft.com/en-us/azure/active-directory-b2c/whats-new-docs
- Microsoft Azure AD B2C app registration and scopes: https://learn.microsoft.com/en-us/azure/active-directory-b2c/configure-authentication-sample-web-app-with-api
- Microsoft Azure AD B2C client credentials: https://learn.microsoft.com/en-us/azure/active-directory-b2c/client-credentials-grant-flow
- GitHub Actions secrets: https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets
- GitHub Actions Docker images: https://docs.github.com/en/actions/tutorials/publish-packages/publish-docker-images
