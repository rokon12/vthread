package ca.bazlur.migratecart.observability;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CountingThreadLocalTest {
    @Test
    void countsOneInitializationPerThreadRegardlessOfReads() {
        AtomicInteger source = new AtomicInteger();
        CountingThreadLocal<Integer> local = new CountingThreadLocal<>(source::incrementAndGet);

        local.get();
        local.get();
        local.get();

        assertEquals(1L, local.initializationCount(),
                "one thread reading three times should initialize once");
    }

    @Test
    void countsEachDistinctThreadSeparately() throws Exception {
        CountingThreadLocal<Object> local = new CountingThreadLocal<>(Object::new);

        Thread first = Thread.ofPlatform().start(local::get);
        first.join();
        Thread second = Thread.ofPlatform().start(local::get);
        second.join();

        assertEquals(2L, local.initializationCount(),
                "two threads should initialize twice");
    }
}
