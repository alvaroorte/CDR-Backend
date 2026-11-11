package bo.cdr.app.appreport.data;

import bo.cdr.app.apporder.domain.entity.Order;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Consultas de reportes (SQL nativo con CTE, como los reportes de la referencia). Solo lectura.
 * <p>
 * Reglas (skill cdr-order-management §12): se leen <b>solo fotografías</b> ({@code orders},
 * {@code order_items}, {@code payments}, {@code cash_session_summaries}), nunca el precio actual del
 * catálogo; los CANCELADOS se excluyen salvo en estados y cancelaciones; el rango es por
 * {@code business_date} (fecha de negocio America/La_Paz).
 * <p>
 * Filtros opcionales con centinela ({@code 0} / {@code ''} = sin filtro) para no enlazar parámetros
 * nulos sin tipo en PostgreSQL. Los alias van entre comillas para conservar el camelCase de las proyecciones.
 */
public interface ReportRepository extends Repository<Order, Integer> {

    String FROM_ORDERS = " FROM orders o JOIN cash_sessions cs ON cs.id = o.cash_session_id ";

    String ORDER_FILTER = """
             WHERE o.business_date BETWEEN :dateFrom AND :dateTo
               AND (:userId = 0 OR cs.user_id = :userId)
               AND (:serviceType = '' OR o.service_type = :serviceType)
            """;

    String NOT_CANCELLED = " AND o.status <> 'CANCELLED' ";

    // ------------------------------------------------------------------ Ventas

    @Query(value = "WITH f AS (SELECT o.id, o.status, o.subtotal, o.discount_amount, o.total" + FROM_ORDERS + ORDER_FILTER + """
            )
            SELECT COUNT(*) FILTER (WHERE f.status <> 'CANCELLED')                          AS "orderCount",
                   COALESCE(SUM(f.subtotal) FILTER (WHERE f.status <> 'CANCELLED'), 0)        AS "gross",
                   COALESCE(SUM(f.discount_amount) FILTER (WHERE f.status <> 'CANCELLED'), 0) AS "discount",
                   COALESCE(SUM(f.total) FILTER (WHERE f.status <> 'CANCELLED'), 0)           AS "net",
                   COUNT(*) FILTER (WHERE f.status = 'CANCELLED')                             AS "cancelledCount",
                   COALESCE(SUM(f.total) FILTER (WHERE f.status = 'CANCELLED'), 0)            AS "cancelledAmount",
                   (SELECT COALESCE(SUM(p.amount), 0) FROM payments p JOIN f f2 ON f2.id = p.order_id
                     WHERE p.status = 'PAID')                                                 AS "paid",
                   (SELECT COALESCE(SUM(p.amount), 0) FROM payments p JOIN f f2 ON f2.id = p.order_id
                     WHERE p.status = 'REFUNDED')                                             AS "refunded"
            FROM f
            """, nativeQuery = true)
    SalesSummaryRow salesSummary(@Param("dateFrom") LocalDate dateFrom, @Param("dateTo") LocalDate dateTo,
                                 @Param("userId") Integer userId, @Param("serviceType") String serviceType);

    /** {@code unit} sale de {@code ReportGroupByEnum} (lista cerrada), nunca del cliente. */
    @Query(value = """
            SELECT CAST(date_trunc(:unit, CAST(o.business_date AS TIMESTAMP)) AS DATE) AS "periodStart",
                   COUNT(*)                AS "orderCount",
                   SUM(o.subtotal)         AS "gross",
                   SUM(o.discount_amount)  AS "discount",
                   SUM(o.total)            AS "net"
            """ + FROM_ORDERS + ORDER_FILTER + NOT_CANCELLED + """
            GROUP BY 1
            ORDER BY 1
            """, nativeQuery = true)
    List<SalesPeriodRow> salesByPeriod(@Param("unit") String unit,
                                       @Param("dateFrom") LocalDate dateFrom, @Param("dateTo") LocalDate dateTo,
                                       @Param("userId") Integer userId, @Param("serviceType") String serviceType);

