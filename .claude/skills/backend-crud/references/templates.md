# Plantillas backend-crud (ejemplo: `Addon` en `appcatalog`)

Sustituye `Addon/addon/Adicional` por la entidad real. Todas derivan del slice Holiday de
`../ticket-backend`. Paquete base `bo.cdr.app`.

## 1. Repository — `appcatalog/data/AddonRepository.java`

```java
@Repository
public interface AddonRepository extends GenericRepository<Addon, Integer> {

    boolean existsByNameIgnoreCaseAndActiveIsTrue(String name);

    boolean existsByNameIgnoreCaseAndActiveIsTrueAndIdNot(String name, Integer id);

    @Query("""
        SELECT new bo.cdr.app.appcatalog.presentation.response.pojo.AddonPojo(
            a.id, a.name, a.price, a.active, a.createdBy, a.updatedBy, a.createdAt, a.updatedAt)
        FROM Addon a
        ORDER BY a.name
    """)
    List<AddonPojo> findAllAddonPojo();

    @Query("""
        SELECT new bo.cdr.app.appcatalog.presentation.response.pojo.AddonPojo(
            a.id, a.name, a.price, a.active, a.createdBy, a.updatedBy, a.createdAt, a.updatedAt)
        FROM Addon a WHERE a.id = :id
    """)
    Optional<AddonPojo> getAddonPojoById(@Param("id") Integer id);

    @Query("""
        SELECT new bo.cdr.app.appcatalog.presentation.response.pojo.AddonPojo(
            a.id, a.name, a.price, a.active, a.createdBy, a.updatedBy, a.createdAt, a.updatedAt)
        FROM Addon a
        WHERE (:some IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :some, '%')))
          AND (:active IS NULL OR a.active = :active)
    """)
    Page<AddonPojo> getFilterAddon(@Param("some") String some, @Param("active") Boolean active, Pageable pageable);
}
```

## 2. Mapper — `appcatalog/domain/mapper/AddonMapper.java`

```java
@Component
public class AddonMapper {
    public Addon fromDto(AddonDto dto, Addon addonFound) {
        Addon addon = addonFound != null ? addonFound : new Addon();
        addon.setName(dto.getName().trim());
        addon.setPrice(MoneyUtil.round(dto.getPrice()));
        return addon;
    }
}
```

## 3. Service — interfaz e implementación

```java
public interface AddonService extends CRUDService<Addon, Integer> {
    Addon create(AddonDto dto);
    Addon update(Integer id, AddonDto dto);
    List<AddonPojo> getAllAddonPojo();
    AddonPojo getAddonPojoById(Integer id);
    PagePojo<AddonPojo> getPageable(int page, int size, String sortBy, String sortOrder, AddonFilter filter);
    void softDelete(Integer id);
}
```

```java
@Service
@RequiredArgsConstructor
public class AddonServiceImpl extends CRUDServiceImpl<Addon, Integer> implements AddonService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "name", "price", "createdAt");

    private final AddonRepository addonRepository;
    private final AddonMapper addonMapper;
    private final PagePojoMapper pagePojoMapper;

    @Override
    protected GenericRepository<Addon, Integer> getRepository() {
        return addonRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Addon create(AddonDto dto) {
        validateDuplicate(dto, null);
        return addonRepository.save(addonMapper.fromDto(dto, null));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Addon update(Integer id, AddonDto dto) {
        Addon addonFound = getById(id);
        validateDuplicate(dto, id);
        return addonRepository.save(addonMapper.fromDto(dto, addonFound));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void softDelete(Integer id) {
        Addon addon = getById(id);
        addon.setActive(false);
        addonRepository.save(addon);
    }

    @Override
    public List<AddonPojo> getAllAddonPojo() {
        return addonRepository.findAllAddonPojo();
    }

    @Override
    public AddonPojo getAddonPojoById(Integer id) {
        return addonRepository.getAddonPojoById(id)
                .orElseThrow(() -> new EntityNotFoundException("Adicional", id));
    }

    @Override
    @Transactional(readOnly = true)
    public PagePojo<AddonPojo> getPageable(int page, int size, String sortBy, String sortOrder, AddonFilter filter) {
        Sort sort = SortUtil.build(sortBy, sortOrder, SORTABLE_FIELDS);   // lista blanca
        Page<AddonPojo> result = addonRepository.getFilterAddon(filter.getSome(), filter.getActive(),
                PageRequest.of(page, size, sort));
        return pagePojoMapper.convertToPagePojo(result);
    }

    private void validateDuplicate(AddonDto dto, Integer excludeId) {
        boolean exists = excludeId == null
                ? addonRepository.existsByNameIgnoreCaseAndActiveIsTrue(dto.getName().trim())
                : addonRepository.existsByNameIgnoreCaseAndActiveIsTrueAndIdNot(dto.getName().trim(), excludeId);
        if (exists) {
            throw new DuplicateException("Adicional", "Nombre", dto.getName());
        }
    }
}
```

## 4. DTO — `presentation/request/dto/AddonDto.java`

```java
@Data
@NoArgsConstructor
public class AddonDto {

    @NotBlank(message = "{addon.name.not-blank}")
    @JsonDeserialize(using = StringOnlyDeserializer.class)
    @Size(min = 2, max = 100, message = "{addon.name.size}")
    private String name;

    @NotNull(message = "{addon.price.not-null}")
    @DecimalMin(value = "0.00", message = "{addon.price.min}")
    @Digits(integer = 10, fraction = 2, message = "{addon.price.digits}")
    private BigDecimal price;
}
```

`validation-messages.properties`:
```properties
addon.name.not-blank=El nombre del adicional es obligatorio.
addon.name.size=El nombre del adicional debe tener entre {min} y {max} caracteres.
addon.price.not-null=El precio del adicional es obligatorio.
addon.price.min=El precio del adicional no puede ser negativo.
addon.price.digits=El precio admite como máximo {integer} enteros y {fraction} decimales.
```

