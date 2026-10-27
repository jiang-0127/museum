package com.museum.reservation.module.museum.controller;

import com.museum.reservation.common.ApiResponse;
import com.museum.reservation.common.PageResult;
import com.museum.reservation.module.exhibition.service.ExhibitionService;
import com.museum.reservation.module.exhibition.vo.ExhibitionVO;
import com.museum.reservation.module.museum.entity.Museum;
import com.museum.reservation.module.museum.service.MuseumService;
import com.museum.reservation.module.ticket.entity.TicketType;
import com.museum.reservation.module.ticket.service.TicketTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "博物馆（公开）")
@RestController
@RequestMapping("/api/museums")
@RequiredArgsConstructor
public class MuseumController {

    private final MuseumService museumService;
    private final ExhibitionService exhibitionService;
    private final TicketTypeService ticketTypeService;

    @Operation(summary = "博物馆分页列表")
    @GetMapping
    public ApiResponse<PageResult<Museum>> page(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "12") long size,
                                                @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(museumService.publicPage(page, size, keyword));
    }

    @Operation(summary = "博物馆详情")
    @GetMapping("/{id}")
    public ApiResponse<Museum> detail(@PathVariable Long id) {
        return ApiResponse.ok(museumService.publicDetail(id));
    }

    @Operation(summary = "博物馆展览")
    @GetMapping("/{id}/exhibitions")
    public ApiResponse<List<ExhibitionVO>> exhibitions(@PathVariable Long id) {
        return ApiResponse.ok(exhibitionService.listByMuseum(id));
    }

    @Operation(summary = "博物馆票种")
    @GetMapping("/{id}/ticket-types")
    public ApiResponse<List<TicketType>> ticketTypes(@PathVariable Long id) {
        return ApiResponse.ok(ticketTypeService.publicList(id));
    }
}
