package simpleeventing;

import org.apache.kafka.common.TopicPartition;

/**
 * A record read off the stream, and where it was. The position is ours, not yours.
 * <p>
 * The application gets one of these from {@link EventStreamReader} and hands it straight back to
 * commit. It never names a Kafka type, because everything here that is one is package-private --
 * the same seam {@code EventStreamConsumer} keeps by never handing a {@code ConsumerRecord} out at
 * all.
 */
public final class StreamRecord<T> {
    private final T message;
    private final String where;
    private final TopicPartition partition;
    private final long offset;

    StreamRecord(T message, String where, TopicPartition partition, long offset) {
        this.message = message;
        this.where = where;
        this.partition = partition;
        this.offset = offset;
    }

    public T message() {
        return message;
    }

    /** "p1@42" -- the partition and offset, for printing. */
    public String where() {
        return where;
    }

    /** Where this record is. Package-private: the application never sees a Kafka type. */
    TopicPartition partition() {
        return partition;
    }

    /**
     * Where the <i>next</i> record is, which is what a commit actually means.
     * <p>
     * <b>A committed offset is "the next one I have not read", not "the last one I did read".</b>
     * The javadoc on {@code KafkaConsumer.commitSync(Map)} says it in as many words -- "the
     * committed offset should be the next message your application will consume" -- and the Java
     * client has no per-record overload that adds the one for you, so this line is the one that
     * does it.
     * <p>
     * Commit this record's own offset instead and you have told the broker to start here again, so
     * a fully drained group sits permanently at lag 1 per partition and replays one record on every
     * rebalance -- which looks exactly like the duplicate Probe D is about, and is not it. Off by
     * one, and the symptom is somebody else's bug.
     */
    long next() {
        return offset + 1;
    }
}
