-- =====================================================================
-- V6: Etapa 4 — parámetros de demora de cocina y permisos de tablero,
-- cambios de estado, cancelación e historial de pedidos.
-- =====================================================================

-- Nueva carpeta de parámetros: COCINA
ALTER TABLE parameters DROP CONSTRAINT ck_parameters_folder;
ALTER TABLE parameters ADD CONSTRAINT ck_parameters_folder CHECK (folder IN ('SYSTEM', 'DISCOUNTS', 'KITCHEN'));

INSERT INTO parameters (folder, code, name, value, value_type, description, editable, created_by, created_at) VALUES
    ('KITCHEN', 'KITCHEN_DELAY_WARNING_MINUTES', 'Pedido demorado (min)', '10', 'NUMBER',
     'Minutos de espera desde el registro a partir de los cuales el tablero de cocina marca el pedido en amarillo.', TRUE, 'system', now()),
    ('KITCHEN', 'KITCHEN_DELAY_CRITICAL_MINUTES', 'Pedido muy demorado (min)', '20', 'NUMBER',
     'Minutos de espera desde el registro a partir de los cuales el tablero de cocina marca el pedido en rojo.', TRUE, 'system', now());

-- Permisos (mismo código que generaría PermissionSeeder; ADMIN no necesita filas)
INSERT INTO security.permission (http_method, url, url_without_ids, code, name, description, created_by, created_at) VALUES
    ('GET',   '/order/board',            '/order/board',            'GET__ORDER_BOARD',            'GET /order/board',            'Permiso para GET /order/board',            'system', now()),
    ('GET',   '/order/kitchen-settings', '/order/kitchen-settings', 'GET__ORDER_KITCHEN-SETTINGS', 'GET /order/kitchen-settings', 'Permiso para GET /order/kitchen-settings', 'system', now()),
    ('PATCH', '/order/{id}/status',      '/order/status',           'PATCH__ORDER_STATUS',         'PATCH /order/status',         'Permiso para PATCH /order/{id}/status',    'system', now()),
    ('POST',  '/order/{id}/cancel',      '/order/cancel',           'POST__ORDER_CANCEL',          'POST /order/cancel',          'Permiso para POST /order/{id}/cancel',     'system', now()),
    ('GET',   '/order/pageable',         '/order/pageable',         'GET__ORDER_PAGEABLE',         'GET /order/pageable',         'Permiso para GET /order/pageable',         'system', now())
ON CONFLICT (code) DO NOTHING;

-- COCINA: tablero, umbrales y cambios de estado (qué transición puede hacer lo decide el servicio)
INSERT INTO security.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM security.role r
JOIN security.permission p ON p.code IN ('GET__ORDER_BOARD', 'GET__ORDER_KITCHEN-SETTINGS', 'PATCH__ORDER_STATUS')
WHERE r.code = 'COCINA'
ON CONFLICT DO NOTHING;

-- CAJERO: seguimiento (entregar), cancelación e historial
INSERT INTO security.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM security.role r
JOIN security.permission p ON p.code IN ('GET__ORDER_BOARD', 'PATCH__ORDER_STATUS', 'POST__ORDER_CANCEL', 'GET__ORDER_PAGEABLE')
WHERE r.code = 'CAJERO'
ON CONFLICT DO NOTHING;
