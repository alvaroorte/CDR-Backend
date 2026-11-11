package bo.cdr.app.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Único punto de redondeo monetario (skill cdr-order-management §2): escala 2, HALF_UP.
 * Nunca usar double/float para importes.
 */
public final class MoneyUtil {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private MoneyUtil() {}

    public static BigDecimal round(BigDecimal value) {
        return value == null ? null : value.setScale(SCALE, ROUNDING);
    }

    /** base × pct / 100, redondeado. */
    public static BigDecimal percentOf(BigDecimal base, BigDecimal percent) {
        return round(base.multiply(percent).divide(ONE_HUNDRED, SCALE + 4, ROUNDING));
    }
}
