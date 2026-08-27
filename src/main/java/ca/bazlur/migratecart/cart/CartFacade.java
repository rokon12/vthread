package ca.bazlur.migratecart.cart;

import ca.bazlur.migratecart.context.RequestContext;
import ca.bazlur.migratecart.context.RequestContextHolder;
import ca.bazlur.migratecart.pricing.PricingClient;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

public class CartFacade {
    private final ExecutorService executor;
    private final PricingClient pricingClient;

    public CartFacade(ExecutorService executor, PricingClient pricingClient) {
        this.executor = executor;
        this.pricingClient = pricingClient;
    }

    public CartView handleRequest(String userId, String traceId, String sku, int quantity) {
        RequestContextHolder.set(new RequestContext(userId, traceId));

        try {
            String price = pricingClient.fetchPrice(sku);
            String childUserId = executor.submit(this::currentUserId).get();
            String childTraceId = executor.submit(this::currentTraceId).get();
            return new CartView(
                    childUserId,
                    childTraceId,
                    sku,
                    quantity,
                    price,
                    "in-stock",
                    "tomorrow");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e.getCause());
        }
    }

    public RequestContext currentContext() {
        return RequestContextHolder.get();
    }

    private String currentUserId() {
        RequestContext context = RequestContextHolder.get();
        return context == null ? "<missing>" : context.userId();
    }

    private String currentTraceId() {
        RequestContext context = RequestContextHolder.get();
        return context == null ? "<missing>" : context.traceId();
    }
}
