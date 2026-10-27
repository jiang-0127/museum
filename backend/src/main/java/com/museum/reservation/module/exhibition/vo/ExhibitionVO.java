package com.museum.reservation.module.exhibition.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ExhibitionVO {
    private Long id;
    private Long museumId;
    private String museumName;
    private String name;
    private String description;
    private String coverUrl;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer status;
    private LocalDateTime createdAt;
}
