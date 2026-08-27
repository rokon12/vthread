package ca.bazlur.migratecart.exercise1;

import ca.bazlur.migratecart.config.ExecutorConfig;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class VirtualThreadExecutorTest {
    @Test
    void tasksRunOnVirtualThreads() throws Exception {
        ExecutorConfig config = new ExecutorConfig();
        try (ExecutorService executor = config.applicationExecutor()) {
            var future = executor.submit(Thread::currentThread);
            assertTrue(future.get().isVirtual(), "expected exercise 1 starter to be migrated to virtual threads");
        }
    }
}
