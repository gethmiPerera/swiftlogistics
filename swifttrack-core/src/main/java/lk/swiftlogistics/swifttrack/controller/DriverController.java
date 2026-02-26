package lk.swiftlogistics.swifttrack.controller;

import lk.swiftlogistics.swifttrack.domain.Order;
import lk.swiftlogistics.swifttrack.domain.OrderStatus;
import lk.swiftlogistics.swifttrack.integrations.CmsSoapClient;
import lk.swiftlogistics.swifttrack.integrations.WmsTcpClient;
import lk.swiftlogistics.swifttrack.realtime.StatusPush;
import lk.swiftlogistics.swifttrack.repo.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/driver")
@CrossOrigin(origins = "*")
public class DriverController {

    private static final Logger log = LoggerFactory.getLogger(DriverController.class);
    private final OrderRepository repo;
    private final StatusPush ws;
    private final CmsSoapClient cms;
    private final WmsTcpClient wms;

    public DriverController(OrderRepository repo, StatusPush ws, CmsSoapClient cms, WmsTcpClient wms) {
        this.repo = repo;
        this.ws = ws;
        this.cms = cms;
        this.wms = wms;
    }

    /**
     * GET /api/driver/manifest?driverId=DRV-001
     * Returns only orders assigned to the specified driver.
     * Shows: READY_FOR_PICKUP (to collect), PICKED_UP (to deliver), DELIVERED, FAILED.
     */
    @GetMapping("/manifest")
    public List<Map<String, Object>> getManifest(@RequestParam(value = "driverId", required = false) String driverId) {
        List<Order> orders = repo.findAll();
        List<Map<String, Object>> manifest = new ArrayList<>();

        for (Order o : orders) {
            // Filter by driverId — drivers only see their own orders
            if (driverId != null && !driverId.isEmpty()
                    && o.getDriverId() != null
                    && !driverId.equals(o.getDriverId())) {
                continue;
            }

            // Include orders relevant to the driver workflow
            if (o.getStatus() == OrderStatus.READY_FOR_PICKUP
                    || o.getStatus() == OrderStatus.PICKED_UP
                    || o.getStatus() == OrderStatus.DELIVERED
                    || o.getStatus() == OrderStatus.FAILED) {

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", o.getDisplayId() != null ? o.getDisplayId() : o.getId().toString().substring(0, 8));
                item.put("uuid", o.getId());
                item.put("address", o.getDropAddress());
                item.put("pickupAddress", o.getPickupAddress());
                item.put("customer", o.getCustomerName() != null ? o.getCustomerName() : o.getClientId());
                item.put("contact", o.getContactNumber() != null ? o.getContactNumber() : "—");
                item.put("priority", o.getPriority());
                item.put("status", mapStatusForDriver(o.getStatus()));
                item.put("rawStatus", o.getStatus().name());
                item.put("eta", "—");
                item.put("notes", o.getFailureReason() != null ? o.getFailureReason() : "");
                manifest.add(item);
            }
        }
        return manifest;
    }

    /**
     * GET /api/driver/route?driverId=DRV-001
     * Returns route steps for the specified driver's orders.
     */
    @GetMapping("/route")
    public List<Map<String, Object>> getRoute(@RequestParam(value = "driverId", required = false) String driverId) {
        List<Order> orders = repo.findAll();
        List<Map<String, Object>> steps = new ArrayList<>();
        int seq = 1;

        for (Order o : orders) {
            // Filter by driverId
            if (driverId != null && !driverId.isEmpty()
                    && o.getDriverId() != null
                    && !driverId.equals(o.getDriverId())) {
                continue;
            }

            if (o.getStatus() == OrderStatus.READY_FOR_PICKUP
                    || o.getStatus() == OrderStatus.PICKED_UP
                    || o.getStatus() == OrderStatus.DELIVERED) {

                Map<String, Object> step = new LinkedHashMap<>();
                step.put("seq", seq++);
                step.put("address", o.getDropAddress());
                step.put("orderId", o.getDisplayId() != null ? o.getDisplayId() : o.getId().toString().substring(0, 8));
                step.put("status", o.getStatus() == OrderStatus.DELIVERED ? "completed"
                        : o.getStatus() == OrderStatus.PICKED_UP ? "current"
                        : "upcoming");
                steps.add(step);
            }
        }
        return steps;
    }

    /**
     * PUT /api/driver/deliveries/{id}
     * Status transitions:
     *   - "PickedUp"  → READY_FOR_PICKUP → PICKED_UP  (driver collected from warehouse)
     *   - "Delivered"  → PICKED_UP → DELIVERED          (driver delivered to customer)
     *   - "Failed"     → any → FAILED                   (delivery failed)
     * Propagates updates to CMS (SOAP) and WMS (TCP).
     */
    @PutMapping("/deliveries/{id}")
    public ResponseEntity<Map<String, Object>> updateDelivery(
            @PathVariable("id") String id,
            @RequestBody Map<String, String> payload) {

        // Find order by displayId or UUID
        Optional<Order> opt = repo.findAll().stream()
                .filter(o -> id.equals(o.getDisplayId()) ||
                        id.equals(o.getId().toString()))
                .findFirst();

        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Order o = opt.get();
        String newStatus = payload.get("status");

        if ("PickedUp".equalsIgnoreCase(newStatus)) {
            // Driver collected the package from warehouse
            o.setStatus(OrderStatus.PICKED_UP);
            log.info("Order {} PICKED UP by driver from warehouse", o.getDisplayId());
        } else if ("Delivered".equalsIgnoreCase(newStatus)) {
            // Driver delivered to customer
            o.setStatus(OrderStatus.DELIVERED);
            log.info("Order {} marked as DELIVERED by driver", o.getDisplayId());
        } else if ("Failed".equalsIgnoreCase(newStatus)) {
            o.setStatus(OrderStatus.FAILED);
            String reason = payload.getOrDefault("reason", "Unknown");
            o.setFailureReason(reason);
            log.info("Order {} marked as FAILED — reason: {}", o.getDisplayId(), reason);
        }

        repo.save(o);
        ws.push(o); // Push real-time update to all connected clients

        // ── Propagate status to backend systems (full middleware loop) ──
        propagateStatusUpdate(o);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", o.getDisplayId());
        response.put("status", o.getStatus().name());
        response.put("message", "Delivery updated and propagated to CMS & WMS");
        return ResponseEntity.ok(response);
    }

    /**
     * Propagates delivery status to CMS (SOAP) and WMS (TCP).
     * Flow: Driver App → Middleware → CMS + WMS → WebSocket → Client UI
     */
    private void propagateStatusUpdate(Order o) {
        String orderId = o.getId().toString();
        String status = o.getStatus().name();

        try {
            cms.updateStatus(orderId, status);
            log.info("Status propagated to CMS for order {}", o.getDisplayId());
        } catch (Exception e) {
            log.error("Failed to propagate status to CMS for order {}: {}", o.getDisplayId(), e.getMessage());
        }

        try {
            wms.updateStatus(orderId, status);
            log.info("Status propagated to WMS for order {}", o.getDisplayId());
        } catch (Exception e) {
            log.error("Failed to propagate status to WMS for order {}: {}", o.getDisplayId(), e.getMessage());
        }
    }

    private String mapStatusForDriver(OrderStatus s) {
        return switch (s) {
            case READY_FOR_PICKUP -> "Ready for Pickup";
            case PICKED_UP -> "Picked Up";
            case DELIVERED -> "Delivered";
            case FAILED -> "Failed";
            default -> "Pending";
        };
    }
}
