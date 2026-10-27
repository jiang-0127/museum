package com.museum.reservation.module.museum.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.museum.reservation.common.BusinessException;
import com.museum.reservation.common.Constants;
import com.museum.reservation.common.PageResult;
import com.museum.reservation.module.museum.dto.MuseumSaveRequest;
import com.museum.reservation.module.museum.entity.Museum;
import com.museum.reservation.module.museum.mapper.MuseumMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class MuseumService {

    private final MuseumMapper museumMapper;

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
                        .orderByDesc(Museum::getSort)
                        .orderByAsc(Museum::getId));
        return PageResult.of(result);
    }

    public Museum getById(Long id) {
        Museum museum = museumMapper.selectById(id);
        if (museum == null) {
            throw new BusinessException(404, "博物馆不存在");
        }
        return museum;
    }

    @Transactional
    public Museum create(MuseumSaveRequest request) {
        Museum museum = new Museum();
        BeanUtils.copyProperties(request, museum);
        museum.setStatus(request.getStatus() == null ? Constants.STATUS_ENABLED : request.getStatus());
        museumMapper.insert(museum);
        return museum;
    }

    @Transactional
    public Museum update(Long id, MuseumSaveRequest request) {
        Museum museum = getById(id);
        BeanUtils.copyProperties(request, museum);
        museumMapper.updateById(museum);
        return museum;
    }

    @Transactional
    public void delete(Long id) {
        Museum museum = getById(id);
        museum.setStatus(Constants.STATUS_DISABLED);
        museumMapper.updateById(museum);
    }
}
