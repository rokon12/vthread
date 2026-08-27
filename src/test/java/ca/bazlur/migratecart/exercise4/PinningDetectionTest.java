package ca.bazlur.migratecart.exercise4;

import ca.bazlur.migratecart.diagnostics.HotPathInventoryCache;
import ca.bazlur.migratecart.diagnostics.PinningLoadService;
import ca.bazlur.migratecart.support.ConcurrencyTestSupport;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PinningDetectionTest {
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
