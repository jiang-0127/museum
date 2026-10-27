package com.museum.reservation.module.slot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlotStatusScheduler {

    private final SlotService slotService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 20_000)
    public void closeElapsedSlots() {
        try {
            int count = slotService.closeElapsedSlots();
            if (count > 0) {
                log.info("已自动关闭 {} 个已结束或正在进行的参观时段", count);
            }
        } catch (Exception e) {
            log.warn("自动关闭参观时段失败: {}", e.getMessage());
        }
    }
}
