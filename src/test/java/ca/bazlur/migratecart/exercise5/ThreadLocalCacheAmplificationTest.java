package ca.bazlur.migratecart.exercise5;

import ca.bazlur.migratecart.reporting.OrderTimestampFormatter;
import ca.bazlur.migratecart.support.ConcurrencyTestSupport;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreadLocalCacheAmplificationTest {
    private static final int TASKS = 1_000;
    private static final int POOL_SIZE = 8;
    private static final long MAX_CREATIONS = POOL_SIZE;
    private static final Instant SAMPLE = Instant.parse("2026-08-10T12:34:56Z");

    @Test
    void fixedPoolBoundsFormatterCreation() {
        OrderTimestampFormatter formatter = new OrderTimestampFormatter();

        try (ExecutorService executor = Executors.newFixedThreadPool(POOL_SIZE)) {
            ConcurrencyTestSupport.runAll(executor, TASKS, () -> formatter.format(SAMPLE));
        }

        assertTrue(formatter.formatterCreations() <= MAX_CREATIONS,
                "a pool of " + POOL_SIZE + " threads should construct at most " + MAX_CREATIONS
                        + " formatters for " + TASKS + " tasks, but constructed "
                        + formatter.formatterCreations());
    }

    @Test
    void virtualThreadsShouldNotRecreateTheCachedFormatter() {
        OrderTimestampFormatter formatter = new OrderTimestampFormatter();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            ConcurrencyTestSupport.runAll(executor, TASKS, () -> formatter.format(SAMPLE));
        }

        assertTrue(formatter.formatterCreations() <= MAX_CREATIONS,
                "expected at most " + MAX_CREATIONS + " formatters for " + TASKS
                        + " tasks, but constructed " + formatter.formatterCreations()
                        + "; virtual threads are not reused, so a per-thread cache becomes a per-task allocation");
    }

    @Test
    void formattingOutputIsUnchangedByTheMigration() {
        assertEquals("2026-08-10T12:34:56Z", new OrderTimestampFormatter().format(SAMPLE),
                "the migration must preserve the rendered timestamp");
    }
}
