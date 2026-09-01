# Bridging Scoped Values to Legacy ThreadLocal Context

Migrating request context from `ThreadLocal` to `ScopedValue` gives the context
a clear lifetime: it exists only for the operation in which it is bound and is
inherited by structured child tasks. A real Spring application, however, rarely
controls every component it calls. Logging, security, tracing, and framework
extensions may still expect their context in a `ThreadLocal`.

The practical answer is a narrow, one-directional bridge:

```text
ScopedValue -> install ThreadLocal state -> invoke legacy code -> restore state
```

`ScopedValue` remains the application's source of truth. The bridge installs
the context immediately before calling legacy code and restores the previous
thread-local value in a `finally` block.

## Complete runnable example

The following is a complete Java 25 program. Its `LEGACY_REQUEST_CONTEXT`
stands in for a thread-local API such as SLF4J MDC, Spring Security's
`SecurityContextHolder`, or Spring's `RequestContextHolder`.

```java
import java.lang.ScopedValue;
import java.util.concurrent.Callable;

public final class LegacyThreadLocalBridgeExample {
    private static final ScopedValue<RequestContext> REQUEST_CONTEXT =
            ScopedValue.newInstance();

    // Represents a framework or library API that still uses ThreadLocal.
    private static final ThreadLocal<RequestContext> LEGACY_REQUEST_CONTEXT =
            new ThreadLocal<>();

    private LegacyThreadLocalBridgeExample() {
    }

    /**
     * Temporarily exposes the current scoped context through the legacy
     * ThreadLocal while the supplied operation runs.
     */
    public static <T> T callWithLegacyContext(Callable<T> operation)
            throws Exception {
        RequestContext previous = LEGACY_REQUEST_CONTEXT.get();
        RequestContext current = REQUEST_CONTEXT.isBound()
                ? REQUEST_CONTEXT.get()
                : null;

        try {
            replaceLegacyContext(current);
            return operation.call();
        } finally {
            // Restore rather than merely clear so nested bridges are safe.
            replaceLegacyContext(previous);
        }
    }

    /**
     * Represents how legacy code obtains its current context.
     */
    public static RequestContext legacyCurrentContext() {
        return LEGACY_REQUEST_CONTEXT.get();
    }

    private static void replaceLegacyContext(RequestContext context) {
        if (context == null) {
            LEGACY_REQUEST_CONTEXT.remove();
        } else {
            LEGACY_REQUEST_CONTEXT.set(context);
        }
    }

    public static void main(String[] args) throws Exception {
        RequestContext outer =
                new RequestContext("user-7", "trace-123");
        RequestContext inner =
                new RequestContext("support-user", "trace-456");

        System.out.printf("Before bridge: %s%n", legacyCurrentContext());

        ScopedValue.where(REQUEST_CONTEXT, outer).call(() ->
                callWithLegacyContext(() -> {
                    System.out.printf(
                            "Outer legacy call: %s%n",
                            legacyCurrentContext());

                    ScopedValue.where(REQUEST_CONTEXT, inner).call(() ->
                            callWithLegacyContext(() -> {
                                System.out.printf(
                                        "Inner legacy call: %s%n",
                                        legacyCurrentContext());
                                return null;
                            }));

                    System.out.printf(
                            "Outer context restored: %s%n",
                            legacyCurrentContext());
                    return null;
                }));

        System.out.printf("After bridge: %s%n", legacyCurrentContext());
    }

    public record RequestContext(String userId, String traceId) {
    }
}
```

Compile and run it from the example directory:

```shell
javac --enable-preview --release 25 LegacyThreadLocalBridgeExample.java
java --enable-preview LegacyThreadLocalBridgeExample
```

The output makes the lifetime and restoration behavior visible:

```text
Before bridge: null
Outer legacy call: RequestContext[userId=user-7, traceId=trace-123]
Inner legacy call: RequestContext[userId=support-user, traceId=trace-456]
Outer context restored: RequestContext[userId=user-7, traceId=trace-123]
After bridge: null
```

## Why restoration matters

Clearing the `ThreadLocal` after every call is insufficient. If bridges are
nested, the inner call would erase the outer call's context. Capturing and
restoring the previous value gives the bridge stack-like behavior. The
`finally` block also guarantees restoration when legacy code throws an
exception.

When no scoped context is bound, the bridge temporarily clears the legacy
holder. This prevents stale thread-local state from being mistaken for the
current request.

## Mapping the example to Spring

The lifecycle stays the same; only the capture, install, and restore operations
change:

- For SLF4J MDC, capture with `MDC.getCopyOfContextMap()`, install with
  `MDC.setContextMap(...)` or `MDC.put(...)`, and restore the captured map or
  call `MDC.clear()`.
- For Spring Security, capture with `SecurityContextHolder.getContext()`,
  install with `SecurityContextHolder.setContext(...)`, and restore the
  captured context or call `SecurityContextHolder.clearContext()`.
- For Spring request attributes, capture with
  `RequestContextHolder.getRequestAttributes()`, install with
  `RequestContextHolder.setRequestAttributes(...)`, and restore the captured
  attributes or call `RequestContextHolder.resetRequestAttributes()`.

Place the bridge immediately around the smallest framework boundary that needs
it: a servlet filter chain, a library callback, or a JDBC interceptor. Do not
populate the legacy holder for the entire application, and do not copy values
back from the `ThreadLocal` into the `ScopedValue`. A one-directional bridge
avoids creating two competing sources of truth.

Finally, an ordinary `ThreadLocal` is not inherited by a structured child task.
The child inherits the scoped value, so any child that calls legacy code must
enter the bridge on its own thread, immediately around that call.

The same complete program, with more detailed JavaDoc, is available as
[`LegacyThreadLocalBridgeExample.java`](./LegacyThreadLocalBridgeExample.java).
