package bo.cdr.app.util.constants;

/** Códigos de rol (tabla security.role). Deben coincidir con V2__seed_roles.sql y con el frontend. */
public final class RoleCodes {

    public static final String ADMIN = "ADMIN";
    public static final String CAJERO = "CAJERO";
    public static final String COCINA = "COCINA";

    /** Roles del sistema: el código depende de ellos, no se borran ni cambian de código. */
    public static final java.util.Set<String> SYSTEM_ROLES = java.util.Set.of(ADMIN, CAJERO, COCINA);

    private RoleCodes() {}
}
