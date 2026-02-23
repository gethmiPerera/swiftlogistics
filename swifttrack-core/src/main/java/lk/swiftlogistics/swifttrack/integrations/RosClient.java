package lk.swiftlogistics.swifttrack.integrations;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class RosClient {

    private final RestTemplate http = new RestTemplate();
    private final ServiceDiscovery sd;

    public RosClient(ServiceDiscovery sd) {
        this.sd = sd;
    }

    public String planRoute(String orderId) {
        String url = sd.resolve("ros-rest") + "/optimize";
        http.postForObject(url, null, String.class);
        return "ROUTE-" + orderId;
    }
}