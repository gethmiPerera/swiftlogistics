package lk.swiftlogistics.cms.ws;

import lk.swiftlogistics.cms.schema.*;
import org.springframework.ws.server.endpoint.annotation.*;

@Endpoint
public class CmsEndpoint {

  private static final String NS = "http://swiftlogistics.lk/cms";

  @PayloadRoot(namespace = NS, localPart = "CreateOrderRequest")
  @ResponsePayload
  public CreateOrderResponse create(@RequestPayload CreateOrderRequest req) {
    System.out.println("[CMS] CREATE order: " + req.getOrderId());
    CreateOrderResponse resp = new CreateOrderResponse();
    resp.setCmsOrderId("CMS-" + req.getOrderId().substring(0, 8));
    resp.setStatus("CREATED");
    return resp;
  }

  @PayloadRoot(namespace = NS, localPart = "CancelOrderRequest")
  @ResponsePayload
  public CancelOrderResponse cancel(@RequestPayload CancelOrderRequest req) {
    System.out.println("[CMS] CANCEL order: " + req.getOrderId() + " — reason: " + req.getReason());
    CancelOrderResponse resp = new CancelOrderResponse();
    resp.setOrderId(req.getOrderId());
    resp.setStatus("CANCELLED");
    return resp;
  }

  @PayloadRoot(namespace = NS, localPart = "UpdateStatusRequest")
  @ResponsePayload
  public UpdateStatusResponse updateStatus(@RequestPayload UpdateStatusRequest req) {
    System.out.println("[CMS] STATUS UPDATE order: " + req.getOrderId() + " → " + req.getStatus());
    UpdateStatusResponse resp = new UpdateStatusResponse();
    resp.setOrderId(req.getOrderId());
    resp.setStatus(req.getStatus());
    return resp;
  }
}