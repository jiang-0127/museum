package com.museum.reservation.module.slot.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class SlotSaveRequest {

    @NotNull(message = "请选择博物馆")
    private Long museumId;

    private Long exhibitionId;

    @NotNull(message = "请选择参观日期")
    private LocalDate visitDate;

    @NotNull(message = "请选择开始时间")
    private LocalTime startTime;

    @NotNull(message = "请选择结束时间")
    private LocalTime endTime;

    @NotNull(message = "请输入总容量")
    @Min(value = 1, message = "总容量至少为 1")
    private Integer totalCapacity;

    private Integer status = 1;
}
