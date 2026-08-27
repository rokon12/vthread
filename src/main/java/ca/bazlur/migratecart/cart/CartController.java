package ca.bazlur.migratecart.cart;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/carts")
public class CartController {
    private final CartAggregationService cartService;

    public CartController(CartAggregationService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/{sku}")
    public CartView loadCart(
            @PathVariable String sku,
            @RequestParam(defaultValue = "1") int quantity,
            @RequestHeader(name = "X-User-Id", defaultValue = "workshop-user") String userId,
            @RequestHeader(name = "X-Trace-Id", defaultValue = "trace-demo") String traceId) {
        if (quantity < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be greater than zero");
        }
        return cartService.loadCart(userId, traceId, sku, quantity);
    }
}
