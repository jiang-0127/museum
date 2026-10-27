package com.museum.reservation.module.notice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticeSaveRequest {

    @NotBlank(message = "请输入公告标题")
    private String title;

    private String summary;
    private String content;
    private String coverUrl;
    private Integer status = 1;
    private LocalDateTime publishTime;
}
