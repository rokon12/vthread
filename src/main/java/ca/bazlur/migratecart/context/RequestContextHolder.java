package ca.bazlur.migratecart.context;

public final class RequestContextHolder {
    //private static final ThreadLocal<RequestContext> CURRENT = new ThreadLocal<>();
    public static final ScopedValue<RequestContext> CURRENT_REQUEST = ScopedValue.newInstance();

    private RequestContextHolder() {
    }

//    public static void set(RequestContext context) {
//        CURRENT.set(context);
//    }

    public static RequestContext get() {
        return CURRENT_REQUEST.isBound() ? CURRENT_REQUEST.get() : null;
    }

    public static RequestContext currentOrNull() {
        return CURRENT_REQUEST.isBound() ? CURRENT_REQUEST.get() : null;
    }
}
