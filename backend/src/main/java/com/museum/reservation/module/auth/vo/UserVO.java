package com.museum.reservation.module.auth.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String idCard;
    private String email;
    private String avatar;
    private Integer status;
    private List<String> roles;
    private List<String> roleNames;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createdAt;
}
