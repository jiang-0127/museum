package com.museum.reservation.module.museum.controller;

import com.museum.reservation.common.ApiResponse;
import com.museum.reservation.common.PageResult;
import com.museum.reservation.common.log.OpLog;
import com.museum.reservation.module.museum.dto.MuseumSaveRequest;
import com.museum.reservation.module.museum.entity.Museum;
import com.museum.reservation.module.museum.service.MuseumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "博物馆管理")
@RestController
@RequestMapping("/api/admin/museums")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('RESERVATION_ADMIN','SYSTEM_ADMIN')")
public class MuseumAdminController {

    private final MuseumService museumService;

    @Operation(summary = "后台博物馆列表")
    @GetMapping
    public ApiResponse<PageResult<Museum>> page(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Integer status) {
        return ApiResponse.ok(museumService.adminPage(page, size, keyword, status));
    }

    @Operation(summary = "博物馆详情")
    @GetMapping("/{id}")
    public ApiResponse<Museum> detail(@PathVariable Long id) {
        return ApiResponse.ok(museumService.getById(id));
    }

    @Operation(summary = "新增博物馆")
    @PostMapping
    @OpLog(module = "博物馆管理", operation = "新增博物馆")
    public ApiResponse<Museum> create(@Valid @RequestBody MuseumSaveRequest request) {
        return ApiResponse.ok(museumService.create(request));
    }

    @Operation(summary = "修改博物馆")
    @PutMapping("/{id}")
    @OpLog(module = "博物馆管理", operation = "修改博物馆")
    public ApiResponse<Museum> update(@PathVariable Long id, @Valid @RequestBody MuseumSaveRequest request) {
        return ApiResponse.ok(museumService.update(id, request));
    }

    @Operation(summary = "下线博物馆")
    @DeleteMapping("/{id}")
    @OpLog(module = "博物馆管理", operation = "下线博物馆")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        museumService.delete(id);
        return ApiResponse.ok();
    }
}
