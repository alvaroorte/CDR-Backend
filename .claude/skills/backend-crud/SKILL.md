---
name: backend-crud
description: Estándar de cdr-backend (Spring Boot 4, Java 21) para crear o ampliar un CRUD completo de una entidad — Repository, Mapper, Service (interfaz + impl sobre CRUDServiceImpl), Controller REST, DTO, Filter, Pojo, mensajes de validación, permisos y tests MockMvc — replicando exactamente el patrón de HolidayController/HolidayServiceImpl de ticket-backend. Úsala siempre que se pida crear endpoints de mantenimiento/administración (categorías, productos, adicionales, adornos de mesa, medios de pago, parámetros, usuarios, roles) o agregar un endpoint REST a una entidad existente, aunque no se diga "CRUD".
---

# CRUD backend — estándar de cdr-backend

Replica 1:1 el patrón del slice **Holiday** de `../ticket-backend` (solo lectura):
`appticket/presentation/controller/HolidayController.java`, `domain/service/interfaces/HolidayService.java`,
`domain/service/implement/HolidayServiceImpl.java`, `domain/mapper/HolidayMapper.java`,
`data/HolidayRepository.java`, `presentation/request/dto/HolidayDto.java`,
`presentation/response/pojo/HolidayPojo.java`, test `HolidayControllerTest.java`.

Plantillas completas: [references/templates.md](references/templates.md).
Si la entidad aún no existe, usa **primero** la skill `backend-entity` (entidad + migración Flyway).
Si toca pedidos, dinero, caja o descuentos, lee también la skill `cdr-order-management`.

## 1. Proceso obligatorio

1. **Requisitos.** Entidad, módulo (`appcatalog`, `apporder`, `appcash`, `appsecurity`), campos con
   tipo/obligatoriedad/validación, relaciones, unicidad, si el borrado es lógico (catálogo: sí), roles
   que acceden. Si falta algo que cambie el contrato, **pregunta**; no inventes.
2. **Buscar reutilizables** (Grep/Glob): ¿existe ya la entidad, el servicio de la relación, el enum,
   la clave de mensaje, el validador?
3. **Implementar en este orden:**
   `XxxRepository` → `XxxMapper` → `XxxService` → `XxxServiceImpl` → `XxxDto` (+ claves en
   `validation-messages.properties`) → `XxxPojo` (+ query JPQL en el repositorio) → `XxxFilter` (si hay
   paginado) → `XxxController` → `SpanishEntityNameProvider.NAME_MAP` → asignación de permisos a roles
   (migración de datos) → `XxxControllerTest` (+ `XxxServiceImplTest` si hay reglas).
4. **Verificar:** `.\gradlew.bat test` → **BUILD SUCCESSFUL** obligatorio.
5. **Informar** decisiones relevantes y archivos compartidos tocados.

## 2. Estructura

```
bo/cdr/app/<modulo>/
├── data/XxxRepository.java
├── domain/entity/Xxx.java                         (skill backend-entity)
├── domain/mapper/XxxMapper.java
├── domain/service/interfaces/XxxService.java
├── domain/service/implement/XxxServiceImpl.java
└── presentation/
    ├── controller/XxxController.java
    ├── request/dto/XxxDto.java
    ├── request/filter/XxxFilter.java              (solo si hay /pageable)
    └── response/pojo/XxxPojo.java
src/main/resources/validation-messages.properties  (+ claves xxx.campo.regla)
src/main/resources/db/migration/V{n}__seed_permissions_xxx.sql  (asignar permisos a CAJERO/COCINA si aplica)
src/test/java/bo/cdr/app/<modulo>/presentation/controller/XxxControllerTest.java
```

## 3. Contrato REST estándar

