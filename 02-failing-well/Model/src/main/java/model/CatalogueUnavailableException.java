package model;

/** Transient. The lookup is unwell; the order is fine. Try again shortly. */
public class CatalogueUnavailableException extends RuntimeException {
    public CatalogueUnavailableException(String sku, int attempt) {
        super("catalogue is unavailable (attempt " + attempt + " for '" + sku + "')");
    }
}
