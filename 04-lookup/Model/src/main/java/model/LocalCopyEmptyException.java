package model;

/**
 * Transient. We have no copy of the catalogue yet -- the price consumer has not started, or has not
 * caught up. The SKU may be perfectly good; we are simply not ready to price it.
 * <p>
 * <b>This is a different fact from {@link UnknownSkuException} and the difference is the point of
 * Probe C.</b> One of them is about the order and one of them is about us.
 */
public class LocalCopyEmptyException extends RuntimeException {
    public LocalCopyEmptyException(String sku) {
        super("cannot price '" + sku + "': the local copy has no prices in it yet");
    }
}
