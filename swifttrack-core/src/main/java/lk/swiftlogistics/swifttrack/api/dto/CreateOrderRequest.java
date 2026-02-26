package lk.swiftlogistics.swifttrack.api.dto;

public class CreateOrderRequest {
    // Fields matching what the frontend form sends
    public String customer;
    public String address;
    public String contact;
    public String priority;
    public String packageDetails;

    // Also accept original field names if called via API
    public String clientId;
    public String pickupAddress;
    public String dropAddress;
}