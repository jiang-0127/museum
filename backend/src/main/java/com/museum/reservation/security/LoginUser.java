package com.museum.reservation.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {
    private Long id;
    private String username;
    private String realName;
    private List<String> roles;
}
