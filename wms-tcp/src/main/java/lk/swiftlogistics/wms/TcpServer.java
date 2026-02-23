package lk.swiftlogistics.wms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

@Component
public class TcpServer implements CommandLineRunner {

  @Value("${wms.port}")
  private int port;

  @Override
  public void run(String... args) throws Exception {
    ServerSocket server = new ServerSocket(port);
    System.out.println("[WMS] TCP server listening on " + port);

    while (true) {
      Socket s = server.accept();
      new Thread(() -> handle(s)).start();
    }
  }

  private void handle(Socket s) {
    try (s) {
      BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
      BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));

      String line = in.readLine();
      if (line == null) return;

      // Expect: REGISTER|orderId|priority|pickup|drop
      if (line.startsWith("REGISTER|")) {
        String[] parts = line.split("\\|", 5);
        String orderId = parts.length > 1 ? parts[1] : "unknown";

        System.out.println("[WMS] REGISTER received for " + orderId);

        out.write("ACK|" + orderId + "\n");
        out.flush();
        return;
      }

      out.write("NACK|bad_request\n");
      out.flush();

    } catch (Exception e) {
      System.err.println("[WMS] error: " + e.getMessage());
    }
  }
}