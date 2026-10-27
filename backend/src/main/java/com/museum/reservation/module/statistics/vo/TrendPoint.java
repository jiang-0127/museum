package com.museum.reservation.module.statistics.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class TrendPoint {
    private LocalDate date;
    private long reservations;
    private long checkins;
    private long visitors;
    private long canceled;
}
