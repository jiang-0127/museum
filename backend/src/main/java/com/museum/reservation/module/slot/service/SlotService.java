package com.museum.reservation.module.slot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.museum.reservation.common.BusinessException;
import com.museum.reservation.common.Constants;
import com.museum.reservation.common.MuseumScheduleUtils;
import com.museum.reservation.common.PageResult;
import com.museum.reservation.config.CacheService;
import com.museum.reservation.module.exhibition.entity.Exhibition;
import com.museum.reservation.module.exhibition.mapper.ExhibitionMapper;
import com.museum.reservation.module.museum.entity.Museum;
import com.museum.reservation.module.museum.mapper.MuseumMapper;
import com.museum.reservation.module.reservation.entity.Reservation;
import com.museum.reservation.module.reservation.mapper.ReservationMapper;
import com.museum.reservation.module.slot.dto.SlotBatchSaveRequest;
import com.museum.reservation.module.slot.dto.SlotSaveRequest;
import com.museum.reservation.module.slot.entity.VisitSlot;
import com.museum.reservation.module.slot.mapper.VisitSlotMapper;
import com.museum.reservation.module.slot.vo.SlotVO;
import com.museum.reservation.module.ticket.entity.TicketType;
import com.museum.reservation.module.ticket.mapper.TicketTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SlotService {

    private static final String CACHE_PREFIX = "slot:detail:";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final VisitSlotMapper slotMapper;
    private final MuseumMapper museumMapper;
    private final ExhibitionMapper exhibitionMapper;
    private final ReservationMapper reservationMapper;
    private final TicketTypeMapper ticketTypeMapper;
    private final CacheService cacheService;

    public List<SlotVO> publicList(Long museumId, Long exhibitionId, LocalDate date, LocalDate fromDate) {
        List<SlotVO> slots = slotMapper.selectVoList(museumId, exhibitionId, date, fromDate,
                Constants.STATUS_ENABLED, false);
        if (slots.isEmpty()) {
            return slots;
        }
        List<Long> museumIds = slots.stream().map(SlotVO::getMuseumId).distinct().toList();
        Map<Long, Museum> museums = museumMapper.selectList(
                        new LambdaQueryWrapper<Museum>().in(Museum::getId, museumIds))
                .stream()
                .collect(Collectors.toMap(Museum::getId, museum -> museum));
        return slots.stream()
                .filter(slot -> !MuseumScheduleUtils.isClosed(museums.get(slot.getMuseumId()),
                        slot.getVisitDate()))
                .toList();
    }

    public SlotVO publicDetail(Long id) {
        String key = CACHE_PREFIX + id;
        SlotVO cached = cacheService.getJson(key, SlotVO.class);
        if (cached != null) {
            return cached;
        }
        SlotVO slot = slotMapper.selectVoById(id);
        if (slot == null || slot.getStatus() == null || slot.getStatus() != Constants.STATUS_ENABLED) {
            throw new BusinessException(404, "入馆批次不存在或已关闭");
        }
        Museum museum = museumMapper.selectById(slot.getMuseumId());
        if (MuseumScheduleUtils.isClosed(museum, slot.getVisitDate())) {
            throw new BusinessException(404, "该日期为博物馆闭馆日");
        }
        cacheService.setJson(key, slot, Duration.ofSeconds(30));
        return slot;
    }

    public SlotVO getById(Long id) {
        SlotVO slot = slotMapper.selectVoById(id);
        if (slot == null || (slot.getStatus() != null && slot.getStatus() == Constants.STATUS_DELETED)) {
            throw new BusinessException(404, "入馆批次不存在");
        }
        return slot;
    }

    public PageResult<SlotVO> adminPage(long page, long size, Long museumId, Long exhibitionId,
                                        LocalDate date, Integer status) {
        slotMapper.closeElapsedSlots();
        Page<SlotVO> result = new Page<>(page, size);
        slotMapper.selectVoPage(result, museumId, exhibitionId, date, null, status, true);
        enrichStats(result.getRecords());
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public int closeElapsedSlots() {
        return slotMapper.closeElapsedSlots();
    }

    @Transactional
    public SlotVO create(SlotSaveRequest request) {
        if (request.getStatus() != null && request.getStatus() == Constants.STATUS_DELETED) {
            throw new BusinessException("新增时不能设置为已删除状态");
        }
        validateRequest(request);
        validateExhibitionPeriod(request.getMuseumId(), request.getVisitDate());
        validateDailyCapacity(request.getMuseumId(), request.getVisitDate(), request.getTotalCapacity(), null);
        if (!request.getVisitDate().atTime(request.getStartTime()).isAfter(LocalDateTime.now())) {
            throw new BusinessException("该入馆批次已开始或已结束，请选择未来的时间");
        }
        Long duplicate = slotMapper.selectCount(new LambdaQueryWrapper<VisitSlot>()
                .eq(VisitSlot::getMuseumId, request.getMuseumId())
                .eq(request.getExhibitionId() != null, VisitSlot::getExhibitionId, request.getExhibitionId())
                .isNull(request.getExhibitionId() == null, VisitSlot::getExhibitionId)
                .eq(VisitSlot::getVisitDate, request.getVisitDate())
                .eq(VisitSlot::getStartTime, request.getStartTime()));
        if (duplicate > 0) {
            throw new BusinessException("同一博物馆、日期和开始时间的时段已存在");
        }
        VisitSlot slot = new VisitSlot();
        BeanUtils.copyProperties(request, slot);
        slot.setBookedCount(0);
        slot.setStatus(request.getStatus() == null ? Constants.STATUS_ENABLED : request.getStatus());
        slotMapper.insert(slot);
        evictAfterCommit(slot.getId());
        return getById(slot.getId());
    }

    @Transactional
    public int createBatch(SlotBatchSaveRequest request) {
        if (request.getStatus() != null && request.getStatus() == Constants.STATUS_DELETED) {
            throw new BusinessException("新增时不能设置为已删除状态");
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException("结束日期不能早于开始日期");
        }
        Museum batchMuseum = museumMapper.selectById(request.getMuseumId());
        if (batchMuseum == null) {
            throw new BusinessException("所属博物馆不存在");
        }
        if (batchMuseum.getStatus() == null || batchMuseum.getStatus() != Constants.STATUS_ENABLED) {
            throw new BusinessException("所属博物馆已停用，不能新增入馆批次");
        }
        validateSlotTime(batchMuseum, request.getStartTime(), request.getEndTime());
        int created = 0;
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        for (LocalDate date = request.getStartDate(); !date.isAfter(request.getEndDate()); date = date.plusDays(1)) {
            if (date.isBefore(today)) {
                continue;
            }
            LocalDateTime start = date.atTime(request.getStartTime());
            if (!start.isAfter(now)) {
                continue;
            }
            if (MuseumScheduleUtils.isClosed(batchMuseum, date)) {
                continue;
            }
            validateExhibitionPeriod(request.getMuseumId(), date);
            Long duplicate = slotMapper.selectCount(new LambdaQueryWrapper<VisitSlot>()
                    .eq(VisitSlot::getMuseumId, request.getMuseumId())
                    .isNull(VisitSlot::getExhibitionId)
                    .eq(VisitSlot::getVisitDate, date)
                    .eq(VisitSlot::getStartTime, request.getStartTime()));
            if (duplicate > 0) {
                continue;
            }
            validateDailyCapacity(request.getMuseumId(), date, request.getTotalCapacity(), null);
            VisitSlot slot = new VisitSlot();
            slot.setMuseumId(request.getMuseumId());
            slot.setVisitDate(date);
            slot.setStartTime(request.getStartTime());
            slot.setEndTime(request.getEndTime());
            slot.setTotalCapacity(request.getTotalCapacity());
            slot.setBookedCount(0);
            slot.setStatus(request.getStatus() == null ? Constants.STATUS_ENABLED : request.getStatus());
            slotMapper.insert(slot);
            evictAfterCommit(slot.getId());
            created++;
        }
        return created;
    }

    @Transactional
    public SlotVO update(Long id, SlotSaveRequest request) {
        if (request.getStatus() != null && request.getStatus() == Constants.STATUS_DELETED) {
            throw new BusinessException("删除请使用删除操作");
        }
        validateRequest(request);
        VisitSlot slot = slotMapper.selectById(id);
        if (slot == null || (slot.getStatus() != null && slot.getStatus() == Constants.STATUS_DELETED)) {
            throw new BusinessException(404, "入馆批次不存在");
        }
        if (!slot.getVisitDate().atTime(slot.getStartTime()).isAfter(LocalDateTime.now())) {
            throw new BusinessException("已开始或已结束的分时批次不能修改");
        }
        if (request.getTotalCapacity() < slot.getBookedCount()) {
            throw new BusinessException("总容量不能小于当前已预约人数 " + slot.getBookedCount());
        }
        validateDailyCapacity(request.getMuseumId(), request.getVisitDate(), request.getTotalCapacity(), id);
        if (slot.getBookedCount() > 0
                && (!slot.getVisitDate().equals(request.getVisitDate())
                || !slot.getStartTime().equals(request.getStartTime())
                || !slot.getEndTime().equals(request.getEndTime()))) {
            throw new BusinessException("该时段已有预约，不能修改日期或时间");
        }
        BeanUtils.copyProperties(request, slot);
        if (!slot.getVisitDate().atTime(slot.getStartTime()).isAfter(LocalDateTime.now())) {
            slot.setStatus(Constants.STATUS_DISABLED);
        }
        slotMapper.updateById(slot);
        evictAfterCommit(id);
        return getById(id);
    }

    @Transactional
    public void delete(Long id) {
        VisitSlot slot = slotMapper.selectById(id);
        if (slot == null || (slot.getStatus() != null && slot.getStatus() == Constants.STATUS_DELETED)) {
            throw new BusinessException(404, "入馆批次不存在");
        }
        Long active = reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getSlotId, id)
                .in(Reservation::getStatus, Constants.RESERVATION_PENDING_PAYMENT,
                        Constants.RESERVATION_RESERVED, Constants.RESERVATION_CHECKED_IN));
        if (active > 0) {
            throw new BusinessException("该入馆批次存在有效预约，不能删除，可先关闭该批次");
        }
        slot.setStatus(Constants.STATUS_DELETED);
        slotMapper.updateById(slot);
        evictAfterCommit(id);
    }

    public void evict(Long id) {
        cacheService.delete(CACHE_PREFIX + id);
    }

    public void evictAfterCommit(Long id) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evict(id);
                }
            });
        } else {
            evict(id);
        }
    }

    private void enrichStats(List<SlotVO> records) {
        if (records.isEmpty()) {
            return;
        }
        List<Long> museumIds = records.stream().map(SlotVO::getMuseumId).distinct().toList();
        Map<Long, List<TicketType>> ticketsByMuseum = ticketTypeMapper.selectList(
                        new LambdaQueryWrapper<TicketType>()
                                .in(TicketType::getMuseumId, museumIds)
                                .eq(TicketType::getStatus, Constants.STATUS_ENABLED)
                                .orderByAsc(TicketType::getSort)
                                .orderByAsc(TicketType::getId))
                .stream()
                .collect(Collectors.groupingBy(TicketType::getMuseumId));

        List<Long> slotIds = records.stream().map(SlotVO::getId).toList();
        Map<Long, List<Reservation>> reservationsBySlot = reservationMapper.selectList(
                        new LambdaQueryWrapper<Reservation>()
                                .in(Reservation::getSlotId, slotIds)
                                .in(Reservation::getStatus,
                                        Constants.RESERVATION_PENDING_PAYMENT,
                                        Constants.RESERVATION_RESERVED,
                                        Constants.RESERVATION_CHECKED_IN,
                                        Constants.RESERVATION_EXPIRED))
                .stream()
                .collect(Collectors.groupingBy(Reservation::getSlotId));

        for (SlotVO slot : records) {
            List<TicketType> ticketTypes = ticketsByMuseum.getOrDefault(slot.getMuseumId(), List.of())
                    .stream()
                    .filter(ticket -> !"BASIC".equals(ticket.getTicketKind() == null
                            ? "BASIC" : ticket.getTicketKind()))
                    .toList();
            slot.setTicketTypeCount(ticketTypes.size());
            slot.setTicketTypeNames(ticketTypes.stream().map(this::ticketLabel).toList());

            List<Reservation> reservations = reservationsBySlot.getOrDefault(slot.getId(), List.of());
            slot.setReservationOrderCount(reservations.size());
            slot.setReservationVisitorCount(
                    reservations.stream().mapToInt(Reservation::getVisitorCount).sum());
        }
    }

    private String ticketLabel(TicketType ticket) {
        String audience = switch (ticket.getAudienceType() == null ? "GENERAL" : ticket.getAudienceType()) {
            case "ADULT" -> "成人票";
            case "STUDENT" -> "学生票";
            case "FAMILY" -> "亲子票";
            default -> "";
        };
        return audience.isEmpty() ? ticket.getName() : ticket.getName() + " · " + audience;
    }

    private void validateDailyCapacity(Long museumId, LocalDate date, Integer newCapacity, Long excludeSlotId) {
        Museum museum = museumMapper.selectById(museumId);
        if (museum == null || museum.getDailyCapacity() == null) {
            return;
        }
        int sum = slotMapper.selectList(new LambdaQueryWrapper<VisitSlot>()
                        .eq(VisitSlot::getMuseumId, museumId)
                        .eq(VisitSlot::getVisitDate, date)
                        .ne(VisitSlot::getStatus, Constants.STATUS_DELETED))
                .stream()
                .filter(slot -> excludeSlotId == null || !excludeSlotId.equals(slot.getId()))
                .mapToInt(slot -> slot.getTotalCapacity() == null ? 0 : slot.getTotalCapacity())
                .sum();
        int capacity = newCapacity == null ? 0 : newCapacity;
        if (sum + capacity > museum.getDailyCapacity()) {
            throw new BusinessException("当天入馆批次额定容量之和不能超过日容量 "
                    + museum.getDailyCapacity() + "，当前已设置 " + sum);
        }
    }

    private void validateSlotTime(Museum museum, LocalTime start, LocalTime end) {
        LocalTime open = parseMuseumTime(museum.getOpenTime());
        LocalTime close = parseMuseumTime(museum.getCloseTime());
        if (open == null || close == null || !close.isAfter(open)) {
            return;
        }
        if (start.isBefore(open) || end.isAfter(close)) {
            throw new BusinessException("分时批次时间需在博物馆开放时间 "
                    + open.format(TIME_FORMAT) + " - " + close.format(TIME_FORMAT) + " 内");
        }
    }

    private LocalTime parseMuseumTime(String value) {
        if (value == null) {
            return null;
        }
        String[] parts = value.trim().split(":");
        if (parts.length < 2) {
            return null;
        }
        try {
            int hour = Integer.parseInt(parts[0].trim());
            int minute = Integer.parseInt(parts[1].trim());
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                return null;
            }
            return LocalTime.of(hour, minute);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void validateExhibitionPeriod(Long museumId, LocalDate visitDate) {
        List<Exhibition> exhibitions = exhibitionMapper.selectList(new LambdaQueryWrapper<Exhibition>()
                .eq(Exhibition::getMuseumId, museumId)
                .eq(Exhibition::getStatus, Constants.STATUS_ENABLED));
        if (exhibitions.isEmpty()) {
            return;
        }
        LocalDate start = exhibitions.stream()
                .map(Exhibition::getStartDate)
                .min(LocalDate::compareTo)
                .orElse(null);
        LocalDate end = exhibitions.stream()
                .map(Exhibition::getEndDate)
                .max(LocalDate::compareTo)
                .orElse(null);
        if (start == null || end == null) {
            return;
        }
        if (visitDate.isBefore(start) || visitDate.isAfter(end)) {
            throw new BusinessException("参观日期需在博物馆展览开放周期内（"
                    + start + " 至 " + end + "）");
        }
    }

    private void validateRequest(SlotSaveRequest request) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }
        Museum museum = museumMapper.selectById(request.getMuseumId());
        if (museum == null) {
            throw new BusinessException("所属博物馆不存在");
        }
        boolean enabling = request.getStatus() == null || request.getStatus() == Constants.STATUS_ENABLED;
        if (enabling && (museum.getStatus() == null || museum.getStatus() != Constants.STATUS_ENABLED)) {
            throw new BusinessException("所属博物馆已停用，不能新增或启用入馆批次");
        }
        if (enabling && MuseumScheduleUtils.isClosed(museum, request.getVisitDate())) {
            throw new BusinessException("该日期为博物馆闭馆日，不能开放或修改分时批次");
        }
        if (enabling) {
            validateSlotTime(museum, request.getStartTime(), request.getEndTime());
        }
        if (request.getExhibitionId() != null) {
            Exhibition exhibition = exhibitionMapper.selectById(request.getExhibitionId());
            if (exhibition == null || !exhibition.getMuseumId().equals(request.getMuseumId())) {
                throw new BusinessException("展览不存在或不属于该博物馆");
            }
            if (enabling && (exhibition.getStatus() == null
                    || exhibition.getStatus() != Constants.STATUS_ENABLED)) {
                throw new BusinessException("关联展览已停用，不能新增或启用入馆批次");
            }
        }
        if (request.getVisitDate().isBefore(LocalDate.now())) {
            throw new BusinessException("不能创建过去日期的入馆批次");
        }
    }
}
