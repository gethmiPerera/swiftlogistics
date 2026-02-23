package lk.swiftlogistics.swifttrack.api;

import lk.swiftlogistics.swifttrack.api.dto.CreateOrderRequest;
import lk.swiftlogistics.swifttrack.api.dto.CreateOrderResponse;
import lk.swiftlogistics.swifttrack.domain.Order;
import lk.swiftlogistics.swifttrack.domain.OrderStatus;
import lk.swiftlogistics.swifttrack.messaging.EventPublisher;
import lk.swiftlogistics.swifttrack.repo.OrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository repo;
    private final EventPublisher publisher;

    public OrderController(OrderRepository repo, EventPublisher publisher) {
        this.repo = repo;
        this.publisher = publisher;
    }

    @PostMapping
    public ResponseEntity<CreateOrderResponse> create(@RequestBody CreateOrderRequest req) {
        Order o = new Order();
        o.setId(UUID.randomUUID());
        o.setClientId(req.clientId);
        o.setPickupAddress(req.pickupAddress);
        o.setDropAddress(req.dropAddress);
        o.setPriority(req.priority);
        o.setStatus(OrderStatus.RECEIVED);

        repo.save(o);
        publisher.publishOrderCreated(o.getId());

        return ResponseEntity.accepted().body(new CreateOrderResponse(o.getId(), o.getStatus().name()));
    }
}