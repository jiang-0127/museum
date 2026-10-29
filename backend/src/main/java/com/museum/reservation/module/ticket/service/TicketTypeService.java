package com.museum.reservation.module.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.museum.reservation.common.BusinessException;
import com.museum.reservation.common.Constants;
import com.museum.reservation.common.PageResult;
import com.museum.reservation.module.museum.entity.Museum;
import com.museum.reservation.module.museum.mapper.MuseumMapper;
import com.museum.reservation.module.exhibition.entity.Exhibition;
import com.museum.reservation.module.exhibition.mapper.ExhibitionMapper;
import com.museum.reservation.module.reservation.entity.Reservation;
import com.museum.reservation.module.reservation.mapper.ReservationMapper;
import com.museum.reservation.module.ticket.dto.TicketTypeSaveRequest;
import com.museum.reservation.module.ticket.entity.TicketType;
import com.museum.reservation.module.ticket.mapper.TicketTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TicketTypeService {

    private final TicketTypeMapper ticketTypeMapper;
    private final MuseumMapper museumMapper;
    private final ExhibitionMapper exhibitionMapper;
    private final ReservationMapper reservationMapper;

    public List<TicketType> publicList(Long museumId) {
        return ticketTypeMapper.selectList(new LambdaQueryWrapper<TicketType>()
                .eq(TicketType::getMuseumId, museumId)
                .eq(TicketType::getStatus, Constants.STATUS_ENABLED)
                .orderByDesc(TicketType::getSort)
                .orderByAsc(TicketType::getId));
    }

    public PageResult<TicketType> adminPage(long page, long size, Long museumId, String keyword, Integer status) {
        Page<TicketType> result = ticketTypeMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<TicketType>()
                        .eq(museumId != null, TicketType::getMuseumId, museumId)
                        .like(keyword != null && !keyword.isBlank(), TicketType::getName, keyword)
                        .eq(status != null, TicketType::getStatus, status)
                        .ne(TicketType::getStatus, Constants.STATUS_DELETED)
                        .orderByDesc(TicketType::getSort)
                        .orderByAsc(TicketType::getId));
        return PageResult.of(result);
    }

    public TicketType getById(Long id) {
        TicketType ticketType = ticketTypeMapper.selectById(id);
        if (ticketType == null || ticketType.getStatus() == Constants.STATUS_DELETED) {
            throw new BusinessException(404, "票种不存在");
        }
        return ticketType;
    }

    @Transactional
    public TicketType create(TicketTypeSaveRequest request) {
        if (request.getStatus() != null && request.getStatus() == Constants.STATUS_DELETED) {
            throw new BusinessException("新增时不能设置为已删除状态");
        }
        validate(request, true);
        TicketType ticketType = new TicketType();
        BeanUtils.copyProperties(request, ticketType);
        ticketType.setStatus(request.getStatus() == null ? Constants.STATUS_ENABLED : request.getStatus());
        ticketTypeMapper.insert(ticketType);
        return ticketType;
    }

    @Transactional
    public TicketType update(Long id, TicketTypeSaveRequest request) {
        if (request.getStatus() != null && request.getStatus() == Constants.STATUS_DELETED) {
            throw new BusinessException("删除请使用删除操作");
        }
        int newStatus = request.getStatus() == null ? Constants.STATUS_ENABLED : request.getStatus();
        validate(request, newStatus == Constants.STATUS_ENABLED);
        TicketType ticketType = getById(id);
        BeanUtils.copyProperties(request, ticketType);
        ticketType.setStatus(newStatus);
        ticketTypeMapper.updateById(ticketType);
        return ticketType;
    }

    @Transactional
    public void delete(Long id) {
        TicketType ticketType = getById(id);
        if ("BASIC".equals(ticketType.getTicketKind())) {
            throw new BusinessException("普通预约票不能删除，只能停用");
        }
        Long active = reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getTicketTypeId, id)
                .in(Reservation::getStatus, Constants.RESERVATION_PENDING_PAYMENT,
                        Constants.RESERVATION_RESERVED, Constants.RESERVATION_CHECKED_IN));
        if (active > 0) {
            throw new BusinessException("该票种存在 " + active + " 条有效预约，不能删除，请先停用并处理完预约");
        }
        ticketType.setStatus(Constants.STATUS_DELETED);
        ticketTypeMapper.updateById(ticketType);
    }

    private void validate(TicketTypeSaveRequest request, boolean requireEnabled) {
        Museum museum = museumMapper.selectById(request.getMuseumId());
        if (museum == null) {
            throw new BusinessException("所属博物馆不存在");
        }
        if (requireEnabled && (museum.getStatus() == null || museum.getStatus() != Constants.STATUS_ENABLED)) {
            throw new BusinessException("所属博物馆已停用，不能新增或启用票种");
        }
        String kind = request.getTicketKind() == null ? "BASIC" : request.getTicketKind().toUpperCase();
        if (!Set.of("BASIC", "SINGLE", "COMBO").contains(kind)) {
            throw new BusinessException("票种类型不正确");
        }
        String audienceType = request.getAudienceType() == null ? "GENERAL" : request.getAudienceType().toUpperCase();
        if (!Set.of("GENERAL", "ADULT", "STUDENT", "FAMILY").contains(audienceType)) {
            throw new BusinessException("适用人群类型不正确");
        }
        if ("SINGLE".equals(kind)) {
            if (request.getExhibitionId() == null) {
                throw new BusinessException("单展票必须选择对应展览");
            }
            Exhibition exhibition = exhibitionMapper.selectById(request.getExhibitionId());
            if (exhibition == null || !exhibition.getMuseumId().equals(request.getMuseumId())) {
                throw new BusinessException("单展票对应的展览不属于该博物馆");
            }
            if (requireEnabled && (exhibition.getStatus() == null
                    || exhibition.getStatus() != Constants.STATUS_ENABLED)) {
                throw new BusinessException("对应展览已停用，不能新增或启用单展票");
            }
        } else {
            request.setExhibitionId(null);
        }
        if ("BASIC".equals(kind)) {
            request.setAudienceType("GENERAL");
        } else {
            request.setAudienceType(audienceType);
        }
        request.setTicketKind(kind);
    }
}
