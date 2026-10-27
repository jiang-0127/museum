package com.museum.reservation.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.museum.reservation.module.system.entity.SystemConfig;
import com.museum.reservation.module.system.mapper.SystemConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SystemConfigInitializer implements ApplicationRunner {

    private static final Map<String, String[]> DEFAULTS = Map.of(
            "advance_booking_days", new String[]{"14", "可提前预约天数"}
    );

    private final SystemConfigMapper configMapper;

    @Override
    public void run(ApplicationArguments args) {
        DEFAULTS.forEach((key, value) -> {
            Long exists = configMapper.selectCount(new LambdaQueryWrapper<SystemConfig>()
                    .eq(SystemConfig::getConfigKey, key));
            if (exists == null || exists == 0) {
                SystemConfig config = new SystemConfig();
                config.setConfigKey(key);
                config.setConfigValue(value[0]);
                config.setDescription(value[1]);
                configMapper.insert(config);
            }
        });
    }
}
