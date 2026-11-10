# Convenciones de código (cdr-backend)

Tomadas del código de `../ticket-backend`. Donde la referencia era inconsistente se elige **una**
forma y se indica.

## 1. Idioma

| Elemento | Idioma | Ejemplo |
|---|---|---|
| Clases, métodos, campos, variables, tablas, columnas | Inglés | `OrderItem.unitPrice`, `order_items.unit_price` |
| Mensajes al usuario (excepciones, validaciones) | Español | `"El número de mesa 5 está en uso"` |
| Etiquetas de enums (`getName()`) | Español | `OrderStatusEnum.PENDING("PENDIENTE")` |
| Comentarios, Javadoc, documentación, planes | Español | |

> Los **estados del pedido** se exponen en la API con sus etiquetas españolas
> (`PENDIENTE`, `EN_PREPARACION`, `LISTO`, `ENTREGADO`, `CANCELADO`), igual que la referencia expone
> `HolidayTypeEnum.getName()`. En BD se guarda el `name()` del enum (`STRING`).

## 2. Sufijos y nombres

| Tipo | Sufijo / forma | Paquete |
|---|---|---|
| Entidad | `Product` (singular, sin sufijo) | `domain/entity` |
| DTO de entrada | `ProductDto` (no `DTO`) | `presentation/request/dto` |
| Filtro de listado | `ProductFilter` | `presentation/request/filter` |
| Respuesta | `ProductPojo` (`@Data`, `extends BasePojo` si lleva auditoría) | `presentation/response/pojo` |
| Respuesta inmutable pequeña | `XxxRecord` (record Java) — solo en `appsecurity`, como la referencia | `presentation/response/record` |
| Proyección Spring Data | `XxxProjection` (bien escrito; la referencia tiene `Proyection`) | `presentation/response/pojo/projection` |
| Mapper | `ProductMapper` (`@Component`) | `domain/mapper` |
| Servicio | `ProductService` / `ProductServiceImpl` | `domain/service/interfaces` / `implement` |
| Repositorio | `ProductRepository`; nativo `XxxRepositoryNative` | `data` |
| Controlador | `ProductController` | `presentation/controller` |
| Enum | `XxxEnum` con etiqueta española y `fromString` | `common/enums` |
| Excepción | `XxxException extends RuntimeException` | `exception` |
| Constantes | clase `final` con `public static final` | `util/constants` (`RoleCodes`, `ParameterCodes`, `Message`) |

Métodos de servicio: `create(dto)`, `update(id, dto)`, `getAllXxxPojo()`, `getXxxPojoById(id)`,
`getPageable(page,size,sortBy,sortOrder,filter)`, `softDelete(id)`, verbos de negocio en inglés
(`changeStatus`, `cancel`, `open`, `close`, `quote`).

## 3. Lombok

- Entidades: `@EqualsAndHashCode(onlyExplicitlyIncluded = true) @Entity @NoArgsConstructor @Data @Table(name="...")`
  con `@EqualsAndHashCode.Include` sobre `id` (forma mayoritaria de la referencia). Las entidades con
  colecciones `@OneToMany` usan `@Getter @Setter` en lugar de `@Data` (evita `toString` recursivo).
- DTO / Pojo: `@Data @NoArgsConstructor` (+ `@AllArgsConstructor` o constructor explícito para JPQL).
- Servicios / controladores: `@RequiredArgsConstructor` (preferido) o `@AllArgsConstructor`, campos `private final`.
- Log: `@Slf4j`.

## 4. Enums (patrón de `HolidayTypeEnum`)

```java
@Getter
public enum OrderStatusEnum {
    PENDING("PENDIENTE"), IN_PREPARATION("EN_PREPARACION"), READY("LISTO"),
    DELIVERED("ENTREGADO"), CANCELLED("CANCELADO");

    private final String name;
    OrderStatusEnum(String name) { this.name = name; }

    private static final Map<String, OrderStatusEnum> lookup = new HashMap<>();
    static { for (OrderStatusEnum s : values()) lookup.put(s.getName(), s); }

    public static OrderStatusEnum fromString(String text) {
        if (text == null || !lookup.containsKey(text.toUpperCase()))
            throw new InvalidEnumValueException("Estado de pedido", text);
        return lookup.get(text.toUpperCase());
    }
}
```
En la entidad: `@Enumerated(EnumType.STRING) @Column(length = 30)` **siempre**.
En el DTO: `String` + `@ValidEnum(enumClass = OrderStatusEnum.class, message = "{...}")`.

## 5. Validación de DTOs

- Jakarta (`@NotNull`, `@NotBlank`, `@Size`, `@Positive`, `@DecimalMin`, `@Digits`) con mensaje por
  clave: `message = "{product.price.not-null}"` → `validation-messages.properties`.
- `@JsonDeserialize(using = StringOnlyDeserializer.class | IntegerOnlyDeserializer.class | IntegerListDeserializer.class)`
  para rechazar coerciones de tipo (copiados de la referencia, Jackson 3).
- Importes: `BigDecimal` + `@DecimalMin("0.00") @Digits(integer = 10, fraction = 2)`.
- FKs como `Integer xxxId` con `@Positive`.
- Imágenes: `String imageBase64` + `@IsImageBase64` (copiado de la referencia).
- Validaciones que dependen de BD (duplicados, existencia) van **en el servicio**, no en validadores.

## 6. Estilo general

- Sin lógica en controllers; sin acceso a repositorios fuera de su servicio (otros servicios usan el servicio).
- Sin `Optional` como parámetro ni campo; sí como retorno de repositorio.
- Sin `System.out`; `log.info` para operaciones críticas (pedido creado, cancelado, caja abierta/cerrada).
- No dejar código comentado ni imports sin usar.
- Fecha/hora: `LocalDateTime` con `Clock` inyectado (`ClockConfig`) para que los tests sean deterministas.
- Jackson 3: `tools.jackson.databind.*`; anotaciones `com.fasterxml.jackson.annotation.*`.
