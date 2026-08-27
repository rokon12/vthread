package ca.bazlur.migratecart.downstream;

/**
 * Signals that a caller arrived at a capacity-limited resource that had no room for it.
 *
 * <p>Unchecked, matching how a connection-pool timeout surfaces through most data-access
 * layers.
 */
public class BackendOverloadedException extends RuntimeException {
    public BackendOverloadedException(String message) {
        super(message);
    }
}
