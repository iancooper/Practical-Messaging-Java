package simpleeventing;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;

public class DataTypeChannelConsumer<T> implements Runnable {

    private final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();
    private final Function<ConsumerRecord<String, String>, T> messageDeserializer;
    private final Function<T, Boolean> handler;
    private final Consumer<String, String> consumer;

    private class HandleRebalance implements ConsumerRebalanceListener {
        public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        }
        public void onPartitionsRevoked(Collection<TopicPartition>  partitions) {
            System.out.println("Lost partitions in rebalance. Committing current offsets:" + currentOffsets);
            consumer.commitSync(currentOffsets);
        }
    }

    public DataTypeChannelConsumer(Function<ConsumerRecord<String, String>, T> messageDeserializer, Function<T, Boolean> handler) {
        this(messageDeserializer, handler, "localhost:9092");
    }

    public DataTypeChannelConsumer(Function<ConsumerRecord<String, String>, T> messageDeserializer, Function<T, Boolean> handler, String bootStrapServer) {
        this.messageDeserializer = messageDeserializer;
        this.handler = handler;

        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServer);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "SimpleEventing");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Collections.singletonList("Pub-Sub-Stream-Biography"), new HandleRebalance());
    }

    public void run() {
        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

                if (!records.isEmpty()){
                    records.forEach(record -> {
                            var data = messageDeserializer.apply(record);
                            var result = handler.apply(data);

                            if (result) {
                                currentOffsets.put(new TopicPartition(record.topic(), record.partition()),
                                        new OffsetAndMetadata(record.offset()+1, "no metadata"));

                                //production code could choose to batch here
                                consumer.commitAsync(currentOffsets, null);
                            }
                    });
                } else {
                    Thread.yield();
                }
            }
        } catch (WakeupException e){
           //shut down the Kafka consumer
        }  finally {
            try {
                consumer.commitSync(currentOffsets);
            } finally {
                consumer.close();
            }
        }
    }

    public void shutdown() {
        // the wakeup() method is a special method to interrupt consumer.poll()
        // it will throw the exception WakeUpException
        consumer.wakeup();
    }
}


