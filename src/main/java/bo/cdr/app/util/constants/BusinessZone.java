package bo.cdr.app.util.constants;

import java.time.ZoneId;

/** Zona horaria del negocio (fecha del correlativo diario de pedidos, reportes). */
public final class BusinessZone {

    public static final ZoneId ZONE = ZoneId.of("America/La_Paz");

    private BusinessZone() {}
}
