# Arquitectura del backend (cdr-backend)

Derivada del código real de `../ticket-backend` (fuente de verdad: el **código**, no su `CLAUDE.md`,
que está desactualizado en mappers, Criteria API y `ddl-auto`).

## 1. Módulos

```
bo.cdr.app
├─ AppApplication                @SpringBootApplication @EnableJpaAuditing @EnableScheduling @EnableCaching
├─ appbase/                      Base genérica de todos los módulos
│  ├─ data/GenericRepository<T,ID>            JpaRepository + JpaSpecificationExecutor, @NoRepositoryBean
│  ├─ domain/entity/Base                      @MappedSuperclass auditable
│  ├─ domain/mapper/PagePojoMapper
│  ├─ domain/service/CRUDService, CRUDServiceImpl
│  └─ presentation/response/BasePojo, PagePojo, ResponsePojo, MessagePojo
├─ appsecurity/                  JWT, refresh token, usuarios, roles, permisos
├─ appcatalog/                   Category, Product, Addon, TableMarker, PaymentMethod, Parameter
├─ apporder/                     Order, OrderItem, OrderItemAddon, OrderStatusHistory, pricing, state machine, eventos
├─ appcash/                      CashSession, CashSessionSummary, CashMovement, Payment
├─ appreport/                    Consultas de reportes (proyecciones / SQL nativo)
├─ config/                       AuditorConfig, WebSocketConfig, WebSocketAuthInterceptor, ClockConfig, MessageConfig, deserializadores
├─ common/                       enums/, custom/validator/, MoneyUtil, SpanishEntityNameProvider, Pojo
├─ exception/                    Excepciones tipadas + ErrorHandler + response/
└─ util/                         CORS, RequestMatcherUtil, PermissionSeeder, SortUtil, Messages, constants/
```

Todos los módulos de dominio usan **exactamente** esta estructura interna (en la referencia variaba):

```
<modulo>/data/
<modulo>/domain/entity/
<modulo>/domain/mapper/
<modulo>/domain/service/interfaces/
<modulo>/domain/service/implement/
<modulo>/presentation/controller/
<modulo>/presentation/request/dto/
<modulo>/presentation/request/filter/
<modulo>/presentation/response/pojo/
```

Clases de dominio que no son CRUD (en `domain/service/implement/` o subpaquete propio si crecen):
`OrderPricingService`, `OrderStateMachine`, `OrderEventPublisher` (apporder); `CashCalculator` (appcash).

## 2. Flujo de una petición

```
HTTP ─► CORS (filtro servlet, HIGHEST_PRECEDENCE)
     ─► JwtAuthenticationFilter   (Bearer → SecurityContext con CustomUserDetails)
     ─► AuthorizationFilter       (permiso por método+URL normalizada; ADMIN omite)
     ─► Controller                (@Valid @RequestBody XxxDto | @ModelAttribute XxxFilter)
     ─► Service (interface)       (@Transactional, reglas, roles finos)
          ├─ Mapper.fromDto(dto, found, relaciones...)   (entidad)
          ├─ Repository           (save / proyección JPQL → Pojo)
          └─ ApplicationEventPublisher → @TransactionalEventListener(AFTER_COMMIT) → STOMP
     ◄─ ResponseEntity<Entity|Pojo>   (o ErrorHandler → ErrorResponse)
```

## 3. Capas — qué hace cada una (con ejemplo real de la referencia)

### Controller
Delgado: valida entrada, delega, fija el status. Patrón literal de `HolidayController`:

```java
@RestController
@AllArgsConstructor
@RequestMapping("/holiday")
public class HolidayController {
    private final HolidayService holidayService;

    @GetMapping
    public ResponseEntity<List<HolidayPojo>> getAllHolidayPojo() {
        return ResponseEntity.status(HttpStatus.OK).body(holidayService.getAllHolidayPojo());
    }
    @PostMapping
    public ResponseEntity<Holiday> create(@Valid @RequestBody HolidayDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(holidayService.create(dto));
    }
    @PutMapping("/{id}")
    public ResponseEntity<Holiday> update(@PathVariable Integer id, @Valid @RequestBody HolidayDto dto) { ... 200 }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) { ... 204 }
}
```

- Ruta en **singular** kebab (`/product`, `/table-marker`, `/cash-session`), sin `/api/v1` (el
  context path `/cdr` hace de prefijo). Excepción heredada: `/users`, `/roles`.
- Sub-acciones en kebab-case: `/order/{id}/status`, `/order/quote`, `/product/{id}/availability`.
  (La referencia mezcla camelCase; aquí **siempre kebab**.)
- **Sin lógica de negocio** ni acceso a repositorios en el controller.
- Para el catálogo se pueden devolver entidades (como la referencia) **solo si** las relaciones
  inversas llevan `@JsonIgnore`. Para pedidos, caja y reportes devolver **siempre Pojos**.

### Service
- Interfaz en `interfaces/` que extiende `CRUDService<T, Integer>`; implementación en `implement/`
  que extiende `CRUDServiceImpl<T, Integer>` e implementa `getRepository()`.
- Inyección por constructor con `@RequiredArgsConstructor` / `@AllArgsConstructor` y campos `private final`.
- Resuelve relaciones (`categoryService.getById(dto.getCategoryId())`) y las pasa al mapper.
- Lanza excepciones tipadas (`DuplicateException`, `EntityNotFoundException`, `ValidationErrorException`, ...).
- `@Transactional(rollbackFor = Exception.class)` en escrituras.
- **Dependencias circulares:** la referencia usa `FunctionManager`/`NamedFunction`. En CDR se evitan
  por diseño (módulos en capas: catalog ← order ← cash ← report). Si aparece un ciclo, consultar antes
  de introducir `FunctionManager`.

