package ca.bazlur.migratecart.diagnostics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

public class PinningLoadService implements AutoCloseable {
    private final HotPathInventoryCache cache;
    private final ExecutorService executor;

    public PinningLoadService(HotPathInventoryCache cache, ExecutorService executor) {
        this.cache = cache;
        this.executor = executor;
    }

    public List<String> runLoad(List<String> skus) {
        try {
            var futures = skus.stream()
                    .map(sku -> executor.submit(() -> cache.refreshAndRead(sku)))
                    .toList();

            List<String> results = new ArrayList<>();
            for (var future : futures) {
                results.add(future.get());
            }
            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e.getCause());
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
