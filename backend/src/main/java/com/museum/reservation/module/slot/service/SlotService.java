package com.museum.reservation.module.slot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.museum.reservation.common.BusinessException;
import com.museum.reservation.common.Constants;
import com.museum.reservation.common.PageResult;
import com.museum.reservation.config.CacheService;
import com.museum.reservation.module.exhibition.entity.Exhibition;
import com.museum.reservation.module.exhibition.mapper.ExhibitionMapper;
import com.museum.reservation.module.museum.mapper.MuseumMapper;
import com.museum.reservation.module.reservation.entity.Reservation;
import com.museum.reservation.module.reservation.mapper.ReservationMapper;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SlotService {

    private static final String CACHE_PREFIX = "slot:detail:";

    private final VisitSlotMapper slotMapper;
    private final MuseumMapper museumMapper;
    private final ExhibitionMapper exhibitionMapper;
    private final ReservationMapper reservationMapper;
    private final TicketTypeMapper ticketTypeMapper;
    private final CacheService cacheService;

    public List<SlotVO> publicList(Long museumId, Long exhibitionId, LocalDate date, LocalDate fromDate) {
        return slotMapper.selectVoList(museumId, exhibitionId, date, fromDate,
                Constants.STATUS_ENABLED, false);
    }

    public SlotVO publicDetail(Long id) {
        String key = CACHE_PREFIX + id;
        SlotVO cached = cacheService.getJson(key, SlotVO.class);
        if (cached != null) {
            return cached;
        }
        SlotVO slot = slotMapper.selectVoById(id);
        if (slot == null || slot.getStatus() == null || slot.getStatus() != Constants.STATUS_ENABLED) {
            throw new BusinessException(404, "参观时段不存在或已关闭");
        }
        cacheService.setJson(key, slot, Duration.ofSeconds(30));
        return slot;
    }

    public SlotVO getById(Long id) {
        SlotVO slot = slotMapper.selectVoById(id);
        if (slot == null) {
            throw new BusinessException(404, "参观时段不存在");
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
        validateRequest(request);
        if (!request.getVisitDate().atTime(request.getStartTime()).isAfter(LocalDateTime.now())) {
            throw new BusinessException("该参观时段已开始或已结束，请选择未来的时间");
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
    public SlotVO update(Long id, SlotSaveRequest request) {
        validateRequest(request);
        VisitSlot slot = slotMapper.selectById(id);
        if (slot == null) {
            throw new BusinessException(404, "参观时段不存在");
        }
        if (request.getTotalCapacity() < slot.getBookedCount()) {
            throw new BusinessException("总容量不能小于当前已预约人数 " + slot.getBookedCount());
        }
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
        if (slot == null) {
            throw new BusinessException(404, "参观时段不存在");
        }
        Long active = reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getSlotId, id)
                .in(Reservation::getStatus, Constants.RESERVATION_PENDING_PAYMENT, Constants.RESERVATION_RESERVED));
        if (active > 0) {
            throw new BusinessException("该时段存在有效预约，不能删除，可先关闭该时段");
        }
        slotMapper.deleteById(id);
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
            List<TicketType> ticketTypes = ticketsByMuseum.getOrDefault(slot.getMuseumId(), List.of());
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

    private void validateRequest(SlotSaveRequest request) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }
        if (museumMapper.selectById(request.getMuseumId()) == null) {
            throw new BusinessException("所属博物馆不存在");
        }
        if (request.getExhibitionId() != null) {
            Exhibition exhibition = exhibitionMapper.selectById(request.getExhibitionId());
            if (exhibition == null || !exhibition.getMuseumId().equals(request.getMuseumId())) {
                throw new BusinessException("展览不存在或不属于该博物馆");
            }
        }
        if (request.getVisitDate().isBefore(LocalDate.now())) {
            throw new BusinessException("不能创建过去日期的参观时段");
        }
    }
}
