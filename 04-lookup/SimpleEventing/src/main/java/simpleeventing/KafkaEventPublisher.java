package simpleeventing;

import simplemessaging.IAmAMessage;
import simplemessaging.IPublishEvents;

/**
 * Implements {@link IPublishEvents} over a Kafka topic. This is the only place the handler's
 * "tell the world" becomes "append to a log".
 */
public final class KafkaEventPublisher<T extends IAmAMessage> implements IPublishEvents<T>, AutoCloseable {
    private final EventStreamProducer<T> producer;

    public KafkaEventPublisher(EventStreamProducer<T> producer) {
        this.producer = producer;
    }

    @Override
    public void publish(T event) throws Exception {
        producer.send(event);
    }

    @Override
    public void close() {
        producer.close();
    }
}
