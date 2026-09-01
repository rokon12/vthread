package ca.bazlur.migratecart.observability;

import ca.bazlur.migratecart.support.BlockingSupport;

public class AuditTrail {

    public String record(String traceId, String userId, String action) {
        BlockingSupport.simulateIo(20);
        return "audit[" + traceId + "/" + userId + "/" + action + "]";
    }
}
