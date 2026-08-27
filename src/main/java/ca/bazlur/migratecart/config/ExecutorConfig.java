package ca.bazlur.migratecart.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecutorConfig {
    public ExecutorService applicationExecutor() {
        return Executors.newFixedThreadPool(4);
    }
}
