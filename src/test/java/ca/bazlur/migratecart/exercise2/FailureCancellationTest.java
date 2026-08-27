package ca.bazlur.migratecart.exercise2;

import ca.bazlur.migratecart.cart.CartAggregationService;
import ca.bazlur.migratecart.inventory.InventoryClient;
import ca.bazlur.migratecart.pricing.PricingClient;
import ca.bazlur.migratecart.shipping.ShippingClient;
import ca.bazlur.migratecart.support.BlockingSupport;
import ca.bazlur.migratecart.support.ConcurrencyTestSupport;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FailureCancellationTest {
    @Test
    void failureShouldSurfaceWithoutWaitingForSlowSiblingTasks() throws Exception {
        PricingClient pricingClient = sku -> {
            BlockingSupport.simulateIo(100);
            return "$42.00";
        };
        InventoryClient inventoryClient = (sku, quantity) -> {
            BlockingSupport.simulateIo(50);
            throw new IllegalStateException("inventory offline");
        };
        ShippingClient shippingClient = (sku, quantity) -> {
            BlockingSupport.simulateIo(2_000);
            return "tomorrow";
        };

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CartAggregationService service = new CartAggregationService(
                    executor,
                    pricingClient,
                    inventoryClient,
                    shippingClient);

            long durationMillis = ConcurrencyTestSupport.measureMillis(() ->
                    assertThrows(CompletionException.class,
                            () -> service.loadCart("user-1", "trace-1", "sku-1", 1)));

            assertTrue(durationMillis < 500,
                    "expected failure to surface without waiting for slow sibling tasks, but it took "
                            + durationMillis + " ms");
        }
    }
}
