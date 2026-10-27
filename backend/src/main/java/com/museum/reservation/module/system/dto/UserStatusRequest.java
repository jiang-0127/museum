package com.museum.reservation.module.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserStatusRequest {

    @NotNull(message = "请选择用户状态")
    private Integer status;
}
