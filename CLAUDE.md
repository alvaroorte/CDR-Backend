# CLAUDE.md — cdr-backend

Guía para Claude Code al trabajar en este repositorio. Sistema de atención y gestión de pedidos
presenciales de una cafetería. **Replica la arquitectura de `../ticket-backend`** (proyecto de
referencia, **solo lectura — nunca modificarlo**).

> **Estado:** Etapas 1 (infraestructura + autenticación) y 2 (administración: usuarios, roles/permisos,
> categorías, productos, adicionales, números de mesa, medios de pago, parámetros) completadas.
> Nombre comercial: **Ciudad de Refugio**.
>
> **Endpoints de la Etapa 2:** `/category` (+`/select`), `/product` (+`PATCH /{id}/availability`, único
> permitido al CAJERO junto a `GET /product`), `/addon` (+`/select`), `/table-marker`, `/payment-method`,
> `/parameter` (solo GET y PUT del valor), `/users` (+`PATCH /{id}/password`), `/roles` (+`/select`,
> `GET|PUT /{id}/permissions`), `/permission` (solo lectura). `DELETE` = desactivar (borrado lógico).
>
> **Etapa 3 (caja + POS + pedidos):** `V5__orders_and_cash.sql`; módulos `appcash` (caja, movimientos,
> pagos) y `apporder` (pedido, detalle con fotografías, historial, `OrderPricingService`).
> Endpoints: `GET /cash-session/current` (204 sin caja), `POST /cash-session/open`, `POST /cash-session/movement`,
> `GET /pos/catalog`, `GET /table-marker/availability`, `POST /order/quote`, `POST /order`, `GET /order/{id}`
> (todos permitidos al CAJERO por V5). Código de pedido **diario** (`order_daily_sequences`, UPSERT atómico);
> total 0 rechazado; adorno ocupado → 409 (pre-chequeo + índice `ux_orders_marker_open`).
>
> **Etapa 4 (tiempo real + estados + cancelación + historial):** `V6__kitchen_and_order_tracking.sql`;
> `config/WebSocketConfig` + `WebSocketAuthInterceptor` (JWT en CONNECT, rol en SUBSCRIBE, SEND rechazado);
> `OrderStateMachine` (con retroceso de un paso para COCINA/ADMIN); eventos `@TransactionalEventListener(AFTER_COMMIT)`
> a `/topic/orders` y `/topic/catalog`. Endpoints: `GET /order/board`, `GET /order/kitchen-settings`,
> `PATCH /order/{id}/status` y `POST /order/{id}/cancel` (ambos con `expectedVersion` → 409), `GET /order/pageable`.
> `hibernate.default_batch_fetch_size=50` para el detalle del tablero.
> Las etapas se ejecutan **solo con aprobación explícita** del usuario (ver `docs/plans/001-arquitectura-inicial.md` §8).

## Documentación obligatoria

| Documento | Cuándo leerlo |
|---|---|
| `docs/architecture/backend-architecture.md` | Antes de crear cualquier clase |
| `docs/architecture/coding-conventions.md` | Nombres, Lombok, sufijos, mensajes |
| `docs/architecture/database-conventions.md` | Al crear/alterar tablas (Flyway) |
| `docs/architecture/security.md` | Endpoints nuevos, roles, permisos, WebSocket |
| `docs/architecture/realtime.md` | Eventos y topics STOMP |
| `docs/plans/001-arquitectura-inicial.md` | Modelo de datos, endpoints y etapas aprobadas |
| Skill `cdr-order-management` (raíz CDR-GPF) | **Cualquier** cosa de pedidos, dinero, descuentos, pagos, caja, cocina |
| Skill `backend-crud` | Crear un CRUD completo (entity → controller → test) |
| Skill `backend-entity` | Crear/modificar una entidad + su migración Flyway |

## Stack (igual que la referencia salvo lo marcado)

- Java 21 · Spring Boot 4.0.3 · Gradle 9.3.1 (wrapper) · PostgreSQL · HikariCP
- Spring Web, Data JPA, Validation, Security, WebSocket (STOMP + SockJS), Cache
- JJWT 0.13 · Caffeine (rate limit login) · Lombok · springdoc-openapi
- **Jackson 3**: imports `tools.jackson.*` (anotaciones siguen en `com.fasterxml.jackson.annotation.*`)
- **NUEVO vs referencia:** Flyway (`flyway-core` + `flyway-database-postgresql`), `ddl-auto=validate`
- **Quitado vs referencia:** LDAP, reCAPTCHA, licencia, mail/Thymeleaf, Firebase, Jasper/POI, ModelMapper

