package com.museum.reservation.module.slot.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
public class SlotVO {
    private Long id;
    private Long museumId;
    private String museumName;
    private Long exhibitionId;
    private String exhibitionName;
    private LocalDate visitDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer totalCapacity;
    private Integer bookedCount;
    private Integer remaining;
    private Integer status;
    private LocalDateTime createdAt;
    private Integer ticketTypeCount;
    private List<String> ticketTypeNames;
    private Integer reservationOrderCount;
    private Integer reservationVisitorCount;
}
