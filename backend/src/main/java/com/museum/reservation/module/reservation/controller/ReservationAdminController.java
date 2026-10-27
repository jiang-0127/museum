package com.museum.reservation.module.reservation.controller;

import com.museum.reservation.common.ApiResponse;
import com.museum.reservation.common.PageResult;
import com.museum.reservation.common.log.OpLog;
import com.museum.reservation.module.reservation.dto.ReservationQuery;
import com.museum.reservation.module.reservation.service.ReservationService;
import com.museum.reservation.module.reservation.vo.ReservationDetailVO;
import com.museum.reservation.module.reservation.vo.ReservationVO;
import com.museum.reservation.security.CurrentUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "预约订单管理")
@RestController
@RequestMapping("/api/admin/reservations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('RESERVATION_ADMIN','SYSTEM_ADMIN')")
public class ReservationAdminController {

    private final ReservationService reservationService;

    @GetMapping
    public ApiResponse<PageResult<ReservationVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long museumId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEnd) {
        ReservationQuery query = new ReservationQuery();
        query.setStatus(status);
        query.setKeyword(keyword);
        query.setMuseumId(museumId);
        query.setDateStart(dateStart);
        query.setDateEnd(dateEnd);
        return ApiResponse.ok(reservationService.adminPage(page, size, query));
    }

    @GetMapping("/{id}")
    public ApiResponse<ReservationDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(reservationService.getDetail(id, CurrentUser.id(), true));
    }

    @PutMapping("/{id}/cancel")
    @OpLog(module = "订单管理", operation = "后台取消预约")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        reservationService.cancel(id, CurrentUser.id(), true);
        return ApiResponse.ok();
    }
}
