package ca.bazlur.migratecart.inventory;

@FunctionalInterface
public interface InventoryClient {
    String checkAvailability(String sku, int quantity);
}
