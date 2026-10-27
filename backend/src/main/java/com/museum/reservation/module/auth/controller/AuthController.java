package com.museum.reservation.module.auth.controller;

import com.museum.reservation.common.ApiResponse;
import com.museum.reservation.common.log.OpLog;
import com.museum.reservation.module.auth.dto.LoginRequest;
import com.museum.reservation.module.auth.dto.ProfileUpdateRequest;
import com.museum.reservation.module.auth.dto.RegisterRequest;
import com.museum.reservation.module.auth.service.AuthService;
import com.museum.reservation.module.auth.vo.LoginVO;
import com.museum.reservation.module.auth.vo.UserVO;
import com.museum.reservation.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    @OpLog(module = "认证", operation = "用户注册", saveParams = false)
    public ApiResponse<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    @OpLog(module = "认证", operation = "用户登录", saveParams = false)
    public ApiResponse<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @Operation(summary = "当前登录用户")
    @GetMapping("/me")
    public ApiResponse<UserVO> me() {
        return ApiResponse.ok(authService.me(CurrentUser.id()));
    }

    @Operation(summary = "修改个人资料")
    @PutMapping("/profile")
    @OpLog(module = "认证", operation = "修改个人资料")
    public ApiResponse<UserVO> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return ApiResponse.ok(authService.updateProfile(CurrentUser.id(), request));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.ok();
    }
}
