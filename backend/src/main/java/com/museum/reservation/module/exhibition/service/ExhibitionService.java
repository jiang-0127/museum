package com.museum.reservation.module.exhibition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.museum.reservation.common.BusinessException;
import com.museum.reservation.common.Constants;
import com.museum.reservation.common.PageResult;
import com.museum.reservation.module.exhibition.dto.ExhibitionSaveRequest;
import com.museum.reservation.module.exhibition.entity.Exhibition;
import com.museum.reservation.module.exhibition.mapper.ExhibitionMapper;
import com.museum.reservation.module.exhibition.vo.ExhibitionVO;
import com.museum.reservation.module.museum.entity.Museum;
import com.museum.reservation.module.museum.mapper.MuseumMapper;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExhibitionService {

    private final ExhibitionMapper exhibitionMapper;
    private final MuseumMapper museumMapper;
    private final TicketTypeMapper ticketTypeMapper;
    private final VisitSlotMapper slotMapper;
    private final ReservationMapper reservationMapper;

    public PageResult<ExhibitionVO> publicPage(long page, long size, Long museumId, String keyword, boolean includePast) {
        Page<Exhibition> result = exhibitionMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Exhibition>()
                        .eq(Exhibition::getStatus, Constants.STATUS_ENABLED)
                        .eq(museumId != null, Exhibition::getMuseumId, museumId)
                        .like(StringUtils.hasText(keyword), Exhibition::getName, keyword)
                        .ge(!includePast, Exhibition::getEndDate, LocalDate.now())
                        .orderByDesc(Exhibition::getStartDate)
                        .orderByDesc(Exhibition::getId));
        return new PageResult<>(toVOList(result.getRecords()), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public ExhibitionVO publicDetail(Long id) {
        Exhibition exhibition = exhibitionMapper.selectById(id);
        if (exhibition == null || exhibition.getStatus() == null || exhibition.getStatus() != Constants.STATUS_ENABLED) {
            throw new BusinessException(404, "展览不存在或已下线");
        }
        return toVO(exhibition);
    }

    public PageResult<ExhibitionVO> adminPage(long page, long size, Long museumId, String keyword, Integer status) {
        Page<Exhibition> result = exhibitionMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Exhibition>()
                        .eq(museumId != null, Exhibition::getMuseumId, museumId)
                        .like(StringUtils.hasText(keyword), Exhibition::getName, keyword)
                        .eq(status != null, Exhibition::getStatus, status)
                        .ne(Exhibition::getStatus, Constants.STATUS_DELETED)
                        .orderByDesc(Exhibition::getStartDate)
                        .orderByDesc(Exhibition::getId));
        return new PageResult<>(toVOList(result.getRecords()), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public List<ExhibitionVO> listByMuseum(Long museumId) {
        List<Exhibition> list = exhibitionMapper.selectList(new LambdaQueryWrapper<Exhibition>()
                .eq(Exhibition::getMuseumId, museumId)
                .eq(Exhibition::getStatus, Constants.STATUS_ENABLED)
                .ge(Exhibition::getEndDate, LocalDate.now())
                .orderByAsc(Exhibition::getStartDate));
        return toVOList(list);
    }

    public Exhibition getById(Long id) {
        Exhibition exhibition = exhibitionMapper.selectById(id);
        if (exhibition == null || exhibition.getStatus() == Constants.STATUS_DELETED) {
            throw new BusinessException(404, "展览不存在");
        }
        return exhibition;
    }

    @Transactional
    public ExhibitionVO create(ExhibitionSaveRequest request) {
        if (request.getStatus() != null && request.getStatus() == Constants.STATUS_DELETED) {
            throw new BusinessException("新增时不能设置为已删除状态");
        }
        validateCommon(request);
        requireMuseumEnabled(request.getMuseumId());
        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new BusinessException("展览开始日期不能早于今天");
        }
        Exhibition exhibition = new Exhibition();
        BeanUtils.copyProperties(request, exhibition);
        exhibition.setStatus(request.getStatus() == null ? Constants.STATUS_ENABLED : request.getStatus());
        exhibitionMapper.insert(exhibition);
        return toVO(exhibition);
    }

    @Transactional
    public ExhibitionVO update(Long id, ExhibitionSaveRequest request) {
        if (request.getStatus() != null && request.getStatus() == Constants.STATUS_DELETED) {
            throw new BusinessException("删除请使用删除操作");
        }
        validateCommon(request);
        Exhibition exhibition = getById(id);
        int oldStatus = exhibition.getStatus() == null ? Constants.STATUS_ENABLED : exhibition.getStatus();
        int newStatus = request.getStatus() == null ? Constants.STATUS_ENABLED : request.getStatus();
        if (newStatus == Constants.STATUS_ENABLED && request.getEndDate().isBefore(LocalDate.now())) {
            throw new BusinessException("展览结束日期不能早于今天");
        }
        if (newStatus == Constants.STATUS_ENABLED) {
            requireMuseumEnabled(request.getMuseumId());
        }
        BeanUtils.copyProperties(request, exhibition);
        exhibition.setStatus(newStatus);
        exhibitionMapper.updateById(exhibition);
        if (oldStatus == Constants.STATUS_ENABLED && newStatus == Constants.STATUS_DISABLED) {
            cascadeDisable(id);
        }
        return toVO(exhibition);
    }

    @Transactional
    public void delete(Long id) {
        Exhibition exhibition = getById(id);
        Long active = reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getExhibitionId, id)
                .in(Reservation::getStatus, Constants.RESERVATION_PENDING_PAYMENT,
                        Constants.RESERVATION_RESERVED, Constants.RESERVATION_CHECKED_IN));
        if (active > 0) {
            throw new BusinessException("该展览存在 " + active + " 条有效预约，不能删除，请先停用并处理完预约");
        }
        exhibition.setStatus(Constants.STATUS_DELETED);
        exhibitionMapper.updateById(exhibition);
        cascadeDelete(id);
    }

    private void cascadeDelete(Long exhibitionId) {
        ticketTypeMapper.update(null, new LambdaUpdateWrapper<TicketType>()
                .eq(TicketType::getExhibitionId, exhibitionId)
                .set(TicketType::getStatus, Constants.STATUS_DELETED));
        slotMapper.update(null, new LambdaUpdateWrapper<VisitSlot>()
                .eq(VisitSlot::getExhibitionId, exhibitionId)
                .set(VisitSlot::getStatus, Constants.STATUS_DELETED));
    }

    private void cascadeDisable(Long exhibitionId) {
        ticketTypeMapper.update(null, new LambdaUpdateWrapper<TicketType>()
                .eq(TicketType::getExhibitionId, exhibitionId)
                .set(TicketType::getStatus, Constants.STATUS_DISABLED));
        slotMapper.update(null, new LambdaUpdateWrapper<VisitSlot>()
                .eq(VisitSlot::getExhibitionId, exhibitionId)
                .ge(VisitSlot::getVisitDate, LocalDate.now())
                .set(VisitSlot::getStatus, Constants.STATUS_DISABLED));
    }

    private void requireMuseumEnabled(Long museumId) {
        Museum museum = museumMapper.selectById(museumId);
        if (museum == null) {
            throw new BusinessException("所属博物馆不存在");
        }
        if (museum.getStatus() == null || museum.getStatus() != Constants.STATUS_ENABLED) {
            throw new BusinessException("所属博物馆已停用，不能新增或启用展览");
        }
    }

    private void validateCommon(ExhibitionSaveRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException("结束日期不能早于开始日期");
        }
        if (museumMapper.selectById(request.getMuseumId()) == null) {
            throw new BusinessException("所属博物馆不存在");
        }
    }

    private ExhibitionVO toVO(Exhibition exhibition) {
        return toVOList(List.of(exhibition)).get(0);
    }

    private List<ExhibitionVO> toVOList(List<Exhibition> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        List<Long> museumIds = list.stream().map(Exhibition::getMuseumId).distinct().toList();
        Map<Long, Museum> museums = museumMapper.selectBatchIds(museumIds).stream()
                .collect(Collectors.toMap(Museum::getId, Function.identity()));
        return list.stream().map(exhibition -> {
            ExhibitionVO vo = new ExhibitionVO();
            BeanUtils.copyProperties(exhibition, vo);
            Museum museum = museums.get(exhibition.getMuseumId());
            vo.setMuseumName(museum == null ? "" : museum.getName());
            return vo;
        }).toList();
    }
}
