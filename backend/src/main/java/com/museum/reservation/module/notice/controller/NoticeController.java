package com.museum.reservation.module.notice.controller;

import com.museum.reservation.common.ApiResponse;
import com.museum.reservation.common.PageResult;
import com.museum.reservation.module.notice.entity.Notice;
import com.museum.reservation.module.notice.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "公告（公开）")
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "公告分页列表")
    @GetMapping
    public ApiResponse<PageResult<Notice>> page(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(noticeService.publicPage(page, size));
    }

    @Operation(summary = "最新公告")
    @GetMapping("/latest")
    public ApiResponse<List<Notice>> latest(@RequestParam(defaultValue = "3") int limit) {
        return ApiResponse.ok(noticeService.latest(limit));
    }

    @Operation(summary = "公告详情")
    @GetMapping("/{id}")
    public ApiResponse<Notice> detail(@PathVariable Long id) {
        return ApiResponse.ok(noticeService.publicDetail(id));
    }
}
