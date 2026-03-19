package com.example.logistics;

import com.example.logistics.api.ShipmentControllerSimulator;
import com.example.logistics.model.Shipment;
import com.example.logistics.queue.BlockingShipmentQueue;
import com.example.logistics.queue.ShipmentQueue;
import com.example.logistics.validation.ShipmentValidator;
import com.example.logistics.worker.BackgroundShipmentWorker;
import com.example.logistics.worker.LoggingShipmentProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple demo that exercises the concurrent producer and background worker behavior.
 *
 * This is not a test; it is a runnable main method that can be used to manually
 * observe concurrent behavior and logs.
 */
public class DemoApplication {

    private static final Logger LOGGER = Logger.getLogger(DemoApplication.class.getName());

    public static void main(String[] args) throws Exception {
        // Reduce log noise for the demo
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.INFO);

        ShipmentQueue queue = new BlockingShipmentQueue();
        ShipmentValidator validator = new ShipmentValidator();
        LoggingShipmentProcessor processor = new LoggingShipmentProcessor();
        BackgroundShipmentWorker worker = new BackgroundShipmentWorker(queue, processor, validator);
        ShipmentControllerSimulator controller = new ShipmentControllerSimulator(queue, validator);

        worker.start();

        int producers = 10;
        int shipmentsPerProducer = 100;
        CountDownLatch latch = new CountDownLatch(producers);

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < producers; i++) {
            Thread t = new Thread(() -> {
                try {
                    for (int j = 0; j < shipmentsPerProducer; j++) {
                        // Every 10th shipment is intentionally invalid (negative weight)
                        boolean invalid = (j % 10 == 0);
                        Shipment shipment = new Shipment(
                                UUID.randomUUID().toString(),
                                invalid ? "" : "Destination-" + j,
                                invalid ? -1.0 : 1.0 + j
                        );
                        controller.enqueueShipment(shipment);
                    }
                } finally {
                    latch.countDown();
                }
            }, "producer-" + i);
            threads.add(t);
            t.start();
        }

        latch.await(30, TimeUnit.SECONDS);
        LOGGER.info("All producers finished. Accepted=" + controller.getAcceptedCount()
                + ", rejected=" + controller.getRejectedCount());

        // Give the worker some time to drain the queue
        worker.stop();
        worker.awaitTermination(30, TimeUnit.SECONDS);

        LOGGER.info("Worker terminated. Processed shipments=" + processor.getProcessedCount());
        LOGGER.info("Demo completed");
    }
}
