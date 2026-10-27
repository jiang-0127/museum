package com.museum.reservation.module.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UserCreateRequest {

    @NotBlank(message = "请输入用户名")
    @Size(min = 3, max = 30, message = "用户名长度为 3-30 位")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    @NotBlank(message = "请输入初始密码")
    @Size(min = 6, max = 32, message = "密码长度为 6-32 位")
    private String password;

    @NotBlank(message = "请输入真实姓名")
    private String realName;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$",
            message = "身份证号格式不正确")
    private String idCard;

    @Email(message = "邮箱格式不正确")
    private String email;

    @NotEmpty(message = "请至少选择一个角色")
    private List<String> roles;
}
