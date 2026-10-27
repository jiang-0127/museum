package com.museum.reservation.module.reservation.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ReservationDetailVO extends ReservationVO {
    private List<VisitorVO> visitors;
    private String paymentPayType;
    private LocalDateTime paymentPaidTime;
    private LocalDateTime paymentRefundTime;
    private String qrToken;
}
