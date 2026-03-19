package com.example.logistics.queue;

import com.example.logistics.model.Shipment;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * {@link ShipmentQueue} implementation backed by a {@link LinkedBlockingQueue}.
 *
 * This class is fully thread-safe and can be accessed concurrently by many
 * producer threads and one or more consumer threads.
 */
public class BlockingShipmentQueue implements ShipmentQueue {

    private final BlockingQueue<Shipment> delegate;

    /**
     * Create an unbounded shipment queue.
     */
    public BlockingShipmentQueue() {
        this.delegate = new LinkedBlockingQueue<>();
    }

    /**
     * Create a bounded shipment queue with the given capacity.
     */
    public BlockingShipmentQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be > 0");
        }
        this.delegate = new LinkedBlockingQueue<>(capacity);
    }

    @Override
    public void enqueue(Shipment shipment) throws InterruptedException {
        Objects.requireNonNull(shipment, "shipment");
        delegate.put(shipment);
    }

    @Override
    public Shipment take() throws InterruptedException {
        return delegate.take();
    }

    @Override
    public int size() {
        return delegate.size();
    }
}
