package com.museum.reservation.module.statistics.controller;

import com.museum.reservation.common.ApiResponse;
import com.museum.reservation.module.statistics.service.StatisticsService;
import com.museum.reservation.module.statistics.vo.NameValue;
import com.museum.reservation.module.statistics.vo.OverviewVO;
import com.museum.reservation.module.statistics.vo.SlotSaturationVO;
import com.museum.reservation.module.statistics.vo.TrendPoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "统计报表")
@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('RESERVATION_ADMIN','SYSTEM_ADMIN')")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "数据概览")
    @GetMapping("/overview")
    public ApiResponse<OverviewVO> overview() {
        return ApiResponse.ok(statisticsService.overview());
    }

    @Operation(summary = "预约趋势")
    @GetMapping("/trend")
    public ApiResponse<List<TrendPoint>> trend(@RequestParam(defaultValue = "7") int days) {
        return ApiResponse.ok(statisticsService.trend(days));
    }

    @Operation(summary = "博物馆热度")
    @GetMapping("/museum-heat")
    public ApiResponse<List<NameValue>> museumHeat(@RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(statisticsService.museumHeat(days));
    }

    @Operation(summary = "票种分布")
    @GetMapping("/ticket-distribution")
    public ApiResponse<List<NameValue>> ticketDistribution() {
        return ApiResponse.ok(statisticsService.ticketDistribution());
    }

    @Operation(summary = "时段饱和度")
    @GetMapping("/slot-saturation")
    public ApiResponse<List<SlotSaturationVO>> slotSaturation() {
        return ApiResponse.ok(statisticsService.slotSaturation());
    }
}
