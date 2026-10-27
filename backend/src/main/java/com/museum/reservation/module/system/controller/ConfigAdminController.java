package com.museum.reservation.module.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.museum.reservation.common.ApiResponse;
import com.museum.reservation.common.BusinessException;
import com.museum.reservation.common.log.OpLog;
import com.museum.reservation.module.system.dto.ConfigUpdateRequest;
import com.museum.reservation.module.system.entity.SystemConfig;
import com.museum.reservation.module.system.mapper.SystemConfigMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "参数配置")
@RestController
@RequestMapping("/api/admin/configs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class ConfigAdminController {

    private final SystemConfigMapper configMapper;

    @GetMapping
    public ApiResponse<List<SystemConfig>> list() {
        return ApiResponse.ok(configMapper.selectList(
                new LambdaQueryWrapper<SystemConfig>().orderByAsc(SystemConfig::getId)));
    }

    @PutMapping("/{key}")
    @OpLog(module = "系统管理", operation = "修改参数配置")
    public ApiResponse<Void> update(@PathVariable String key,
                                    @Valid @RequestBody ConfigUpdateRequest request) {
        SystemConfig config = configMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, key));
        if (config == null) {
            throw new BusinessException(404, "配置项不存在");
        }
        config.setConfigValue(request.getConfigValue());
        configMapper.updateById(config);
        return ApiResponse.ok();
    }
}
