package ca.bazlur.migratecart.cart;

import ca.bazlur.migratecart.pricing.PricingClient;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class CartService {
    private final ExecutorService executor;
    private final PricingClient pricingClient;

    public CartService(ExecutorService executor, PricingClient pricingClient) {
        this.executor = executor;
        this.pricingClient = pricingClient;
    }

    public List<String> priceAll(List<String> skus) {
        try {
            List<Future<String>> futures = skus.stream()
                    .map(sku -> executor.submit(() -> pricingClient.fetchPrice(sku)))
                    .toList();

            List<String> result = new ArrayList<>();
            for (Future<String> future : futures) {
                result.add(future.get());
            }
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e.getCause());
        }
    }
}
