package com.museum.reservation.module.reservation.controller;

import com.museum.reservation.common.ApiResponse;
import com.museum.reservation.common.PageResult;
import com.museum.reservation.common.log.OpLog;
import com.museum.reservation.module.reservation.dto.ReservationCreateRequest;
import com.museum.reservation.module.reservation.service.ReservationService;
import com.museum.reservation.module.reservation.vo.ReservationDetailVO;
import com.museum.reservation.module.reservation.vo.ReservationVO;
import com.museum.reservation.module.reservation.vo.TicketVO;
import com.museum.reservation.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "预约")
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @Operation(summary = "提交预约")
    @PostMapping
    @OpLog(module = "预约", operation = "提交预约")
    public ApiResponse<ReservationDetailVO> create(@Valid @RequestBody ReservationCreateRequest request) {
        return ApiResponse.ok(reservationService.create(CurrentUser.id(), request));
    }

    @Operation(summary = "我的预约")
    @GetMapping("/my")
    public ApiResponse<PageResult<ReservationVO>> my(@RequestParam(defaultValue = "1") long page,
                                                     @RequestParam(defaultValue = "10") long size,
                                                     @RequestParam(required = false) String status) {
        return ApiResponse.ok(reservationService.myPage(CurrentUser.id(), page, size, status));
    }

    @Operation(summary = "预约详情")
    @GetMapping("/{id}")
    public ApiResponse<ReservationDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(reservationService.getDetail(id, CurrentUser.id(), CurrentUser.isAdmin()));
    }

    @Operation(summary = "取消预约")
    @PutMapping("/{id}/cancel")
    @OpLog(module = "预约", operation = "取消预约")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        reservationService.cancel(id, CurrentUser.id(), CurrentUser.isAdmin());
        return ApiResponse.ok();
    }

    @Operation(summary = "电子票二维码")
    @GetMapping("/{id}/ticket")
    public ApiResponse<TicketVO> ticket(@PathVariable Long id) {
        return ApiResponse.ok(reservationService.getTicket(id, CurrentUser.id(), CurrentUser.isAdmin()));
    }
}
