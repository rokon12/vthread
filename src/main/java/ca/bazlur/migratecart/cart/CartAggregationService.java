package ca.bazlur.migratecart.cart;

import ca.bazlur.migratecart.inventory.InventoryClient;
import ca.bazlur.migratecart.pricing.PricingClient;
import ca.bazlur.migratecart.shipping.ShippingClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.StructuredTaskScope;

public class CartAggregationService {
    private final Executor executor;
    private final PricingClient pricingClient;
    private final InventoryClient inventoryClient;
    private final ShippingClient shippingClient;

    public CartAggregationService(
            Executor executor,
            PricingClient pricingClient,
            InventoryClient inventoryClient,
            ShippingClient shippingClient) {
        this.executor = executor;
        this.pricingClient = pricingClient;
        this.inventoryClient = inventoryClient;
        this.shippingClient = shippingClient;
    }

    public CartView loadCart(String userId, String traceId, String sku, int quantity) {
        try (var scope = StructuredTaskScope.open()) {
            var priceTask = scope.fork(() -> pricingClient.fetchPrice(sku));
            var availabilityTask = scope.fork(() -> inventoryClient.checkAvailability(sku, quantity));
            var estimate = scope.fork(() -> shippingClient.estimate(sku, quantity));

            scope.join();

            return new CartView(
                    userId,
                    traceId,
                    sku,
                    quantity,
                    priceTask.get(),
                    availabilityTask.get(),
                    estimate.get());
        }  catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (RuntimeException e) {
            throw new CompletionException(e);
        }
    }
}
