package com.museum.reservation.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public final class OrderNoGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private OrderNoGenerator() {
    }

    public static String nextOrderNo() {
        return "MR" + LocalDateTime.now().format(FORMATTER)
                + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    public static String nextPaymentNo() {
        return "PAY" + LocalDateTime.now().format(FORMATTER)
                + ThreadLocalRandom.current().nextInt(100, 1000);
    }
}
