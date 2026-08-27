package ca.bazlur.migratecart.exercise2;

import ca.bazlur.migratecart.cart.CartAggregationService;
import ca.bazlur.migratecart.inventory.InventoryClient;
import ca.bazlur.migratecart.pricing.PricingClient;
import ca.bazlur.migratecart.shipping.ShippingClient;
import ca.bazlur.migratecart.support.BlockingSupport;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoOrphanedTasksTest {
    @Test
    void slowSiblingShouldBeCancelledWhenAnotherBranchFails() {
        AtomicBoolean completed = new AtomicBoolean();
        AtomicBoolean interrupted = new AtomicBoolean();

        PricingClient pricingClient = sku -> "$42.00";
        InventoryClient inventoryClient = (sku, quantity) -> {
            BlockingSupport.simulateIo(50);
            throw new IllegalStateException("inventory offline");
        };
        ShippingClient shippingClient = (sku, quantity) -> {
            try {
                Thread.sleep(2_000);
                completed.set(true);
                return "tomorrow";
            } catch (InterruptedException e) {
                interrupted.set(true);
                Thread.currentThread().interrupt();
                throw new IllegalStateException("cancelled", e);
            }
        };

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CartAggregationService service = new CartAggregationService(
                    executor,
                    pricingClient,
                    inventoryClient,
                    shippingClient);

            assertThrows(CompletionException.class,
                    () -> service.loadCart("user-1", "trace-1", "sku-1", 1));

            assertTrue(interrupted.get() && !completed.get(),
                    "expected the slow sibling to be interrupted instead of running to normal completion");
        }
    }
}
