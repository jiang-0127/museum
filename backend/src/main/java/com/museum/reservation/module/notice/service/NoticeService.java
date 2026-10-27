package com.museum.reservation.module.notice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.museum.reservation.common.BusinessException;
import com.museum.reservation.common.Constants;
import com.museum.reservation.common.PageResult;
import com.museum.reservation.module.notice.dto.NoticeSaveRequest;
import com.museum.reservation.module.notice.entity.Notice;
import com.museum.reservation.module.notice.mapper.NoticeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeMapper noticeMapper;

    public PageResult<Notice> publicPage(long page, long size) {
        Page<Notice> result = noticeMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Notice>()
                        .eq(Notice::getStatus, Constants.STATUS_ENABLED)
                        .and(q -> q.isNull(Notice::getPublishTime).or().le(Notice::getPublishTime, LocalDateTime.now()))
                        .orderByDesc(Notice::getPublishTime)
                        .orderByDesc(Notice::getId));
        return PageResult.of(result);
    }

    public List<Notice> latest(int limit) {
        return noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, Constants.STATUS_ENABLED)
                .and(q -> q.isNull(Notice::getPublishTime).or().le(Notice::getPublishTime, LocalDateTime.now()))
                .orderByDesc(Notice::getPublishTime)
                .orderByDesc(Notice::getId)
                .last("LIMIT " + Math.max(1, Math.min(limit, 20))));
    }

    public Notice publicDetail(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null || notice.getStatus() == null || notice.getStatus() != Constants.STATUS_ENABLED) {
            throw new BusinessException(404, "公告不存在");
        }
        return notice;
    }

    public PageResult<Notice> adminPage(long page, long size, String keyword, Integer status) {
        Page<Notice> result = noticeMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Notice>()
                        .like(StringUtils.hasText(keyword), Notice::getTitle, keyword)
                        .eq(status != null, Notice::getStatus, status)
                        .orderByDesc(Notice::getPublishTime)
                        .orderByDesc(Notice::getId));
        return PageResult.of(result);
    }

    public Notice getById(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException(404, "公告不存在");
        }
        return notice;
    }

    @Transactional
    public Notice create(NoticeSaveRequest request) {
        Notice notice = new Notice();
        BeanUtils.copyProperties(request, notice);
        if (notice.getPublishTime() == null) {
            notice.setPublishTime(LocalDateTime.now());
        }
        if (notice.getStatus() == null) {
            notice.setStatus(Constants.STATUS_ENABLED);
        }
        noticeMapper.insert(notice);
        return notice;
    }

    @Transactional
    public Notice update(Long id, NoticeSaveRequest request) {
        Notice notice = getById(id);
        BeanUtils.copyProperties(request, notice);
        noticeMapper.updateById(notice);
        return notice;
    }

    @Transactional
    public void delete(Long id) {
        Notice notice = getById(id);
        notice.setStatus(Constants.STATUS_DISABLED);
        noticeMapper.updateById(notice);
    }
}
