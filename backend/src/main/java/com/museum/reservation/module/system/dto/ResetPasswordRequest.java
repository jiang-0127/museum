package com.museum.reservation.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 32, message = "密码长度为 6-32 位")
    private String password;
}
