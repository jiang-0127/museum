package com.museum.reservation.module.reservation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ReservationCreateRequest {

    private Long slotId;

    private LocalDate visitDate;

    @NotNull(message = "请选择票种")
    private Long ticketTypeId;

    @NotBlank(message = "请输入联系人姓名")
    private String contactName;

    @NotBlank(message = "请输入联系人手机号")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系人手机号格式不正确")
    private String contactPhone;

    @NotEmpty(message = "请至少填写一位参观者")
    @Valid
    private List<VisitorRequest> visitors;

    private String remark;
}
