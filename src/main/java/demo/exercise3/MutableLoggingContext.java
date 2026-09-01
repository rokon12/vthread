package demo.exercise3;

public class MutableLoggingContext {
    // A ThreadLocal holding the current log level (e.g., "INFO", "DEBUG", etc.)
    private static final ThreadLocal<String> LOG_LEVEL = new ThreadLocal<>();

    public static void setLogLevel(String level) {
        LOG_LEVEL.set(level);
    }

    public static String getLogLevel() {
        return LOG_LEVEL.get();
    }

    public static void log(String message) {
        System.out.println("[" + getLogLevel() + "] " + message);
    }

    void main() throws InterruptedException {
        setLogLevel("INFO");
        log("Starting process...");

        new Thread(() -> {
            setLogLevel("DEBUG");
            log("Thread-specific debug mode enabled");
        }).start();

        Thread.sleep(100); // Wait for the thread to finish
        log("Continuing with INFO level...");
    }
}
