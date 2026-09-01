package demo.exercise1;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public final class ExecutorBeforeAfter {
    private static final int TASKS = 8;

    void main() throws Exception {
        run("problem: fixed pool", Executors.newFixedThreadPool(2));
        run("solution: virtual thread per task", Executors.newVirtualThreadPerTaskExecutor());
    }

    private static void run(String label, ExecutorService executor) throws Exception {
        var virtualThreads = new AtomicInteger();
        var futures = new ArrayList<Future<?>>();
        long startedAt = System.nanoTime();

        try (executor) {
            for (int task = 0; task < TASKS; task++) {
                futures.add(executor.submit(() -> {
                    if (Thread.currentThread().isVirtual()) {
                        virtualThreads.incrementAndGet();
                    }
                    Thread.sleep(100); // Simulated blocking call.
                    return null;
                }));
            }
            for (var future : futures) {
                future.get();
            }
        }

        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
        System.out.printf(
                "%s: tasks=%d virtualTasks=%d elapsedMs=%d%n",
                label,
                TASKS,
                virtualThreads.get(),
                elapsedMillis);
    }
}
