package lk.swiftlogistics.swifttrack.domain;

public enum OrderStatus {
    RECEIVED,
    CMS_CREATED,
    WMS_REGISTERED,
    ROUTE_PLANNED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    FAILED
}