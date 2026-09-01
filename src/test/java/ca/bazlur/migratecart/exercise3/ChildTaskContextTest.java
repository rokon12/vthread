package ca.bazlur.migratecart.exercise3;

import ca.bazlur.migratecart.cart.CartFacade;
import ca.bazlur.migratecart.cart.CartView;
import ca.bazlur.migratecart.context.RequestContextHolder;
import ca.bazlur.migratecart.pricing.SlowPricingClient;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChildTaskContextTest {

    @Test
    void childTasksShouldObserveTheBoundRequestContext() {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CartFacade facade = new CartFacade(executor, new SlowPricingClient(10, "$42.00"));

            CartView view = facade.handleRequest("user-7", "trace-123", "sku-1", 1);

            assertEquals("user-7", view.userId());
            assertEquals("trace-123", view.traceId());
        }
    }
}
