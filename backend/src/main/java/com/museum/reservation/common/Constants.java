package com.museum.reservation.common;

public final class Constants {

    private Constants() {
    }

    public static final String ROLE_VISITOR = "VISITOR";
    public static final String ROLE_RESERVATION_ADMIN = "RESERVATION_ADMIN";
    public static final String ROLE_SYSTEM_ADMIN = "SYSTEM_ADMIN";

    public static final String RESERVATION_PENDING_PAYMENT = "PENDING_PAYMENT";
    public static final String RESERVATION_RESERVED = "RESERVED";
    public static final String RESERVATION_CANCELED = "CANCELED";
    public static final String RESERVATION_CHECKED_IN = "CHECKED_IN";
    public static final String RESERVATION_EXPIRED = "EXPIRED";

    public static final String PAYMENT_UNPAID = "UNPAID";
    public static final String PAYMENT_PAID = "PAID";
    public static final String PAYMENT_REFUNDED = "REFUNDED";
    public static final String PAYMENT_CLOSED = "CLOSED";

    public static final int STATUS_ENABLED = 1;
    public static final int STATUS_DISABLED = 0;
    public static final int STATUS_DELETED = 2;

    public static final String TICKET_PREFIX = "MUSEUM-TICKET:";
    public static final String LOG_SUCCESS = "SUCCESS";
}
