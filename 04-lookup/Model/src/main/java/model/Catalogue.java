package model;

import java.math.BigDecimal;

/**
 * Reference data the handler needs: what does this SKU cost?
 * <p>
 * <b>Everything about how this answers has changed, and its signature has not.</b> In exercises 1
 * to 3 it was a map that pretended to be a service call, and it failed in three ways: slow
 * (GIZMO-SLOW), briefly unwell (FLAKY-1) and unknown. Two of those three are gone, and their going
 * is the lesson -- they were <i>on-demand</i> failures, and there is no longer a call to be slow or
 * unwell. Get It In Advance did not fix them. It removed the thing that could fail, and bought you
 * Probe B instead.
 * <p>
 * The handler did not change. It still asks the catalogue for a price, and the catalogue still
 * decides where prices come from. That is what the seam was for.
 */
public class Catalogue {
    private final IPriceStore prices;

    public Catalogue(IPriceStore prices) {
        this.prices = prices;
    }

    public BigDecimal priceOf(String sku) {
        Price price = prices.lookup(sku);
        if (price != null) {
            return price.amount();
        }

        // Two different failures wear the same shape -- a lookup that returned nothing -- and
        // exercise 2 spent forty minutes on why that matters. "I have no copy yet" is about us and
        // will fix itself; "that SKU is not a thing" is about the order and never will.
        //
        // **The domain's job is to say which.** What to do about each is the pump's policy and not
        // ours, and Probe C is about the fact that the pump currently does the same thing with both.
        if (prices.count() == 0) {
            throw new LocalCopyEmptyException(sku);
        }

        throw new UnknownSkuException(sku);
    }
}
