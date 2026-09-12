package model;

import simplemessaging.IAmAHandler;

import java.math.BigDecimal;
import java.text.NumberFormat;

/**
 * Application code, and this is what it should look like: a domain type in, a return or a throw
 * out. No delivery, no headers, no acknowledgement, no broker.
 * <p>
 * A test can call this. So can an HTTP endpoint. That is a consequence of the separation rather
 * than the reason for it, but it is a good smoke alarm: if you cannot call your handler from a test
 * without a broker running, the mapper has not finished its job.
 */
public class PlaceOrderHandler implements IAmAHandler<PlaceOrder> {
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance();

    private final Catalogue catalogue;

    public PlaceOrderHandler(Catalogue catalogue) {
        this.catalogue = catalogue;
    }

    @Override
    public void handle(PlaceOrder order) throws Exception {
        BigDecimal price = catalogue.priceOf(order.sku());
        BigDecimal total = price.multiply(BigDecimal.valueOf(order.quantity()));

        System.out.printf("  placed order %s: %d x %s for %s (customer %s)%n",
                order.id(), order.quantity(), order.sku(), CURRENCY.format(total), order.customerId());
    }
}
