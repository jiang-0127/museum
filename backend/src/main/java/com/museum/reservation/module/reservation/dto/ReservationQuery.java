package com.museum.reservation.module.reservation.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ReservationQuery {
    private Long userId;
    private Long museumId;
    private String status;
    private String keyword;
    private LocalDate dateStart;
    private LocalDate dateEnd;
}
