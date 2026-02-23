package lk.swiftlogistics.swifttrack.integrations;

import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class ServiceDiscovery {

    private final DiscoveryClient client;

    public ServiceDiscovery(DiscoveryClient client) {
        this.client = client;
    }

    public URI resolve(String serviceName) {
        return client.getInstances(serviceName)
                .stream()
                .findFirst()
                .orElseThrow()
                .getUri();
    }
}