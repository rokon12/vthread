package ca.bazlur.migratecart.pricing;

@FunctionalInterface
public interface PricingClient {
    String fetchPrice(String sku);
}
