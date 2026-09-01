package ca.bazlur.migratecart.cart;

import ca.bazlur.migratecart.context.RequestContext;
import ca.bazlur.migratecart.context.RequestContextHolder;
import ca.bazlur.migratecart.observability.AuditTrail;
import ca.bazlur.migratecart.observability.TraceReporter;
import ca.bazlur.migratecart.pricing.PricingClient;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class CartFacade {
    private final ExecutorService executor;
    private final PricingClient pricingClient;
    private final AuditTrail auditTrail = new AuditTrail();
    private final TraceReporter traceReporter = new TraceReporter();

    public CartFacade(ExecutorService executor, PricingClient pricingClient) {
        this.executor = executor;
        this.pricingClient = pricingClient;
    }

    public CartView handleRequest(String userId, String traceId, String sku, int quantity) {
        RequestContextHolder.set(new RequestContext(userId, traceId));

        try {
            Future<String> price = executor.submit(() -> pricingClient.fetchPrice(sku));
            Future<String> auditedUserId = executor.submit(this::auditCartAccess);
            Future<String> reportedTraceId = executor.submit(this::reportCartSpan);
            return new CartView(
                    auditedUserId.get(),
                    reportedTraceId.get(),
                    sku,
                    quantity,
                    price.get(),
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

    private String auditCartAccess() {
        RequestContext context = RequestContextHolder.get();
        if (context == null) {
            return "<missing>";
        }
        auditTrail.record(context.traceId(), context.userId(), "cart-read");
        return context.userId();
    }

    private String reportCartSpan() {
        RequestContext context = RequestContextHolder.get();
        if (context == null) {
            return "<missing>";
        }
        traceReporter.report(context.traceId(), context.userId(), "cart.load");
        return context.traceId();
    }
}
