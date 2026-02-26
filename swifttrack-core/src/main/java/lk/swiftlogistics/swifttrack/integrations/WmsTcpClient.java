package lk.swiftlogistics.swifttrack.integrations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

@Service
public class WmsTcpClient {

    private static final Logger log = LoggerFactory.getLogger(WmsTcpClient.class);

    @Value("${swift.wms.host}")
    private String host;

    @Value("${swift.wms.port}")
    private int port;

    /** Register a package in the warehouse (TCP proprietary protocol) */
    public void register(String orderId) {
        try (Socket s = new Socket(host, port);
             PrintWriter pw = new PrintWriter(s.getOutputStream(), true);
             BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            pw.println("REGISTER|" + orderId);
            String response = br.readLine();
            log.info("WMS REGISTER response: {}", response);
        } catch (Exception e) {
            throw new RuntimeException("WMS register failed: " + e.getMessage(), e);
        }
    }

    /**
     * Release a package from the warehouse — Saga compensation.
     * Called when a subsequent saga step fails and we need to rollback.
     */
    public void release(String orderId) {
        try (Socket s = new Socket(host, port);
             PrintWriter pw = new PrintWriter(s.getOutputStream(), true);
             BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            pw.println("RELEASE|" + orderId);
            String response = br.readLine();
            log.info("COMPENSATION — WMS package {} released: {}", orderId, response);
        } catch (Exception e) {
            log.error("COMPENSATION FAILED — could not release WMS package {}: {}", orderId, e.getMessage());
        }
    }

    /**
     * Update package status in WMS — called when driver updates delivery status.
     * Keeps the warehouse system in sync with the middleware.
     */
    public void updateStatus(String orderId, String status) {
        try (Socket s = new Socket(host, port);
             PrintWriter pw = new PrintWriter(s.getOutputStream(), true);
             BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            pw.println("STATUS|" + orderId + "|" + status);
            String response = br.readLine();
            log.info("WMS status updated: order {} → {} — response: {}", orderId, status, response);
        } catch (Exception e) {
            log.error("WMS status update failed for order {}: {}", orderId, e.getMessage());
        }
    }
}