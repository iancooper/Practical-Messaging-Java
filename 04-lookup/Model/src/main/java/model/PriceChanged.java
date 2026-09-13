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
import java.util.UUID;

/**
 * The ECST event: the catalogue service, which owns SKUs and prices, says what one costs now.
 * <p>
 * <b>It is a snapshot, not a delta</b>, and that is the decision the whole exercise turns on.
 * "WIDGET-1 is now 11.99" can be applied twice with no harm; "WIDGET-1 went up by 2.00" cannot.
 * Probe D is where that stops being a matter of taste -- see README.md, step 1.
 * <p>
 * {@code ChangedAt} is here for two reasons and both are probes. Probe A subtracts it from the
 * moment the consumer applies the record, and that difference is your staleness. Probe C asks how
 * old your local copy is, and a copy that does not carry a date cannot answer.
 * <p>
 * The price is a {@link BigDecimal} and not a double, here and everywhere downstream of here.
 * Money is decimal, and 9.99 is not a number a double can hold.
 */
public record PriceChanged(
        @JsonProperty("Id") String id,
        @JsonProperty("Sku") String sku,
        @JsonProperty("Price") BigDecimal price,
        @JsonProperty("ChangedAt") OffsetDateTime changedAt) implements IAmAMessage {

    private static final ObjectMapper STRICT = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public PriceChanged {
        if (id == null || sku == null || price == null || changedAt == null) {
            throw new IllegalArgumentException("PriceChanged requires Id, Sku, Price and ChangedAt");
        }
    }

    public static String serialize(PriceChanged event) {
        try {
            return STRICT.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("could not serialize a PriceChanged", e);
        }
    }

    /**
     * Unlike the queue side, there is no mapper and no UnmappableMessageException here -- because
     * there is nowhere for an unreadable record to go. See EventStreamReader.
     */
    public static PriceChanged deserialize(String body) {
        try {
            return STRICT.readValue(body, PriceChanged.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Record is not a PriceChanged: " + e.getMessage(), e);
        }
    }

    public static PriceChanged of(String sku, BigDecimal price) {
        return new PriceChanged(UUID.randomUUID().toString(), sku, price, OffsetDateTime.now());
    }
}
