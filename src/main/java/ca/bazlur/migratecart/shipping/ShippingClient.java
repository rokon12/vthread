package ca.bazlur.migratecart.shipping;

@FunctionalInterface
public interface ShippingClient {
    String estimate(String sku, int quantity);
}
