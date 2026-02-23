package lk.swiftlogistics.cms.ws;

import lk.swiftlogistics.cms.schema.CreateOrderRequest;
import lk.swiftlogistics.cms.schema.CreateOrderResponse;
import org.springframework.ws.server.endpoint.annotation.*;

@Endpoint
public class CmsEndpoint {

  private static final String NS = "http://swiftlogistics.lk/cms";

  @PayloadRoot(namespace = NS, localPart = "CreateOrderRequest")
  @ResponsePayload
  public CreateOrderResponse create(@RequestPayload CreateOrderRequest req) {
    CreateOrderResponse resp = new CreateOrderResponse();
    resp.setCmsOrderId("CMS-" + req.getOrderId().substring(0, 8));
    resp.setStatus("CREATED");
    return resp;
  }
}