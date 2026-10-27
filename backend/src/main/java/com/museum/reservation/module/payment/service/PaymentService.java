package com.museum.reservation.module.payment.service;

import com.museum.reservation.common.BusinessException;
import com.museum.reservation.common.Constants;
import com.museum.reservation.common.qr.QrTokenService;
import com.museum.reservation.module.payment.entity.PaymentOrder;
import com.museum.reservation.module.payment.mapper.PaymentOrderMapper;
import com.museum.reservation.module.reservation.entity.Reservation;
import com.museum.reservation.module.reservation.mapper.ReservationMapper;
import com.museum.reservation.module.reservation.service.ReservationService;
import com.museum.reservation.module.reservation.vo.ReservationDetailVO;
import com.museum.reservation.module.slot.entity.VisitSlot;
import com.museum.reservation.module.slot.mapper.VisitSlotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentOrderMapper paymentOrderMapper;
    private final ReservationMapper reservationMapper;
    private final VisitSlotMapper slotMapper;
    private final ReservationService reservationService;
    private final QrTokenService qrTokenService;

    @Transactional(rollbackFor = Exception.class)
    public ReservationDetailVO mockPay(String orderNo, Long userId) {
        PaymentOrder payment = paymentOrderMapper.selectByOrderNo(orderNo);
        if (payment == null) {
            throw new BusinessException(404, "支付单不存在");
        }
        if (!payment.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权支付该订单");
        }
        if (Constants.PAYMENT_PAID.equals(payment.getStatus())) {
            return reservationService.getDetailInternal(payment.getReservationId(), userId, false);
        }
        if (!Constants.PAYMENT_UNPAID.equals(payment.getStatus())) {
            throw new BusinessException("支付单状态异常，无法支付");
        }

        Reservation reservation = reservationMapper.selectForUpdate(payment.getReservationId());
        if (reservation == null || !Constants.RESERVATION_PENDING_PAYMENT.equals(reservation.getStatus())) {
            throw new BusinessException("预约订单状态已变化，无法支付");
        }
        if (reservation.getCreatedAt() != null
                && reservation.getCreatedAt().plusMinutes(reservationService.getPaymentTimeoutMinutes())
                .isBefore(LocalDateTime.now())) {
            throw new BusinessException("订单已超过支付时限，请重新预约");
        }

        VisitSlot slot = slotMapper.selectById(reservation.getSlotId());
        if (slot == null) {
            throw new BusinessException("参观时段不存在");
        }
        LocalDateTime expireAt = slot.getVisitDate().atTime(slot.getEndTime()).plusMinutes(30);
        String token = qrTokenService.generate(reservation.getOrderNo(),
                expireAt.atZone(ZoneId.systemDefault()).toInstant());

        payment.setStatus(Constants.PAYMENT_PAID);
        payment.setPaidTime(LocalDateTime.now());
        paymentOrderMapper.updateById(payment);

        reservation.setStatus(Constants.RESERVATION_RESERVED);
        reservation.setQrToken(token);
        reservation.setQrExpireAt(expireAt);
        reservationMapper.updateById(reservation);

        return reservationService.getDetailInternal(reservation.getId(), userId, false);
    }

    public PaymentOrder getByOrderNo(String orderNo) {
        return paymentOrderMapper.selectByOrderNo(orderNo);
    }
}
