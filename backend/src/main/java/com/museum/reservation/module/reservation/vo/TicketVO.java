package com.museum.reservation.module.reservation.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class TicketVO {
    private Long reservationId;
    private String orderNo;
    private String status;
    private String museumName;
    private String exhibitionName;
    private LocalDate visitDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer visitorCount;
    private String contactName;
    private String ticketCode;
    private LocalDateTime expireAt;
    private String qrImage;
}
