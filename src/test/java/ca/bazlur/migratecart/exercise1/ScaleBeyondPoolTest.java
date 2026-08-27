package ca.bazlur.migratecart.exercise1;

import ca.bazlur.migratecart.cart.CartService;
import ca.bazlur.migratecart.config.ExecutorConfig;
import ca.bazlur.migratecart.pricing.SlowPricingClient;
import ca.bazlur.migratecart.support.ConcurrencyTestSupport;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ScaleBeyondPoolTest {
    @Test
    void pricingWorkloadShouldNotBeBoundByPoolWaves() throws Exception {
        ExecutorConfig config = new ExecutorConfig();
        try (ExecutorService executor = config.applicationExecutor()) {
            CartService service = new CartService(executor, new SlowPricingClient(250, "$42.00"));
            List<String> skus = IntStream.range(0, 12).mapToObj(i -> "sku-" + i).toList();

            long durationMillis = ConcurrencyTestSupport.measureMillis(() -> service.priceAll(skus));

            assertTrue(durationMillis < 500,
                    "expected the workload to complete without fixed-pool wave behavior, but it took "
                            + durationMillis + " ms");
        }
    }
}
