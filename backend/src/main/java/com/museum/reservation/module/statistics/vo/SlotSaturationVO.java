package com.museum.reservation.module.statistics.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class SlotSaturationVO {
    private Long id;
    private String museumName;
    private LocalDate visitDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer totalCapacity;
    private Integer bookedCount;
    private Integer saturation;
}
