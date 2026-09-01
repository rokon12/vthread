package ca.bazlur.migratecart.cart;

import ca.bazlur.migratecart.context.RequestContext;
import ca.bazlur.migratecart.context.RequestContextHolder;
import ca.bazlur.migratecart.observability.AuditTrail;
import ca.bazlur.migratecart.observability.TraceReporter;
import ca.bazlur.migratecart.pricing.PricingClient;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.StructuredTaskScope;

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

        return ScopedValue.where(RequestContextHolder.CURRENT_REQUEST, new RequestContext(userId, traceId))
                .call(() -> buildCartView(sku, quantity));
    }

    private CartView buildCartView(String sku, int quantity) {
        try (var scope = StructuredTaskScope.<String, Void>open(
                StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow(),
                config -> config.withName("request-context").withThreadFactory(Thread.ofVirtual().factory()))) {
            String price = pricingClient.fetchPrice(sku);
            var userIdTask = scope.fork(this::auditCartAccess);
            var traceIdTask = scope.fork(this::reportCartSpan);

            scope.join();

            return new CartView(
                    userIdTask.get(),
                    traceIdTask.get(),
                    sku,
                    quantity,
                    price,
                    "in-stock",
                    "tomorrow");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }


    public RequestContext currentContext() {
        return RequestContextHolder.get();
    }

    private String auditCartAccess() {
        RequestContext context = RequestContextHolder.currentOrNull();
        if (context == null) {
            return "<missing>";
        }
        auditTrail.record(context.traceId(), context.userId(), "cart-read");
        return context.userId();
    }

    private String reportCartSpan() {
        RequestContext context = RequestContextHolder.currentOrNull();
        if (context == null) {
            return "<missing>";
        }
        traceReporter.report(context.traceId(), context.userId(), "cart.load");
        return context.traceId();
    }
}
