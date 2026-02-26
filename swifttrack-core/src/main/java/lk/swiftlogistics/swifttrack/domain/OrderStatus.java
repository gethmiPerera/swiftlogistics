package lk.swiftlogistics.swifttrack.domain;

public enum OrderStatus {
    RECEIVED,
    CMS_CREATED,
    WMS_REGISTERED,
    ROUTE_PLANNED,
    READY_FOR_PICKUP,
    PICKED_UP,
    DELIVERED,
    FAILED
}