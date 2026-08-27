package ca.bazlur.migratecart.exercise3;

import ca.bazlur.migratecart.cart.CartFacade;
import ca.bazlur.migratecart.context.RequestContextHolder;
import ca.bazlur.migratecart.pricing.SlowPricingClient;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class ContextPropagationTest {
    @AfterEach
    void cleanup() {
        RequestContextHolder.clear();
    }

    @Test
    void requestContextShouldBeClearedAfterTheRequestCompletes() {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CartFacade facade = new CartFacade(executor, new SlowPricingClient(10, "$42.00"));

            facade.handleRequest("user-1", "trace-A", "sku-1", 1);

            assertNull(facade.currentContext(),
                    "expected request context to be cleared instead of leaking on the caller thread");
        }
    }
}
