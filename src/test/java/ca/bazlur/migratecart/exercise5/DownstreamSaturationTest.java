package ca.bazlur.migratecart.exercise5;

import ca.bazlur.migratecart.downstream.BoundedBackend;
import ca.bazlur.migratecart.downstream.InventoryGateway;
import ca.bazlur.migratecart.support.ConcurrencyTestSupport;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownstreamSaturationTest {
    private static final int TASKS = 500;
    private static final int CAPACITY = 20;
    private static final long LATENCY_MILLIS = 10;

    @Test
    void fixedPoolWasTheImplicitLimiter() {
        BoundedBackend backend = new BoundedBackend(CAPACITY, LATENCY_MILLIS);
        InventoryGateway gateway = new InventoryGateway(backend);

        ConcurrencyTestSupport.Outcome outcome;
        try (ExecutorService executor = Executors.newFixedThreadPool(CAPACITY)) {
            outcome = ConcurrencyTestSupport.runAll(executor, TASKS, () -> gateway.checkAvailability("sku-1"));
        }

        assertEquals(0L, backend.rejections(),
                "a pool of " + CAPACITY + " threads cannot put more than " + CAPACITY
                        + " callers in flight, so the backend was never overloaded");
        assertEquals(TASKS, outcome.succeeded(), "every task should have completed");
    }

    @Test
    void virtualThreadFanOutShouldNotOverwhelmTheBoundedBackend() {
        BoundedBackend backend = new BoundedBackend(CAPACITY, LATENCY_MILLIS);
        InventoryGateway gateway = new InventoryGateway(backend);

        ConcurrencyTestSupport.Outcome outcome;
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            outcome = ConcurrencyTestSupport.runAll(executor, TASKS, () -> gateway.checkAvailability("sku-1"));
        }

        assertEquals(0L, backend.rejections(),
                "expected no rejections, but the backend rejected " + backend.rejections()
                        + " of " + TASKS + " calls; removing the pool removed the only thing bounding concurrency");
        assertEquals(TASKS, outcome.succeeded(),
                "expected every task to succeed, but " + outcome.failed() + " failed");
    }

    @Test
    void concurrencyAtTheBoundedResourceShouldStayWithinCapacity() {
        BoundedBackend backend = new BoundedBackend(CAPACITY, LATENCY_MILLIS);
        InventoryGateway gateway = new InventoryGateway(backend);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            ConcurrencyTestSupport.runAll(executor, TASKS, () -> gateway.checkAvailability("sku-1"));
        }

        assertTrue(backend.peakInFlight() <= CAPACITY,
                "expected at most " + CAPACITY + " concurrent callers at the backend, but peaked at "
                        + backend.peakInFlight());
    }
}
