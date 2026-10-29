package com.museum.reservation.module.museum.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class BasicQuotaVO {

    private Long museumId;
    private LocalDate visitDate;
    private Integer dailyCapacity;
    private Integer slotCapacitySum;
    private Integer slotCount;
    private Integer basicQuota;
    private Integer basicOrderCount;
    private Integer basicBooked;
    private Integer basicRemaining;
    private Boolean closed;
}
