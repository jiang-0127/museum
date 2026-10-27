package com.museum.reservation.module.exhibition.controller;

import com.museum.reservation.common.ApiResponse;
import com.museum.reservation.common.PageResult;
import com.museum.reservation.common.log.OpLog;
import com.museum.reservation.module.exhibition.dto.ExhibitionSaveRequest;
import com.museum.reservation.module.exhibition.service.ExhibitionService;
import com.museum.reservation.module.exhibition.vo.ExhibitionVO;
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

@Tag(name = "展览管理")
@RestController
@RequestMapping("/api/admin/exhibitions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('RESERVATION_ADMIN','SYSTEM_ADMIN')")
public class ExhibitionAdminController {

    private final ExhibitionService exhibitionService;

    @GetMapping
    public ApiResponse<PageResult<ExhibitionVO>> page(@RequestParam(defaultValue = "1") long page,
                                                      @RequestParam(defaultValue = "10") long size,
                                                      @RequestParam(required = false) Long museumId,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) Integer status) {
        return ApiResponse.ok(exhibitionService.adminPage(page, size, museumId, keyword, status));
    }

    @PostMapping
    @OpLog(module = "展览管理", operation = "新增展览")
    public ApiResponse<ExhibitionVO> create(@Valid @RequestBody ExhibitionSaveRequest request) {
        return ApiResponse.ok(exhibitionService.create(request));
    }

    @PutMapping("/{id}")
    @OpLog(module = "展览管理", operation = "修改展览")
    public ApiResponse<ExhibitionVO> update(@PathVariable Long id,
                                            @Valid @RequestBody ExhibitionSaveRequest request) {
        return ApiResponse.ok(exhibitionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @OpLog(module = "展览管理", operation = "下线展览")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        exhibitionService.delete(id);
        return ApiResponse.ok();
    }
}
