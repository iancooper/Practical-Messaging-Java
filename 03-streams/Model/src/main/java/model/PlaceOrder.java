package model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import simplemessaging.IAmAMessage;

import java.util.UUID;

/**
 * A Command Message: go and do this. One recipient, and it is allowed to fail.
 * <p>
 * Every member is required and unmapped members are disallowed, so a body that is not exactly
 * this shape will not deserialize. That is deliberate -- you need a message the receiver cannot
 * understand, and "nearly the right JSON" is the realistic version of one.
 * <p>
 * The field names on the wire are capitalised, and the same in every language this course ships
 * in, so a Python sender and a Java receiver understand each other.
 */
public record PlaceOrder(
        @JsonProperty("Id") String id,
        @JsonProperty("Sku") String sku,
        @JsonProperty("Quantity") int quantity,
        @JsonProperty("CustomerId") String customerId) implements IAmAMessage {

    /**
     * FAIL_ON_UNKNOWN_PROPERTIES is on -- it is Jackson's default, and turning it off is how most
     * services quietly accept a message they have not understood.
     */
    private static final ObjectMapper STRICT = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    /**
     * Jackson will happily leave a missing field null, so "required" is enforced here. Without
     * this, a body with no Sku at all becomes a PlaceOrder whose sku is null and the failure
     * moves from the mapper into the handler -- which is the wrong place for it.
     */
    public PlaceOrder {
        if (id == null || sku == null || customerId == null || quantity <= 0) {
            throw new IllegalArgumentException(
                    "PlaceOrder requires Id, Sku, Quantity and CustomerId");
        }
    }

    public static String serialize(PlaceOrder order) {
        try {
            return STRICT.writeValueAsString(order);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("could not serialize a PlaceOrder", e);
        }
    }

    /** @throws JsonProcessingException if the body is not a PlaceOrder. */
    public static PlaceOrder deserialize(String body) throws JsonProcessingException {
        return STRICT.readValue(body, PlaceOrder.class);
    }

    public static PlaceOrder forSku(String sku) {
        return forSku(sku, 1, "CUST-001");
    }

    public static PlaceOrder forSku(String sku, int quantity, String customerId) {
        return new PlaceOrder(UUID.randomUUID().toString(), sku, quantity, customerId);
    }
}
