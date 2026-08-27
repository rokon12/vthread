package ca.bazlur.migratecart.observability;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * A {@link ThreadLocal} that reports how many distinct threads have initialized a value.
 *
 * <p>Use this to audit {@code ThreadLocal.withInitial(...)} caches before a virtual-thread
 * migration. A cache designed for a pooled executor initializes once per pooled thread. The
 * same cache on virtual threads initializes once per task, because virtual threads are not
 * reused. Nothing throws when that happens; the only symptom is allocation.
 */
public final class CountingThreadLocal<T> extends ThreadLocal<T> {
    private final Supplier<? extends T> supplier;
    private final AtomicLong initializations = new AtomicLong();

    public CountingThreadLocal(Supplier<? extends T> supplier) {
        this.supplier = supplier;
    }

    @Override
    protected T initialValue() {
        initializations.incrementAndGet();
        return supplier.get();
    }

    public long initializationCount() {
        return initializations.get();
    }
}
