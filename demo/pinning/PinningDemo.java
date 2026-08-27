import java.util.concurrent.CountDownLatch;

/**
 * Isolates carrier pinning from ordinary monitor contention.
 *
 * <p>Every task owns a different monitor, so the tasks have no reason to
 * serialize at the application level. On JDK 21, sleeping while holding the
 * monitor pins the virtual thread to its carrier. On JDK 24 and later, the
 * same virtual thread can unmount while it holds the monitor.</p>
 */
public final class PinningDemo {
    private static final int TASK_COUNT = 8;
    private static final long BLOCKING_MILLIS = 100;
    private static final Object[] MONITORS = createMonitors();

    private PinningDemo() {
    }

    public static void main(String[] args) throws InterruptedException {
        var startGate = new CountDownLatch(1);
        var threads = new Thread[TASK_COUNT];

        for (int index = 0; index < TASK_COUNT; index++) {
            int taskIndex = index;
            threads[index] = Thread.ofVirtual()
                    .name("pinning-demo-" + taskIndex)
                    .unstarted(() -> blockWhileHolding(MONITORS[taskIndex], startGate));
            threads[index].start();
        }

        long startedAt = System.nanoTime();
        startGate.countDown();

        for (var thread : threads) {
            thread.join();
        }

        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
        System.out.printf(
                "tasks=%d blockingMillis=%d elapsedMs=%d%n",
                TASK_COUNT,
                BLOCKING_MILLIS,
                elapsedMillis);
    }

    private static void blockWhileHolding(Object monitor, CountDownLatch startGate) {
        await(startGate);
        synchronized (monitor) {
            try {
                Thread.sleep(BLOCKING_MILLIS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Pinning demo was interrupted", interrupted);
            }
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Pinning demo was interrupted", interrupted);
        }
    }

    private static Object[] createMonitors() {
        var monitors = new Object[TASK_COUNT];
        for (int index = 0; index < TASK_COUNT; index++) {
            monitors[index] = new Object();
        }
        return monitors;
    }
}
