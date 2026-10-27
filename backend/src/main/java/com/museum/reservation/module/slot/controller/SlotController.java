package com.museum.reservation.module.slot.controller;

import com.museum.reservation.common.ApiResponse;
import com.museum.reservation.module.slot.service.SlotService;
import com.museum.reservation.module.slot.vo.SlotVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "参观时段（公开）")
@RestController
@RequestMapping("/api/slots")
@RequiredArgsConstructor
public class SlotController {

    private final SlotService slotService;

    @Operation(summary = "查询可预约时段与余票")
    @GetMapping
    public ApiResponse<List<SlotVO>> list(@RequestParam(required = false) Long museumId,
                                          @RequestParam(required = false) Long exhibitionId,
                                          @RequestParam(required = false)
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                          @RequestParam(required = false)
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate) {
        return ApiResponse.ok(slotService.publicList(museumId, exhibitionId, date, fromDate));
    }

    @Operation(summary = "时段详情")
    @GetMapping("/{id}")
    public ApiResponse<SlotVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(slotService.publicDetail(id));
    }
}
