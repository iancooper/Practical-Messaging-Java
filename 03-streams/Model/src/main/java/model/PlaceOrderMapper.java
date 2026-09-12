package model;

import com.fasterxml.jackson.core.JsonProcessingException;
import simplemessaging.IAmAMessageMapper;
import simplemessaging.UnmappableMessageException;

/**
 * The Translate stage for this channel: a body becomes a {@link PlaceOrder}, or it does not and we
 * say so clearly.
 * <p>
 * Note what it does <i>not</i> do: it does not log, it does not decide anything, and it does not
 * know a broker exists. It converts, or it throws.
 */
public class PlaceOrderMapper implements IAmAMessageMapper<PlaceOrder> {
    @Override
    public PlaceOrder mapToRequest(String body) {
        try {
            return PlaceOrder.deserialize(body);
        } catch (JsonProcessingException e) {
            // Translate the serializer's complaint into the gateway's vocabulary. The pump should
            // not have to know we chose JSON.
            throw new UnmappableMessageException("Body is not a PlaceOrder: " + e.getMessage(), e);
        }
    }
}
