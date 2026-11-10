-- =====================================================================
-- V2: roles iniciales aprobados (ADMIN, CAJERO, COCINA).
-- El usuario ADMIN inicial lo crea AdminBootstrap en el primer arranque
-- (contraseña desde APP_ADMIN_INITIAL_PASSWORD), nunca en una migración.
-- =====================================================================
INSERT INTO security.role (name, code, description, created_by, created_at) VALUES
    ('Administrador', 'ADMIN',  'Acceso completo al sistema',                                  'system', now()),
    ('Cajero',        'CAJERO', 'Registro de pedidos, pagos y operaciones de caja',            'system', now()),
    ('Cocina',        'COCINA', 'Visualización y actualización del estado de preparación',     'system', now());
