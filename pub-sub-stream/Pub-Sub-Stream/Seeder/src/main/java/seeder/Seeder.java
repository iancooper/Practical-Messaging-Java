package seeder;

import biographies.Biography;
import com.fasterxml.jackson.databind.ObjectMapper;
import simpleeventing.DataTypeChannelProducer;

import java.io.IOException;
import java.util.concurrent.Executors;

public class Seeder {
    public static void main(String[] args) throws IOException {

        try (var executorService = Executors.newSingleThreadExecutor()) {
            System.out.println("Seeding biographies");

            // has its own thread and will continue until signaled
            var future = executorService.submit(Seeder::send);
            System.in.read();
            System.out.println("Biographies seeded");
            future.cancel(true);
        }
    }

    private static void send() {
        ObjectMapper objectMapper = new ObjectMapper();

        try (DataTypeChannelProducer<Biography> producer = new DataTypeChannelProducer<>(bio -> {
            try {
                return objectMapper.writeValueAsString(bio);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        })) {
            Biography[] biographies = {
                    new Biography("Clarissa Harlow", "A young woman whose quest for virtue is continually thwarted by her family."),
                    new Biography("Pamela Andrews", "\"A young woman whose virtue is rewarded."),
                    new Biography("Harriet Byron", "An orphan, and heir to a considerable fortune of fifteen thousand pounds"),
                    new Biography("Charles Grandison", "A man of feeling who truly cannot be said to feel")
                    // Add more biographies as needed
            };

            for (Biography bio : biographies){
                producer.send(bio);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
