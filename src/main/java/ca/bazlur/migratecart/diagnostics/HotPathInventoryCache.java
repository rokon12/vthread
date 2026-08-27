package ca.bazlur.migratecart.diagnostics;

import ca.bazlur.migratecart.support.BlockingSupport;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class HotPathInventoryCache {
    private final Map<String, String> entries = new HashMap<>();
    private final Function<String, String> loader;

    public HotPathInventoryCache() {
        this(HotPathInventoryCache::loadInventory);
    }

    public HotPathInventoryCache(Function<String, String> loader) {
        this.loader = Objects.requireNonNull(loader);
    }

    public synchronized String refreshAndRead(String sku) {
        String cached = entries.get(sku);
        if (cached != null) {
            return cached;
        }

        String loaded = loader.apply(sku);
        entries.put(sku, loaded);
        return loaded;
    }

    private static String loadInventory(String sku) {
        BlockingSupport.simulateIo(100);
        return "in-stock";
    }
}