## 5. Pojo y Filter

```java
@EqualsAndHashCode(callSuper = true)
@Pojo
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddonPojo extends BasePojo {
    private Integer id;
    private String name;
    private BigDecimal price;
    private Boolean active;

    public AddonPojo(Integer id, String name, BigDecimal price, Boolean active,
                     String createdBy, String updatedBy, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id; this.name = name; this.price = price; this.active = active;
        this.setCreatedBy(createdBy); this.setUpdatedBy(updatedBy);
        this.setCreatedAt(createdAt); this.setUpdatedAt(updatedAt);
    }
}
```

```java
@Data
@NoArgsConstructor
public class AddonFilter {
    private String some;      // búsqueda global (mismo nombre que la referencia)
    private Boolean active;
}
```

## 6. Controller — `presentation/controller/AddonController.java`

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/addon")
public class AddonController {

    private final AddonService addonService;

    @GetMapping
    public ResponseEntity<List<AddonPojo>> getAll() {
        return ResponseEntity.status(HttpStatus.OK).body(addonService.getAllAddonPojo());
    }

    @GetMapping("/pageable")
    public ResponseEntity<PagePojo<AddonPojo>> getPageable(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @ModelAttribute AddonFilter filter) {
        return ResponseEntity.ok(addonService.getPageable(page, size, sortBy, sortOrder, filter));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AddonPojo> getById(@PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.OK).body(addonService.getAddonPojoById(id));
    }

    @PostMapping
    public ResponseEntity<Addon> create(@Valid @RequestBody AddonDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addonService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Addon> update(@PathVariable Integer id, @Valid @RequestBody AddonDto dto) {
        return ResponseEntity.status(HttpStatus.OK).body(addonService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        addonService.softDelete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
```

> Devolver la entidad en POST/PUT es el patrón de la referencia. Hazlo **solo** si la entidad no tiene
> relaciones perezosas ni colecciones sin `@JsonIgnore`; si no, devuelve el `XxxPojo`.

## 7. Test — `XxxControllerTest` (patrón `HolidayControllerTest`)

```java
@ExtendWith(MockitoExtension.class)
class AddonControllerTest {

    @Mock
    AddonService addonService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcTestSupport.buildMockMvc(new AddonController(addonService));
    }

    private static final String VALID_JSON = """
            { "name": "Leche de almendra", "price": 3.00 }
            """;

    @Test
    void getAll_returns200() throws Exception {
        when(addonService.getAllAddonPojo()).thenReturn(List.of(new AddonPojo()));
        mockMvc.perform(get("/addon")).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
    }

    @Test
    void getById_returns404_whenNotFound() throws Exception {
        when(addonService.getAddonPojoById(99)).thenThrow(new EntityNotFoundException("Adicional", 99));
        mockMvc.perform(get("/addon/99")).andExpect(status().isNotFound());
    }

    @Test
    void create_returns201_whenBodyValid() throws Exception {
        when(addonService.create(any(AddonDto.class))).thenReturn(new Addon());
        mockMvc.perform(post("/addon").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
               .andExpect(status().isCreated());
    }

    @Test
    void create_returns400_whenRequiredFieldsMissing() throws Exception {
        mockMvc.perform(post("/addon").contentType(MediaType.APPLICATION_JSON).content("{}"))
               .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns409_whenDuplicate() throws Exception {
        when(addonService.create(any(AddonDto.class)))
                .thenThrow(new DuplicateException("Adicional", "Nombre", "Leche de almendra"));
        mockMvc.perform(post("/addon").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
               .andExpect(status().isConflict());
    }

    @Test
    void delete_returns204() throws Exception {
        doNothing().when(addonService).softDelete(1);
        mockMvc.perform(delete("/addon/1")).andExpect(status().isNoContent());
    }
}
```

Si el DTO usa un validador que inyecta beans de Spring, usar
`MockMvcTestSupport.buildMockMvc(customConstraintValidatorFactory, controller)` (ver CLAUDE.md de la referencia).

## 8. Asignar permisos a roles — `V{n}__seed_permissions_addon.sql`

Flyway corre **antes** que `PermissionSeeder` y cada migración se ejecuta **una sola vez**, así que
una migración no puede depender de permisos que el seeder todavía no creó. Por eso la migración
**inserta el permiso ella misma** (con el mismo `code` que generaría el seeder, que luego lo omite
porque `existsByCode` = true) y a continuación lo asigna al rol. Ejemplo (CAJERO solo lectura):

```sql
INSERT INTO security.permission (http_method, url, url_without_ids, code, name, description)
VALUES ('GET', '/addon',          '/addon',          'GET__ADDON',          'GET /addon',          'Permiso para GET /addon'),
       ('GET', '/addon/pageable', '/addon/pageable', 'GET__ADDON_PAGEABLE', 'GET /addon/pageable', 'Permiso para GET /addon/pageable')
ON CONFLICT (code) DO NOTHING;

INSERT INTO security.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM security.role r
JOIN security.permission p ON p.code IN ('GET__ADDON', 'GET__ADDON_PAGEABLE')
WHERE r.code = 'CAJERO'
ON CONFLICT DO NOTHING;
```
> El `code` debe ser **idéntico** al de `PermissionSeeder.generateCode` (copiado de la referencia:
> `method + "_" + url.replace("/", "_")…toUpperCase()` → `GET` + `_` + `_ADDON` = `GET__ADDON`).
> Requiere `UNIQUE(code)` en `security.permission`. Endpoints solo para ADMIN no necesitan migración
> (ADMIN omite la comprobación).
