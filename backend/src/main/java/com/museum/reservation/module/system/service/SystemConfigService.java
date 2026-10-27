package com.museum.reservation.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.museum.reservation.module.system.entity.SystemConfig;
import com.museum.reservation.module.system.mapper.SystemConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigMapper configMapper;

    public String getValue(String key, String fallback) {
        SystemConfig config = configMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, key));
        return config == null || config.getConfigValue() == null ? fallback : config.getConfigValue();
    }

    public int getInt(String key, int fallback) {
        try {
            return Integer.parseInt(getValue(key, String.valueOf(fallback)).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
