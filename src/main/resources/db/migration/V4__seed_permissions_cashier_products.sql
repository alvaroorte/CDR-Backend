-- =====================================================================
-- V4: permisos del CAJERO sobre productos (Etapa 2, decisión aprobada):
-- puede ver el listado de productos y marcarlos agotados/disponibles.
-- Flyway corre antes que PermissionSeeder: se insertan los permisos con el
-- mismo código que generaría el seeder (ver skill backend-crud §8).
-- ADMIN no necesita filas: AuthorizationFilter lo omite.
-- =====================================================================
INSERT INTO security.permission (http_method, url, url_without_ids, code, name, description, created_by, created_at) VALUES
    ('GET',   '/product',                   '/product',              'GET__PRODUCT',              'GET /product',                   'Permiso para GET /product',                   'system', now()),
    ('PATCH', '/product/{id}/availability', '/product/availability', 'PATCH__PRODUCT_AVAILABILITY', 'PATCH /product/availability',  'Permiso para PATCH /product/{id}/availability', 'system', now())
ON CONFLICT (code) DO NOTHING;

INSERT INTO security.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM security.role r
JOIN security.permission p ON p.code IN ('GET__PRODUCT', 'PATCH__PRODUCT_AVAILABILITY')
WHERE r.code = 'CAJERO'
ON CONFLICT DO NOTHING;
