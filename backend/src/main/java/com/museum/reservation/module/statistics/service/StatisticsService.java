package com.museum.reservation.module.statistics.service;

import com.museum.reservation.config.CacheService;
import com.museum.reservation.module.statistics.mapper.StatisticsMapper;
import com.museum.reservation.module.statistics.vo.NameValue;
import com.museum.reservation.module.statistics.vo.OverviewVO;
import com.museum.reservation.module.statistics.vo.SlotSaturationVO;
import com.museum.reservation.module.statistics.vo.TrendPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final StatisticsMapper statisticsMapper;
    private final CacheService cacheService;

    public OverviewVO overview() {
        String key = "stats:overview";
        OverviewVO cached = cacheService.getJson(key, OverviewVO.class);
        if (cached != null) {
            return cached;
        }
        OverviewVO vo = new OverviewVO();
        vo.setTodayReservations(statisticsMapper.countTodayReservations());
        vo.setTodayVisitors(statisticsMapper.countTodayVisitors());
        vo.setTodayCheckins(statisticsMapper.countTodayCheckins());
        vo.setTodayCanceled(statisticsMapper.countTodayCanceled());
        vo.setPendingPayments(statisticsMapper.countPendingPayments());
        vo.setTotalMuseums(statisticsMapper.countMuseums());
        vo.setTotalUsers(statisticsMapper.countUsers());
        vo.setTotalReservations(statisticsMapper.countReservations());
        cacheService.setJson(key, vo, Duration.ofSeconds(30));
        return vo;
    }

    public List<TrendPoint> trend(int days) {
        int safeDays = Math.max(7, Math.min(days, 90));
        LocalDateTime start = LocalDate.now().minusDays(safeDays - 1L).atStartOfDay();
        Map<LocalDate, TrendPoint> data = new HashMap<>();
        for (TrendPoint point : statisticsMapper.selectReservationTrend(start)) {
            TrendPoint target = data.computeIfAbsent(point.getDate(), this::emptyPoint);
            target.setReservations(point.getReservations());
            target.setVisitors(point.getVisitors());
        }
        for (TrendPoint point : statisticsMapper.selectCheckinTrend(start)) {
            data.computeIfAbsent(point.getDate(), this::emptyPoint).setCheckins(point.getCheckins());
        }
        for (TrendPoint point : statisticsMapper.selectCancelTrend(start)) {
            data.computeIfAbsent(point.getDate(), this::emptyPoint).setCanceled(point.getCanceled());
        }
        List<TrendPoint> result = new ArrayList<>();
        for (int i = 0; i < safeDays; i++) {
            LocalDate date = start.toLocalDate().plusDays(i);
            result.add(data.getOrDefault(date, emptyPoint(date)));
        }
        return result;
    }

    public List<NameValue> museumHeat(int days) {
        LocalDateTime start = LocalDate.now().minusDays(Math.max(1, Math.min(days, 365)) - 1L).atStartOfDay();
        return statisticsMapper.selectMuseumHeat(start);
    }

    public List<NameValue> ticketDistribution() {
        return statisticsMapper.selectTicketDistribution();
    }

    public List<SlotSaturationVO> slotSaturation() {
        return statisticsMapper.selectSlotSaturation();
    }

    private TrendPoint emptyPoint(LocalDate date) {
        TrendPoint point = new TrendPoint();
        point.setDate(date);
        return point;
    }
}