| Método | Ruta | Respuesta | Notas |
|---|---|---|---|
| GET | `/xxx` | 200 `List<XxxPojo>` | proyección JPQL |
| GET | `/xxx/{id}` | 200 `XxxPojo` / 404 | |
| GET | `/xxx/pageable` | 200 `PagePojo<XxxPojo>` | `page,size,sortBy,sortOrder` + `@ModelAttribute XxxFilter` |
| GET | `/xxx/select` | 200 `List<XxxSelectPojo>` | solo activos, para combos (patrón `/parameter/select`) |
| POST | `/xxx` | **201** entidad o Pojo | `@Valid @RequestBody XxxDto` |
| PUT | `/xxx/{id}` | 200 | |
| DELETE | `/xxx/{id}` | **204** | catálogo ⇒ `softDelete` |
| PATCH | `/xxx/{id}/active` | 200 | reactivar/desactivar explícito (usuarios, productos) |

Ruta en **singular kebab-case** (`/table-marker`, `/payment-method`). Sin `/api/v1`.

## 4. Reglas

- **Controller delgado**: sin lógica, sin repositorios; `ResponseEntity.status(HttpStatus.X).body(...)`.
- **Service**: interfaz `extends CRUDService<Xxx, Integer>`; impl `extends CRUDServiceImpl<Xxx, Integer>`,
  `getRepository()`, `@Transactional(rollbackFor = Exception.class)` en `create/update/softDelete`.
- **Duplicados** con `existsBy...AndActiveIsTrue[AndIdNot]` → `DuplicateException("Producto", "Nombre", dto.getName())` (409).
- **Relaciones**: el servicio resuelve con el servicio de la relación (`categoryService.getById(id)`),
  valida que esté activa, y pasa la entidad al mapper. Nunca `new Category(id)`.
- **Mapper**: `fromDto(dto, found, ...relaciones)` reutiliza `found` en update. Sin MapStruct/ModelMapper.
- **Lecturas** como `XxxPojo` vía `SELECT new ...` (incluye `createdBy, updatedBy, createdAt, updatedAt` si extiende `BasePojo`).
- **Paginado**: lista blanca de `sortBy` con `SortUtil`; `PagePojoMapper.convertToPagePojo(page)`.
- **Borrado**: catálogo ⇒ `softDelete` (`active=false`); antes `checkIfElementsExist(entity.hasDependencies())`
  solo si el borrado fuese físico. Lo histórico no se borra.
- **Dinero**: `BigDecimal`, `@DecimalMin("0.00")`, `@Digits(integer=10, fraction=2)`, `MoneyUtil.round` antes de guardar.
- **Mensajes** en español por clave; nunca texto literal en anotaciones.
- **Permisos**: `PermissionSeeder` crea el permiso; tú **asignas** a CAJERO/COCINA vía migración de datos si corresponde.
  Rutas públicas o solo-autenticadas → `RequestMatcherUtil` (justificar).
- **No inventar** endpoints extra, campos, ni respuestas. **No** añadir `@PreAuthorize`, envoltorios ni librerías.
- **Test obligatorio** (regla de la referencia): cada endpoint con happy path + ≥1 error (400/404/409).

## 5. Checklist

- [ ] Estructura de paquetes exacta (`data/`, `domain/service/interfaces|implement`, `presentation/...`).
- [ ] Repository extiende `GenericRepository<Xxx, Integer>`; queries JPQL a Pojo; `exists...` para duplicados.
- [ ] Service interfaz + impl; `@Transactional(rollbackFor = Exception.class)` en escrituras.
- [ ] DTO con Jakarta + `@JsonDeserialize` estrictos + claves de mensaje creadas.
- [ ] Controller con códigos 200/201/204 correctos; `@Valid`.
- [ ] Nombre español en `SpanishEntityNameProvider.NAME_MAP`.
- [ ] Permisos asignados a roles (migración) si no es solo ADMIN.
- [ ] `XxxControllerTest` con `MockMvcTestSupport`; `XxxServiceImplTest` si hay reglas de negocio.
- [ ] `.\gradlew.bat test` en verde.
