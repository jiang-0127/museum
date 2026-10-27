package com.museum.reservation.module.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.museum.reservation.common.ApiResponse;
import com.museum.reservation.common.PageResult;
import com.museum.reservation.module.system.entity.OperationLog;
import com.museum.reservation.module.system.mapper.OperationLogMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "操作日志")
@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class LogAdminController {

    private final OperationLogMapper operationLogMapper;

    @GetMapping
    public ApiResponse<PageResult<OperationLog>> page(@RequestParam(defaultValue = "1") long page,
                                                      @RequestParam(defaultValue = "10") long size,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) String module,
                                                      @RequestParam(required = false) Integer success) {
        Page<OperationLog> result = operationLogMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<OperationLog>()
                        .and(StringUtils.hasText(keyword), q -> q.like(OperationLog::getUsername, keyword)
                                .or().like(OperationLog::getOperation, keyword)
                                .or().like(OperationLog::getModule, keyword))
                        .eq(StringUtils.hasText(module), OperationLog::getModule, module)
                        .eq(success != null, OperationLog::getSuccess, success)
                        .orderByDesc(OperationLog::getCreateTime));
        return ApiResponse.ok(PageResult.of(result));
    }
}