Paquete base: `bo.cdr.app`. Context path `/cdr`, puerto `8086`.
Zona horaria `America/La_Paz`.

## Comandos

```powershell
.\gradlew.bat build
.\gradlew.bat bootRun                    # perfil dev por defecto → http://localhost:8086/cdr
.\gradlew.bat test                       # BUILD SUCCESSFUL obligatorio antes de commitear
.\gradlew.bat test --tests "bo.cdr.app.appsecurity.presentation.controller.AuthControllerTest"
```

**Dev local:** PostgreSQL en `localhost:5432`, base `cdr_db` (dedicada a este proyecto). Los valores
por defecto de `application-dev.properties` (BD, JWT, admin inicial) son **solo para desarrollo**;
qa/prod exigen las variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `APP_JWT_SECRET`,
`APP_CORS_ALLOWED_ORIGINS` y, en el primer arranque, `APP_ADMIN_INITIAL_PASSWORD`.
`AdminBootstrap` crea el usuario `admin` (rol ADMIN) si no existe ningún ADMIN activo.

**Seguridad (Etapa 1):** login por **username** (no email), contraseña en base64 desde el frontend,
JWT 12 h + refresh 7 días con rotación, `GET /users/me`. `open-in-view=false`: las relaciones LAZY
que se usen fuera del repositorio deben traerse con `JOIN FETCH` (ver `UserRoleRepository.findByUser`).

## Arquitectura en una línea

Módulos de dominio (`appsecurity`, `appcatalog`, `apporder`, `appcash`, `appreport`) sobre una base
genérica (`appbase`), cada uno con **la misma** estructura (la referencia era inconsistente; aquí no):

```
<modulo>/
  data/                               # *Repository extends GenericRepository<T,Integer>
  domain/entity/                      # @Entity extends Base
  domain/mapper/                      # *Mapper @Component manual (fromDto)
  domain/service/interfaces/          # *Service extends CRUDService<T,Integer>
  domain/service/implement/           # *ServiceImpl extends CRUDServiceImpl implements *Service
  presentation/controller/            # @RestController
  presentation/request/dto/           # *Dto (entrada, Jakarta Validation)
  presentation/request/filter/        # *Filter (@ModelAttribute)
  presentation/response/pojo/         # *Pojo (salida, constructor para JPQL)
```

Transversales: `config/`, `common/` (enums, validators, `MoneyUtil`, `SpanishEntityNameProvider`),
`exception/` (excepciones tipadas + `ErrorHandler`), `util/` (`CORS`, `RequestMatcherUtil`,
`PermissionSeeder`, `SortUtil`, `constants/`).

## Reglas que no se negocian

1. **Seguir la referencia.** Ante la duda, abrir el equivalente en `../ticket-backend` (p. ej.
   `appticket/presentation/controller/HolidayController.java`) y replicarlo. No introducir patrones,
   librerías ni capas nuevas sin aprobación (no MapStruct, no `@PreAuthorize`, no envoltorio de respuesta).
2. **Sin envoltorio de respuesta:** `ResponseEntity<T>` directo. POST 201 · GET/PUT/PATCH 200 · DELETE 204.
   Errores con `ErrorResponse{code,status,message,path}` vía `ErrorHandler`.
3. **Mensajes al usuario en español**, código en inglés.
4. **Dinero:** `BigDecimal` + `NUMERIC(12,2)` + `MoneyUtil` (HALF_UP, escala 2). Nunca `double`.
5. **Esquema solo por Flyway** (`src/main/resources/db/migration/V{n}__{descripcion}.sql`). Nunca `ddl-auto=update`.
6. **Transacciones:** `@Transactional(rollbackFor = Exception.class)` en todo método de escritura de
   servicio; `@Transactional(readOnly = true)` en lecturas complejas.
7. **Bloqueo optimista:** `@Version` en `Order`, `Product`, `CashSession`. `ObjectOptimisticLockingFailureException` → 409.
8. **Enums** siempre `@Enumerated(EnumType.STRING)`.
9. **Nada histórico se borra físicamente** (pedidos, pagos, caja, historial). Catálogos: borrado lógico `active=false`.
10. **Autorización en backend siempre**: `AuthorizationFilter` (permiso por URL) + reglas finas en el
    servicio con `JwtService.getCurrentAuthorities()` (p. ej. límite de descuento del CAJERO).
