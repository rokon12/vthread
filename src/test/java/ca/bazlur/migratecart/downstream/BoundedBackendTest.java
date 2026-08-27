package ca.bazlur.migratecart.downstream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BoundedBackendTest {
    @Test
    void servesCallsWithinCapacityAndRecordsThePeak() {
        BoundedBackend backend = new BoundedBackend(2, 0);

        assertEquals("in-stock:sku-1", backend.call("sku-1"));
        assertEquals("in-stock:sku-2", backend.call("sku-2"));

        assertEquals(0L, backend.rejections(), "sequential calls should never be rejected");
        assertEquals(1, backend.peakInFlight(), "sequential calls never overlap");
    }

    @Test
    void rejectsAndCountsCallersArrivingAboveCapacity() throws Exception {
        BoundedBackend backend = new BoundedBackend(1, 200);

        Thread holder = Thread.ofPlatform().start(() -> backend.call("sku-held"));
        while (backend.peakInFlight() < 1) {
            Thread.onSpinWait();
        }

        assertThrows(BackendOverloadedException.class, () -> backend.call("sku-rejected"));
        holder.join();

        assertEquals(1L, backend.rejections(), "the second concurrent caller should be rejected");
        assertEquals(2, backend.peakInFlight(), "the peak includes the rejected caller");
    }
}
