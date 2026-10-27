package com.museum.reservation.module.ticket.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TicketTypeSaveRequest {

    @NotNull(message = "请选择博物馆")
    private Long museumId;

    @NotBlank(message = "请输入票种名称")
    private String name;

    @NotNull(message = "请输入票价")
    @DecimalMin(value = "0", message = "票价不能为负数")
    private BigDecimal price;

    private String ticketKind = "BASIC";
    private Long exhibitionId;
    private String audienceType = "GENERAL";
    private String tagline;
    private String audience;
    private String description;
    private String benefits;
    private String rules;
    private Integer status = 1;
    private Integer sort = 0;
}
