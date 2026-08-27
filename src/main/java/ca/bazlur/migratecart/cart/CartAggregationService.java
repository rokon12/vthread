package ca.bazlur.migratecart.cart;

import ca.bazlur.migratecart.inventory.InventoryClient;
import ca.bazlur.migratecart.pricing.PricingClient;
import ca.bazlur.migratecart.shipping.ShippingClient;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

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
        CompletableFuture<String> priceFuture = CompletableFuture.supplyAsync(
                () -> pricingClient.fetchPrice(sku), executor);
        CompletableFuture<String> inventoryFuture = CompletableFuture.supplyAsync(
                () -> inventoryClient.checkAvailability(sku, quantity), executor);
        CompletableFuture<String> shippingFuture = CompletableFuture.supplyAsync(
                () -> shippingClient.estimate(sku, quantity), executor);

        CompletableFuture.allOf(priceFuture, inventoryFuture, shippingFuture).join();

        return new CartView(
                userId,
                traceId,
                sku,
                quantity,
                priceFuture.join(),
                inventoryFuture.join(),
                shippingFuture.join());
    }
}
