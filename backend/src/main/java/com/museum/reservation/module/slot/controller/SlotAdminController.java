package com.museum.reservation.module.slot.controller;

import com.museum.reservation.common.ApiResponse;
import com.museum.reservation.common.PageResult;
import com.museum.reservation.common.log.OpLog;
import com.museum.reservation.module.slot.dto.SlotSaveRequest;
import com.museum.reservation.module.slot.service.SlotService;
import com.museum.reservation.module.slot.vo.SlotVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;

@Tag(name = "时段与票额管理")
@RestController
@RequestMapping("/api/admin/slots")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('RESERVATION_ADMIN','SYSTEM_ADMIN')")
public class SlotAdminController {

    private final SlotService slotService;

    @GetMapping
    public ApiResponse<PageResult<SlotVO>> page(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) Long museumId,
                                                @RequestParam(required = false) Long exhibitionId,
                                                @RequestParam(required = false)
                                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                @RequestParam(required = false) Integer status) {
        return ApiResponse.ok(slotService.adminPage(page, size, museumId, exhibitionId, date, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<SlotVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(slotService.getById(id));
    }

    @PostMapping
    @OpLog(module = "时段管理", operation = "新增参观时段")
    public ApiResponse<SlotVO> create(@Valid @RequestBody SlotSaveRequest request) {
        return ApiResponse.ok(slotService.create(request));
    }

    @PutMapping("/{id}")
    @OpLog(module = "时段管理", operation = "修改参观时段")
    public ApiResponse<SlotVO> update(@PathVariable Long id, @Valid @RequestBody SlotSaveRequest request) {
        return ApiResponse.ok(slotService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @OpLog(module = "时段管理", operation = "删除参观时段")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        slotService.delete(id);
        return ApiResponse.ok();
    }
}
