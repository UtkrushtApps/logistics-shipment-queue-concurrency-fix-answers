package com.example.logistics.worker;

import com.example.logistics.model.Shipment;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple processor that logs processed shipments and keeps a count for observability/testing.
 */
public class LoggingShipmentProcessor implements ShipmentProcessor {

    private static final Logger LOGGER = Logger.getLogger(LoggingShipmentProcessor.class.getName());

    private final AtomicInteger processedCount = new AtomicInteger();

    @Override
    public void process(Shipment shipment) {
        int count = processedCount.incrementAndGet();
        LOGGER.log(Level.FINE, () -> "Processed shipment " + shipment.getId() + " (total processed=" + count + ")");
        // In a real system, more complex business logic would go here (persistence, external calls, etc.).
    }

    public int getProcessedCount() {
        return processedCount.get();
    }
}
