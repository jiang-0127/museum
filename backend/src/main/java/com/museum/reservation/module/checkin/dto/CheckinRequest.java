package com.museum.reservation.module.checkin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckinRequest {

    @NotBlank(message = "请输入电子票票码")
    private String ticketCode;

    private String gate = "正门闸机";
}
