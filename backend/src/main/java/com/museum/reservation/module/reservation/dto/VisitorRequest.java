package com.museum.reservation.module.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VisitorRequest {

    @NotBlank(message = "请输入参观者姓名")
    private String name;

    @NotBlank(message = "请输入参观者身份证号")
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$",
            message = "参观者身份证号格式不正确")
    private String idCard;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "参观者手机号格式不正确")
    private String phone;
}
