package demo.exercise2;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicBoolean;

public final class FanOutBeforeAfter {
    public static void main(String[] args) throws Exception {
        runUnstructuredProblem();
        runStructuredSolution();
    }

    // PROBLEM: observing one failed future does not cancel its sibling.
    private static void runUnstructuredProblem() throws Exception {
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        var slowStarted = new CountDownLatch(1);
        var slowInterrupted = new AtomicBoolean();

        try {
            var slowSibling = CompletableFuture.supplyAsync(
                    () -> slowCall(slowStarted, slowInterrupted), executor);
            slowStarted.await();
            var failedChild = CompletableFuture.supplyAsync(FanOutBeforeAfter::failFast, executor);

            try {
                failedChild.join();
            } catch (RuntimeException expected) {
                // The request fails, but slowSibling is still running.
            }

            Thread.sleep(50);
            System.out.printf(
                    "problem: siblingStillRunning=%s siblingInterrupted=%s%n",
                    !slowSibling.isDone(),
                    slowInterrupted.get());
        } finally {
            executor.shutdownNow(); // Demo cleanup, not part of the broken request path.
            executor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS);
        }
    }

    // SOLUTION: the scope owns both children and cancels the sibling on failure.
    private static void runStructuredSolution() throws InterruptedException {
        var slowStarted = new CountDownLatch(1);
        var slowInterrupted = new AtomicBoolean();

        try (var scope = StructuredTaskScope.<String, Void>open(
                StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow())) {
            scope.fork(() -> slowCall(slowStarted, slowInterrupted));
            slowStarted.await();
            scope.fork(FanOutBeforeAfter::failFast);

            try {
                scope.join();
            } catch (RuntimeException expected) {
                // Closing the failed scope waits for cancellation to finish.
            }
        }

        System.out.printf(
                "solution: siblingStillRunning=false siblingInterrupted=%s%n",
                slowInterrupted.get());
    }

    private static String slowCall(CountDownLatch started, AtomicBoolean interrupted) {
        started.countDown();
        try {
            Thread.sleep(1_000);
            return "slow-result";
        } catch (InterruptedException cancellation) {
            interrupted.set(true);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("slow sibling cancelled", cancellation);
        }
    }

    private static String failFast() {
        throw new IllegalStateException("downstream failed");
    }
}
