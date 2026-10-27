package com.museum.reservation.module.ticket.controller;

import com.museum.reservation.common.ApiResponse;
import com.museum.reservation.common.PageResult;
import com.museum.reservation.common.log.OpLog;
import com.museum.reservation.module.ticket.dto.TicketTypeSaveRequest;
import com.museum.reservation.module.ticket.entity.TicketType;
import com.museum.reservation.module.ticket.service.TicketTypeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

@Tag(name = "票种管理")
@RestController
@RequestMapping("/api/admin/ticket-types")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('RESERVATION_ADMIN','SYSTEM_ADMIN')")
public class TicketTypeAdminController {

    private final TicketTypeService ticketTypeService;

    @GetMapping
    public ApiResponse<PageResult<TicketType>> page(@RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) Long museumId,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) Integer status) {
        return ApiResponse.ok(ticketTypeService.adminPage(page, size, museumId, keyword, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<TicketType> detail(@PathVariable Long id) {
        return ApiResponse.ok(ticketTypeService.getById(id));
    }

    @PostMapping
    @OpLog(module = "票种管理", operation = "新增票种")
    public ApiResponse<TicketType> create(@Valid @RequestBody TicketTypeSaveRequest request) {
        return ApiResponse.ok(ticketTypeService.create(request));
    }

    @PutMapping("/{id}")
    @OpLog(module = "票种管理", operation = "修改票种")
    public ApiResponse<TicketType> update(@PathVariable Long id,
                                          @Valid @RequestBody TicketTypeSaveRequest request) {
        return ApiResponse.ok(ticketTypeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @OpLog(module = "票种管理", operation = "停用票种")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        ticketTypeService.delete(id);
        return ApiResponse.ok();
    }
}
