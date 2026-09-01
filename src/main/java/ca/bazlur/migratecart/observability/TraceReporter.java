package ca.bazlur.migratecart.observability;

import ca.bazlur.migratecart.support.BlockingSupport;

public class TraceReporter {

    public String report(String traceId, String userId, String operation) {
        BlockingSupport.simulateIo(20);
        return "span[" + traceId + "/" + userId + "/" + operation + "]";
    }
}