### Mapper
`@Component` manual, sin MapStruct ni ModelMapper. Reusa la entidad encontrada al actualizar:

```java
@Component
public class HolidayMapper {
    public Holiday fromDto(HolidayDto dto, Holiday holidayFound, Country country, TerritorialDivision td) {
        Holiday holiday = holidayFound != null ? holidayFound : new Holiday();
        holiday.setDay(dto.getDay());
        ...
        return holiday;
    }
}
```

### Repository
- `interface XxxRepository extends GenericRepository<Xxx, Integer>` con `@Repository`.
- Lecturas para la UI: **proyección JPQL con constructor** hacia el Pojo
  (`SELECT new ...XxxPojo(x.id, ..., x.createdBy, x.updatedBy, x.createdAt, x.updatedAt) FROM Xxx x LEFT JOIN ...`).
- Consultas derivadas para validaciones (`existsByNameIgnoreCaseAndActiveIsTrueAndIdNot`).
- Reportes complejos: SQL nativo con CTE en una clase `*RepositoryNative` con `EntityManager`
  (patrón `TicketRepositoryCriteria` + `NativeQueryPageUtil` de la referencia).

### Paginación, filtros y orden (patrón `UserServiceImpl`/`UserController` de la referencia)
- Parámetros: `@RequestParam(defaultValue="0") int page`, `@RequestParam(defaultValue="15") int size`,
  `@RequestParam(defaultValue="id") String sortBy`, `@RequestParam(defaultValue="desc") String sortOrder`,
  `@ModelAttribute XxxFilter filter` (incluye `some` = búsqueda global).
- El servicio valida `sortBy` contra una **lista blanca** (`SortUtil`) y crea `PageRequest.of(page, size, sort)`.
- Respuesta `PagePojo<T>{content,last,pageNumber,pageSize,totalPages,totalElements}` vía `PagePojoMapper.convertToPagePojo(page)`.
- Endpoint: `GET /<recurso>/pageable`.

## 4. Base genérica

`Base` (copiada de la referencia):
```java
@Getter @Setter @MappedSuperclass @EntityListeners(AuditingEntityListener.class)
public class Base {
    private Boolean active = true;
    @CreatedBy private String createdBy;
    @LastModifiedBy private String updatedBy;
    @CreatedDate @Column(updatable = false) private LocalDateTime createdAt;
    @LastModifiedDate private LocalDateTime updatedAt;
}
```
`AuditorConfig.auditorProvider()` devuelve el `username` del `CustomUserDetails` o `"system"`.

`CRUDServiceImpl` (copiada) aporta `create`, `update`, `getAll`, `getById` (→ `EntityNotFoundException`
con nombre en español), `getByIds`, `existsById`, `delete`, `checkIfElementsExist(Map)`.
**Cambio en CDR:** se añade `softDelete(id)` (`active=false` + `save`) que es lo que usan los
servicios de catálogo; `delete` físico solo para tablas puente sin historia.

## 5. Excepciones → HTTP

`ErrorHandler` (`@ControllerAdvice(basePackages = "bo.cdr.app")`) — copiar de la referencia y añadir:

| Excepción | HTTP |
|---|---|
| `EntityNotFoundException`, `NoHandlerFoundException` | 404 |
| `ValidationErrorException`, `InvalidValueException`, `HasAssociatedEntityException`, `InvalidEnumValueException`, `NotAllowedException`, `InvalidStatusTransitionException` *(nueva)*, `DiscountNotAllowedException` *(nueva)*, `CashSessionRequiredException` *(nueva)* | 400 |
| `MethodArgumentNotValidException` | 400 `ValidationErrorResponse` |
| `UnauthorizedException`, `BadCredentialsException` | 401 |
| `AccessDeniedException` (filtros) | 403 vía `CustomAccessDeniedHandler` |
| `DuplicateException`, `ObjectOptimisticLockingFailureException`, `ConcurrentModificationException` *(nueva)*, violación de `ux_orders_marker_open` | **409** |
| `TooManyLoginAttemptsException` | 429 |
| otras | 500 "Error interno del servidor" (log) |

Mensajes desde `notification-messages.properties` (`Messages.getProperty(...)`) y
`validation-messages.properties` (`{product.name.not-blank}`).

## 6. Configuración y perfiles

- `application.properties` + `application-{dev,prod}.properties`: **solo dos entornos** (la referencia tiene además
  test/qa/etr). `prod` usa `server.forward-headers-strategy=native` (IP real del cliente detrás del proxy, para el límite
  de intentos de login) y no expone detalles de error.
- `spring.jpa.hibernate.ddl-auto=validate` en **todos** los perfiles; `spring.flyway.enabled=true`.
- `spring.jackson.time-zone=America/La_Paz`; `server.servlet.context-path=/cdr`; `server.port=8086`.
- `app.cors.allowed-origins`, `app.jwt.secret=${APP_JWT_SECRET}`, `spring.datasource.password=${DB_PASSWORD}`.
- `spring.task.scheduling.enabled=false` en dev (como la referencia).
- Docker/CI: mismo esquema que la referencia (Dockerfile Gradle + compose por entorno) — se define en Etapa 1.

## 7. Tests

Copiados de la referencia: JUnit 5 + Mockito, `MockMvcTestSupport.buildMockMvc(controller)` (standalone,
con `ErrorHandler`, validador por reflexión y `JsonMapper` de Jackson 3). Nombres
`metodo_returnsXXX_whenCondicion`, cuerpos JSON en text blocks. Ver skill `backend-crud`.
Tests de integración con Testcontainers: **decisión pendiente** (Etapa 7).
