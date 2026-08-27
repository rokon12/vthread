package ca.bazlur.migratecart.exercise4;

import ca.bazlur.migratecart.diagnostics.HotPathInventoryCache;
import ca.bazlur.migratecart.diagnostics.PinningLoadService;
import ca.bazlur.migratecart.support.BlockingSupport;
import ca.bazlur.migratecart.support.ConcurrencyTestSupport;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PinningDetectionTest {
    @Test
    void repeatedReadsShouldUseTheCachedValue() {
        AtomicInteger loads = new AtomicInteger();
        HotPathInventoryCache cache = new HotPathInventoryCache(sku -> {
            loads.incrementAndGet();
            return "in-stock:" + sku;
        });

        assertEquals("in-stock:sku-1", cache.refreshAndRead("sku-1"));
        assertEquals("in-stock:sku-1", cache.refreshAndRead("sku-1"));
        assertEquals(1, loads.get(), "a cached SKU should not be loaded twice");
    }

    @Test
    void concurrentReadersShouldAgreeOnThePublishedValue() {
        AtomicInteger versions = new AtomicInteger();
        HotPathInventoryCache cache = new HotPathInventoryCache(sku -> {
            int version = versions.incrementAndGet();
            BlockingSupport.simulateIo(25);
            return "in-stock:" + sku + ":v" + version;
        });
        List<String> sameSku = IntStream.range(0, 32).mapToObj(i -> "sku-1").toList();

        List<String> results;
        try (PinningLoadService service = new PinningLoadService(
                cache,
                Executors.newVirtualThreadPerTaskExecutor())) {
            results = service.runLoad(sameSku);
        }

        String published = cache.refreshAndRead("sku-1");
        assertTrue(results.stream().allMatch(published::equals),
                "all concurrent readers should observe the value that won publication");
    }

    @Test
    void loadShouldNotSerializeBehindAMonitorInTheHotPath() throws Exception {
        List<String> skus = IntStream.range(0, 8).mapToObj(i -> "sku-" + i).toList();

        try (PinningLoadService service = new PinningLoadService(
                new HotPathInventoryCache(),
                Executors.newVirtualThreadPerTaskExecutor())) {
            long durationMillis = ConcurrencyTestSupport.measureMillis(() -> service.runLoad(skus));

            assertTrue(durationMillis < 400,
                    "expected virtual-thread load to avoid monitor-bound serialization, but it took "
                            + durationMillis + " ms");
        }
    }
}
