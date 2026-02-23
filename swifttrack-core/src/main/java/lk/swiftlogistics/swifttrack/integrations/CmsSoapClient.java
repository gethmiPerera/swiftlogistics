package lk.swiftlogistics.swifttrack.integrations;

import org.springframework.stereotype.Service;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.xml.transform.StringSource;

@Service
public class CmsSoapClient {

    private final WebServiceTemplate ws = new WebServiceTemplate();
    private final ServiceDiscovery sd;

    public CmsSoapClient(ServiceDiscovery sd) {
        this.sd = sd;
    }

    public String createOrder(String orderId) {
        String url = sd.resolve("cms-soap") + "/ws";

        String xml =
          "<soapenv:Envelope xmlns:soapenv='http://schemas.xmlsoap.org/soap/envelope/'>" +
          "<soapenv:Body><CreateOrderRequest>" +
          "<orderId>" + orderId + "</orderId>" +
          "</CreateOrderRequest></soapenv:Body></soapenv:Envelope>";

                ws.sendSourceAndReceive(url, new StringSource(xml), source -> null);
        return "CMS-" + orderId;
    }
}