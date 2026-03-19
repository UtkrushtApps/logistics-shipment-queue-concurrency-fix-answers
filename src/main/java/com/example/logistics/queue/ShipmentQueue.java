package com.example.logistics.queue;

import com.example.logistics.model.Shipment;

/**
 * Thread-safe abstraction for a shipment queue.
 */
public interface ShipmentQueue {

    /**
     * Enqueue a shipment, potentially blocking if the underlying queue is bounded and full.
     */
    void enqueue(Shipment shipment) throws InterruptedException;

    /**
     * Take the next shipment, blocking until one is available.
     */
    Shipment take() throws InterruptedException;

    /**
     * Current size of the queue (approximate in highly concurrent scenarios).
     */
    int size();
}
