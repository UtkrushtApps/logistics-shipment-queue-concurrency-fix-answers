package com.example.logistics.worker;

import com.example.logistics.model.Shipment;
import com.example.logistics.queue.ShipmentQueue;
import com.example.logistics.validation.InvalidShipmentException;
import com.example.logistics.validation.ShipmentValidator;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Background worker that continuously pulls shipments from a queue and processes them.
 *
 * The worker has an explicit lifecycle (start/stop) and guarantees that no valid shipment
 * already enqueued will be lost when stopping. Invalid shipments are rejected and logged
 * without impacting other processing.
 */
public class BackgroundShipmentWorker {

    private static final Logger LOGGER = Logger.getLogger(BackgroundShipmentWorker.class.getName());

    private final ShipmentQueue shipmentQueue;
    private final ShipmentProcessor shipmentProcessor;
    private final ShipmentValidator shipmentValidator;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread workerThread;

    public BackgroundShipmentWorker(ShipmentQueue shipmentQueue,
                                    ShipmentProcessor shipmentProcessor,
                                    ShipmentValidator shipmentValidator) {
        this.shipmentQueue = Objects.requireNonNull(shipmentQueue, "shipmentQueue");
        this.shipmentProcessor = Objects.requireNonNull(shipmentProcessor, "shipmentProcessor");
        this.shipmentValidator = Objects.requireNonNull(shipmentValidator, "shipmentValidator");
    }

    /**
     * Start the worker thread. Safe to call only once.
     */
    public synchronized void start() {
        if (running.get()) {
            LOGGER.warning("BackgroundShipmentWorker.start() called but worker is already running");
            return;
        }
        running.set(true);
        workerThread = new Thread(this::runWorkerLoop, "shipment-worker");
        workerThread.setDaemon(true);
        workerThread.start();
        LOGGER.info("Background shipment worker started");
    }

    /**
     * Signal the worker to stop and interrupt any blocking queue operations.
     * This method returns immediately; use {@link #awaitTermination(long, TimeUnit)} to wait.
     */
    public synchronized void stop() {
        if (!running.getAndSet(false)) {
            LOGGER.warning("BackgroundShipmentWorker.stop() called but worker is not running");
            return;
        }
        if (workerThread != null) {
            workerThread.interrupt();
        }
        LOGGER.info("Background shipment worker stop signal sent");
    }

    /**
     * Wait for the worker thread to terminate.
     */
    public void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        Thread t;
        synchronized (this) {
            t = workerThread;
        }
        if (t != null) {
            t.join(unit.toMillis(timeout));
        }
    }

    private void runWorkerLoop() {
        try {
            while (shouldContinueProcessing()) {
                Shipment shipment = fetchNextShipment();
                if (shipment == null) {
                    // Timed out while draining remaining work after stop; exit loop.
                    break;
                }
                handleShipment(shipment);
            }
        } finally {
            LOGGER.info("Background shipment worker stopped");
        }
    }

    /**
     * Decide whether to continue processing.
     *
     * We keep processing items that are already in the queue even after stop() is called,
     * but we avoid blocking indefinitely when the queue is empty and the worker is stopping.
     */
    private boolean shouldContinueProcessing() {
        return running.get() || shipmentQueue.size() > 0;
    }

    /**
     * Fetch the next shipment from the queue.
     *
     * While running, we block indefinitely until an item is available. After a stop signal,
     * we switch to a short timed poll so we can exit once the queue is drained.
     */
    private Shipment fetchNextShipment() {
        try {
            if (running.get()) {
                return shipmentQueue.take();
            } else {
                // Draining mode: avoid blocking forever.
                long timeoutMillis = 200L;
                return pollWithTimeout(timeoutMillis);
            }
        } catch (InterruptedException e) {
            // If we were interrupted while stopping, we may still have items to process.
            Thread.currentThread().interrupt();
            LOGGER.log(Level.FINE, "Worker thread interrupted", e);
            return null;
        }
    }

    private Shipment pollWithTimeout(long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            // There's no non-blocking timed poll on our abstraction, so we emulate
            // with a short sleep if queue is empty.
            if (shipmentQueue.size() > 0) {
                return shipmentQueue.take();
            }
            Thread.sleep(20L);
        }
        return null;
    }

    private void handleShipment(Shipment shipment) {
        try {
            // Validate again in the worker to guard against bypassed validation
            // or older queued items that might no longer be valid.
            shipmentValidator.validate(shipment);
            shipmentProcessor.process(shipment);
        } catch (InvalidShipmentException invalid) {
            LOGGER.log(Level.WARNING,
                    () -> "Dropping invalid shipment id=" + shipment.getId() + ": " + invalid.getMessage());
        } catch (Exception ex) {
            // Catch all processing exceptions so a single bad shipment does not
            // kill the entire worker.
            LOGGER.log(Level.SEVERE,
                    "Unexpected error while processing shipment id=" + shipment.getId(), ex);
        }
    }
}
