# oficioya-service

## Requerimientos Funcionales - Dominio Service

| Campo | Descripción|
|---|---|
| **Proyecto** | OficioYa |
| **Squad** | 4 |
| **Dominio** | Service |
| **Product Owner** | Juan Jose Rivera |
| **Analista Funcional** | Santiago Gomez |
| **Lider Técnico** | Juan Esteban Laverde |
| **Equipo de Desarrollo** | Brian Fierro, Diego Mesa |

## Diagrama entidad-relación

![ER Service](/src/main/resources/docs/uml/ER-Service.png)

## Diagrama de clases

![Diagrama de clases Service](/src/main/resources/docs/uml/DiagramaClases-Service.png)

## URLs de producción

- Service: http://ofioya-serviceapi-achydycpa3fgbsch.chilecentral-01.azurewebsites.net/
- Notification: http://oficioya-notificationapi-apffc2eygzgcbghp.brazilsouth-01.azurewebsites.net/
- Health: http://ofioya-serviceapi-achydycpa3fgbsch.chilecentral-01.azurewebsites.net/api/v1/service/health
- OpenAPI: http://ofioya-serviceapi-achydycpa3fgbsch.chilecentral-01.azurewebsites.net/v3/api-docs
- Swagger UI: http://ofioya-serviceapi-achydycpa3fgbsch.chilecentral-01.azurewebsites.net/swagger-ui/index.html

## Cómo ejecutar

1. Levantar el back con perfil `dev`. El puerto se define con la variable `SERVER_PORT` (por defecto 8086 en `application.properties`).
2. El front se ejecuta en `http://localhost:5173` y se conecta por proxy a `/api`.
3. Repositorio del front: https://github.com/juanjorivlo/DOSW_Lab9_Front_FLRGM

## Autenticación

Los endpoints, salvo `/auth/**`, `/v3/api-docs` y Swagger UI, exigen un JWT en el header `Authorization: Bearer <token>`. El servicio lee el rol del claim `roles` (lista) o, si no viene, del claim `role` (texto). El id del usuario viaja en el claim `sub` (UUID).

## Endpoints

La lista completa de endpoints, con sus roles y contratos, está en el Swagger UI (prefijo `/api/v1`). El Swagger es la fuente de referencia.

### Crear solicitud

`POST /requests` recibe `workersId` (lista de UUID, al menos uno), `description`, `neighborhood`, `exactAddress`, `scheduledDateTime` e `images` (opcional). Los cinco primeros son obligatorios y, si faltan o están vacíos, el servicio responde 400. Crea una solicitud por cada trabajador válido, todas con el mismo `rootId`, y responde `rootId`, `workersId`, `description`, `scheduledDateTime` y `sent` (cantidad de solicitudes creadas).

## Integración con otros microservicios

- **Notification:** `HttpNotificationService` envía eventos (creación, aceptación, cancelación, completado) a `POST /api/v1/notification`.
- **Reputation:** penalización de -0.5 al cancelar con menos de 24h (placeholder).
- **Worker:** reserva/liberación de franja horaria (placeholder).
- **Search:** contrato OpenAPI disponible en `/v3/api-docs`.
- **User/Identity:** validación de existencia de usuario por UUID.

## Pruebas

- Ejecutar: `mvn clean verify`
- Cobertura mínima: 80% (regla de JaCoCo configurada en el `pom.xml`)
- Perfil de prueba: H2 en memoria (`application-test.properties`)

## Mock Up

### Link
[Link del figma](https://www.figma.com/make/wy8eVjfR1SbrWb3wUqMIkM/Sistema-de-calificaci%C3%B3n-5-estrellas?t=FlzxIc07XaISpXqc-20&fullscreen=1)

### Fotos
![1. Rellenar datos](/src/main/resources/docs/images/1.Rellenar%20datos.png)
![1. Escoger trabajadores](/src/main/resources/docs/images/1.Escoger%20trabajadores.png)
![1. Confirmar datos](/src/main/resources/docs/images/1.Confirmar%20datos.png)
![1. Solicitud enviada](/src/main/resources/docs/images/1.Solicitud%20enviada.png)
![2. Rellenar datos](/src/main/resources/docs/images/2.Rellenar%20datos.png)
![2. Confirmar datos](/src/main/resources/docs/images/2.Confirmar%20datos.png)
![2. Solicitud enviada](/src/main/resources/docs/images/2.Solicitud%20enviada.png)