package com.museum.reservation.module.museum.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MuseumSaveRequest {

    @NotBlank(message = "请输入博物馆名称")
    private String name;

    @NotBlank(message = "请输入博物馆地址")
    private String address;

    private String description;
    private String openTime;
    private String closeTime;
    private String phone;
    private String coverUrl;
    private Integer status = 1;
    private Integer sort = 0;
}
