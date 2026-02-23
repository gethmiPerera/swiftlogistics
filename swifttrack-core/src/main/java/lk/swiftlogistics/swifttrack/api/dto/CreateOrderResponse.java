package lk.swiftlogistics.swifttrack.api.dto;

import java.util.UUID;

public class CreateOrderResponse {
  public UUID orderId;
  public String status;

  public CreateOrderResponse(UUID orderId, String status) {
    this.orderId = orderId;
    this.status = status;
  }
}