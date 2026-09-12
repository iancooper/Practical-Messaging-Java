package model;

import com.rabbitmq.client.GetResponse;
import simplemessaging.IAmAHandler;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;

/**
 * Application code. What the business actually wanted: price the order and place it.
 *
 * <hr>
 * <b>THIS HANDLER IS PART OF THE EXERCISE. See PROBE.md.</b>
 * <p>
 * Ask yourself one question before you read any further: how much of this method is about
 * placing an order?
 * <hr>
 */
public class PlaceOrderHandler implements IAmAHandler<PlaceOrder> {
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance();

    private final Catalogue catalogue;

    public PlaceOrderHandler(Catalogue catalogue) {
        this.catalogue = catalogue;
    }

    @Override
    public void handle(GetResponse delivery) throws Exception {
        String body = new String(delivery.getBody(), StandardCharsets.UTF_8);
        PlaceOrder order = PlaceOrder.deserialize(body);

        BigDecimal price = catalogue.priceOf(order.sku());
        BigDecimal total = price.multiply(BigDecimal.valueOf(order.quantity()));

        System.out.printf("  placed order %s: %d x %s for %s (customer %s)%n",
                order.id(), order.quantity(), order.sku(), CURRENCY.format(total), order.customerId());
    }
}
