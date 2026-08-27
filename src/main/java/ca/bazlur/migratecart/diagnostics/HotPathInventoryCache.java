package ca.bazlur.migratecart.diagnostics;

import ca.bazlur.migratecart.support.BlockingSupport;

public class HotPathInventoryCache {
    public synchronized String refreshAndRead(String sku) {
        BlockingSupport.simulateIo(100);
        return "in-stock";
    }
}
