package demo.pinning;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public final class PinningBeforeAfter {
    private static final int TASKS = 8;
    private static final long REMOTE_CALL_MILLIS = 100;

    interface PriceLookup {
        String find(String sku);
    }

    // PROBLEM: the simulated remote call blocks while this object's monitor is held.
    static final class PinnedPriceLookup implements PriceLookup {
        private final Map<String, String> prices = new HashMap<>();

        @Override
        public synchronized String find(String sku) {
            var cached = prices.get(sku);
            if (cached == null) {
                cached = loadPrice(sku); // Blocks for 100 ms while synchronized.
                prices.put(sku, cached);
            }
            return cached;
        }
    }

    // SOLUTION: lock only around the in-memory check and publication.
    static final class UnpinnedPriceLookup implements PriceLookup {
        private final Map<String, String> prices = new HashMap<>();

        @Override
        public String find(String sku) {
            synchronized (prices) {
                var cached = prices.get(sku);
                if (cached != null) {
                    return cached;
                }
            }

            var loaded = loadPrice(sku); // Blocking work happens outside the lock.

            synchronized (prices) {
                return prices.computeIfAbsent(sku, ignored -> loaded);
            }
        }
    }

    private PinningBeforeAfter() {
    }

    public static void main(String[] args) throws InterruptedException {
        if (args.length != 1 || !(args[0].equals("broken") || args[0].equals("fixed"))) {
            throw new IllegalArgumentException("Expected one argument: broken or fixed");
        }

        PriceLookup lookup = args[0].equals("broken")
                ? new PinnedPriceLookup()
                : new UnpinnedPriceLookup();
        var startGate = new CountDownLatch(1);
        var threads = new Thread[TASKS];

        for (int index = 0; index < TASKS; index++) {
            int task = index;
            threads[index] = Thread.ofVirtual().start(() -> {
                await(startGate);
                lookup.find("sku-" + task);
            });
        }

        long startedAt = System.nanoTime();
        startGate.countDown();
        for (var thread : threads) {
            thread.join();
        }

        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
        System.out.printf("mode=%s tasks=%d elapsedMs=%d%n", args[0], TASKS, elapsedMillis);
    }

    private static String loadPrice(String sku) {
        try {
            Thread.sleep(REMOTE_CALL_MILLIS);
            return "$42.00:" + sku;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Price lookup interrupted", interrupted);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Start interrupted", interrupted);
        }
    }
}
