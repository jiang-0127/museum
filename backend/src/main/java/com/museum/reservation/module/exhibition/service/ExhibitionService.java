package com.museum.reservation.module.exhibition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
        if (exhibition == null) {
            throw new BusinessException(404, "展览不存在");
        }
        return exhibition;
    }

    @Transactional
    public ExhibitionVO create(ExhibitionSaveRequest request) {
        validate(request);
        Exhibition exhibition = new Exhibition();
        BeanUtils.copyProperties(request, exhibition);
        exhibition.setStatus(request.getStatus() == null ? Constants.STATUS_ENABLED : request.getStatus());
        exhibitionMapper.insert(exhibition);
        return toVO(exhibition);
    }

    @Transactional
    public ExhibitionVO update(Long id, ExhibitionSaveRequest request) {
        validate(request);
        Exhibition exhibition = getById(id);
        BeanUtils.copyProperties(request, exhibition);
        exhibitionMapper.updateById(exhibition);
        return toVO(exhibition);
    }

    @Transactional
    public void delete(Long id) {
        Exhibition exhibition = getById(id);
        exhibition.setStatus(Constants.STATUS_DISABLED);
        exhibitionMapper.updateById(exhibition);
    }

    private void validate(ExhibitionSaveRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException("结束日期不能早于开始日期");
        }
        Museum museum = museumMapper.selectById(request.getMuseumId());
        if (museum == null) {
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
