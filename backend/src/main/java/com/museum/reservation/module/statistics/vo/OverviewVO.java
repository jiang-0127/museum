package com.museum.reservation.module.statistics.vo;

import lombok.Data;

@Data
public class OverviewVO {
    private long todayReservations;
    private long todayVisitors;
    private long todayCheckins;
    private long todayCanceled;
    private long pendingPayments;
    private long totalMuseums;
    private long totalUsers;
    private long totalReservations;
}
