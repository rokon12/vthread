package ca.bazlur.migratecart.support;

public final class BlockingSupport {
    private BlockingSupport() {
    }

    public static void simulateIo(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted", e);
        }
    }
}
