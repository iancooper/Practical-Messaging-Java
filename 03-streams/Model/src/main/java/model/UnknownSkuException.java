package model;

/** Permanent. This SKU does not exist and asking again will not change that. */
public class UnknownSkuException extends RuntimeException {
    public UnknownSkuException(String sku) {
        super("'" + sku + "' is not in the catalogue");
    }
}