11. **Secretos solo por variables de entorno** (`${DB_PASSWORD}`, `${APP_JWT_SECRET}`...). Nunca en properties.
12. **Test unitario obligatorio por cambio de comportamiento** (mismo commit), igual que la referencia:
    nuevo endpoint → `*ControllerTest` (happy path + ≥1 error); nuevo método de servicio → test con mocks;
    bug → test que lo reproduce. Excepciones: cambios estructurales, config, docs, entidades/DTOs sin lógica.
13. **Rutas públicas** solo en `RequestMatcherUtil.TOKEN_NOT_REQUIRED`; solo-autenticadas en `PERMISSION_NOT_REQUIRED`.
14. Nuevas entidades → registrar nombre en español en `SpanishEntityNameProvider.NAME_MAP`.
15. Planes de cambios grandes en `docs/plans/NNN-titulo-kebab.md`.

## Desviaciones documentadas respecto a la referencia

| Referencia | cdr-backend | Motivo |
|---|---|---|
| `ddl-auto=update`, sin migraciones | Flyway + `validate` | Integridad de datos históricos y monetarios |
| Sin `@Version` | `@Version` en entidades concurrentes | Varios usuarios sobre el mismo pedido |
| Enums ORDINAL por defecto | Siempre `STRING` | Robustez ante reordenamientos |
| WebSocket público | JWT en STOMP CONNECT + autorización de SUBSCRIBE | Seguridad |
| `data/` vs `repository/`, `Interfaces/` | Una sola estructura | Consistencia |
| `delete` genérico físico | Lógico en catálogo, prohibido en histórico | Trazabilidad |
| Secretos en properties | Variables de entorno | Seguridad |
| LDAP / captcha / licencia | No se incluyen | No aplican a la cafetería |
| `CLAUDE.md` desactualizado (ModelMapper, Criteria API) | Fuente de verdad = código de referencia | Ver `../docs/00-analisis-proyectos-referencia.md` |
| Login por email (usuarios corporativos) | Login por `username`; email opcional | Personal de cafetería sin correo corporativo |
| JSON mal formado → 500 genérico | `HttpMessageNotReadableException` → 400 | Bug de la referencia detectado por test |
| `DataIntegrityViolationException` → 400 | → 409 (+ `ObjectOptimisticLockingFailureException` → 409) | Conflictos de concurrencia/unicidad |
| Paquetes `appsecurity/repository` | `appsecurity/data` | Estructura única |
| `open-in-view` activo (por defecto) | `spring.jpa.open-in-view=false` + `JOIN FETCH` | Evita consultas perezosas ocultas en la vista |
| Servicios de `appsecurity` planos | `domain/service/interfaces` + `implement` como el resto | Estructura única (corregido en la Etapa 2) |
| Un rol inactivo seguía otorgándose | Roles inactivos no se incluyen en el JWT ni en `/users/me` | Desactivar un rol debe quitar el acceso |
| Parámetros con CRUD completo | Solo se edita el valor (validado por `value_type`); los define una migración | El código depende de sus códigos |
| Sin conflicto de negocio tipado | `ConflictException` → 409 con mensaje específico (adorno en uso, caja ya abierta) | El usuario puede resolverlo eligiendo otra opción |
| — | `domain/model/` para objetos de dominio internos sin tabla (`PricedOrder`, `ResolvedPayment`) | Resultado del cálculo / validación que no es entidad ni DTO |
| `@Pojo` en todas las respuestas | Las respuestas `record` (p. ej. `PosCatalogPojo`, `CashMethodTotalPojo`) **no** llevan `@Pojo` | `@Pojo` es un `@Component`: Spring no puede instanciar un record sin constructor vacío |
| — | `Payment.orderId` / `CashMovement.orderId` como columna simple (FK en BD, sin relación JPA) | Evita acoplar `appcash` con la entidad `Order` |
| — | Superar el tope de descuento del CAJERO → **400** (`NotAllowedException`), no 403 | El 403 del frontend significa "sin permiso de ruta"; aquí el mensaje explica el tope |
| `WebSocketConfig.setAllowedOriginPatterns(String)` | Se separa la propiedad por comas | Permite varios orígenes en qa/prod |
| — | `expectedVersion` distinto del actual → `ConflictException` (409) antes de tocar nada; `@Version` cubre la carrera restante | Mensaje claro con el código del pedido |
