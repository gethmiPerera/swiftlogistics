package lk.swiftlogistics.swifttrack.integrations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.net.Socket;

@Service
public class WmsTcpClient {

    @Value("${swift.wms.host}")
    private String host;

    @Value("${swift.wms.port}")
    private int port;

    public void register(String orderId) {
        try (Socket s = new Socket(host, port);
             PrintWriter pw = new PrintWriter(s.getOutputStream(), true)) {
            pw.println("REGISTER|" + orderId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}