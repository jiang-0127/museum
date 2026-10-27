package com.museum.reservation.module.payment.controller;

import com.museum.reservation.common.ApiResponse;
import com.museum.reservation.common.log.OpLog;
import com.museum.reservation.module.payment.service.PaymentService;
import com.museum.reservation.module.reservation.vo.ReservationDetailVO;
import com.museum.reservation.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "支付")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "模拟支付")
    @PostMapping("/{orderNo}/mock-pay")
    @OpLog(module = "支付", operation = "模拟支付")
    public ApiResponse<ReservationDetailVO> mockPay(@PathVariable String orderNo) {
        return ApiResponse.ok(paymentService.mockPay(orderNo, CurrentUser.id()));
    }
}
