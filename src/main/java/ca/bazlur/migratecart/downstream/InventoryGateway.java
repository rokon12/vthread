package ca.bazlur.migratecart.downstream;

/**
 * Fetches inventory availability through the bounded backend.
 *
 * <p>Nothing here limits how many callers reach the backend at once. Under a fixed thread
 * pool that was invisible, because the pool did the limiting.
 */
public class InventoryGateway {
    private final BoundedBackend backend;

    public InventoryGateway(BoundedBackend backend) {
        this.backend = backend;
    }

    public String checkAvailability(String sku) {
        return backend.call(sku);
    }
}
