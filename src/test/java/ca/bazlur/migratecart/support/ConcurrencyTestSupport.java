package ca.bazlur.migratecart.support;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class ConcurrencyTestSupport {
    private ConcurrencyTestSupport() {
    }

    public static long measureMillis(ThrowingRunnable action) throws Exception {
        long start = System.nanoTime();
        action.run();
        return Duration.ofNanos(System.nanoTime() - start).toMillis();
    }

    /**
     * Submits {@code taskCount} copies of {@code task} and waits for all of them.
     *
     * <p>Task failures are counted rather than rethrown, because the failure count is the
     * observable in Exercise 5, not an accident.
     */
    public static Outcome runAll(ExecutorService executor, int taskCount, Callable<?> task) {
        List<Future<?>> futures = new ArrayList<>(taskCount);
        for (int i = 0; i < taskCount; i++) {
            futures.add(executor.submit(task));
        }

        int succeeded = 0;
        int failed = 0;
        for (Future<?> future : futures) {
            try {
                future.get(30, TimeUnit.SECONDS);
                succeeded++;
            } catch (ExecutionException e) {
                failed++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted while collecting results", e);
            } catch (TimeoutException e) {
                throw new IllegalStateException(
                        "a task did not complete within 30s — check that every acquired permit is released on every path",
                        e);
            }
        }
        return new Outcome(succeeded, failed);
    }

    public record Outcome(int succeeded, int failed) {
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}
