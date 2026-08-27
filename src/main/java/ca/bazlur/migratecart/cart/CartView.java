package ca.bazlur.migratecart.cart;

public record CartView(
        String userId,
        String traceId,
        String sku,
        int quantity,
        String price,
        String inventoryStatus,
        String shippingEta) {
}
