package consumer;

import biographies.Biography;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import simpleeventing.DataTypeChannelConsumer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class Consumer {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/Lookup";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "root";
    public static void main(String[] args) throws IOException {

        Function<ConsumerRecord<String, String>, Biography> deserializer  = record -> {
            try {
                return new ObjectMapper().readValue(record.value(), Biography.class);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        };

        Function<Biography, Boolean> handler = biography -> {
            var sql = "INSERT INTO Biography (Id, Description) VALUES (?, ?)";

            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                try (Connection connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD)) {
                    try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                        preparedStatement.setString(1, biography.getId());
                        preparedStatement.setString(2, biography.getDescription());
                        preparedStatement.executeUpdate();
                    }
                    return true;
                }
            } catch (SQLException | ClassNotFoundException e) {
                e.printStackTrace();
                return false;
            }
        };

        var consumer = new DataTypeChannelConsumer<>(deserializer, handler);

        final Thread mainThread = Thread.currentThread();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            consumer.shutdown();
            try {
                mainThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));


        try (var executorService = Executors.newSingleThreadExecutor()) {
            System.out.println("Consumer running, entering loop until signalled");
            System.out.println(" Press [enter] to exit.");

            // has its own thread and will continue until signaled
            var future = executorService.submit(consumer);
            System.in.read();
            System.out.println("Exiting Consumer");
            future.cancel(true);
        }
    }
}
