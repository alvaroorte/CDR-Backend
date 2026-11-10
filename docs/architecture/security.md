# Seguridad (cdr-backend)

Replica el modelo de `../ticket-backend/appsecurity` **sin** LDAP, reCAPTCHA, licencia, API externa ni FCM.

## 1. Autenticación

- **Login** `POST /login` `{ username, password }` → `{ token, refreshToken, username, message }`.
  - Solo `DaoAuthenticationProvider` contra `security.user_app` con **BCrypt** (en la referencia era el
    *fallback* tras LDAP).
  - La referencia enviaba el password en base64 (`btoa`) desde el frontend: se **mantiene** por
    compatibilidad con el patrón (no es cifrado; la protección real es HTTPS).
  - Usuario inactivo (`active=false`) → 401 "Usuario deshabilitado".
  - Rate limit por IP+usuario con Caffeine (`LoginRateLimiterService` de la referencia) → 429.
- **JWT** (JJWT 0.13, HS256, secreto base64 `app.jwt.secret=${APP_JWT_SECRET}`):
  claims `sub`=username, `authorities` (códigos de rol), `isAdmin`, `userId`. Expiración: *decisión
  pendiente* (la referencia usa 24 h; para un POS compartido se sugiere un turno, p. ej. 12 h).
- **Refresh token** opaco, guardado con hash SHA-256 en `security.refresh_token` (7 días), endpoints
  `POST /auth/refresh` y `POST /auth/logout` (revoca). Igual que la referencia.
- Sesión `STATELESS`, CSRF deshabilitado, `CustomAuthenticationEntryPoint` (401) y
  `CustomAccessDeniedHandler` (403) con cuerpo `ErrorResponse`.

## 2. Cadena de filtros

```
CORS (servlet filter, @Order(HIGHEST_PRECEDENCE), orígenes de app.cors.allowed-origins)
  → JwtAuthenticationFilter   (antes de UsernamePasswordAuthenticationFilter)
  → AuthorizationFilter       (después del AuthorizationFilter de Spring)
```

Usuario actual: `JwtService.getCurrentUser()`, `getCurrentUsername()`, `getCurrentUserId()`,
`getCurrentAuthorities()` (copiados de la referencia). `CustomUserDetails{username, userId, authorities}`.

## 3. Autorización — RBAC en base de datos (patrón de la referencia)

Tablas `security.role`, `security.permission(http_method, url, url_without_ids, code, name, description)`,
`security.role_permission`, `security.user_role`.

1. **`PermissionSeeder`** (`CommandLineRunner`) recorre `RequestMappingHandlerMapping` al arrancar y
   crea un `Permission` por cada método+URL que no exista (código `GET__PRODUCT`, `PATCH__ORDER_STATUS`...).
   → Un endpoint nuevo genera su permiso solo; **hay que asignarlo a los roles**.
2. **Asignación inicial** de permisos a `CAJERO` y `COCINA`: migración Flyway de datos
   (`V{n}__seed_permissions_xxx.sql`). Como Flyway corre **antes** que el seeder y una sola vez, la
   migración **inserta el permiso con el mismo `code`** que generaría el seeder (`ON CONFLICT (code) DO NOTHING`)
   y luego la fila de `role_permission`; el seeder lo encuentra y no lo duplica. Plantilla en la skill
   `backend-crud` §8. Cambios posteriores de permisos: pantalla de Roles (ADMIN).
3. **`AuthorizationFilter`** normaliza la URL (quita context path y segmentos numéricos) y llama
   `userService.hasPermission(userId, method, url)`. Rol **`ADMIN` omite la comprobación** (en la referencia `ADM`).
4. **`RequestMatcherUtil`**:
   - `TOKEN_NOT_REQUIRED`: `/login`, `/auth/refresh`, `/auth/logout`, `/error`, `/ws-cdr/**`
     (el handshake es público; la autenticación ocurre en el frame CONNECT, ver §5),
     `/v3/api-docs/**`, `/swagger-ui/**` (swagger solo en dev).
   - `PERMISSION_NOT_REQUIRED` (solo autenticado): `/users/me`, `/users/{id}/roles` del propio usuario,
     `/cash-session/current`, `/pos/catalog`.

### Reglas finas (en el servicio, además del permiso por URL)

El permiso por URL dice *quién puede llamar*; el servicio decide *qué puede hacer*:

| Regla | Dónde |
|---|---|
| Límite de descuento CAJERO / prohibido COCINA | `OrderPricingService.validateDiscount(...)` |
| Transiciones de estado por rol | `OrderStateMachine.assertTransition(from, to, roles)` |
| CAJERO cancela solo PENDIENTE | `OrderServiceImpl.cancel(...)` |
| Operar solo sobre **su** sesión de caja (salvo ADMIN) | `CashSessionServiceImpl` |
| Un usuario no se desactiva a sí mismo ni quita el último ADMIN | `UserServiceImpl` |

Los roles se leen de `jwtService.getCurrentAuthorities()` — **nunca** de un campo del request.

## 4. Roles iniciales

| Código | Descripción | Acceso típico |
|---|---|---|
| `ADMIN` | Administrador | Todo (bypass de permisos) |
| `CAJERO` | Cajero | POS, pedidos (crear, ver, entregar, cancelar PENDIENTE), caja propia, catálogo lectura, disponibilidad de producto |
| `COCINA` | Cocina | Tablero, cambio de estado a EN_PREPARACION / LISTO, detalle de pedido |

Constantes en `util/constants/RoleCodes`.

## 5. WebSocket (desviación aprobada: la referencia no autenticaba)

- Endpoint `/ws-cdr` con SockJS (transports websocket + xhr, igual que `WebSocketConfig` de la referencia).
- `WebSocketAuthInterceptor implements ChannelInterceptor`, registrado en
  `configureClientInboundChannel(registration)`:
  - `CONNECT`: lee `Authorization` de los native headers, valida con `JwtService`, crea un
    `UsernamePasswordAuthenticationToken` y lo asigna con `accessor.setUser(auth)`. Sin token válido → rechaza.
  - `SUBSCRIBE`: verifica rol según destino (`/topic/orders` → ADMIN, CAJERO, COCINA;
    `/topic/catalog` → ADMIN, CAJERO). No autorizado → `AccessDeniedException` (frame ERROR).
  - `SEND` a `/app/**`: no se usa en v1 → rechazar.
- Expiración del token con la conexión abierta: el cliente reconecta con el token refrescado; el
  servidor no corta conexiones vivas en v1 (*documentado como limitación*).

## 6. Otros

- CORS: filtro propio de la referencia (`util/CORS`), lista blanca por propiedad.
- Secretos: **solo** variables de entorno (`APP_JWT_SECRET`, `DB_PASSWORD`, `APP_ADMIN_INITIAL_PASSWORD`).
- Swagger deshabilitado en `prod` (`springdoc.api-docs.enabled=false`).
- Imágenes base64: validar tipo (`@IsImageBase64`) y tamaño máximo (*valor pendiente*, sugerido 1 MB).
