package model;

public class UnknownSkuException extends RuntimeException {
    public UnknownSkuException(String sku) {
        super("'" + sku + "' is not in the catalogue");
    }
}
