package com.museum.reservation.module.system.controller;

import com.museum.reservation.common.ApiResponse;
import com.museum.reservation.common.PageResult;
import com.museum.reservation.common.log.OpLog;
import com.museum.reservation.module.auth.vo.UserVO;
import com.museum.reservation.module.system.dto.ResetPasswordRequest;
import com.museum.reservation.module.system.dto.RoleAssignRequest;
import com.museum.reservation.module.system.dto.UserCreateRequest;
import com.museum.reservation.module.system.dto.UserStatusRequest;
import com.museum.reservation.module.system.entity.SysRole;
import com.museum.reservation.module.system.service.UserAdminService;
import com.museum.reservation.security.CurrentUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "用户与角色管理")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class UserAdminController {

    private final UserAdminService userAdminService;

    @GetMapping("/users")
    public ApiResponse<PageResult<UserVO>> users(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Integer status,
                                                 @RequestParam(required = false) String roleCode) {
        return ApiResponse.ok(userAdminService.page(page, size, keyword, status, roleCode));
    }

    @PostMapping("/users")
    @OpLog(module = "系统管理", operation = "新增用户")
    public ApiResponse<UserVO> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.ok(userAdminService.create(request));
    }

    @PutMapping("/users/{id}/status")
    @OpLog(module = "系统管理", operation = "修改用户状态")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                          @Valid @RequestBody UserStatusRequest request) {
        userAdminService.updateStatus(id, request.getStatus(), CurrentUser.id());
        return ApiResponse.ok();
    }

    @PutMapping("/users/{id}/roles")
    @OpLog(module = "系统管理", operation = "分配用户角色")
    public ApiResponse<Void> assignRoles(@PathVariable Long id,
                                         @Valid @RequestBody RoleAssignRequest request) {
        userAdminService.assignRoles(id, request.getRoles(), CurrentUser.id());
        return ApiResponse.ok();
    }

    @PutMapping("/users/{id}/password")
    @OpLog(module = "系统管理", operation = "重置用户密码", saveParams = false)
    public ApiResponse<Void> resetPassword(@PathVariable Long id,
                                           @Valid @RequestBody ResetPasswordRequest request) {
        userAdminService.resetPassword(id, request.getPassword());
        return ApiResponse.ok();
    }

    @GetMapping("/roles")
    public ApiResponse<List<SysRole>> roles() {
        return ApiResponse.ok(userAdminService.roles());
    }
}