    @Query(value = """
            SELECT o.service_type AS "serviceType", COUNT(*) AS "orderCount", SUM(o.total) AS "net"
            """ + FROM_ORDERS + ORDER_FILTER + NOT_CANCELLED + """
            GROUP BY o.service_type
            ORDER BY 3 DESC
            """, nativeQuery = true)
    List<ServiceTypeRow> salesByServiceType(@Param("dateFrom") LocalDate dateFrom, @Param("dateTo") LocalDate dateTo,
                                            @Param("userId") Integer userId, @Param("serviceType") String serviceType);

    // ------------------------------------------------------------------ Productos y categorías

    /** Nombre = la fotografía más reciente (un producto renombrado aparece una sola vez). */
    @Query(value = """
            SELECT oi.product_id                                              AS "productId",
                   (ARRAY_AGG(oi.product_name ORDER BY o.id DESC))[1]          AS "productName",
                   (ARRAY_AGG(oi.category_name ORDER BY o.id DESC))[1]         AS "categoryName",
                   SUM(oi.quantity)                                           AS "quantity",
                   COUNT(DISTINCT o.id)                                       AS "orderCount",
                   SUM(oi.line_subtotal)                                      AS "gross"
            FROM order_items oi JOIN orders o ON o.id = oi.order_id JOIN cash_sessions cs ON cs.id = o.cash_session_id
            """ + ORDER_FILTER + NOT_CANCELLED + """
               AND (:categoryId = 0 OR oi.category_id = :categoryId)
            GROUP BY oi.product_id
            ORDER BY 4 DESC, 6 DESC
            """, nativeQuery = true)
    List<ProductRow> productSales(@Param("dateFrom") LocalDate dateFrom, @Param("dateTo") LocalDate dateTo,
                                  @Param("userId") Integer userId, @Param("serviceType") String serviceType,
                                  @Param("categoryId") Integer categoryId);

    @Query(value = """
            SELECT oi.category_id                                             AS "categoryId",
                   (ARRAY_AGG(oi.category_name ORDER BY o.id DESC))[1]         AS "categoryName",
                   SUM(oi.quantity)                                           AS "quantity",
                   SUM(oi.line_subtotal)                                      AS "gross"
            FROM order_items oi JOIN orders o ON o.id = oi.order_id JOIN cash_sessions cs ON cs.id = o.cash_session_id
            """ + ORDER_FILTER + NOT_CANCELLED + """
               AND (:categoryId = 0 OR oi.category_id = :categoryId)
            GROUP BY oi.category_id
            ORDER BY 4 DESC
            """, nativeQuery = true)
    List<CategoryRow> categorySales(@Param("dateFrom") LocalDate dateFrom, @Param("dateTo") LocalDate dateTo,
                                    @Param("userId") Integer userId, @Param("serviceType") String serviceType,
                                    @Param("categoryId") Integer categoryId);

    // ------------------------------------------------------------------ Medios de pago y cajas

    /** Incluye pedidos cancelados para mostrar sus devoluciones (pagos REFUNDED). */
    @Query(value = """
            SELECT pm.id                                                           AS "paymentMethodId",
                   pm.name                                                         AS "paymentMethodName",
                   COUNT(*) FILTER (WHERE p.status = 'PAID')                       AS "paidCount",
                   COALESCE(SUM(p.amount) FILTER (WHERE p.status = 'PAID'), 0)     AS "paid",
                   COUNT(*) FILTER (WHERE p.status = 'REFUNDED')                   AS "refundedCount",
                   COALESCE(SUM(p.amount) FILTER (WHERE p.status = 'REFUNDED'), 0) AS "refunded"
            FROM payments p JOIN orders o ON o.id = p.order_id JOIN cash_sessions cs ON cs.id = o.cash_session_id
                 JOIN payment_methods pm ON pm.id = p.payment_method_id
            """ + ORDER_FILTER + """
               AND (:paymentMethodId = 0 OR p.payment_method_id = :paymentMethodId)
            GROUP BY pm.id, pm.name
            ORDER BY 4 DESC
            """, nativeQuery = true)
    List<PaymentMethodRow> paymentMethods(@Param("dateFrom") LocalDate dateFrom, @Param("dateTo") LocalDate dateTo,
                                          @Param("userId") Integer userId, @Param("serviceType") String serviceType,
                                          @Param("paymentMethodId") Integer paymentMethodId);

