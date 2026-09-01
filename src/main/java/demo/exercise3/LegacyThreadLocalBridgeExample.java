package demo.exercise3;

import java.lang.ScopedValue;
import java.util.concurrent.Callable;

/**
 * Demonstrates a narrow, one-directional bridge from {@link ScopedValue} to a
 * legacy {@link ThreadLocal} context.
 *
 * <p>The {@code ScopedValue} is the application's source of truth. The bridge
 * copies its value into the legacy holder only while code that still requires
 * the {@code ThreadLocal} is running. It then restores the value that was in
 * the holder before the call. Restoring, instead of merely clearing, makes the
 * bridge safe when calls are nested.
 *
 * <p>In a Spring application, {@code LEGACY_REQUEST_CONTEXT} represents an API
 * backed by a {@code ThreadLocal}, for example:
 *
 * <ul>
 *   <li>SLF4J MDC: capture with {@code MDC.getCopyOfContextMap()}, install with
 *       {@code MDC.setContextMap(...)} or {@code MDC.put(...)}, and restore or
 *       clear in {@code finally}.</li>
 *   <li>Spring Security: capture with
 *       {@code SecurityContextHolder.getContext()}, install with
 *       {@code SecurityContextHolder.setContext(...)}, and restore with
 *       {@code setContext(...)} or {@code clearContext()}.</li>
 *   <li>Spring request attributes: capture with
 *       {@code RequestContextHolder.getRequestAttributes()}, install with
 *       {@code setRequestAttributes(...)}, and finish with
 *       {@code resetRequestAttributes()} or restoration of the previous
 *       attributes.</li>
 * </ul>
 *
 * <p>Place the bridge immediately around the framework boundary that needs
 * legacy context, such as a servlet filter chain, a library callback, or a
 * JDBC interceptor. Do not populate the legacy holder for the entire
 * application. If legacy code runs in a child task, enter the bridge inside
 * that child task: the scoped value is inherited by structured children, but
 * an ordinary {@code ThreadLocal} is not.
 *
 * <p>The central pattern is:
 *
 * <pre>{@code
 * ScopedValue.where(REQUEST_CONTEXT, requestContext).call(() ->
 *         callWithLegacyContext(legacyComponent::invoke));
 * }</pre>
 */
public final class LegacyThreadLocalBridgeExample {
    private static final ScopedValue<RequestContext> REQUEST_CONTEXT = ScopedValue.newInstance();
    private static final ThreadLocal<RequestContext> LEGACY_REQUEST_CONTEXT = new ThreadLocal<>();

    private LegacyThreadLocalBridgeExample() {
    }

    /**
     * Runs one legacy operation with the current scoped request context
     * temporarily installed in the legacy {@code ThreadLocal} holder.
     *
     * <p>This method deliberately performs four steps:
     *
     * <ol>
     *   <li>Capture the legacy value already associated with this thread.</li>
     *   <li>Copy the current scoped value into the legacy holder.</li>
     *   <li>Invoke the smallest possible unit of legacy code.</li>
     *   <li>Restore the captured value in a {@code finally} block.</li>
     * </ol>
     *
     * <p>If no request context is bound, the holder is cleared while the
     * operation runs. This prevents stale thread-local state from being
     * mistaken for the current request.
     *
     * @param operation legacy code that reads its context from a
     *                  {@code ThreadLocal}
     * @param <T> the operation's result type
     * @return the result returned by {@code operation}
     * @throws Exception if the legacy operation fails
     */
    public static <T> T callWithLegacyContext(Callable<T> operation) throws Exception {
        RequestContext previous = LEGACY_REQUEST_CONTEXT.get();
        RequestContext current = REQUEST_CONTEXT.isBound() ? REQUEST_CONTEXT.get() : null;

        try {
            replaceLegacyContext(current);
            return operation.call();
        } finally {
            replaceLegacyContext(previous);
        }
    }

    /**
     * Returns the context as legacy code would see it.
     *
     * <p>Application code should read {@link #REQUEST_CONTEXT} instead. This
     * accessor exists only to represent a framework or library API that cannot
     * yet consume a {@code ScopedValue} directly.
     *
     * @return the legacy context, or {@code null} outside a bridge
     */
    public static RequestContext legacyCurrentContext() {
        return LEGACY_REQUEST_CONTEXT.get();
    }

    /**
     * Runs a nested example to make restoration visible: the inner bridge sees
     * the inner request, leaving it restores the outer request, and leaving the
     * outer bridge removes the legacy context completely.
     *
     * @param args ignored
     * @throws Exception if an example operation fails
     */
    public static void main(String[] args) throws Exception {
        RequestContext outer = new RequestContext("user-7", "trace-123");
        RequestContext inner = new RequestContext("support-user", "trace-456");

        System.out.printf("Before bridge: %s%n", legacyCurrentContext());

        ScopedValue.where(REQUEST_CONTEXT, outer).call(() ->
                callWithLegacyContext(() -> {
                    System.out.printf("Outer legacy call: %s%n", legacyCurrentContext());

                    ScopedValue.where(REQUEST_CONTEXT, inner).call(() ->
                            callWithLegacyContext(() -> {
                                System.out.printf("Inner legacy call: %s%n", legacyCurrentContext());
                                return null;
                            }));

                    System.out.printf("Outer context restored: %s%n", legacyCurrentContext());
                    return null;
                }));

        System.out.printf("After bridge: %s%n", legacyCurrentContext());
    }

    private static void replaceLegacyContext(RequestContext context) {
        if (context == null) {
            LEGACY_REQUEST_CONTEXT.remove();
        } else {
            LEGACY_REQUEST_CONTEXT.set(context);
        }
    }

    /**
     * Minimal request metadata used by the example.
     *
     * @param userId authenticated or calling user identifier
     * @param traceId distributed tracing identifier
     */
    public record RequestContext(String userId, String traceId) {
    }
}
