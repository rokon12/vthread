package ca.bazlur.migratecart;

import ca.bazlur.migratecart.cart.CartAggregationService;
import ca.bazlur.migratecart.cart.CartFacade;
import ca.bazlur.migratecart.cart.CartService;
import ca.bazlur.migratecart.config.ExecutorConfig;
import ca.bazlur.migratecart.diagnostics.HotPathInventoryCache;
import ca.bazlur.migratecart.diagnostics.PinningLoadService;
import ca.bazlur.migratecart.downstream.BoundedBackend;
import ca.bazlur.migratecart.downstream.InventoryGateway;
import ca.bazlur.migratecart.pricing.SlowPricingClient;
import ca.bazlur.migratecart.reporting.OrderTimestampFormatter;
import ca.bazlur.migratecart.support.BlockingSupport;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

/**
 * Runnable diagnostics for the five workshop exercises.
 *
 * <p>The application reports behavior rather than asserting it. Run a scenario before and
 * after an exercise to see what the code change did at runtime.
 */
public final class MigrateCartApplication {
    private static final String JAR_NAME = "target/vithread-workshop-1.0-SNAPSHOT.jar";

    private MigrateCartApplication() {
    }

    public static void main(String[] args) throws Exception {
        String scenario = args.length == 0 ? "all" : args[0].toLowerCase();

        System.out.printf("MigrateCart workshop diagnostics — Java %s%n", Runtime.version().feature());
        switch (scenario) {
            case "all" -> runAllScenarios();
            case "exercise1", "1" -> runExecutorScenario();
            case "exercise2", "2" -> runFanOutScenario();
            case "exercise3", "3" -> runContextScenario();
            case "exercise4", "4" -> runPinningScenario();
            case "exercise5", "5" -> runBottleneckScenario();
            case "help", "--help", "-h" -> printUsage();
            default -> {
                System.err.println("Unknown scenario: " + scenario);
                printUsage();
            }
        }
    }

    private static void runAllScenarios() throws Exception {
        runExecutorScenario();
        runFanOutScenario();
        runContextScenario();
        runPinningScenario();
        runBottleneckScenario();
    }

    private static void runExecutorScenario() throws Exception {
        heading("Exercise 1 — executor and pool waves");
        List<String> skus = IntStream.range(0, 12).mapToObj(i -> "sku-" + i).toList();

        try (ExecutorService executor = new ExecutorConfig().applicationExecutor()) {
            Thread worker = executor.submit(Thread::currentThread).get();
            CartService service = new CartService(executor, new SlowPricingClient(100, "$42.00"));
            Timed<List<String>> result = measure(() -> service.priceAll(skus));

            System.out.printf("worker: %s (virtual=%s)%n", worker, worker.isVirtual());
            System.out.printf("priced: %d SKUs in %d ms%n", result.value().size(), result.elapsedMillis());
        }
    }

    private static void runFanOutScenario() {
        heading("Exercise 2 — fan-out failure and cancellation");
        AtomicBoolean shippingCompleted = new AtomicBoolean();
        AtomicBoolean shippingInterrupted = new AtomicBoolean();

        try (ExecutorService executor = new ExecutorConfig().applicationExecutor()) {
            CartAggregationService service = new CartAggregationService(
                    executor,
                    new SlowPricingClient(100, "$42.00"),
                    (sku, quantity) -> {
                        BlockingSupport.simulateIo(50);
                        throw new IllegalStateException("inventory offline");
                    },
                    (sku, quantity) -> {
                        try {
                            BlockingSupport.simulateIo(500);
                            shippingCompleted.set(true);
                            return "tomorrow";
                        } catch (IllegalStateException e) {
                            shippingInterrupted.set(Thread.currentThread().isInterrupted());
                            throw e;
                        }
                    });

            long started = System.nanoTime();
            try {
                service.loadCart("user-1", "trace-1", "sku-1", 1);
                System.out.println("unexpected: fan-out completed successfully");
            } catch (RuntimeException expected) {
                System.out.printf("failure surfaced in %d ms: %s%n",
                        elapsedMillis(started), rootCause(expected).getMessage());
            }
            System.out.printf("shipping: completed=%s, interrupted=%s%n",
                    shippingCompleted.get(), shippingInterrupted.get());
        }
    }

