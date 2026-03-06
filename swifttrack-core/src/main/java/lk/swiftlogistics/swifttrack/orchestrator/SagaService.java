package lk.swiftlogistics.swifttrack.orchestrator;

import lk.swiftlogistics.swifttrack.domain.Order;
import lk.swiftlogistics.swifttrack.domain.OrderStatus;
import lk.swiftlogistics.swifttrack.integrations.*;
import lk.swiftlogistics.swifttrack.repo.OrderRepository;
import lk.swiftlogistics.swifttrack.realtime.StatusPush;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class SagaService {

    private static final Logger log = LoggerFactory.getLogger(SagaService.class);
    private static final int MAX_ROS_RETRIES = 3;

    private final OrderRepository repo;
    private final CmsSoapClient cms;
    private final WmsTcpClient wms;
    private final RosClient ros;
    private final StatusPush ws;

    public SagaService(OrderRepository repo,
                       CmsSoapClient cms,
                       WmsTcpClient wms,
                       RosClient ros,
                       StatusPush ws) {
        this.repo = repo;
        this.cms = cms;
        this.wms = wms;
        this.ros = ros;
        this.ws = ws;
    }

    public void process(UUID orderId) {
        Order o = repo.findById(orderId).orElseThrow();
        log.info("SAGA START — processing order {}", orderId);

        // Track which steps completed for compensation
        boolean cmsCreated = false;
        boolean wmsRegistered = false;

        try {
            // ── Step 1: CMS (SOAP) ──────────────────────────────
            log.info("Step 1/3 — calling CMS-SOAP for order {}", orderId);
            o.setCmsOrderId(cms.createOrder(orderId.toString()));
            o.setStatus(OrderStatus.CMS_CREATED);
            repo.save(o);
            ws.push(o);
            cmsCreated = true;
            log.info("Step 1/3 DONE — CMS order created: {}", o.getCmsOrderId());

            // ── Step 2: WMS (TCP) ───────────────────────────────
            log.info("Step 2/3 — calling WMS-TCP for order {}", orderId);
            wms.register(orderId.toString());
            o.setStatus(OrderStatus.WMS_REGISTERED);
            repo.save(o);
            ws.push(o);
            wmsRegistered = true;
            log.info("Step 2/3 DONE — WMS registered");

            // ── Step 3: ROS (REST) — with retry & exponential back-off ──
            Map<String, String> rosResult = callRosWithRetry(o);
            o.setRouteId(rosResult.get("routeId"));
            o.setDriverId(rosResult.getOrDefault("driverAssigned", "DRV-001"));
            o.setStatus(OrderStatus.ROUTE_PLANNED);
            repo.save(o);
            ws.push(o);

            // ── All steps succeeded ─────────────────────────────
            o.setStatus(OrderStatus.READY_FOR_PICKUP);
            repo.save(o);
            ws.push(o);
            log.info("SAGA COMPLETE — order {} is READY_FOR_PICKUP at warehouse, driver: {}", orderId, o.getDriverId());

        } catch (Exception e) {
            log.error("SAGA FAILED — order {} — reason: {}", orderId, e.getMessage(), e);

            // ── COMPENSATION (Saga rollback) ────────────────────
            compensate(orderId.toString(), cmsCreated, wmsRegistered, e.getMessage());

            o.setStatus(OrderStatus.FAILED);
            o.setFailureReason(e.getMessage());
            repo.save(o);
            ws.push(o);
        }
    }

    /**
     * Saga compensation — reverse completed steps in reverse order.
     * This is a key Saga pattern: if step N fails, undo steps N-1, N-2, etc.
     */
    private void compensate(String orderId, boolean cmsCreated, boolean wmsRegistered, String reason) {
        log.warn("COMPENSATION START — reversing completed steps for order {}", orderId);

        // Reverse step 2: Release from WMS
        if (wmsRegistered) {
            log.info("COMPENSATION — releasing order {} from WMS", orderId);
            wms.release(orderId);
        }

        // Reverse step 1: Cancel in CMS
        if (cmsCreated) {
            log.info("COMPENSATION — cancelling order {} in CMS", orderId);
            cms.cancelOrder(orderId, reason);
        }

        log.warn("COMPENSATION COMPLETE — order {} rolled back", orderId);
    }

    /**
     * Calls ROS planRoute with up to MAX_ROS_RETRIES attempts.
     * Uses exponential back-off: 1 s → 2 s → 4 s.
     * Returns a Map with routeId and driverAssigned.
     */
    private Map<String, String> callRosWithRetry(Order o) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_ROS_RETRIES; attempt++) {
            try {
                log.info("Step 3/3 — ROS attempt {}/{} for order {}", attempt, MAX_ROS_RETRIES, o.getId());
                Map<String, String> result = ros.planRoute(o.getId().toString());
                log.info("Step 3/3 DONE — route planned: {}, driver: {}", result.get("routeId"), result.get("driverAssigned"));
                return result;
            } catch (Exception e) {
                lastException = e;
                o.setRetryCount(attempt);
                repo.save(o);

                if (attempt < MAX_ROS_RETRIES) {
                    long delayMs = (long) Math.pow(2, attempt - 1) * 1000; // 1s, 2s, 4s
                    log.warn("ROS retry attempt {}/{} failed — waiting {}ms before next attempt. Reason: {}",
                            attempt, MAX_ROS_RETRIES, delayMs, e.getMessage());
                    ws.push(o); // push retry status to UI
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                } else {
                    log.error("ROS failed after {}/{} attempts. Reason: {}",
                            attempt, MAX_ROS_RETRIES, e.getMessage());
                }
            }
        }

        throw new RuntimeException("ROS route optimization failed after " + MAX_ROS_RETRIES +
                " retries: " + (lastException != null ? lastException.getMessage() : "unknown"));
    }
}