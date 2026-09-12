package model;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

/**
 * Reference data the handler needs: what does this SKU cost?
 * <p>
 * It fails in the three ways a real lookup fails, and telling them apart is the exercise:
 * <pre>
 *   WIDGET-1, GIZMO-2   fine
 *   GIZMO-SLOW          in the catalogue, but the lookup takes 30 seconds
 *   FLAKY-1             fails twice, then works -- a service that was restarting
 *   anything else       not in the catalogue, and never will be
 * </pre>
 */
public class Catalogue {
    public static final Duration SLOW_LOOKUP = Duration.ofSeconds(30);

    /** How many times FLAKY-1 fails before it starts working. */
    public static final int FLAKY_FAILURES = 2;

    private static final Map<String, BigDecimal> PRICES = Map.of(
            "WIDGET-1", new BigDecimal("9.99"),
            "GIZMO-2", new BigDecimal("24.50"),
            "GIZMO-SLOW", new BigDecimal("24.50"),
            "FLAKY-1", new BigDecimal("12.00"));

    private int flakyAttempts;

    public BigDecimal priceOf(String sku) throws InterruptedException {
        if (sku.equals("GIZMO-SLOW")) {
            System.out.printf("  catalogue: looking up %s (this one takes %ds)%n",
                    sku, SLOW_LOOKUP.toSeconds());
            Thread.sleep(SLOW_LOOKUP.toMillis());
        }

        if (sku.equals("FLAKY-1")) {
            flakyAttempts++;
            if (flakyAttempts <= FLAKY_FAILURES) {
                throw new CatalogueUnavailableException(sku, flakyAttempts);
            }
            System.out.printf("  catalogue: %s worked on attempt %d%n", sku, flakyAttempts);
        }

        BigDecimal price = PRICES.get(sku);
        if (price == null) {
            throw new UnknownSkuException(sku);
        }

        return price;
    }
}
