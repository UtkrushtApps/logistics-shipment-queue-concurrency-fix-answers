package com.example.logistics.api;

import com.example.logistics.model.Shipment;
import com.example.logistics.queue.ShipmentQueue;
import com.example.logistics.validation.ShipmentValidator;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simulates an HTTP endpoint/controller that enqueues shipments from many concurrent producers.
 *
 * In a real application, this would be a REST controller; here it is a simple façade
 * that exposes a thread-safe {@link #enqueueShipment(Shipment)} method.
 */
public class ShipmentControllerSimulator {

    private static final Logger LOGGER = Logger.getLogger(ShipmentControllerSimulator.class.getName());

    private final ShipmentQueue shipmentQueue;
    private final ShipmentValidator shipmentValidator;

    private final AtomicLong acceptedCount = new AtomicLong();
    private final AtomicLong rejectedCount = new AtomicLong();

    public ShipmentControllerSimulator(ShipmentQueue shipmentQueue, ShipmentValidator shipmentValidator) {
        this.shipmentQueue = Objects.requireNonNull(shipmentQueue, "shipmentQueue");
        this.shipmentValidator = Objects.requireNonNull(shipmentValidator, "shipmentValidator");
    }

    /**
     * Enqueue a shipment after basic validation.
     *
     * This method is safe to be called concurrently by multiple threads.
     */
    public void enqueueShipment(Shipment shipment) {
        try {
            shipmentValidator.validate(shipment);
            shipmentQueue.enqueue(shipment);
            long count = acceptedCount.incrementAndGet();
            LOGGER.log(Level.FINER, () -> "Enqueued shipment " + shipment.getId() + " (total accepted=" + count + ")");
        } catch (Exception e) {
            long count = rejectedCount.incrementAndGet();
            LOGGER.log(Level.WARNING, () -> "Rejected shipment (total rejected=" + count + "): " + e.getMessage());
        }
    }

    public long getAcceptedCount() {
        return acceptedCount.get();
    }

    public long getRejectedCount() {
        return rejectedCount.get();
    }
}
