package com.museum.reservation.module.notice.controller;

import com.museum.reservation.common.ApiResponse;
import com.museum.reservation.common.PageResult;
import com.museum.reservation.common.log.OpLog;
import com.museum.reservation.module.notice.dto.NoticeSaveRequest;
import com.museum.reservation.module.notice.entity.Notice;
import com.museum.reservation.module.notice.service.NoticeService;
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

@Tag(name = "公告管理")
@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('RESERVATION_ADMIN','SYSTEM_ADMIN')")
public class NoticeAdminController {

    private final NoticeService noticeService;

    @GetMapping
    public ApiResponse<PageResult<Notice>> page(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Integer status) {
        return ApiResponse.ok(noticeService.adminPage(page, size, keyword, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<Notice> detail(@PathVariable Long id) {
        return ApiResponse.ok(noticeService.getById(id));
    }

    @PostMapping
    @OpLog(module = "公告管理", operation = "发布公告")
    public ApiResponse<Notice> create(@Valid @RequestBody NoticeSaveRequest request) {
        return ApiResponse.ok(noticeService.create(request));
    }

    @PutMapping("/{id}")
    @OpLog(module = "公告管理", operation = "修改公告")
    public ApiResponse<Notice> update(@PathVariable Long id, @Valid @RequestBody NoticeSaveRequest request) {
        return ApiResponse.ok(noticeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @OpLog(module = "公告管理", operation = "下线公告")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return ApiResponse.ok();
    }
}
