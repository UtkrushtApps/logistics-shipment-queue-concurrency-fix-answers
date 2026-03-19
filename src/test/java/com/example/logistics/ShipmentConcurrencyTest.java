package com.example.logistics;

import com.example.logistics.api.ShipmentControllerSimulator;
import com.example.logistics.model.Shipment;
import com.example.logistics.queue.BlockingShipmentQueue;
import com.example.logistics.queue.ShipmentQueue;
import com.example.logistics.validation.ShipmentValidator;
import com.example.logistics.worker.BackgroundShipmentWorker;
import com.example.logistics.worker.LoggingShipmentProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Concurrency tests ensuring no shipments are lost or duplicated.
 */
public class ShipmentConcurrencyTest {

    private ShipmentQueue queue;
    private ShipmentValidator validator;
    private LoggingShipmentProcessor processor;
    private BackgroundShipmentWorker worker;
    private ShipmentControllerSimulator controller;

    @BeforeEach
    void setUp() {
        queue = new BlockingShipmentQueue();
        validator = new ShipmentValidator();
        processor = new LoggingShipmentProcessor();
        worker = new BackgroundShipmentWorker(queue, processor, validator);
        controller = new ShipmentControllerSimulator(queue, validator);
        worker.start();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        worker.stop();
        worker.awaitTermination(10, TimeUnit.SECONDS);
    }

    @Test
    void concurrentProducers_allValidShipmentsProcessedWithoutLoss() throws InterruptedException {
        int producers = 8;
        int shipmentsPerProducer = 200;
        CountDownLatch latch = new CountDownLatch(producers);

        for (int i = 0; i < producers; i++) {
            Thread t = new Thread(() -> {
                try {
                    for (int j = 0; j < shipmentsPerProducer; j++) {
                        Shipment shipment = new Shipment(
                                UUID.randomUUID().toString(),
                                "DEST-" + j,
                                5.0
                        );
                        controller.enqueueShipment(shipment);
                    }
                } finally {
                    latch.countDown();
                }
            });
            t.start();
        }

        boolean finished = latch.await(30, TimeUnit.SECONDS);
        Assertions.assertTrue(finished, "Producers did not finish in time");

        // Wait for worker to drain queue
        worker.stop();
        worker.awaitTermination(30, TimeUnit.SECONDS);

        int expectedValid = producers * shipmentsPerProducer;
        Assertions.assertEquals(expectedValid, controller.getAcceptedCount(), "All shipments should be accepted");
        Assertions.assertEquals(0, controller.getRejectedCount(), "No shipments should be rejected");
        Assertions.assertEquals(expectedValid, processor.getProcessedCount(),
                "All accepted shipments must be processed exactly once");
    }

    @Test
    void invalidShipmentsAreRejectedAndNotProcessed() throws InterruptedException {
        int total = 100;
        for (int i = 0; i < total; i++) {
            boolean invalid = (i % 2 == 0);
            Shipment shipment = new Shipment(
                    UUID.randomUUID().toString(),
                    invalid ? "" : "DEST-" + i,
                    invalid ? -5.0 : 3.0
            );
            controller.enqueueShipment(shipment);
        }

        // Stop worker and wait for it to process remaining valid items
        worker.stop();
        worker.awaitTermination(10, TimeUnit.SECONDS);

        long accepted = controller.getAcceptedCount();
        long rejected = controller.getRejectedCount();

        Assertions.assertEquals(total / 2, accepted, 5, "Roughly half should be accepted (depending on validation) ");
        Assertions.assertTrue(rejected > 0, "Some shipments should be rejected");

        // All accepted shipments must have been processed
        Assertions.assertEquals(accepted, processor.getProcessedCount());
    }
}
