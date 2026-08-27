package ca.bazlur.migratecart.reporting;

import ca.bazlur.migratecart.observability.CountingThreadLocal;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Formats order timestamps for reporting.
 *
 * <p>{@link SimpleDateFormat} is not thread-safe, so it is cached per thread. That is the
 * standard workaround on a pooled executor, where the cache is populated once per pooled
 * thread and reused for the life of the process.
 */
public class OrderTimestampFormatter {
    private static final String PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private final AtomicLong formatterCreations = new AtomicLong();
    private final CountingThreadLocal<SimpleDateFormat> cached =
            new CountingThreadLocal<>(this::newFormatter);

    public String format(Instant instant) {
        return cached.get().format(Date.from(instant));
    }

    /**
     * How many times the expensive formatter object has actually been constructed.
     *
     * <p>This counts construction rather than {@code ThreadLocal} initialization on purpose:
     * the number stays meaningful after the cache is removed.
     */
    public long formatterCreations() {
        return formatterCreations.get();
    }

    private SimpleDateFormat newFormatter() {
        formatterCreations.incrementAndGet();
        SimpleDateFormat format = new SimpleDateFormat(PATTERN, Locale.ROOT);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format;
    }
}
