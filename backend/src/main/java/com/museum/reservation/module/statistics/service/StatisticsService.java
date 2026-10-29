package com.museum.reservation.module.statistics.service;

import com.museum.reservation.config.CacheService;
import com.museum.reservation.module.statistics.mapper.StatisticsMapper;
import com.museum.reservation.module.statistics.vo.NameValue;
import com.museum.reservation.module.statistics.vo.OverviewVO;
import com.museum.reservation.module.statistics.vo.SlotSaturationVO;
import com.museum.reservation.module.statistics.vo.TicketDistributionVO;
import com.museum.reservation.module.statistics.vo.TicketKindDetailRow;
import com.museum.reservation.module.statistics.vo.TrendPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
        List<NameValue> result = new ArrayList<>();
        for (TicketKindDetailRow row : statisticsMapper.selectTicketDistributionRows()) {
            NameValue item = new NameValue();
            item.setName(kindLabel(row.getKindName()));
            item.setValue(row.getValue());
            result.add(item);
        }
        return result;
    }

    public List<TicketDistributionVO> ticketDistributionDetail() {
        Map<String, TicketDistributionVO> museumMap = new LinkedHashMap<>();
        for (TicketKindDetailRow row : statisticsMapper.selectTicketDistributionDetail()) {
            String museumName = row.getMuseumName() == null || row.getMuseumName().isBlank()
                    ? "未知博物馆" : row.getMuseumName();
            TicketDistributionVO museum = museumMap.computeIfAbsent(museumName, name -> {
                TicketDistributionVO vo = new TicketDistributionVO();
                vo.setName(name);
                vo.setValue(0);
                vo.setChildren(new ArrayList<>());
                return vo;
            });
            museum.setValue(museum.getValue() + row.getValue());
            NameValue child = new NameValue();
            child.setName(row.getTicketName() == null || row.getTicketName().isBlank()
                    ? "未知票种" : row.getTicketName());
            child.setValue(row.getValue());
            museum.getChildren().add(child);
        }
        List<TicketDistributionVO> result = new ArrayList<>(museumMap.values());
        result.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        return result;
    }

    private String kindLabel(String kind) {
        if (kind == null) {
            return "普通预约票";
        }
        return switch (kind) {
            case "SINGLE" -> "单展票";
            case "COMBO" -> "全馆联票";
            default -> "普通预约票";
        };
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
