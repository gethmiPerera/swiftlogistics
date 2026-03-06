package lk.swiftlogistics.swifttrack.integrations;

import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;

@Component
public class ServiceDiscovery {

    private static final Logger log = LoggerFactory.getLogger(ServiceDiscovery.class);

    private final DiscoveryClient client;

    /* Fallback: Docker DNS hostnames when Eureka cache is not yet populated */
    private static final Map<String, String> FALLBACK_URLS = Map.of(
        "cms-soap", "http://cms-soap:8081",
        "ros-rest", "http://ros-rest:8082",
        "wms-tcp",  "http://wms-tcp:9099"
    );

    public ServiceDiscovery(DiscoveryClient client) {
        this.client = client;
    }

    /**
     * Resolves a service URI via Eureka, with retry (up to 3 attempts, 2s apart).
     * Falls back to Docker DNS hostname if Eureka returns no instances.
     */
    public URI resolve(String serviceName) {
        // Try Eureka first with retries
        for (int i = 1; i <= 3; i++) {
            var instances = client.getInstances(serviceName);
            if (instances != null && !instances.isEmpty()) {
                URI uri = instances.get(0).getUri();
                log.debug("Resolved {} via Eureka → {}", serviceName, uri);
                return uri;
            }
            log.warn("Eureka has no instances for '{}' (attempt {}/3)", serviceName, i);
            if (i < 3) {
                try { Thread.sleep(2000); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // Fallback to Docker DNS hostname
        String fallback = FALLBACK_URLS.get(serviceName);
        if (fallback != null) {
            log.info("Using Docker DNS fallback for '{}' → {}", serviceName, fallback);
            return URI.create(fallback);
        }

        throw new RuntimeException("Cannot resolve service: " + serviceName);
    }
}