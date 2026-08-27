package ca.bazlur.migratecart.config;

import ca.bazlur.migratecart.cart.CartAggregationService;
import ca.bazlur.migratecart.inventory.InventoryClient;
import ca.bazlur.migratecart.pricing.PricingClient;
import ca.bazlur.migratecart.pricing.SlowPricingClient;
import ca.bazlur.migratecart.shipping.ShippingClient;
import ca.bazlur.migratecart.support.BlockingSupport;
import java.util.concurrent.ExecutorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MigrateCartConfiguration {
    @Bean(destroyMethod = "close")
    ExecutorService applicationExecutor() {
        return new ExecutorConfig().applicationExecutor();
    }

    @Bean
    PricingClient pricingClient() {
        return new SlowPricingClient(100, "$42.00");
    }

    @Bean
    InventoryClient inventoryClient() {
        return (sku, quantity) -> {
            BlockingSupport.simulateIo(80);
            return quantity <= 10 ? "in-stock" : "back-order";
        };
    }

    @Bean
    ShippingClient shippingClient() {
        return (sku, quantity) -> {
            BlockingSupport.simulateIo(120);
            return quantity <= 5 ? "tomorrow" : "in 3 days";
        };
    }

    @Bean
    CartAggregationService cartAggregationService(
            ExecutorService applicationExecutor,
            PricingClient pricingClient,
            InventoryClient inventoryClient,
            ShippingClient shippingClient) {
        return new CartAggregationService(
                applicationExecutor,
                pricingClient,
                inventoryClient,
                shippingClient);
    }
}
