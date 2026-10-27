package com.museum.reservation.module.system.controller;

import com.museum.reservation.common.ApiResponse;
import com.museum.reservation.module.museum.service.MuseumService;
import com.museum.reservation.module.system.service.SystemConfigService;
import com.museum.reservation.module.system.vo.SiteSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "站点概览（公开）")
@RestController
@RequestMapping("/api/site")
@RequiredArgsConstructor
public class SiteController {

    private final MuseumService museumService;
    private final SystemConfigService systemConfigService;

    @Operation(summary = "首页站点概览")
    @GetMapping("/summary")
    public ApiResponse<SiteSummaryVO> summary() {
        return ApiResponse.ok(new SiteSummaryVO(
                museumService.publicCount(),
                systemConfigService.getInt("advance_booking_days", 14),
                systemConfigService.getInt("max_visitors_per_order", 6)
        ));
    }
}
