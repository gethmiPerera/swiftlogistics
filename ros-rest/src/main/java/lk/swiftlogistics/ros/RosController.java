package lk.swiftlogistics.ros;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Random;

@RestController
public class RosController {

  private final Random rnd = new Random();

  @Value("${ros.failRate}")
  private double failRate;

  @PostMapping("/optimize")
  public ResponseEntity<?> optimize(@RequestHeader(value="X-API-KEY", required=false) String apiKey,
                                   @RequestBody Map<String, Object> body) {
    if (apiKey == null || !apiKey.equals("demo-key")) {
      return ResponseEntity.status(401).body(Map.of("error", "Missing/invalid API key"));
    }

    if (rnd.nextDouble() < failRate) {
      return ResponseEntity.status(503).body(Map.of("error", "ROS temporary unavailable"));
    }

    String orderId = String.valueOf(body.get("orderId"));
    String routeId = "ROUTE-" + orderId.substring(0, 8);

    // Assign driver from a pool (simulated)
    String[] drivers = {"DRV-001", "DRV-002", "DRV-003"};
    String assignedDriver = drivers[rnd.nextInt(drivers.length)];

    return ResponseEntity.ok(Map.of(
      "routeId", routeId,
      "driverAssigned", assignedDriver,
      "etaMinutes", 90,
      "message", "Optimized route created"
    ));
  }
}