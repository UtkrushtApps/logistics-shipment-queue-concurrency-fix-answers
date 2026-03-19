package com.example.logistics.worker;

import com.example.logistics.model.Shipment;

/**
 * Processes shipments taken from the queue.
 */
public interface ShipmentProcessor {

    /**
     * Process a single shipment. Implementations should be thread-safe if used by
     * more than one worker.
     */
    void process(Shipment shipment) throws Exception;
}
