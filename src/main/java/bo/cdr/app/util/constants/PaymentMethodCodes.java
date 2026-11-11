package bo.cdr.app.util.constants;

/** Códigos de medios de pago que el código usa (deben coincidir con V3__catalog.sql; el código no se edita). */
public final class PaymentMethodCodes {

    /** Efectivo: suma al efectivo esperado de la caja. */
    public static final String CASH = "EFECTIVO";

    private PaymentMethodCodes() {}
}
