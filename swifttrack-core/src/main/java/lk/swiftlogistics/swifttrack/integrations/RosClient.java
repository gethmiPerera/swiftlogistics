package lk.swiftlogistics.swifttrack.integrations;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class RosClient {

    private final RestTemplate http = new RestTemplate();
    private final ServiceDiscovery sd;

    public RosClient(ServiceDiscovery sd) {
        this.sd = sd;
    }

    /**
     * Calls ROS route-optimization service with API-key authentication.
     * Sends the orderId in the JSON body and returns routeId + driverAssigned.
     */
    public Map<String, String> planRoute(String orderId) {
        String url = sd.resolve("ros-rest") + "/optimize";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-KEY", "demo-key");

        Map<String, Object> body = Map.of("orderId", orderId);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = http.exchange(url, HttpMethod.POST, request, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, String> result = new HashMap<>();
            Object routeId = response.getBody().get("routeId");
            Object driver = response.getBody().get("driverAssigned");
            result.put("routeId", routeId != null ? routeId.toString() : "ROUTE-" + orderId);
            result.put("driverAssigned", driver != null ? driver.toString() : "DRV-001");
            return result;
        }

        throw new RuntimeException("ROS returned " + response.getStatusCode());
    }
}