package com.museum.reservation.module.exhibition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ExhibitionSaveRequest {

    @NotNull(message = "请选择所属博物馆")
    private Long museumId;

    @NotBlank(message = "请输入展览名称")
    private String name;

    private String description;
    private String coverUrl;

    @NotNull(message = "请选择开始日期")
    private LocalDate startDate;

    @NotNull(message = "请选择结束日期")
    private LocalDate endDate;

    private Integer status = 1;
}
