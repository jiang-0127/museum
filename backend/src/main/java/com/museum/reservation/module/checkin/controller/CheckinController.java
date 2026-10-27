package com.museum.reservation.module.checkin.controller;

import com.museum.reservation.common.ApiResponse;
import com.museum.reservation.common.PageResult;
import com.museum.reservation.common.log.OpLog;
import com.museum.reservation.module.checkin.dto.CheckinRequest;
import com.museum.reservation.module.checkin.entity.CheckinRecord;
import com.museum.reservation.module.checkin.service.CheckinService;
import com.museum.reservation.module.checkin.vo.CheckinResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "票码核销")
@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('RESERVATION_ADMIN','SYSTEM_ADMIN')")
public class CheckinController {

    private final CheckinService checkinService;

    @Operation(summary = "输入票码核销电子票")
    @PostMapping("/verify")
    @OpLog(module = "核销", operation = "票码核销")
    public ApiResponse<CheckinResultVO> verify(@Valid @RequestBody CheckinRequest request) {
        return ApiResponse.ok(checkinService.verify(request));
    }

    @Operation(summary = "核销记录")
    @GetMapping("/records")
    public ApiResponse<PageResult<CheckinRecord>> records(@RequestParam(defaultValue = "1") long page,
                                                          @RequestParam(defaultValue = "10") long size,
                                                          @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(checkinService.recordPage(page, size, keyword));
    }
}
