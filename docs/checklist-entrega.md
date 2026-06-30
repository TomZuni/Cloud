# Checklist de entrega Sumativa 2

Usar esta lista antes de grabar y antes de subir el ZIP/RAR al AVA.

## Backend

- [ ] El repositorio GitHub contiene el codigo completo.
- [ ] `mvn -B test` pasa correctamente.
- [ ] `mvn -B package` genera el jar.
- [ ] El perfil de despliegue es `online,dispatch-guides`.
- [ ] `JWT_VALIDATION_ENABLED=true` en EC2.

## GitHub Actions

- [ ] Secrets de Docker Hub configurados.
- [ ] Secrets de EC2 configurados.
- [ ] Secrets de AWS Academy configurados.
- [ ] Secrets de Azure AD B2C configurados.
- [ ] Workflow `Build and Deploy` termina en verde.
- [ ] Imagen publicada en Docker Hub.
- [ ] Contenedor `course-enrollment-api` corre en EC2.

## AWS

- [ ] EC2 encendido.
- [ ] Security Group permite `8080` para API Gateway.
- [ ] S3 bucket creado en `us-east-1`.
- [ ] Bucket S3 no es publico.
- [ ] API Gateway HTTP API creado.
- [ ] Stage `prod` con auto-deploy.
- [ ] Todas las rutas estan creadas.
- [ ] Todas las rutas apuntan al EC2.
- [ ] Todas las rutas tienen `JWT Auth`.

## Azure AD B2C / IDaaS

- [ ] Tenant B2C o tenant de clientes creado.
- [ ] User flow `B2C_1_susi` creado.
- [ ] App registration de API creada.
- [ ] API expuesta con scopes `guides.download` y `guides.manage`.
- [ ] Roles `GUIDES_DOWNLOAD` y `GUIDES_MANAGE` creados si el docente los exige como roles.
- [ ] App registration para Postman creada.
- [ ] Secret de Postman creado.
- [ ] Postman puede obtener JWT.

## Postman

- [ ] `base_url` apunta a API Gateway, no a localhost ni EC2.
- [ ] `access_token` contiene JWT valido.
- [ ] Crear guia responde `201`.
- [ ] Descargar guia temporal responde PDF.
- [ ] Subir guia a S3 responde `201`.
- [ ] S3 muestra el PDF creado.
- [ ] Descargar guia desde S3 funciona con `accessCode`.
- [ ] Consultar por transportista y fecha funciona.
- [ ] Actualizar guia funciona.
- [ ] Eliminar guia responde `204`.
- [ ] Sin JWT responde `401`.
- [ ] JWT alterado responde `401`.

## Word de evidencias

- [ ] Captura de GitHub repo.
- [ ] Captura de workflow exitoso.
- [ ] Captura de Docker Hub.
- [ ] Captura de EC2 con Docker.
- [ ] Captura de S3 antes y despues.
- [ ] Captura de Azure AD B2C tenant.
- [ ] Captura de app API, scopes/roles y secret.
- [ ] Captura de API Gateway Integrations.
- [ ] Captura de API Gateway Authorization.
- [ ] Capturas de Postman.

## Video

- [ ] Duracion entre 5 y 10 minutos.
- [ ] Se prueba todo en directo.
- [ ] Se usa solo API Gateway.
- [ ] Se muestra S3 cambiando.
- [ ] Se muestra obtencion de JWT.
- [ ] Se muestra caso negativo `401`.
- [ ] Se explica como cada parte cumple la pauta.
