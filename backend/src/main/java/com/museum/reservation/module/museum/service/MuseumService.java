package com.museum.reservation.module.museum.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.museum.reservation.common.BusinessException;
import com.museum.reservation.common.Constants;
import com.museum.reservation.common.MuseumScheduleUtils;
import com.museum.reservation.common.PageResult;
import com.museum.reservation.module.exhibition.entity.Exhibition;
import com.museum.reservation.module.exhibition.mapper.ExhibitionMapper;
import com.museum.reservation.module.museum.dto.MuseumSaveRequest;
import com.museum.reservation.module.museum.entity.Museum;
import com.museum.reservation.module.museum.mapper.MuseumMapper;
import com.museum.reservation.module.museum.vo.BasicQuotaVO;
import com.museum.reservation.module.reservation.entity.Reservation;
import com.museum.reservation.module.reservation.mapper.ReservationMapper;
import com.museum.reservation.module.slot.entity.VisitSlot;
import com.museum.reservation.module.slot.mapper.VisitSlotMapper;
import com.museum.reservation.module.ticket.entity.TicketType;
import com.museum.reservation.module.ticket.mapper.TicketTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MuseumService {

    private final MuseumMapper museumMapper;
    private final TicketTypeMapper ticketTypeMapper;
    private final ExhibitionMapper exhibitionMapper;
    private final VisitSlotMapper slotMapper;
    private final ReservationMapper reservationMapper;

    public PageResult<Museum> publicPage(long page, long size, String keyword) {
        Page<Museum> result = museumMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Museum>()
                        .eq(Museum::getStatus, Constants.STATUS_ENABLED)
                        .and(StringUtils.hasText(keyword), q -> q.like(Museum::getName, keyword)
                                .or().like(Museum::getAddress, keyword))
                        .orderByDesc(Museum::getSort)
                        .orderByAsc(Museum::getId));
        return PageResult.of(result);
    }

    public long publicCount() {
        return museumMapper.selectCount(new LambdaQueryWrapper<Museum>()
                .eq(Museum::getStatus, Constants.STATUS_ENABLED));
    }

    public Museum publicDetail(Long id) {
        Museum museum = museumMapper.selectById(id);
        if (museum == null || museum.getStatus() == null || museum.getStatus() != Constants.STATUS_ENABLED) {
            throw new BusinessException(404, "博物馆不存在或已下线");
        }
        return museum;
    }

    public PageResult<Museum> adminPage(long page, long size, String keyword, Integer status) {
        Page<Museum> result = museumMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Museum>()
                        .like(StringUtils.hasText(keyword), Museum::getName, keyword)
                        .eq(status != null, Museum::getStatus, status)
                        .ne(Museum::getStatus, Constants.STATUS_DELETED)
                        .orderByDesc(Museum::getSort)
                        .orderByAsc(Museum::getId));
        return PageResult.of(result);
    }

    public Museum getById(Long id) {
        Museum museum = museumMapper.selectById(id);
        if (museum == null || museum.getStatus() == Constants.STATUS_DELETED) {
            throw new BusinessException(404, "博物馆不存在");
        }
        return museum;
    }

    public BasicQuotaVO basicQuota(Long museumId, LocalDate date) {
        Museum museum = publicDetail(museumId);
        int daily = museum.getDailyCapacity() == null ? 1200 : museum.getDailyCapacity();
        List<VisitSlot> slots = slotMapper.selectList(new LambdaQueryWrapper<VisitSlot>()
                .eq(VisitSlot::getMuseumId, museumId)
                .eq(VisitSlot::getVisitDate, date)
                .ne(VisitSlot::getStatus, Constants.STATUS_DELETED));
        int slotSum = slots.stream()
                .mapToInt(slot -> slot.getTotalCapacity() == null ? 0 : slot.getTotalCapacity())
                .sum();
        int quota = Math.max(0, daily - slotSum);
        List<Reservation> basics = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getMuseumId, museumId)
                .eq(Reservation::getVisitDate, date)
                .isNull(Reservation::getSlotId)
                .in(Reservation::getStatus, Constants.RESERVATION_PENDING_PAYMENT,
                        Constants.RESERVATION_RESERVED, Constants.RESERVATION_CHECKED_IN));
        int booked = basics.stream()
                .mapToInt(Reservation::getVisitorCount)
                .sum();
        boolean closed = MuseumScheduleUtils.isClosed(museum, date);
        BasicQuotaVO vo = new BasicQuotaVO();
        vo.setMuseumId(museumId);
        vo.setVisitDate(date);
        vo.setDailyCapacity(daily);
        vo.setSlotCapacitySum(slotSum);
        vo.setSlotCount(slots.size());
        vo.setBasicQuota(closed ? 0 : quota);
        vo.setBasicOrderCount(basics.size());
        vo.setBasicBooked(booked);
        vo.setBasicRemaining(closed ? 0 : Math.max(0, quota - booked));
        vo.setClosed(closed);
        return vo;
    }

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private LocalTime parseTime(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("请填写博物馆" + label);
        }
        String[] parts = value.trim().split(":");
        if (parts.length < 2) {
            throw new BusinessException("博物馆" + label + "格式不正确，应为 HH:mm");
        }
        try {
            int hour = Integer.parseInt(parts[0].trim());
            int minute = Integer.parseInt(parts[1].trim());
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                throw new NumberFormatException();
            }
            return LocalTime.of(hour, minute);
        } catch (NumberFormatException e) {
            throw new BusinessException("博物馆" + label + "格式不正确，应为 HH:mm");
        }
    }

    private LocalTime tryParseTime(String value) {
        try {
            return parseTime(value, "");
        } catch (BusinessException e) {
            return null;
        }
    }

    private void normalizeHours(MuseumSaveRequest request) {
        LocalTime open = parseTime(request.getOpenTime(), "开放时间");
        LocalTime close = parseTime(request.getCloseTime(), "闭馆时间");
        if (!close.isAfter(open)) {
            throw new BusinessException("闭馆时间必须晚于开放时间");
        }
        request.setOpenTime(open.format(TIME_FORMAT));
        request.setCloseTime(close.format(TIME_FORMAT));
    }

    private void validateFutureSlotsWithinHours(Long museumId, LocalTime open, LocalTime close) {
        List<VisitSlot> slots = slotMapper.selectList(new LambdaQueryWrapper<VisitSlot>()
                .eq(VisitSlot::getMuseumId, museumId)
                .ge(VisitSlot::getVisitDate, LocalDate.now())
                .ne(VisitSlot::getStatus, Constants.STATUS_DELETED));
        long conflict = slots.stream()
                .filter(slot -> slot.getStartTime() != null && slot.getEndTime() != null)
                .filter(slot -> slot.getStartTime().isBefore(open) || slot.getEndTime().isAfter(close))
                .count();
        if (conflict > 0) {
            throw new BusinessException("有 " + conflict + " 个未来分时批次超出新的开放时间 "
                    + open.format(TIME_FORMAT) + " - " + close.format(TIME_FORMAT) + "，请先调整批次时间");
        }
    }

    private void normalizeClosedDays(MuseumSaveRequest request) {
        String raw = request.getClosedDays();
        if (raw == null || raw.isBlank()) {
            request.setClosedDays(null);
            return;
        }
        TreeSet<Integer> days = new TreeSet<>();
        for (String part : raw.split(",")) {
            if (part.isBlank()) {
                continue;
            }
            try {
                int day = Integer.parseInt(part.trim());
                if (day < 1 || day > 7) {
                    throw new NumberFormatException();
                }
                days.add(day);
            } catch (NumberFormatException e) {
                throw new BusinessException("闭馆日格式不正确，应为 1-7 的星期编号");
            }
        }
        if (days.size() >= 7) {
            throw new BusinessException("每周至少保留一天开放");
        }
        request.setClosedDays(days.isEmpty() ? null
                : days.stream().map(String::valueOf).collect(Collectors.joining(",")));
    }
    @Transactional
    public Museum create(MuseumSaveRequest request) {
        if (request.getStatus() != null && request.getStatus() == Constants.STATUS_DELETED) {
            throw new BusinessException("新增时不能设置为已删除状态");
        }
        normalizeHours(request);
        normalizeClosedDays(request);
        Museum museum = new Museum();
        BeanUtils.copyProperties(request, museum);
        museum.setStatus(request.getStatus() == null ? Constants.STATUS_ENABLED : request.getStatus());
        if (museum.getDailyCapacity() == null || museum.getDailyCapacity() < 1) {
            museum.setDailyCapacity(1200);
        }
        museumMapper.insert(museum);
        ensureBasicTicket(museum.getId(), museum.getStatus(), true);
        return museum;
    }

    @Transactional
    public Museum update(Long id, MuseumSaveRequest request) {
        if (request.getStatus() != null && request.getStatus() == Constants.STATUS_DELETED) {
            throw new BusinessException("删除请使用删除操作");
        }
        normalizeHours(request);
        normalizeClosedDays(request);
        Museum museum = getById(id);
        int oldStatus = museum.getStatus() == null ? Constants.STATUS_ENABLED : museum.getStatus();
        int newStatus = request.getStatus() == null ? Constants.STATUS_ENABLED : request.getStatus();
        LocalTime oldOpen = tryParseTime(museum.getOpenTime());
        LocalTime oldClose = tryParseTime(museum.getCloseTime());
        LocalTime newOpen = parseTime(request.getOpenTime(), "开放时间");
        LocalTime newClose = parseTime(request.getCloseTime(), "闭馆时间");
        if (newStatus == Constants.STATUS_ENABLED
                && (!newOpen.equals(oldOpen) || !newClose.equals(oldClose))) {
            validateFutureSlotsWithinHours(id, newOpen, newClose);
        }
        Integer dailyCapacity = request.getDailyCapacity();
        BeanUtils.copyProperties(request, museum);
        if (dailyCapacity == null || dailyCapacity < 1) {
            museum.setDailyCapacity(1200);
        }
        museumMapper.updateById(museum);
        ensureBasicTicket(museum.getId(), newStatus, oldStatus != newStatus);
        if (oldStatus == Constants.STATUS_ENABLED && newStatus == Constants.STATUS_DISABLED) {
            cascadeDisable(id);
        }
        return museum;
    }

    private void ensureBasicTicket(Long museumId, Integer museumStatus, boolean syncStatus) {
        TicketType basic = ticketTypeMapper.selectOne(new LambdaQueryWrapper<TicketType>()
                .eq(TicketType::getMuseumId, museumId)
                .eq(TicketType::getTicketKind, "BASIC")
                .last("limit 1"));
        if (basic != null) {
            if (!syncStatus) {
                return;
            }
            int expectedStatus = museumStatus == null ? Constants.STATUS_ENABLED : museumStatus;
            if (basic.getStatus() == null || basic.getStatus() != expectedStatus) {
                basic.setStatus(expectedStatus);
                ticketTypeMapper.updateById(basic);
            }
            return;
        }
        TicketType ticket = new TicketType();
        ticket.setMuseumId(museumId);
        ticket.setName("普通预约票");
        ticket.setPrice(BigDecimal.ZERO);
        ticket.setTicketKind("BASIC");
        ticket.setAudienceType("GENERAL");
        ticket.setTagline("基础入馆");
        ticket.setAudience("所有观众");
        ticket.setDescription("免选入馆批次，开放时间内任意时间入馆，含常设展与基础开放区域");
        ticket.setBenefits("常设展与基本陈列\n基础开放区域\n开放时间内任意时间入馆");
        ticket.setRules("每单最多 6 人\n入馆须携带本人有效身份证件\n参观日期当天可取消");
        ticket.setStatus(museumStatus == null ? Constants.STATUS_ENABLED : museumStatus);
        ticket.setSort(300);
        ticketTypeMapper.insert(ticket);
    }

    @Transactional
    public void delete(Long id) {
        Museum museum = getById(id);
        Long active = reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getMuseumId, id)
                .in(Reservation::getStatus, Constants.RESERVATION_PENDING_PAYMENT,
                        Constants.RESERVATION_RESERVED, Constants.RESERVATION_CHECKED_IN));
        if (active > 0) {
            throw new BusinessException("该博物馆存在 " + active + " 条有效预约，不能删除，请先停用并处理完预约");
        }
        museum.setStatus(Constants.STATUS_DELETED);
        museumMapper.updateById(museum);
        cascadeDelete(id);
    }

    private void cascadeDelete(Long museumId) {
        exhibitionMapper.update(null, new LambdaUpdateWrapper<Exhibition>()
                .eq(Exhibition::getMuseumId, museumId)
                .set(Exhibition::getStatus, Constants.STATUS_DELETED));
        ticketTypeMapper.update(null, new LambdaUpdateWrapper<TicketType>()
                .eq(TicketType::getMuseumId, museumId)
                .set(TicketType::getStatus, Constants.STATUS_DELETED));
        slotMapper.update(null, new LambdaUpdateWrapper<VisitSlot>()
                .eq(VisitSlot::getMuseumId, museumId)
                .set(VisitSlot::getStatus, Constants.STATUS_DELETED));
    }

    private void cascadeDisable(Long museumId) {
        exhibitionMapper.update(null, new LambdaUpdateWrapper<Exhibition>()
                .eq(Exhibition::getMuseumId, museumId)
                .set(Exhibition::getStatus, Constants.STATUS_DISABLED));
        ticketTypeMapper.update(null, new LambdaUpdateWrapper<TicketType>()
                .eq(TicketType::getMuseumId, museumId)
                .set(TicketType::getStatus, Constants.STATUS_DISABLED));
        slotMapper.update(null, new LambdaUpdateWrapper<VisitSlot>()
                .eq(VisitSlot::getMuseumId, museumId)
                .ge(VisitSlot::getVisitDate, LocalDate.now())
                .set(VisitSlot::getStatus, Constants.STATUS_DISABLED));
    }
}