    private static void runContextScenario() {
        heading("Exercise 3 — request context lifetime");
        try (ExecutorService executor = new ExecutorConfig().applicationExecutor()) {
            CartFacade facade = new CartFacade(executor, new SlowPricingClient(10, "$42.00"));
            var view = facade.handleRequest("user-7", "trace-123", "sku-1", 1);

            System.out.printf("child context: user=%s, trace=%s%n", view.userId(), view.traceId());
            System.out.printf("context after request: %s%n", facade.currentContext());
        }
    }

    private static void runPinningScenario() throws Exception {
        heading("Exercise 4 — serialized hot path");
        List<String> skus = IntStream.range(0, 8).mapToObj(i -> "sku-" + i).toList();

        try (PinningLoadService service = new PinningLoadService(
                new HotPathInventoryCache(),
                Executors.newVirtualThreadPerTaskExecutor())) {
            Timed<List<String>> result = measure(() -> service.runLoad(skus));
            System.out.printf("loaded: %d SKUs in %d ms%n", result.value().size(), result.elapsedMillis());
        }
    }

    private static void runBottleneckScenario() {
        heading("Exercise 5 — allocation and downstream limits");
        OrderTimestampFormatter formatter = new OrderTimestampFormatter();
        Instant sample = Instant.parse("2026-08-10T12:34:56Z");

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Outcome formatting = runTasks(executor, 200, () -> formatter.format(sample));
            System.out.printf("timestamp formatting: succeeded=%d, formatter creations=%d%n",
                    formatting.succeeded(), formatter.formatterCreations());
        }

        BoundedBackend backend = new BoundedBackend(20, 10);
        InventoryGateway gateway = new InventoryGateway(backend);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Outcome inventory = runTasks(executor, 100, () -> gateway.checkAvailability("sku-1"));
            System.out.printf("inventory: succeeded=%d, failed=%d, rejected=%d, peak=%d, capacity=%d%n",
                    inventory.succeeded(), inventory.failed(), backend.rejections(),
                    backend.peakInFlight(), backend.capacity());
        }
    }

    private static Outcome runTasks(ExecutorService executor, int taskCount, Callable<?> task) {
        List<Future<?>> futures = new ArrayList<>(taskCount);
        for (int i = 0; i < taskCount; i++) {
            futures.add(executor.submit(task));
        }

        int succeeded = 0;
        int failed = 0;
        for (Future<?> future : futures) {
            try {
                future.get();
                succeeded++;
            } catch (ExecutionException e) {
                failed++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted while collecting scenario results", e);
            }
        }
        return new Outcome(succeeded, failed);
    }

    private static <T> Timed<T> measure(Callable<T> action) throws Exception {
        long started = System.nanoTime();
        T value = action.call();
        return new Timed<>(value, elapsedMillis(started));
    }

    private static long elapsedMillis(long started) {
        return Duration.ofNanos(System.nanoTime() - started).toMillis();
    }

    private static Throwable rootCause(Throwable failure) {
        Throwable result = failure;
        while (result.getCause() != null) {
            result = result.getCause();
        }
        return result;
    }

    private static void heading(String text) {
        System.out.println();
        System.out.println(text);
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  make run");
        System.out.println("  make run ARGS=exercise1");
        System.out.println("  java --enable-preview -jar " + JAR_NAME
                + " [all|exercise1|exercise2|exercise3|exercise4|exercise5]");
    }

    private record Timed<T>(T value, long elapsedMillis) {
    }

    private record Outcome(int succeeded, int failed) {
    }
}
