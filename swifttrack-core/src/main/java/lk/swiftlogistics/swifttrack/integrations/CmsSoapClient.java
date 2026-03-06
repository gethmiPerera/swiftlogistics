package lk.swiftlogistics.swifttrack.integrations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.xml.transform.StringSource;

@Service
public class CmsSoapClient {

    private static final Logger log = LoggerFactory.getLogger(CmsSoapClient.class);
    private final WebServiceTemplate ws = new WebServiceTemplate();
    private final ServiceDiscovery sd;

    public CmsSoapClient(ServiceDiscovery sd) {
        this.sd = sd;
    }

    /** Create a new order in CMS (SOAP/XML) */
    public String createOrder(String orderId) {
        String url = sd.resolve("cms-soap") + "/ws";

        String xml =
          "<CreateOrderRequest xmlns='http://swiftlogistics.lk/cms'>" +
          "<orderId>" + orderId + "</orderId>" +
          "</CreateOrderRequest>";

        ws.sendSourceAndReceive(url, new StringSource(xml), source -> null);
        return "CMS-" + orderId;
    }

    /**
     * Cancel an order in CMS — Saga compensation.
     * Called when a subsequent saga step fails and we need to rollback the CMS entry.
     */
    public void cancelOrder(String orderId, String reason) {
        try {
            String url = sd.resolve("cms-soap") + "/ws";

            String xml =
              "<CancelOrderRequest xmlns='http://swiftlogistics.lk/cms'>" +
              "<orderId>" + orderId + "</orderId>" +
              "<reason>" + reason + "</reason>" +
              "</CancelOrderRequest>";

            ws.sendSourceAndReceive(url, new StringSource(xml), source -> null);
            log.info("COMPENSATION — CMS order {} cancelled successfully", orderId);
        } catch (Exception e) {
            log.error("COMPENSATION FAILED — could not cancel CMS order {}: {}", orderId, e.getMessage());
        }
    }

    /**
     * Update order status in CMS — called when driver updates delivery status.
     * Keeps the legacy CMS system in sync with the middleware.
     */
    public void updateStatus(String orderId, String status) {
        try {
            String url = sd.resolve("cms-soap") + "/ws";

            String xml =
              "<UpdateStatusRequest xmlns='http://swiftlogistics.lk/cms'>" +
              "<orderId>" + orderId + "</orderId>" +
              "<status>" + status + "</status>" +
              "</UpdateStatusRequest>";

            ws.sendSourceAndReceive(url, new StringSource(xml), source -> null);
            log.info("CMS status updated: order {} → {}", orderId, status);
        } catch (Exception e) {
            log.error("CMS status update failed for order {}: {}", orderId, e.getMessage());
        }
    }
}