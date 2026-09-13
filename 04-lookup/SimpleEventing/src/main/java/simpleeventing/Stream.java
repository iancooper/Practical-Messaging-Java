package simpleeventing;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.errors.TopicExistsException;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

/**
 * The stream's names and shape, in one place -- and notice how much shorter this is than
 * {@code simplemessaging.Channel}.
 * <p>
 * There is one topic. There is no retry topic, no invalid record topic and no dead letter topic,
 * because nothing in Kafka will move a record to one for you. If you want any of those, you write
 * the producer, the consumer, the scheduler and the state yourself.
 * <p>
 * <b>Three partitions</b>, because a partition is the unit of both ordering and parallelism: one
 * consumer in a group holds a partition at a time, records within a partition are ordered, and
 * records in different partitions are not ordered relative to each other at all.
 */
public final class Stream {
    private Stream() {
    }

    public static final String BOOTSTRAP_SERVERS = "localhost:9092";
    public static final String CONSUMER_GROUP = "practical-messaging-streams";

    /**
     * The price consumer reads a different topic for a different reason, so it gets a group of its
     * own. Two consumers in one group would be told to share the partitions of everything the group
     * subscribes to, which is not what either of them wants -- and the offsets of
     * streams.OrderPlaced and streams.PriceChanged have nothing to do with each other.
     * <p>
     * <b>A consumer group is a unit of work-sharing, not a name for your application.</b>
     */
    public static final String PRICE_CONSUMER_GROUP = "practical-messaging-prices";

    public static final int PARTITIONS = 3;

    public static String topicFor(Class<?> type) {
        return "streams." + type.getSimpleName();
    }

    /**
     * Create the topic if it is not there.
     * <p>
     * Both the producer and the consumer call this, so it does not matter which you start first.
     * (Compare RabbitMQ, where only the consumer declares the queue -- so anything published before
     * the consumer's first ever run went nowhere. Kafka's topic is shared state that either end can
     * create, which is a small but real difference in how the two feel to operate.)
     * <p>
     * It is here rather than left to the broker's auto-create so that the partition count is ours to
     * choose, and so the exercises do not depend on a broker setting.
     */
    public static void ensureTopicExists(String topic) {
        ensureTopicExists(topic, BOOTSTRAP_SERVERS);
    }

    public static void ensureTopicExists(String topic, String bootstrapServers) {
        Properties config = new Properties();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        try (Admin admin = Admin.create(config)) {
            admin.createTopics(List.of(new NewTopic(topic, PARTITIONS, (short) 1))).all().get();
            System.out.printf("Created topic %s with %d partitions%n", topic, PARTITIONS);
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof TopicExistsException)) {
                throw new IllegalStateException("could not create topic " + topic, e);
            }
            // Somebody got there first, which is the normal case after the first run.
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted creating topic " + topic, e);
        }
    }
}