    /** Cajas abiertas en el rango (fecha de apertura). Totales del arqueo solo si está cerrada. */
    @Query(value = """
            SELECT cs.id                     AS "id",
                   u.username                AS "username",
                   u.full_name               AS "userFullName",
                   cs.status                 AS "status",
                   cs.opened_at              AS "openedAt",
                   cs.closed_at              AS "closedAt",
                   cs.closed_by              AS "closedBy",
                   cs.opening_amount         AS "openingAmount",
                   (SELECT COALESCE(SUM(o.total), 0) FROM orders o
                     WHERE o.cash_session_id = cs.id AND o.status <> 'CANCELLED') AS "salesTotal",
                   SUM(s.expected_amount)    AS "expectedTotal",
                   SUM(s.declared_amount)    AS "declaredTotal",
                   SUM(s.difference)         AS "differenceTotal"
            FROM cash_sessions cs
                 JOIN security.user_app u ON u.id = cs.user_id
                 LEFT JOIN cash_session_summaries s ON s.cash_session_id = cs.id
            WHERE CAST(cs.opened_at AS DATE) BETWEEN :dateFrom AND :dateTo
              AND (:userId = 0 OR cs.user_id = :userId)
            GROUP BY cs.id, u.username, u.full_name
            ORDER BY cs.opened_at, cs.id
            """, nativeQuery = true)
    List<CashSessionRow> cashSessions(@Param("dateFrom") LocalDate dateFrom, @Param("dateTo") LocalDate dateTo,
                                      @Param("userId") Integer userId);

    // ------------------------------------------------------------------ Usuarios, estados, descuentos, cancelaciones

    @Query(value = """
            SELECT u.id                                                                AS "userId",
                   u.username                                                          AS "username",
                   u.full_name                                                         AS "userFullName",
                   COUNT(*) FILTER (WHERE o.status <> 'CANCELLED')                     AS "orderCount",
                   COALESCE(SUM(o.subtotal) FILTER (WHERE o.status <> 'CANCELLED'), 0) AS "gross",
                   COALESCE(SUM(o.discount_amount) FILTER (WHERE o.status <> 'CANCELLED'), 0) AS "discount",
                   COALESCE(SUM(o.total) FILTER (WHERE o.status <> 'CANCELLED'), 0)    AS "net",
                   COUNT(*) FILTER (WHERE o.status = 'CANCELLED')                      AS "cancelledCount"
            """ + FROM_ORDERS + " JOIN security.user_app u ON u.id = cs.user_id " + ORDER_FILTER + """
            GROUP BY u.id, u.username, u.full_name
            ORDER BY 7 DESC
            """, nativeQuery = true)
    List<UserRow> salesByUser(@Param("dateFrom") LocalDate dateFrom, @Param("dateTo") LocalDate dateTo,
                              @Param("userId") Integer userId, @Param("serviceType") String serviceType);

    @Query(value = """
            SELECT o.status AS "status", COUNT(*) AS "orderCount", SUM(o.total) AS "total"
            """ + FROM_ORDERS + ORDER_FILTER + """
            GROUP BY o.status
            """, nativeQuery = true)
    List<StatusRow> ordersByStatus(@Param("dateFrom") LocalDate dateFrom, @Param("dateTo") LocalDate dateTo,
                                   @Param("userId") Integer userId, @Param("serviceType") String serviceType);

    @Query(value = """
            SELECT o.id AS "orderId", o.code AS "code", o.business_date AS "businessDate", o.created_at AS "createdAt",
                   u.full_name AS "cashier", o.discount_type AS "discountType", o.discount_value AS "discountValue",
                   o.subtotal AS "subtotal", o.discount_amount AS "discountAmount", o.total AS "total",
                   o.discount_reason AS "discountReason", o.discount_applied_by AS "appliedBy"
            """ + FROM_ORDERS + " JOIN security.user_app u ON u.id = cs.user_id " + ORDER_FILTER + NOT_CANCELLED + """
               AND o.discount_amount > 0
            ORDER BY o.created_at, o.id
            """, nativeQuery = true)
    List<DiscountRow> discounts(@Param("dateFrom") LocalDate dateFrom, @Param("dateTo") LocalDate dateTo,
                                @Param("userId") Integer userId, @Param("serviceType") String serviceType);

