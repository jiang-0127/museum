package com.museum.reservation.module.system.vo;

public record SiteSummaryVO(
        long museumCount,
        int advanceBookingDays,
        int maxVisitorsPerOrder
) {
}
