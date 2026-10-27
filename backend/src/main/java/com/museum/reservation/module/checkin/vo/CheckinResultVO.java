package com.museum.reservation.module.checkin.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class CheckinResultVO {
    private boolean success;
    private String message;
    private String orderNo;
    private String museumName;
    private String contactName;
    private LocalDate visitDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer visitorCount;
    private String status;
    private LocalDateTime checkinTime;
}