    @Query(value = """
            SELECT o.id AS "orderId", o.code AS "code", o.business_date AS "businessDate", o.created_at AS "createdAt",
                   u.full_name AS "cashier", o.total AS "total", o.cancel_reason AS "cancelReason",
                   o.cancelled_by AS "cancelledBy", o.cancelled_at AS "cancelledAt"
            """ + FROM_ORDERS + " JOIN security.user_app u ON u.id = cs.user_id " + ORDER_FILTER + """
               AND o.status = 'CANCELLED'
            ORDER BY o.cancelled_at, o.id
            """, nativeQuery = true)
    List<CancellationRow> cancellations(@Param("dateFrom") LocalDate dateFrom, @Param("dateTo") LocalDate dateTo,
                                        @Param("userId") Integer userId, @Param("serviceType") String serviceType);

    // ------------------------------------------------------------------ Proyecciones

    interface SalesSummaryRow {
        Long getOrderCount();
        BigDecimal getGross();
        BigDecimal getDiscount();
        BigDecimal getNet();
        Long getCancelledCount();
        BigDecimal getCancelledAmount();
        BigDecimal getPaid();
        BigDecimal getRefunded();
    }

    interface SalesPeriodRow {
        LocalDate getPeriodStart();
        Long getOrderCount();
        BigDecimal getGross();
        BigDecimal getDiscount();
        BigDecimal getNet();
    }

    interface ServiceTypeRow {
        String getServiceType();
        Long getOrderCount();
        BigDecimal getNet();
    }

    interface ProductRow {
        Integer getProductId();
        String getProductName();
        String getCategoryName();
        Long getQuantity();
        Long getOrderCount();
        BigDecimal getGross();
    }

    interface CategoryRow {
        Integer getCategoryId();
        String getCategoryName();
        Long getQuantity();
        BigDecimal getGross();
    }

    interface PaymentMethodRow {
        Integer getPaymentMethodId();
        String getPaymentMethodName();
        Long getPaidCount();
        BigDecimal getPaid();
        Long getRefundedCount();
        BigDecimal getRefunded();
    }

    interface CashSessionRow {
        Integer getId();
        String getUsername();
        String getUserFullName();
        String getStatus();
        LocalDateTime getOpenedAt();
        LocalDateTime getClosedAt();
        String getClosedBy();
        BigDecimal getOpeningAmount();
        BigDecimal getSalesTotal();
        BigDecimal getExpectedTotal();
        BigDecimal getDeclaredTotal();
        BigDecimal getDifferenceTotal();
    }

    interface UserRow {
        Integer getUserId();
        String getUsername();
        String getUserFullName();
        Long getOrderCount();
        BigDecimal getGross();
        BigDecimal getDiscount();
        BigDecimal getNet();
        Long getCancelledCount();
    }

    interface StatusRow {
        String getStatus();
        Long getOrderCount();
        BigDecimal getTotal();
    }

    interface DiscountRow {
        Integer getOrderId();
        Integer getCode();
        LocalDate getBusinessDate();
        LocalDateTime getCreatedAt();
        String getCashier();
        String getDiscountType();
        BigDecimal getDiscountValue();
        BigDecimal getSubtotal();
        BigDecimal getDiscountAmount();
        BigDecimal getTotal();
        String getDiscountReason();
        String getAppliedBy();
    }

    interface CancellationRow {
        Integer getOrderId();
        Integer getCode();
        LocalDate getBusinessDate();
        LocalDateTime getCreatedAt();
        String getCashier();
        BigDecimal getTotal();
        String getCancelReason();
        String getCancelledBy();
        LocalDateTime getCancelledAt();
    }
}
