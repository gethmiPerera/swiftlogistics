package lk.swiftlogistics.swifttrack.controller;

import lk.swiftlogistics.swifttrack.api.dto.CreateOrderRequest;
import lk.swiftlogistics.swifttrack.domain.Order;
import lk.swiftlogistics.swifttrack.domain.OrderStatus;
import lk.swiftlogistics.swifttrack.messaging.EventPublisher;
import lk.swiftlogistics.swifttrack.repo.OrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderRepository repo;
    private final EventPublisher publisher;

    /* Simple counter for display IDs (SL-1001, SL-1002, ...) */
    private final AtomicInteger displayIdCounter = new AtomicInteger(1000);

    public OrderController(OrderRepository repo, EventPublisher publisher) {
        this.repo = repo;
        this.publisher = publisher;
    }

    /** POST /api/orders — create a new order (accepts frontend form fields) */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody CreateOrderRequest req) {
        Order o = new Order();
        o.setId(UUID.randomUUID());

        // Generate sequential display ID
        String displayId = "SL-" + displayIdCounter.incrementAndGet();
        o.setDisplayId(displayId);

        // Map frontend fields (customer/address/contact) with fallback to API fields
        String customerName = req.customer != null ? req.customer : req.clientId;
        o.setClientId(customerName);
        o.setCustomerName(customerName);
        o.setDropAddress(req.address != null ? req.address : req.dropAddress);
        o.setPickupAddress(req.pickupAddress != null ? req.pickupAddress : "SwiftLogistics Warehouse");
        o.setContactNumber(req.contact);
        o.setPriority(req.priority != null ? req.priority : "Normal");
        o.setPackageDetails(req.packageDetails);
        o.setDriverId("Unassigned");
        o.setStatus(OrderStatus.RECEIVED);

        repo.save(o);
        publisher.publishOrderCreated(o.getId());

        // Return response that matches what the frontend expects
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("orderId", o.getId());
        response.put("displayId", displayId);
        response.put("status", o.getStatus().name());
        return ResponseEntity.accepted().body(response);
    }

    /** GET /api/orders — list all orders in frontend-friendly format */
    @GetMapping
    public List<Map<String, Object>> listAll() {
        List<Order> orders = repo.findAll();
        // Sort by createdAt descending (newest first)
        orders.sort((a, b) -> {
            if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });
        List<Map<String, Object>> result = new ArrayList<>();
        for (Order o : orders) {
            result.add(toDisplayMap(o));
        }
        return result;
    }

    /** GET /api/orders/{id} — get single order by UUID */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable("id") UUID id) {
        return repo.findById(id)
                .map(o -> ResponseEntity.ok(toDisplayMap(o)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** GET /api/orders/{id}/track — tracking timeline for an order */
    @GetMapping("/{id}/track")
    public ResponseEntity<Map<String, Object>> track(@PathVariable("id") UUID id) {
        Optional<Order> opt = repo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Order o = opt.get();
        List<Map<String, Object>> timeline = buildTimeline(o);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("order", toDisplayMap(o));
        result.put("timeline", timeline);
        return ResponseEntity.ok(result);
    }

    /* ── Helpers ── */

    private Map<String, Object> toDisplayMap(Order o) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", o.getDisplayId() != null ? o.getDisplayId() : o.getId().toString().substring(0, 8));
        m.put("uuid", o.getId());
        m.put("customer", o.getCustomerName() != null ? o.getCustomerName() : o.getClientId());
        m.put("address", o.getDropAddress());
        m.put("contact", o.getContactNumber() != null ? o.getContactNumber() : "—");
        m.put("priority", o.getPriority());
        m.put("status", mapStatusForDisplay(o.getStatus()));
        m.put("driver", o.getDriverId() != null ? o.getDriverId() : "Unassigned");
        m.put("updated", o.getUpdatedAt() != null ? o.getUpdatedAt().format(fmt) : "—");
        m.put("routeId", o.getRouteId());
        m.put("failureReason", o.getFailureReason());
        m.put("retryCount", o.getRetryCount());
        return m;
    }

    /** Map internal statuses to display-friendly names */
    private String mapStatusForDisplay(OrderStatus s) {
        if (s == null) return "Pending";
        return switch (s) {
            case RECEIVED -> "Pending";
            case CMS_CREATED, WMS_REGISTERED -> "Processing";
            case ROUTE_PLANNED -> "Processing";
            case READY_FOR_PICKUP -> "Ready for Pickup";
            case PICKED_UP -> "In Transit";
            case DELIVERED -> "Delivered";
            case FAILED -> "Failed";
        };
    }

    private List<Map<String, Object>> buildTimeline(Order o) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String ts = o.getUpdatedAt() != null ? o.getUpdatedAt().format(fmt) : "";

        List<Map<String, Object>> timeline = new ArrayList<>();
        int statusOrdinal = o.getStatus().ordinal();

        String[][] steps = {
            {"Order Received", "RECEIVED"},
            {"Sent to CMS (SOAP/XML)", "CMS_CREATED"},
            {"Registered in WMS (TCP)", "WMS_REGISTERED"},
            {"Route Optimised (ROS/REST)", "ROUTE_PLANNED"},
            {"Processing in Warehouse", "READY_FOR_PICKUP"},
            {"Picked Up by Driver", "PICKED_UP"},
            {"Delivered", "DELIVERED"},
        };

        for (String[] step : steps) {
            OrderStatus stepStatus = OrderStatus.valueOf(step[1]);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("step", step[0]);
            item.put("done", statusOrdinal >= stepStatus.ordinal() && o.getStatus() != OrderStatus.FAILED);
            item.put("current", o.getStatus() == stepStatus);
            item.put("time", statusOrdinal >= stepStatus.ordinal() ? ts : "");
            timeline.add(item);
        }

        // If failed, add failure entry
        if (o.getStatus() == OrderStatus.FAILED) {
            Map<String, Object> failItem = new LinkedHashMap<>();
            failItem.put("step", "Failed: " + (o.getFailureReason() != null ? o.getFailureReason() : "Unknown error"));
            failItem.put("done", false);
            failItem.put("current", true);
            failItem.put("time", ts);
            timeline.add(failItem);
        }

        return timeline;
    }
}