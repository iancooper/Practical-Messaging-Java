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

        //TODO: Create a ConsumerConfig file to configure Kafka. You will need to set:
        // BootstrapServers
        // GroupId
        // AutoOffsetReset (earliest)
        // EnableAutoCommit (true)
        // EnableAutoOffsetStore (false)
        // Key and Value String Serializers

        //TODO: Build a Consumer using the ConsumerConfig above
        // SetErrorHandler to write to the console
        // SetLogHandler to write to the console
        // SetPartitionsRevokedHandler to store the offset for each partition

        //TODO: Subscribe to the topic "Pub-Sub-Stream-Biography" + typeof(T).FullName
    }

    public void run() {
        try {
            while (true) {

                //TODO: Poll Kafka for records
                //If there are records
                //  For each record
                //  Translate the message using the _translator function
                //  Handle the message using the _handler function
                //  If the handler was successful, Store sthe offset for the partition in the background thread
                //Else
                // yield

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


