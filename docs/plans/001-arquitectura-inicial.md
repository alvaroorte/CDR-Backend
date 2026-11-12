# Plan: Sistema de Gestión de Pedidos para Cafetería (cdr-backend + cdr-frontend)

## Contexto

El objetivo es un nuevo sistema de pedidos presenciales para una cafetería. Debe replicar la arquitectura, las convenciones y la identidad visual de dos proyectos de referencia, que **no se modifican**:
- `ticket-backend`: Spring Boot 4.0.3, Java 21, PostgreSQL y Gradle 9.3.1.
- `ticket-frontend`: Angular 21 standalone, PrimeNG 21 (Lara) y PrimeFlex.

Esta primera etapa es solo de análisis y planificación. Después de aprobar el plan se ejecuta la **Etapa 0**: crear la documentación y las Skills, sin código. Al terminarla se vuelve a pedir aprobación antes de la Etapa 1. **No se avanza de etapa sin autorización explícita.**

### Decisiones ya aprobadas

| Tema | Decisión |
|---|---|
| Ubicación | `CDR-GPF/cdr-backend` y `CDR-GPF/cdr-frontend`, hermanos de los proyectos de referencia |
| Migraciones | **Flyway** + `ddl-auto=validate` (se aparta de la referencia; queda documentado) |
| Seguridad | Mismo JWT + refresh token y el mismo RBAC en BD (`PermissionSeeder` + `AuthorizationFilter`). **Sin** LDAP, reCAPTCHA ni licencia. Login solo con BCrypt |
| WebSocket | STOMP/SockJS como en la referencia, con un **`ChannelInterceptor` que valida el JWT en CONNECT** y autoriza cada SUBSCRIBE por rol |
| Cobro | **Siempre al registrar el pedido**: el pedido y sus pagos se graban en la misma transacción |
| Pagos | **Varios pagos por pedido** (1:N). **No se guardan el vuelto ni el importe recibido**: la pantalla calcula el vuelto solo como ayuda y la BD guarda el importe aplicado de cada pago |
| Mesas | **Adornos numerados** (`TableMarker`): al registrar un pedido en mesa, el cajero elige el número del adorno que entrega al cliente, y el cliente lo coloca en su mesa. El número está ocupado mientras el pedido esté PENDIENTE, EN_PREPARACION o LISTO, y se libera automáticamente al pasar a ENTREGADO o CANCELADO. Ocupación derivada, sin estado manual |
| Para llevar | Se identifica solo por el código del pedido (#015), con el nombre del cliente opcional |
| Skill angular-developer | Se **instala** en cdr-frontend y se tiene en cuenta. En caso de conflicto se aplican estas reglas: **Reactive Forms tipados** (no Signal Forms), **Vitest** (no Karma), patrón de referencia en los CRUD, y `computed`/`linkedSignal`/`httpResource` en pantallas nuevas (POS, carrito, cocina) cuando simplifiquen. Los `EventEmitter` de servicios se sustituyen por signals/`output()` |
| Dispositivos | Escritorio y tablet; los teléfonos se bloquean (patrón `mobile-app-required`); misma barra superior |
| Cancelación | CAJERO solo en estado PENDIENTE; ADMIN en cualquier estado salvo ENTREGADO. Motivo obligatorio, egreso de devolución en caja y pagos marcados como REEMBOLSADO |
| Adicionales | Catálogo global asignable a productos (N:M); el precio queda como fotografía en el pedido |
| Imágenes | **Base64 en la BD**, con el patrón `@IsImageBase64` / `ImageBase64Util` de la referencia |
| Caja | Una **sesión de caja por cajero**; cada pago queda ligado a la sesión abierta del usuario |

---

## 1. Resumen del análisis de los proyectos de referencia

### 1.1 Backend (`ticket-backend`): patrones que se conservan

- **Estructura modular DDD por capas**, una carpeta por dominio:
  - `domain/entity`, `domain/mapper`, `domain/service/interfaces`, `domain/service/implement`
  - `data` (repositorios)
  - `presentation/controller`, `presentation/request/dto`, `presentation/request/filter`, `presentation/response/pojo`
- **Base genérica** (`appbase/`), que se copia adaptada:
  - `Base` (`@MappedSuperclass` con `active`, `createdBy`, `updatedBy`, `createdAt`, `updatedAt`), alimentada por `AuditorConfig`
  - `GenericRepository<T,ID>`
  - `CRUDService` / `CRUDServiceImpl`, que incluye `checkIfElementsExist` y `hasDependencies()`
  - `PagePojo` y `PagePojoMapper`
- **Mappers manuales** como `@Component` (`HolidayMapper.fromDto(dto, found, ...relaciones)`). No se usa MapStruct.
- **Lecturas**: proyecciones JPQL con constructor (`SELECT new ...Pojo(...)`); SQL nativo con CTE solo en reportes.
- **DTOs**:
  - Entrada `XxxDto` con Jakarta Validation y mensajes en `validation-messages.properties`.
  - Deserializadores estrictos `StringOnly`, `IntegerOnly` e `IntegerList`.
  - Validadores propios como `@ValidEnum` e `@IsImageBase64`.
- **Respuestas**:
  - `ResponseEntity<T>` directo, sin envoltorio; POST → 201, GET/PUT → 200, DELETE → 204.
  - Paginación con `page`, `size`, `sortBy` y `sortOrder`, más `@ModelAttribute XxxFilter` y una lista blanca de campos ordenables.
- **Errores**:
  - `ErrorHandler` (`@ControllerAdvice`) junto con unas 30 excepciones tipadas.
  - Formatos `ErrorResponse{code,status,message,path}` y `ValidationErrorResponse`.
  - Mensajes en español, con `SpanishEntityNameProvider` y `notification-messages.properties`.
- **Seguridad**:
  - Filtros `JwtAuthenticationFilter` → `AuthorizationFilter`; las rutas públicas viven en `RequestMatcherUtil`.
  - El usuario actual se obtiene con `JwtService.getCurrentUser()` / `getCurrentUserId()` / `getCurrentAuthorities()`.
  - Refresh token opaco con hash SHA-256, en `security.refresh_token`.
  - CORS mediante un filtro propio; rate limit del login con Caffeine.
- **Tiempo real**: `WebSocketConfig` (STOMP, `/topic`, SockJS), envío con `SimpMessagingTemplate` y `NotificationService`.
- **Convenciones**:
  - Código en inglés; mensajes, comentarios y documentación en español.
  - IDs `Integer IDENTITY`; tablas en snake_case y en plural; esquema `security` para las tablas de seguridad.
  - Planes en `docs/plans/NNN-titulo.md`; zona horaria `America/La_Paz`.
- **Tests**:
  - JUnit 5 + Mockito.
  - `MockMvcTestSupport` en modo standalone, con nombres del estilo `metodo_returnsXXX_whenCondicion`.
  - Regla: un test por cada cambio de comportamiento.

### 1.2 Backend: problemas de la referencia que se corrigen (documentados como desviación)

| Problema | Corrección |
|---|---|
| Inconsistencias de carpetas (`data/`, `repository/`, `Interfaces/`) | Una sola forma en todos los módulos: `data/` + `interfaces/implement/` |
| Esquema creado por `ddl-auto=update` | Flyway |
| No existe `@Version` | `@Version` en `Order`, `Product` y `CashSession` (bloqueo optimista) |
| Enums guardados como ORDINAL | Siempre `@Enumerated(EnumType.STRING)` |
| WebSocket sin autenticación | JWT en CONNECT + autorización de SUBSCRIBE |
| Secretos en `application-*.properties` | Solo `${ENV}` |
| `delete` genérico físico | Borrado lógico (`active=false`) en catálogos con historial; nada se borra en pedidos, pagos ni caja |
| `@Transactional` disperso | `@Transactional(rollbackFor = Exception.class)` en toda escritura, `readOnly` en lecturas |
| `CLAUDE.md` desactualizado | Se toma el código como fuente de verdad (ModelMapper casi no se usa, sí se usa `@Query`, `PagePojo` incluye `last`/`totalPages`) |

### 1.3 Frontend (`ticket-frontend`): patrones que se conservan

- **Rutas**: Angular 21 standalone; cada ruta usa `loadComponent` con `canActivate: [authGuard]`; el mapa de roles por ruta está en `core/constants/header-links.ts` (`urlsWithPermissions`).
- **Estructura**:
  - `core/`: constants, enums, guards, interceptors, interfaces, layout, models, pipes, services, utils y validators.
  - `modules/<feature>/`: componente, `services/` y `components/modal-form/`.
  - `shared/components/`.
- **Servicios**:
  - Cada servicio inyecta `HttpClient` y construye la URL con `environment.server_url`; **no** se usa `curd.service.ts`.
  - Cada servicio implementa `IServiceCommon` (`eventFormComponent`, `eventTableComponent`, `eventModalDeleteComponent`, `delete`).
- **Componentes compartidos**: `GenericTable`, `TableOnDemand`, `Toolbar`, `ModalDelete`, `MessageRequiredField`, `Loader`, `InputFile`, `Stepper` y `TableColumnDefinitions`.
- **Formularios**:
  - Reactive Forms sin tipar (`FormBuilder.group`), con `LIMITS`, `regex` y `noSpaceValidator`.
  - Plantilla con `p-floatlabel` + `small.p-error`.
  - Modal `p-dialog` con botones redondeados.
- **Estado**: signals (`signal`, `computed`, `input`, `output`, `viewChild`) y `EventEmitter` en servicios para abrir modales; sin NgRx.
- **Autenticación**:
  - `HeaderInterceptor`: Bearer, refresh compartido con `shareReplay` y reintento.
  - `LoaderInterceptor`.
  - `HttpErrorHandlerService`, que muestra toasts.
  - `LoginService`, con signals y `haveSomeRole()`.
- **Estilos**:
  - Preset PrimeNG Lara y PrimeFlex.
  - Temas `styles-light.scss` / `styles-dark.scss`, intercambiados en `#theme-main`, con la clase `.ticket-mode-dark`.
  - Paleta teal (`--p-teal-800/600/500`) + grises "Lynch" (`#f6f7f9` … `#23272e`), con acentos `#d1a000`, `#ebbc05` y `#243070`.
  - `html{font-size:13px}`, `.sombra-black`, `p-fieldset` con efecto blur, cabecera de tabla teal-600, diálogos con radio 15px y clases `hover-*`.
- **Layout**: barra superior sticky con efecto glass, menú "Configuraciones", campana de notificaciones, interruptor de tema y footer fijo.
- **Tiempo real**: `MessageWsService` (`@stomp/stompjs` + `sockjs-client`, `reconnectDelay: 3000`).
- **Textos**: constantes `LABELS`, `MESSAGES`, `TOOLTIPS` y `PLACEHOLDERS`.
- **Tests**: Karma/Jasmine con `TestBed.runInInjectionContext` y stubs `jasmine.createSpy`.
- **Skills existentes**:
  - `ticket-frontend/.claude/skills/angular-crud` ya está escrita para CDR-GPF. Se **reutiliza adaptada**: formularios tipados, Vitest y signals en lugar de `EventEmitter`.
  - `angular-developer` (Google, MIT, registrada en `skills-lock.json`) se **instala** en cdr-frontend como guía general de Angular 21.

### 1.4 Frontend: lo que no se copia

- Código muerto: `curd.service.ts`, `http.service.ts`, ngx-translate, `jwt-decode` y highcharts (salvo que los reportes lo necesiten).
- Alias muertos y erratas (`nout-found`, `TableOnDemnad`, `*.inteface..ts`, `Calcelar`).
- Karma/Jasmine, que se sustituye por Vitest, y los formularios sin tipar.
- Inconsistencias de nombres: modelos siempre en kebab-case; interfaces siempre con prefijo `I`.

---

## 2. Etapa 0 (después de aprobar): documentación y Skills

El mecanismo es el de Claude Code: `CLAUDE.md` por proyecto, `.claude/skills/<nombre>/SKILL.md` (+ `references/`) y documentación en `docs/`. Se adapta la estructura `.ai/` propuesta en el brief.

```
CDR-GPF/
├── docs/
│   └── 00-analisis-proyectos-referencia.md   # §1 completo: conservar / mejorar / nuevo
├── .claude/skills/
│   └── cdr-order-management/SKILL.md        # dominio: dinero, descuentos, estados, snapshots, eventos WS
├── cdr-backend/
│   ├── CLAUDE.md                              # comandos, arquitectura, reglas (incluido "test por cambio")
│   ├── docs/architecture/
│   │   ├── backend-architecture.md            # módulos, capas, flujo de una petición, ejemplos reales
│   │   ├── coding-conventions.md              # nombres, Lombok, sufijos, mensajes en español
│   │   ├── database-conventions.md            # Flyway, NUMERIC, snake_case, índices, borrado lógico
│   │   ├── security.md                        # JWT, RBAC, PermissionSeeder, WS auth
│   │   └── realtime.md                        # topics, eventos, dedup, reconexión
│   ├── docs/plans/001-arquitectura-inicial.md # §3–§8 de este plan
│   └── .claude/skills/
│       ├── backend-crud/SKILL.md (+references/templates.md)  # Entity→Repo→Mapper→Service→Controller→Test
│       └── backend-entity/SKILL.md                            # entidad + migración Flyway + dependencias
└── cdr-frontend/
    ├── CLAUDE.md
    ├── docs/{frontend-architecture.md, design-system.md}      # paleta, tipografía, componentes, layouts POS/cocina
    └── .claude/skills/
        ├── angular-developer/ (copiada de ticket-frontend/.claude/skills; + skills-lock.json)
        ├── angular-crud/ (copiada de ticket-frontend y ajustada: forms tipados, Vitest, signals)
        ├── frontend-component/SKILL.md        # componente standalone, signals, PrimeModules, estilos
        └── realtime-ws/SKILL.md               # cliente STOMP con JWT, resync y dedup por versión
```

Cada skill incluye fragmentos reales de la referencia como plantilla, por ejemplo `HolidayController`/`HolidayServiceImpl`/`HolidayMapper` en el backend y `modules/schedule` en el frontend.

El `CLAUDE.md` del frontend define la **precedencia entre skills**: `angular-crud` y los patrones del proyecto mandan sobre las recomendaciones genéricas de `angular-developer`. Las cuatro reglas de conflicto de la tabla de decisiones quedan escritas de forma explícita.

---

## 3. Arquitectura propuesta

### 3.1 Backend `cdr-backend`

- **Paquete base**: `bo.cdr.app`, que imita `esyc.ticket.app` (decisión pendiente).
- **Context path**: `/cdr`; puerto 8086.

```
bo.cdr.app
├─ appbase/       Base, GenericRepository, CRUDService(Impl), PagePojo(+Mapper), MessagePojo
├─ appsecurity/   auth (JwtService, filtros, SecurityConfig), user/role/permission/refresh_token, AuthController, UserController…
├─ appcatalog/    Category, Product, Addon, ProductAddon, TableMarker, PaymentMethod, Parameter
├─ apporder/      Order, OrderItem, OrderItemAddon, OrderStatusHistory, OrderPricingService, OrderStateMachine, OrderEventPublisher
├─ appcash/       CashSession, CashSessionSummary, CashMovement, Payment
├─ appreport/     consultas de reportes (proyecciones / SQL nativo), exportación
├─ config/        AuditorConfig, WebSocketConfig, WebSocketAuthInterceptor, ClockConfig, deserializadores
├─ common/        enums, validators (@ValidEnum, @IsImageBase64), SpanishEntityNameProvider, MoneyUtil
├─ exception/     excepciones tipadas + ErrorHandler (+ ConcurrentModificationException → 409)
└─ util/          CORS, RequestMatcherUtil, PermissionSeeder, SortUtil, constants/(RoleCodes, Parameters…)
```

- Cada módulo usa `data/`, `domain/{entity,mapper,service/interfaces,service/implement}` y `presentation/{controller,request/dto,request/filter,response/pojo}`.
- Dependencias: las mismas de la referencia (web, data-jpa, validation, security, websocket, jjwt 0.13, caffeine, lombok, postgresql, springdoc), **más `flyway-core` y `flyway-database-postgresql`**.
- Se quitan: LDAP, mail, thymeleaf, firebase, jasper y POI. La exportación de reportes se decide en la Etapa 6.

### 3.2 Frontend `cdr-frontend`

```
src/app/
├─ core/  constants (LABELS, MESSAGES, header-links con roles), enums (roles, order-status, service-type, discount-type…),
│         guards/auth.guard, interceptors (header, loader), interfaces, models, pipes (currency-bs), services
│         (login, local-storage, helpers, http-error-handler, loader, order-ws.service), utils, validators, layout (header, footer)
├─ modules/
│   ├─ login/  users/  roles/  permissions/            (admin)
│   ├─ categories/ products/ addons/ table-markers/ payment-methods/ parameters/   (CRUDs, patrón schedule)
│   ├─ pos/            # nuevo pedido: category-grid, product-grid, modal-product-config, cart-panel,
│   │                  #  discount-form, service-type-step, payment-step, order-summary  + services/cart.service (signals)
│   ├─ orders/         # seguimiento/historial (table-on-demand + detalle + timeline de estados)
│   ├─ kitchen/        # tablero cocina: columnas PENDIENTE / EN_PREPARACION / LISTO, tarjetas y botones grandes
│   ├─ cash-register/  # abrir caja, movimientos, cerrar caja con arqueo por medio de pago
│   └─ reports/
└─ shared/  generic-table, table-on-demand, toolbar, modal-delete, message-required-field, loader, input-file,
            stepper, mobile-app-required, not-found, order-status-tag, elapsed-time
```

- Las dependencias son un subconjunto de la referencia: angular 21, primeng 21, @primeng/themes, primeflex, primeicons, @stomp/stompjs, sockjs-client, rxjs y exceljs + file-saver si hay exportación.
- Para tests se usa **Vitest**, el runner por defecto de Angular 21, en lugar de Karma/Jasmine. Se mantiene el estilo de los specs de referencia (`runInInjectionContext` + stubs con `vi.fn`).

---

## 4. Modelo de datos

**Reglas generales**
- Todo importe usa `NUMERIC(12,2)` ↔ `BigDecimal`, con escala 2 y `RoundingMode.HALF_UP` centralizado en `MoneyUtil`; los porcentajes usan `NUMERIC(5,2)`; moneda Bs.
- Restricciones `CHECK` en BD: importes ≥ 0, `quantity > 0`, `discount_amount <= subtotal`.
- Las tablas de catálogo extienden `Base` y se borran de forma lógica. Pedidos, pagos y caja no se borran nunca.

**Esquema `security`**
- `user_app(id, username, email, full_name, password_bcrypt, active, …Base)`
- `role(id, code[ADMIN|CAJERO|COCINA], name)`
- `permission(id, http_method, url, url_without_ids, code)`
- `role_permission`, `user_role`
- `refresh_token(id, user_id, token_hash, expires_at, revoked)`

**Catálogo**
- `categories(id, name UQ, description, image_base64, ranking, active, …)`
- `products(id, category_id FK, name, description, image_base64, price, available, active, version, …)`
  - `available=false` marca el producto como agotado temporalmente sin quitarlo del catálogo.
- `addons(id, name UQ, price, active, …)` y `product_addons(product_id, addon_id)` (N:M).
- `table_markers(id, number UQ, description, active, …)`: los adornos numerados físicos que el cajero entrega al cliente.
  - La ocupación no se guarda: un número está "en uso" si tiene un pedido en PENDIENTE, EN_PREPARACION o LISTO.
  - Un índice único parcial en `orders(table_marker_id) WHERE status IN (PENDIENTE, EN_PREPARACION, LISTO)` impide que dos cajeros asignen el mismo número a la vez.
- `payment_methods(id, code UQ[EFECTIVO|TRANSFERENCIA|QR…], name, gives_change, requires_reference, active)`
- `parameters(id, folder, code UQ, value, description, editable, …)`
  - Sigue el patrón `Parameter` de la referencia, pero añade `code` para valores de configuración, por ejemplo `CASHIER_MAX_DISCOUNT_PERCENT=15` y `DISCOUNT_REASON_REQUIRED_ABOVE_PERCENT=10`.

**Pedidos**
- `orders`:
  - Identificación y modalidad: `id`, `code` (número del día, visible para la cocina), `service_type[DINE_IN|TAKEAWAY]`, `table_marker_id` (FK, obligatorio en DINE_IN y nulo en TAKEAWAY), `table_marker_number` (fotografía) y `customer_name` (opcional, para llevar).
  - Estado e importes: `status`, `subtotal`, `discount_type[PERCENTAGE|FIXED_AMOUNT]` (nulo si no hay descuento), `discount_value`, `discount_amount` y `total`.
  - Trazabilidad del descuento: `discount_reason`, `discount_applied_by` y `discount_applied_at`.
  - Cancelación: `cancel_reason`, `cancelled_by` y `cancelled_at`.
  - Caja y control: `cash_session_id`, `version` y los campos de `Base` (createdBy = usuario que registra).
- `order_items(id, order_id, product_id, product_name, category_id, category_name, quantity, unit_price, addons_unit_total, line_subtotal, notes)`
- `order_item_addons(id, order_item_id, addon_id, addon_name, unit_price)`
- `order_status_history(id, order_id, from_status, to_status, changed_by, changed_at, note)`

**Caja y pagos**
- `cash_sessions(id, user_id, opened_at, opening_amount, closed_at, status[OPEN|CLOSED], closing_note, version)`, con un índice único parcial que impide más de una sesión OPEN por usuario.
- `cash_session_summaries(id, cash_session_id, payment_method_id, expected_amount, declared_amount, difference)`
- `payments(id, order_id, cash_session_id, payment_method_id, amount, reference, status[PAID|REFUNDED], …)`.
  - `amount` es el importe aplicado al pedido. No se guardan el importe recibido ni el vuelto.
- `cash_movements(id, cash_session_id, type[OPENING|SALE|REFUND|INCOME|EXPENSE], payment_method_id, amount, order_id, payment_id, description, …)`

```
user_app ─< user_role >─ role ─< role_permission >─ permission
category ─< product >─< product_addons >─ addon
table_marker ─< order >─ cash_session ─< payment >─ payment_method
order ─< order_item ─< order_item_addon
order ─< order_status_history ;  cash_session ─< cash_movement ; cash_session ─< cash_session_summary
```

---

## 5. Estrategias clave

### Precios y descuentos (`OrderPricingService`, la única fuente de verdad)

1. El backend **ignora cualquier precio que llegue del cliente**. Recibe `productId`, `quantity`, `addonIds`, `notes`, el descuento, `serviceType`, `tableId` y los pagos.
2. Carga el precio actual de cada producto y adicional, y comprueba que estén `active` y `available` y que el adicional pertenezca al producto.
3. `line_subtotal = (unit_price + Σ addon) × qty` → `subtotal = Σ líneas`.
4. Validaciones del descuento:
   - `PERCENTAGE` está entre 0 y 100.
   - `FIXED` cumple que el valor sea ≤ subtotal.
   - `discount_amount = round(subtotal × %/100)` y `total = subtotal − discount_amount`, que debe ser ≥ 0.
5. Permisos sobre el descuento:
   - COCINA no puede aplicar descuentos.
   - CAJERO puede hacerlo hasta el valor de `CASHIER_MAX_DISCOUNT_PERCENT`; un monto fijo se convierte a porcentaje para compararlo.
   - ADMIN no tiene límite.
   - Por encima del umbral se exige `discount_reason`.
6. `POST /order/quote` devuelve los totales calculados por el backend para el resumen del paso 6. El frontend calcula solo una vista previa.

### Registro del pedido: una sola transacción

`POST /order` sigue estos pasos:
1. Exige que el usuario tenga una sesión de caja abierta.
2. Calcula los precios.
3. En DINE_IN exige `tableMarkerId` activo y libre. El índice parcial protege contra dos cajeros que asignan el mismo número a la vez y responde 409 con "Número de mesa en uso".
4. Comprueba que `Σ payments.amount == total`. El vuelto lo calcula solo el frontend y no se envía.
5. Un pago que no es en efectivo puede pedir `reference`.
6. Inserta el pedido, sus ítems, los adicionales, el historial (`null → PENDIENTE`), los pagos y los movimientos `SALE`.
7. Después del commit publica `ORDER_CREATED` mediante `@TransactionalEventListener(AFTER_COMMIT)`.

### Máquina de estados (`OrderStateMachine`, validada en el servicio)

| Transición | Roles |
|---|---|
| PENDIENTE → EN_PREPARACION | COCINA, ADMIN |
| EN_PREPARACION → LISTO | COCINA, ADMIN |
| LISTO → ENTREGADO | CAJERO, ADMIN |
| PENDIENTE → CANCELADO | CAJERO, ADMIN |
| EN_PREPARACION / LISTO → CANCELADO | ADMIN |

- `PATCH /order/{id}/status {toStatus, expectedVersion}`.
  - Si la versión no coincide (`@Version`) devuelve **409**, y el cliente recarga.
- Cada cambio de estado escribe una fila en `order_status_history`.
- La cancelación exige motivo, marca los pagos como REFUNDED y crea un movimiento `REFUND` (egreso) en la sesión de caja de **quien cancela**, que debe tenerla abierta.

### Tiempo real

- **Conexión**: endpoint `/ws-cdr` con SockJS. El cliente envía `Authorization: Bearer` en la trama STOMP CONNECT y `WebSocketAuthInterceptor` la valida.
- **Topics**:
  - `/topic/orders`: CAJERO, COCINA y ADMIN. El interceptor rechaza un SUBSCRIBE no autorizado.
  - `/topic/catalog`: cambios de disponibilidad de productos, para el POS.
- **Eventos**: `ORDER_CREATED`, `ORDER_STATUS_CHANGED`, `ORDER_CANCELLED`, `PRODUCT_AVAILABILITY_CHANGED`.
- **Payload**: `{eventId(UUID), type, orderId, version, status, occurredAt, order: OrderBoardPojo}`.
- **Sincronización inicial**: al conectar y en **cada reconexión**, el cliente llama a `GET /order/board?statuses=…`, que es la fuente de verdad, y aplica después los eventos.
- **Duplicados**: el cliente guarda un `Map<orderId, version>` e ignora los eventos con una versión ≤ a la local.
- **Reconexión**: `reconnectDelay: 3000` (igual que la referencia), con indicador visual de "desconectado".
- **Cocina**: ve PENDIENTE y EN_PREPARACION de forma destacada y LISTO en una columna compacta.
- **Broker**: el broker simple en memoria basta para una sola réplica. Si en el futuro hay varias réplicas habrá que pasar a un relay; queda documentado.

### Pagos y caja

- **Apertura**: `POST /cash-session/open {openingAmount}` crea la sesión y el movimiento OPENING.
- **Movimientos manuales**: `POST /cash-session/movement`, para ingresos y egresos con descripción.
- **Cierre**: `POST /cash-session/close {declared[]: {paymentMethodId, amount}}`.
  - Calcula lo esperado por medio de pago a partir de los movimientos; en efectivo se incluye el monto de apertura.
  - Guarda las diferencias y pasa la sesión a CLOSED.
  - El cierre falla si el cajero tiene pedidos propios no entregados (decisión pendiente).

### Reportes

- Se calculan **solo a partir de las fotografías** (`order_items`, `orders.discount_*`, `payments`); nunca con el precio del catálogo.
- Distinguen siempre entre bruto (`subtotal`), descuento, neto (`total`) y pagado (`payments` PAID − REFUNDED).
- Por defecto excluyen los pedidos CANCELADO, salvo el reporte por estado.
- Filtros: rango de fechas, usuario, categoría, modalidad y medio de pago.

### Auditoría

- `Base` (createdBy/updatedBy y fechas) en todas las tablas.
- `order_status_history` y los campos de descuento y cancelación en `orders`.
- `cash_movements` como bitácora inmutable de caja.
- Log `INFO` para las operaciones críticas.

---

## 6. Endpoints (convención de la referencia: recurso en singular, sin `/api/v1`, context path `/cdr`)

| Recurso | Endpoints |
|---|---|
| auth | `POST /login`, `POST /auth/refresh`, `POST /auth/logout` |
| users | CRUD `/users`, `GET /users/pageable`, `PATCH /users/{id}/active`, `GET /users/{id}/roles` |
| roles/permissions | CRUD `/roles`, `GET /permission`, `PUT /roles/{id}/permissions` |
| catálogo | CRUD `/category`, `/product` (+`PATCH /product/{id}/availability`), `/addon`, `/table-marker` (+`GET /table-marker/availability` = números libres/en uso), `/payment-method`, `/parameter` |
| POS | `GET /pos/catalog` (categorías + productos activos + adicionales) |
| order | `POST /order/quote`, `POST /order`, `GET /order/pageable`, `GET /order/{id}`, `GET /order/board`, `PATCH /order/{id}/status`, `POST /order/{id}/cancel` |
| caja | `GET /cash-session/current`, `POST /cash-session/open`, `POST /cash-session/movement`, `POST /cash-session/close`, `GET /cash-session/pageable`, `GET /cash-session/{id}` |
| report | `GET /report/sales?groupBy=day\|week\|month`, `/report/top-products`, `/report/by-category`, `/report/by-status`, `/report/by-user`, `/report/discounts`, `/report/by-service-type`, `/report/by-payment-method`, `/report/cash-sessions` |

`PermissionSeeder` genera los permisos al arrancar, y una migración Flyway de datos asigna los permisos iniciales a CAJERO y COCINA. ADMIN no pasa por la comprobación, igual que ADM en la referencia.

---

## 7. Experiencia de usuario (frontend)

- **POS** (`/pos`), en dos columnas en escritorio y tablet horizontal:
  - Izquierda: los pasos categoría → producto, con tarjetas PrimeNG (`p-card`, `sombra-black`, imagen, nombre, precio en Bs y un `p-tag` de "Agotado").
  - Derecha: el carrito, siempre visible, que muestra:
    - las líneas con cantidad editable y observaciones;
    - el subtotal;
    - el descuento (`p-selectbutton` %/Bs + `p-inputnumber`);
    - el total.
  - Al pulsar "Continuar" se abre un `app-stepper` con estos pasos:
    1. Modalidad:
       - Para llevar, con el nombre del cliente opcional.
       - En mesa, con una cuadrícula de **números de adorno** libres (y los ocupados deshabilitados) para elegir el que se entrega al cliente.
    2. Pago, con varios medios. En efectivo, un campo "Recibido" calcula y muestra el vuelto solo en pantalla; no se envía ni se guarda.
    3. Resumen calculado por `/order/quote`.
    4. Confirmar.
  - El estado del carrito vive en `CartService` con signals.
- **Cocina** (`/kitchen`):
  - Pantalla completa sin footer, con tres columnas por estado.
  - Las tarjetas muestran el código, el **número de adorno grande** (o "PARA LLEVAR" + nombre), los ítems con adicionales y notas, y el tiempo transcurrido; el color del borde cambia según el estado y el tiempo.
  - Botones grandes (`size="large"`) para la siguiente transición; sonido al llegar un pedido nuevo, igual que en la referencia.
- **Pedidos** (`/orders`): `app-table-on-demand`, con filtros, un detalle en modal y el timeline del historial.
- **CRUDs**: siguen exactamente el patrón `schedule`/`user` mediante la skill `angular-crud`.
- **Identidad visual**: los mismos SCSS light/dark, la misma paleta teal + Lynch, el mismo header/footer y las mismas clases utilitarias. Solo cambian el logo y los textos de la marca.

---

## 8. Plan de implementación por etapas (cada una requiere tu aprobación)

| Etapa | Contenido | Verificación |
|---|---|---|
| **0** | Documentación y Skills (§2). Sin código | Revisión de los documentos |
| **1** | Proyectos base. Backend: Gradle, `appbase`, `exception`, `config`, Flyway V1 con seguridad, `appsecurity` completo, CORS y PermissionSeeder. Frontend: scaffold, temas y estilos copiados, layout, login, interceptores, guard, `mobile-app-required` ajustado para permitir tablets | `gradlew test`; login → JWT → ruta protegida; 403 por permiso |
| **2** | Administración: usuarios (activar/desactivar), roles y permisos, categorías, productos (imagen base64, disponibilidad), adicionales (N:M), adornos numerados, medios de pago, parámetros | Tests de controller y service por CRUD; `ng build` + specs de Vitest |
| **3** | Caja (apertura y movimientos), POS completo, cotización, registro transaccional con pagos, descuentos con límite por rol | Tests de `OrderPricingService` (redondeo, límites, total ≥ 0) y de registro (rollback si fallan los pagos) |
| **4** | Tiempo real: WS con JWT, `/order/board`, máquina de estados, cancelación con devolución, pantallas de cocina y seguimiento | Tests del interceptor WS y de las transiciones; dos navegadores (caja y cocina) con actualización en vivo; 409 cuando hay concurrencia |
| **5** | Cierre de caja con arqueo, historial de sesiones | Tests de cálculo de lo esperado y de las diferencias |
| **6** | Reportes y exportación | Datos de prueba con cambio de precio: el reporte conserva el precio histórico |
| **7** | Endurecimiento: tests de concurrencia, revisión de seguridad, índices, rendimiento | Checklist de seguridad, `/security-review` |

**Cambio respecto al orden sugerido**: Caja básica entra en la Etapa 3 en lugar de la 5. Como el pago es al registrar, no se puede crear un pedido sin una sesión de caja abierta. La Etapa 5 queda para el cierre y el arqueo.

---

## 9. Decisiones pendientes (se confirman en la Etapa 0 o antes de su etapa)

1. **Paquete Java y nombre comercial**: propongo `bo.cdr.app`; falta el nombre de la cafetería y su logo (claro/oscuro).
2. **Valores por defecto**: límite de descuento del CAJERO (propuesta: 15 %) y umbral a partir del cual se exige motivo.
3. **Código del pedido**: secuencia diaria reiniciada (#001…) o correlativo global.
4. **Cierre de caja con pedidos abiertos**: bloquearlo o permitirlo con una advertencia.
5. **Exportación de reportes**: Excel en el frontend con exceljs (patrón `excel.service` de la referencia) o PDF/Excel en el backend con Jasper/POI.
6. **¿COCINA puede revertir un estado** (por ejemplo LISTO → EN_PREPARACION)? Propuesta: no en la v1.
7. **Medios de pago iniciales**: EFECTIVO, TRANSFERENCIA y QR.
8. **Tests de integración**: ¿añadir Testcontainers para los tests de concurrencia y de Flyway? No existe en la referencia; se plantearía en la Etapa 7.

## Archivos críticos de referencia, para reutilizar como plantilla

- Backend:
  - `ticket-backend/src/main/java/esyc/ticket/app/appbase/**`
  - `appticket/presentation/controller/HolidayController.java` con su servicio, mapper y DTO
  - `exception/ErrorHandler.java`
  - `appsecurity/auth/jwt/*`
  - `util/{RequestMatcherUtil,PermissionSeeder,CORS}.java`
  - `config/{AuditorConfig,WebSocketConfig}.java`
  - `src/test/.../util/MockMvcTestSupport.java`
- Frontend:
  - `ticket-frontend/src/app/modules/schedule/**`
  - `modules/user/**` (paginado)
  - `core/{interceptors,guards,services/login.service.ts,services/http-error-handler.service.ts,services/message.service.ts,constants/header-links.ts,utils/table-column-definitions.ts}`
  - `shared/components/{generic-table,table-on-demand,toolbar,modal-delete,stepper}`
  - `src/styles-light.scss`, `src/styles-dark.scss`, `src/styles/general.scss`
  - `.claude/skills/angular-crud/**`

### Decisiones pendientes adicionales (detectadas al redactar la Etapa 0)

9. **Pedido con total 0** (descuento 100 %): ¿se permite sin pagos? Mientras tanto se rechaza.
10. **`strictNullChecks`** en cdr-frontend: la referencia lo tiene desactivado; recomendado activarlo al ser proyecto nuevo.
11. **Expiración del JWT**: la referencia usa 24 h; para un POS compartido se sugiere la duración de un turno (p. ej. 12 h).
12. **Password en base64 desde el login** (patrón de la referencia, no es cifrado): ¿se mantiene o se envía en claro sobre HTTPS?
13. **Tamaño máximo de imagen base64** (sugerido 1 MB).
14. **Umbrales de "pedido demorado"** en cocina (sugerido 10 y 20 min), ¿configurables como parámetro?
15. **¿CAJERO puede marcar productos como agotados?** (`PATCH /product/{id}/availability`).
16. **URLs de entornos** dev/qa/prod del backend y frontend, puerto definitivo.
17. **Contraseña del ADMIN inicial**: crearlo en el primer arranque desde variable de entorno.

### Decisiones resueltas en la Etapa 1 (2026-11-10)

| # | Decisión |
|---|---|
| 1 | Paquete `bo.cdr.app`; nombre comercial **Ciudad de Refugio** (logo aún pendiente) |
| 10 | `strictNullChecks` activo en cdr-frontend |
| 11 | JWT 12 h + refresh token 7 días (con rotación) |
| 12 | Contraseña en base64 desde el login (patrón de la referencia) |
| 16 | Dev local: PostgreSQL `localhost:5432`, base `cdr_db`; URLs de dev/qa/prod siguen pendientes |
| 17 | ADMIN inicial creado en el primer arranque desde `APP_ADMIN_INITIAL_PASSWORD` |
| — | Login por **nombre de usuario** (no email); email opcional |

### Decisiones resueltas en la Etapa 2 (2026-11-10)

| # | Decisión |
|---|---|
| 2 | Descuento máximo del CAJERO **50 %**; **todo descuento requiere motivo** (`DISCOUNT_REASON_REQUIRED_ABOVE_PERCENT = 0`). Ambos editables en Parámetros |
| 7 | Medios de pago iniciales: **EFECTIVO** (calcula vuelto) y **QR** (exige referencia) |
| 13 | Imágenes base64 de hasta **1 MB** (validado en frontend y backend) |
| 15 | El **CAJERO puede marcar productos agotados/disponibles** (`PATCH /product/{id}/availability`); el resto del CRUD de productos es solo ADMIN |

### Decisiones resueltas en la Etapa 3 (2026-11-10)

| # | Decisión |
|---|---|
| 9 | **Pedido con total 0: se rechaza** (400 "El total del pedido debe ser mayor a 0"), también para ADMIN |
| 3 | **Código del pedido diario**: `#001` reinicia cada día (fecha de negocio en America/La_Paz). Correlativo atómico con UPSERT sobre `order_daily_sequences`; único por `(business_date, code)` |
| — | **Catálogo del POS en una sola llamada con imágenes** (`GET /pos/catalog`): categorías y productos activos (agotados incluidos y marcados), adicionales con precio, medios de pago activos y reglas de descuento |

Implementación de la Etapa 3 (detalle en `cdr-backend/CLAUDE.md`):
- `V5__orders_and_cash.sql`: `cash_sessions`, `order_daily_sequences`, `orders`, `order_items`,
  `order_item_addons`, `order_status_history`, `payments`, `cash_movements` + permisos del CAJERO.
- `GET /cash-session/current` (204 sin caja), `POST /cash-session/open`, `POST /cash-session/movement`
  (solo INCOME/EXPENSE), `GET /pos/catalog`, `GET /table-marker/availability`, `POST /order/quote`,
  `POST /order`, `GET /order/{id}`.
- Movimientos OPENING/INCOME/EXPENSE sin medio de pago = efectivo; SALE/REFUND llevan medio de pago.
- `ORDER_CREATED` por WebSocket queda para la Etapa 4.

### Decisiones resueltas en la Etapa 4 (2026-11-11)

| # | Decisión |
|---|---|
| 6 | **COCINA y ADMIN pueden retroceder un paso** (LISTO → EN_PREPARACION → PENDIENTE); queda en el historial. ENTREGADO y CANCELADO siguen siendo finales |
| 14 | **Demoras de cocina como parámetros editables** (carpeta COCINA): `KITCHEN_DELAY_WARNING_MINUTES = 10` (amarillo) y `KITCHEN_DELAY_CRITICAL_MINUTES = 20` (rojo), contados desde el registro |
| — | **Pantalla Pedidos = seguimiento en vivo + historial** paginado con filtros (fechas, estado, modalidad, código) y detalle con línea de tiempo |

Implementación de la Etapa 4:
- `V6__kitchen_and_order_tracking.sql`: carpeta KITCHEN + 2 parámetros; permisos COCINA (`GET /order/board`,
  `GET /order/kitchen-settings`, `PATCH /order/{id}/status`) y CAJERO (board, status, cancel, pageable).
- WebSocket `/ws-cdr` (SockJS) con `WebSocketAuthInterceptor`; eventos tras el commit (`OrderEventPublisher`,
  `CatalogEventPublisher`). `OrderStateMachine` en `apporder/domain/model`.
- Endpoints: `GET /order/board`, `GET /order/kitchen-settings`, `PATCH /order/{id}/status`,
  `POST /order/{id}/cancel`, `GET /order/pageable`; `GET /order/{id}` incluye `statusHistory` y cancelación.

### Decisiones resueltas en la Etapa 5 (2026-11-11)

| # | Decisión |
|---|---|
| 4 | **Cierre con pedidos sin entregar: permitido con advertencia** (el preview los lista y la UI pide confirmar); siguen su curso |
| — | **Arqueo de todos los medios**: el cajero declara efectivo contado y el total de cada otro medio; se guarda esperado, declarado y diferencia |
| — | **El ADMIN puede cerrar la caja de otro** (cajas olvidadas) con arqueo y **nota obligatoria**; queda `closed_by` |
| — | **Historial de cajas: ADMIN todas, CAJERO solo las suyas** |

Implementación de la Etapa 5:
- `V7__cash_close.sql`: `cash_sessions.closed_by`, tabla `cash_session_summaries` (CHECK difference = declarado − esperado), permisos CAJERO.
- Endpoints: `GET /cash-session/{id}/close-preview`, `POST /cash-session/{id}/close`, `GET /cash-session/{id}`,
  `GET /cash-session/pageable`, `GET /cash-session/open-sessions` (solo ADMIN).
- Concurrencia: cada cobro/devolución/movimiento toma la caja con `PESSIMISTIC_FORCE_INCREMENT` (FOR UPDATE + versión):
  los cobros del mismo cajero se serializan sin fallar; el cierre toma `PESSIMISTIC_WRITE` y compara `expectedVersion`,
  así un arqueo hecho antes de un cobro recibe 409 y un cobro que espera a un cierre ya no encuentra la caja abierta.

### Decisiones resueltas en la Etapa 6 (2026-11-11)

| # | Decisión |
|---|---|
| 5 | **Exportación: Excel generado en el backend con Apache POI** (base64, como `AttachmentRecord` de la referencia). Sin PDF en v1 |
| — | **Pantalla: tarjetas de totales + tablas PrimeNG**, sin librería de gráficos |
| — | **Acceso: solo ADMIN** |
| — | **Reportes v1: los cuatro grupos** — ventas por período y modalidad; productos y categorías; medios de pago y cajas; cajeros, estados, descuentos y cancelaciones |

Implementación de la Etapa 6 (sin migración):
- `appreport`: `ReportRepository` (SQL nativo + CTE sobre fotografías), `ReportServiceImpl` (validación, etiquetas,
  participaciones %, ticket promedio con `MoneyUtil`), `ReportExcelServiceImpl` (una hoja por sección, importes numéricos `#,##0.00`).
- Endpoints: `GET /report/sales|products|payments|operations` y `GET /report/export?report=`; filtros `dateFrom`/`dateTo`
  (obligatorios, fecha de negocio), `groupBy`, `userId` (dueño de la caja), `serviceType`, `categoryId` (productos), `paymentMethodId` (pagos).
- Definiciones: Bruto = Σ subtotal, Neto = Σ total, Pagado = pagos PAID, Devuelto = pagos REFUNDED (pedidos cancelados);
  el importe por producto es la suma de líneas **antes** del descuento general (no se reparte por línea).
- Cajas del reporte: las abiertas en el rango (fecha de `opened_at`); arqueo en null si siguen abiertas.

### Decisiones resueltas en la Etapa 7 (2026-11-11)

| # | Decisión |
|---|---|
| 8 | **Testcontainers** para concurrencia y migraciones (tarea `integrationTest` aparte; requiere Docker) |
| 16 | **Solo dos entornos: dev (localhost) y prod.** URL de prod y empaquetado aún sin definir: el frontend de prod usa `/cdr/` (mismo dominio) |
| — | Alcance: concurrencia y migraciones, seguridad, índices y rendimiento, navegador/tablet y preparación de prod |

Resultados de la Etapa 7:
- **Seguridad:** el JWT ya no es fuente de roles: cada petición (y cada STOMP CONNECT) valida que el usuario siga activo y
  carga sus roles vigentes; antes un ADMIN desactivado conservaba acceso total hasta 12 h. Prod toma la IP real detrás del
  proxy (`forward-headers-strategy=native`) para que el límite de intentos no bloquee a todos, y no expone detalles de error.
  Frontend: quitados pipes sin uso que saltaban el sanitizador de Angular. `npm audit`: 0 vulnerabilidades en producción
  (16 moderadas solo en herramientas de build).
- **Rendimiento** (≈110 mil pedidos, 1 año): tablero, mesas, historiales y detalles 12–80 ms; reportes de un mes 0,1–0,2 s;
  de un año 0,4–1,7 s (lectura completa; un índice no ayuda). **Sin migración V8.** Excel con SXSSF: 5,9 s → 2,4 s.
- **UI** (Chrome headless, escritorio/iPad/Android/teléfono): sin errores de consola ni HTTP; corregidos estados técnicos
  visibles, cocina en 2 columnas en tablet horizontal, menú del ADMIN en dos filas, desborde en tablet vertical y formato de %.
- **Pendiente:** ejecutar `integrationTest` (Docker Desktop no arranca en el equipo de desarrollo: WSL sin distribución),
  URL de prod, empaquetado (Docker o no) y logo.
