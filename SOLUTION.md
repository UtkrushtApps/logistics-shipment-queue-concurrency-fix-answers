# Solution Steps

1. Create a Shipment domain model that is immutable and thread-safe: define id, destination, weightKg and createdAt fields, add constructors, getters, and proper toString/equals/hashCode implementations.

2. Introduce an InvalidShipmentException runtime exception class to represent validation failures cleanly and separate them from infrastructure errors.

3. Implement a ShipmentValidator class that performs basic validation rules: non-null shipment, non-blank id, non-blank destination, and positive weight; throw InvalidShipmentException on any violation and add fine-grained logging hooks.

4. Define a ShipmentQueue interface that abstracts the queuing behavior with enqueue(Shipment), take(), and size() so the rest of the code is decoupled from the concrete concurrent collection used underneath.

5. Create BlockingShipmentQueue as a concrete ShipmentQueue backed by a LinkedBlockingQueue; delegate enqueue to put(), take to take(), and size to size(), ensuring full thread-safety without manual synchronization.

6. Introduce a ShipmentProcessor interface with a single process(Shipment) method so the background worker can remain generic and testable, independent of actual business logic.

7. Provide a LoggingShipmentProcessor implementation that logs each processed shipment and maintains an AtomicInteger counter of processed shipments for observability and for use in tests.

8. Implement the BackgroundShipmentWorker class that owns the worker thread: inject ShipmentQueue, ShipmentProcessor, and ShipmentValidator via the constructor and keep an AtomicBoolean running flag plus a Thread reference for lifecycle management.

9. In BackgroundShipmentWorker.start(), guard against multiple starts, set running=true, construct a daemon Thread that executes a private runWorkerLoop() method, and start it; log that the worker started.

10. In BackgroundShipmentWorker.stop(), atomically flip running to false, interrupt the worker thread to unblock any waiting on the queue, and log that a stop signal was issued without blocking the caller.

11. Implement runWorkerLoop() to continuously pull shipments from the queue and process them while shouldContinueProcessing() is true, catching any exceptions so that a single bad shipment cannot terminate the worker; ensure a final log message on stop.

12. Create shouldContinueProcessing() so it returns true while the worker is running or there are still items in the queue, guaranteeing that enqueued shipments are not abandoned during shutdown.

13. Implement fetchNextShipment() such that while running is true it blocks on shipmentQueue.take(), and after stop() is called it switches into a short timed polling/draining mode (using size() checks and short sleeps) so the worker can exit once the queue is empty and not block forever.

14. In handleShipment(), use ShipmentValidator.validate() again to enforce validation at the worker boundary, call ShipmentProcessor.process(), catch InvalidShipmentException to log and drop invalid shipments, and catch generic Exception to log severe processing issues without killing the worker.

15. Add an awaitTermination(timeout, unit) method in BackgroundShipmentWorker that joins the worker thread for the given timeout so callers (tests/demo) can wait for a clean shutdown.

16. Create ShipmentControllerSimulator to mimic an HTTP endpoint: inject ShipmentQueue and ShipmentValidator, maintain AtomicLong counters for accepted and rejected shipments, and expose a thread-safe enqueueShipment(Shipment) method that validates, enqueues, updates counters, and logs at appropriate levels.

17. Implement getAcceptedCount() and getRejectedCount() on ShipmentControllerSimulator to expose metrics for tests and demos.

18. Add a DemoApplication main class that wires up BlockingShipmentQueue, ShipmentValidator, LoggingShipmentProcessor, BackgroundShipmentWorker, and ShipmentControllerSimulator, starts the worker, spawns multiple producer threads that concurrently enqueue shipments (including some intentionally invalid ones), waits for producers to finish, stops and awaits the worker, and logs final metrics.

19. Create a JUnit test class ShipmentConcurrencyTest with setup/teardown that starts a fresh queue, validator, processor, worker, and controller for each test and ensures the worker is stopped and joined after each test.

20. Write a test concurrentProducers_allValidShipmentsProcessedWithoutLoss that launches several producer threads, each enqueuing many valid shipments; after all have finished and the worker has drained the queue, assert that acceptedCount equals total produced, rejectedCount is zero, and processedCount equals acceptedCount to verify no loss, duplication, or corruption under concurrency.

21. Write a test invalidShipmentsAreRejectedAndNotProcessed that enqueues a mix of valid and invalid shipments synchronously, stops the worker after enqueuing, waits for shutdown, and then asserts that some shipments were rejected, that acceptedCount is consistent with validation rules, and that processedCount equals acceptedCount, verifying that invalid shipments do not get processed and do not interfere with valid ones.

