package model;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

/**
 * Reference data the handler needs in order to do its job: what does this SKU cost?
 * <p>
 * Here it is a map, because exercises 1 to 3 are not about lookups. It behaves like the real
 * thing in the two ways that matter to us: it can be slow, and it can not know.
 * <p>
 * (Exercise 4, if you get to it, replaces this with a local copy filled from a stream.)
 */
public class Catalogue {
    /** How long a lookup takes. GIZMO-SLOW is the one that hurts. */
    public static final Duration SLOW_LOOKUP = Duration.ofSeconds(30);

    private static final Map<String, BigDecimal> PRICES = Map.of(
            "WIDGET-1", new BigDecimal("9.99"),
            "GIZMO-2", new BigDecimal("24.50"),
            "GIZMO-SLOW", new BigDecimal("24.50"));   // in the catalogue, but the lookup crawls

    public BigDecimal priceOf(String sku) throws InterruptedException {
        if (sku.equals("GIZMO-SLOW")) {
            System.out.printf("  catalogue: looking up %s (this one takes %ds)%n",
                    sku, SLOW_LOOKUP.toSeconds());
            Thread.sleep(SLOW_LOOKUP.toMillis());
        }

        BigDecimal price = PRICES.get(sku);
        if (price == null) {
            throw new UnknownSkuException(sku);
        }

        return price;
    }
}
