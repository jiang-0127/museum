package com.museum.reservation.module.checkin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.museum.reservation.common.BusinessException;
import com.museum.reservation.common.Constants;
import com.museum.reservation.common.PageResult;
import com.museum.reservation.common.qr.QrTokenService;
import com.museum.reservation.module.checkin.dto.CheckinRequest;
import com.museum.reservation.module.checkin.entity.CheckinRecord;
import com.museum.reservation.module.checkin.mapper.CheckinRecordMapper;
import com.museum.reservation.module.checkin.vo.CheckinResultVO;
import com.museum.reservation.module.museum.entity.Museum;
import com.museum.reservation.module.museum.mapper.MuseumMapper;
import com.museum.reservation.module.reservation.entity.Reservation;
import com.museum.reservation.module.reservation.mapper.ReservationMapper;
import com.museum.reservation.module.slot.entity.VisitSlot;
import com.museum.reservation.module.slot.mapper.VisitSlotMapper;
import com.museum.reservation.security.CurrentUser;
import com.museum.reservation.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class CheckinService {

    private final ReservationMapper reservationMapper;
    private final CheckinRecordMapper checkinRecordMapper;
    private final VisitSlotMapper slotMapper;
    private final MuseumMapper museumMapper;
    private final QrTokenService qrTokenService;

    @Transactional(rollbackFor = Exception.class)
    public CheckinResultVO verify(CheckinRequest request) {
        String code = request.getTicketCode().trim();
        if (!code.startsWith(Constants.TICKET_PREFIX)) {
            code = Constants.TICKET_PREFIX + code;
        }
        QrTokenService.TicketPayload payload = qrTokenService.parse(code);
        Reservation reservation = reservationMapper.selectByOrderNo(payload.orderNo());
        if (reservation == null) {
            throw new BusinessException("无效电子票，未找到对应预约");
        }
        reservation = reservationMapper.selectForUpdate(reservation.getId());
        if (reservation == null) {
            throw new BusinessException("无效电子票，未找到对应预约");
        }
        if (reservation.getQrToken() == null || !code.equals(reservation.getQrToken())) {
            throw new BusinessException("电子票校验失败，请使用最新电子票");
        }
        if (Constants.RESERVATION_CHECKED_IN.equals(reservation.getStatus())) {
            CheckinRecord record = checkinRecordMapper.selectOne(new LambdaQueryWrapper<CheckinRecord>()
                    .eq(CheckinRecord::getReservationId, reservation.getId()));
            String time = record == null ? "" : record.getCheckinTime().toString().replace('T', ' ').substring(0, 19);
            throw new BusinessException("该电子票已于 " + time + " 核销，请勿重复入场");
        }
        if (Constants.RESERVATION_PENDING_PAYMENT.equals(reservation.getStatus())) {
            throw new BusinessException("该订单尚未支付，不能入场");
        }
        if (Constants.RESERVATION_CANCELED.equals(reservation.getStatus())) {
            throw new BusinessException("该预约已取消，不能入场");
        }
        if (Constants.RESERVATION_EXPIRED.equals(reservation.getStatus())) {
            throw new BusinessException("该预约已过期，不能入场");
        }
        if (!Constants.RESERVATION_RESERVED.equals(reservation.getStatus())) {
            throw new BusinessException("当前预约状态不能核销");
        }

        LocalDate visitDate;
        LocalTime startTime = null;
        LocalTime endTime = null;
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        if (reservation.getSlotId() == null) {
            visitDate = reservation.getVisitDate();
            if (visitDate == null || !visitDate.equals(today)) {
                throw new BusinessException("电子票仅限参观日期当天使用，票面日期为 " + visitDate);
            }
            Museum museum = museumMapper.selectById(reservation.getMuseumId());
            LocalTime open = parseTime(museum == null ? null : museum.getOpenTime(), LocalTime.of(9, 0));
            LocalTime close = parseTime(museum == null ? null : museum.getCloseTime(), LocalTime.of(17, 0));
            LocalTime current = now.toLocalTime();
            if (current.isBefore(open) || current.isAfter(close)) {
                throw new BusinessException("当前不在博物馆开放时间内，无法核销");
            }
        } else {
            VisitSlot slot = slotMapper.selectById(reservation.getSlotId());
            if (slot == null) {
                throw new BusinessException("参观时段不存在");
            }
            visitDate = slot.getVisitDate();
            startTime = slot.getStartTime();
            endTime = slot.getEndTime();
            if (!visitDate.equals(today)) {
                throw new BusinessException("电子票仅限参观日期当天使用，票面日期为 " + visitDate);
            }
            LocalDateTime start = visitDate.atTime(startTime);
            LocalDateTime end = visitDate.atTime(endTime);
            if (now.isBefore(start.minusMinutes(90))) {
                throw new BusinessException("尚未到入场时间，可提前 90 分钟入场");
            }
            if (now.isAfter(end.plusMinutes(30))) {
                throw new BusinessException("已超过入场时段，无法核销");
            }
        }

        LoginUser operator = CurrentUser.get();
        LocalDateTime checkinTime = LocalDateTime.now();
        CheckinRecord record = new CheckinRecord();
        record.setReservationId(reservation.getId());
        record.setOrderNo(reservation.getOrderNo());
        record.setCheckinTime(checkinTime);
        record.setGate(request.getGate() == null || request.getGate().isBlank() ? "正门闸机" : request.getGate());
        record.setOperatorId(operator == null ? null : operator.getId());
        record.setOperatorName(operator == null ? "系统" : operator.getRealName());
        record.setStatus(Constants.LOG_SUCCESS);
        checkinRecordMapper.insert(record);

        reservation.setStatus(Constants.RESERVATION_CHECKED_IN);
        reservation.setCheckinTime(checkinTime);
        reservationMapper.updateById(reservation);

        Museum museum = museumMapper.selectById(reservation.getMuseumId());
        CheckinResultVO result = new CheckinResultVO();
        result.setSuccess(true);
        result.setMessage("核销成功，请通行");
        result.setOrderNo(reservation.getOrderNo());
        result.setMuseumName(museum == null ? "" : museum.getName());
        result.setContactName(reservation.getContactName());
        result.setVisitDate(visitDate);
        result.setStartTime(startTime);
        result.setEndTime(endTime);
        result.setVisitorCount(reservation.getVisitorCount());
        result.setStatus(Constants.RESERVATION_CHECKED_IN);
        result.setCheckinTime(checkinTime);
        return result;
    }

    private LocalTime parseTime(String value, LocalTime fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LocalTime.parse(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    public PageResult<CheckinRecord> recordPage(long page, long size, String keyword) {        Page<CheckinRecord> result = checkinRecordMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<CheckinRecord>()
                        .like(keyword != null && !keyword.isBlank(), CheckinRecord::getOrderNo, keyword)
                        .orderByDesc(CheckinRecord::getCheckinTime));
        return PageResult.of(result);
    }
}
