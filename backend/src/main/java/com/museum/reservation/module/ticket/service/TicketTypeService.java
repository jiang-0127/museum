package com.museum.reservation.module.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.museum.reservation.common.BusinessException;
import com.museum.reservation.common.Constants;
import com.museum.reservation.common.PageResult;
import com.museum.reservation.module.museum.mapper.MuseumMapper;
import com.museum.reservation.module.exhibition.entity.Exhibition;
import com.museum.reservation.module.exhibition.mapper.ExhibitionMapper;
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
                        .orderByDesc(TicketType::getSort)
                        .orderByAsc(TicketType::getId));
        return PageResult.of(result);
    }

    public TicketType getById(Long id) {
        TicketType ticketType = ticketTypeMapper.selectById(id);
        if (ticketType == null) {
            throw new BusinessException(404, "票种不存在");
        }
        return ticketType;
    }

    @Transactional
    public TicketType create(TicketTypeSaveRequest request) {
        validate(request);
        TicketType ticketType = new TicketType();
        BeanUtils.copyProperties(request, ticketType);
        ticketType.setStatus(request.getStatus() == null ? Constants.STATUS_ENABLED : request.getStatus());
        ticketTypeMapper.insert(ticketType);
        return ticketType;
    }

    @Transactional
    public TicketType update(Long id, TicketTypeSaveRequest request) {
        validate(request);
        TicketType ticketType = getById(id);
        BeanUtils.copyProperties(request, ticketType);
        ticketTypeMapper.updateById(ticketType);
        return ticketType;
    }

    @Transactional
    public void delete(Long id) {
        TicketType ticketType = getById(id);
        ticketType.setStatus(Constants.STATUS_DISABLED);
        ticketTypeMapper.updateById(ticketType);
    }

    private void validate(TicketTypeSaveRequest request) {
        if (museumMapper.selectById(request.getMuseumId()) == null) {
            throw new BusinessException("所属博物馆不存在");
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
