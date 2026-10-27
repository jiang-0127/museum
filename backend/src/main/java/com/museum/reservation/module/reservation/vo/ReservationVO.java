package com.museum.reservation.module.reservation.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class ReservationVO {
    private Long id;
    private String orderNo;
    private Long userId;
    private Long museumId;
    private String museumName;
    private Long exhibitionId;
    private String exhibitionName;
    private Long slotId;
    private LocalDate visitDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Long ticketTypeId;
    private String ticketTypeName;
    private Integer visitorCount;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private String contactName;
    private String contactPhone;
    private String status;
    private String paymentNo;
    private String paymentStatus;
    private LocalDateTime qrExpireAt;
    private LocalDateTime cancelTime;
    private LocalDateTime checkinTime;
    private LocalDateTime createdAt;
}
