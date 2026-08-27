package ca.bazlur.migratecart.downstream;

import ca.bazlur.migratecart.support.BlockingSupport;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A downstream resource with a hard concurrency limit, such as a JDBC connection pool.
 *
 * <p>It rejects rather than queues. An internal queue would hide the very thing this class
 * exists to expose: once the thread pool stops bounding concurrency, nothing else does.
 */
public class BoundedBackend {
    private final int capacity;
    private final long latencyMillis;
    private final AtomicInteger inFlight = new AtomicInteger();
    private final AtomicInteger peakInFlight = new AtomicInteger();
    private final AtomicLong rejections = new AtomicLong();

    public BoundedBackend(int capacity, long latencyMillis) {
        this.capacity = capacity;
        this.latencyMillis = latencyMillis;
    }

    public String call(String sku) {
        int current = inFlight.incrementAndGet();
        try {
            peakInFlight.accumulateAndGet(current, Math::max);
            if (current > capacity) {
                rejections.incrementAndGet();
                throw new BackendOverloadedException(
                        "backend capacity " + capacity + " exceeded by " + current + " concurrent callers");
            }
            BlockingSupport.simulateIo(latencyMillis);
            return "in-stock:" + sku;
        } finally {
            inFlight.decrementAndGet();
        }
    }

    public int capacity() {
        return capacity;
    }

    public int peakInFlight() {
        return peakInFlight.get();
    }

    public long rejections() {
        return rejections.get();
    }
}
