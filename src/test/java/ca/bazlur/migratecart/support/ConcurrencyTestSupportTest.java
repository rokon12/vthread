package ca.bazlur.migratecart.support;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConcurrencyTestSupportTest {
    @Test
    void countsSuccessesAndFailuresWithoutRethrowing() {
        AtomicInteger calls = new AtomicInteger();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            ConcurrencyTestSupport.Outcome outcome = ConcurrencyTestSupport.runAll(executor, 10, () -> {
                if (calls.incrementAndGet() % 2 == 0) {
                    throw new IllegalStateException("boom");
                }
                return "ok";
            });

            assertEquals(5, outcome.succeeded(), "half the tasks should succeed");
            assertEquals(5, outcome.failed(), "half the tasks should fail");
        }
    }
}
