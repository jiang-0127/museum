package com.museum.reservation.module.exhibition.controller;

import com.museum.reservation.common.ApiResponse;
import com.museum.reservation.common.PageResult;
import com.museum.reservation.module.exhibition.service.ExhibitionService;
import com.museum.reservation.module.exhibition.vo.ExhibitionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "展览（公开）")
@RestController
@RequestMapping("/api/exhibitions")
@RequiredArgsConstructor
public class ExhibitionController {

    private final ExhibitionService exhibitionService;

    @Operation(summary = "展览分页列表")
    @GetMapping
    public ApiResponse<PageResult<ExhibitionVO>> page(@RequestParam(defaultValue = "1") long page,
                                                      @RequestParam(defaultValue = "12") long size,
                                                      @RequestParam(required = false) Long museumId,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(defaultValue = "false") boolean includePast) {
        return ApiResponse.ok(exhibitionService.publicPage(page, size, museumId, keyword, includePast));
    }

    @Operation(summary = "展览详情")
    @GetMapping("/{id}")
    public ApiResponse<ExhibitionVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(exhibitionService.publicDetail(id));
    }
}
