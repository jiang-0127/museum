package com.museum.reservation.module.reservation.service;

import com.museum.reservation.module.reservation.entity.Reservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationScheduler {

    private final ReservationService reservationService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void closeTimeoutPayments() {
        List<Reservation> timeoutList = reservationService.findTimeoutPending();
        for (Reservation reservation : timeoutList) {
            try {
                reservationService.closePendingReservation(reservation.getId());
            } catch (Exception e) {
                log.warn("关闭超时订单失败, orderNo={}: {}", reservation.getOrderNo(), e.getMessage());
            }
        }
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 45_000)
    public void expireReservations() {
        List<Reservation> expiredList = reservationService.findExpiredReserved();
        for (Reservation reservation : expiredList) {
            try {
                reservationService.expireReservation(reservation.getId());
            } catch (Exception e) {
                log.warn("过期预约处理失败, orderNo={}: {}", reservation.getOrderNo(), e.getMessage());
            }
        }
    }
}
