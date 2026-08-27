package ca.bazlur.migratecart.pricing;

import ca.bazlur.migratecart.support.BlockingSupport;

public class SlowPricingClient implements PricingClient {
    private final long delayMillis;
    private final String price;

    public SlowPricingClient(long delayMillis, String price) {
        this.delayMillis = delayMillis;
        this.price = price;
    }

    @Override
    public String fetchPrice(String sku) {
        BlockingSupport.simulateIo(delayMillis);
        return price;
    }
}
