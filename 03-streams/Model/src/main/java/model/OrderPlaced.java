package model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import simplemessaging.IAmAMessage;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * An Event Message: this happened. Many readers may care, none of them may reply, and it is a
 * statement about the past rather than a request.
 * <p>
 * Contrast {@link PlaceOrder}, which was a Command: one recipient, allowed to fail. Same system, ten
 * milliseconds apart, and the difference in intent is the whole reason one goes on a queue and the
 * other on a stream.
 */
public record OrderPlaced(
        @JsonProperty("Id") String id,
        @JsonProperty("OrderId") String orderId,
        @JsonProperty("Sku") String sku,
        @JsonProperty("Quantity") int quantity,
        @JsonProperty("Total") BigDecimal total,
        @JsonProperty("PlacedAt") OffsetDateTime placedAt) implements IAmAMessage {

    private static final ObjectMapper STRICT = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public OrderPlaced {
        if (id == null || orderId == null || sku == null || total == null || placedAt == null) {
            throw new IllegalArgumentException(
                    "OrderPlaced requires Id, OrderId, Sku, Quantity, Total and PlacedAt");
        }
    }

    public static String serialize(OrderPlaced event) {
        try {
            return STRICT.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("could not serialize an OrderPlaced", e);
        }
    }

    /**
     * Unlike the queue side, there is no mapper and no UnmappableMessageException here -- because
     * there is nowhere for an unreadable record to go. See EventStreamConsumer.
     */
    public static OrderPlaced deserialize(String body) {
        try {
            return STRICT.readValue(body, OrderPlaced.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Record is not an OrderPlaced: " + e.getMessage(), e);
        }
    }
}
