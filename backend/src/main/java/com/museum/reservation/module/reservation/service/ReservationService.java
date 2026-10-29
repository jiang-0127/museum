package com.museum.reservation.module.reservation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.museum.reservation.common.BusinessException;
import com.museum.reservation.common.Constants;
import com.museum.reservation.common.MuseumScheduleUtils;
import com.museum.reservation.common.MaskUtil;
import com.museum.reservation.common.OrderNoGenerator;
import com.museum.reservation.common.PageResult;
import com.museum.reservation.common.qr.QrCodeService;
import com.museum.reservation.common.qr.QrTokenService;
import com.museum.reservation.module.museum.entity.Museum;
import com.museum.reservation.module.museum.mapper.MuseumMapper;
import com.museum.reservation.module.payment.entity.PaymentOrder;
import com.museum.reservation.module.payment.mapper.PaymentOrderMapper;
import com.museum.reservation.module.reservation.dto.ReservationCreateRequest;
import com.museum.reservation.module.reservation.dto.ReservationQuery;
import com.museum.reservation.module.reservation.dto.VisitorRequest;
import com.museum.reservation.module.reservation.entity.Reservation;
import com.museum.reservation.module.reservation.entity.SlotGuestLock;
import com.museum.reservation.module.reservation.entity.Visitor;
import com.museum.reservation.module.reservation.mapper.ReservationMapper;
import com.museum.reservation.module.reservation.mapper.SlotGuestLockMapper;
import com.museum.reservation.module.reservation.mapper.VisitorMapper;
import com.museum.reservation.module.reservation.vo.ReservationDetailVO;
import com.museum.reservation.module.reservation.vo.ReservationVO;
import com.museum.reservation.module.reservation.vo.TicketVO;
import com.museum.reservation.module.reservation.vo.VisitorVO;
import com.museum.reservation.module.slot.entity.VisitSlot;
import com.museum.reservation.module.slot.mapper.VisitSlotMapper;
import com.museum.reservation.module.slot.service.SlotService;
import com.museum.reservation.module.slot.vo.SlotVO;
import com.museum.reservation.module.system.service.SystemConfigService;
import com.museum.reservation.module.ticket.entity.TicketType;
import com.museum.reservation.module.ticket.mapper.TicketTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationMapper reservationMapper;
    private final VisitorMapper visitorMapper;
    private final SlotGuestLockMapper guestLockMapper;
    private final VisitSlotMapper slotMapper;
    private final TicketTypeMapper ticketTypeMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final MuseumMapper museumMapper;
    private final SlotService slotService;
    private final QrTokenService qrTokenService;
    private final QrCodeService qrCodeService;
    private final SystemConfigService systemConfigService;

    @Value("${app.reservation.max-visitors:6}")
    private int defaultMaxVisitors;

    @Value("${app.reservation.payment-timeout-minutes:15}")
    private long paymentTimeoutMinutes;

    @Transactional(rollbackFor = Exception.class)
    public ReservationDetailVO create(Long userId, ReservationCreateRequest request) {
        List<VisitorRequest> visitors = request.getVisitors();
        int maxVisitors = systemConfigService.getInt("max_visitors_per_order", defaultMaxVisitors);
        if (visitors.size() > maxVisitors) {
            throw new BusinessException("单笔预约最多 " + maxVisitors + " 人");
        }
        Set<String> idCards = new HashSet<>();
        for (VisitorRequest visitor : visitors) {
            String idCard = visitor.getIdCard().toUpperCase();
            if (!idCards.add(idCard)) {
                throw new BusinessException("同一订单中不能出现重复的身份证号");
            }
        }

        TicketType ticketType = ticketTypeMapper.selectById(request.getTicketTypeId());
        if (ticketType == null || ticketType.getStatus() == null
                || ticketType.getStatus() != Constants.STATUS_ENABLED) {
            throw new BusinessException("票种不存在或已停用");
        }
        Museum museum = museumMapper.selectById(ticketType.getMuseumId());
        if (museum == null) {
            throw new BusinessException("所属博物馆不存在");
        }

        String kind = ticketType.getTicketKind() == null ? "BASIC" : ticketType.getTicketKind();
        int count = visitors.size();
        LocalDateTime now = LocalDateTime.now();
        LocalDate visitDate;
        Long slotId = null;
        LocalDateTime expireAt;
        BigDecimal totalAmount = ticketType.getPrice().multiply(BigDecimal.valueOf(count));
        boolean free = totalAmount.compareTo(BigDecimal.ZERO) <= 0;
        String orderNo = OrderNoGenerator.nextOrderNo();

        if ("BASIC".equals(kind)) {
            SlotVO slot = request.getSlotId() == null ? null : slotMapper.selectVoById(request.getSlotId());
            visitDate = request.getVisitDate() != null ? request.getVisitDate()
                    : slot == null ? null : slot.getVisitDate();
            if (visitDate == null) {
                throw new BusinessException("请选择参观日期");
            }
            if (visitDate.isBefore(LocalDate.now())) {
                throw new BusinessException("参观日期不能早于今天");
            }
            if (MuseumScheduleUtils.isClosed(museum, visitDate)) {
                throw new BusinessException("该日期为博物馆闭馆日，暂不可预约");
            }
            LocalTime closeTime = parseTime(museum.getCloseTime(), LocalTime.of(17, 0));
            expireAt = visitDate.atTime(closeTime);
            if (!visitDate.isAfter(LocalDate.now()) && now.isAfter(expireAt)) {
                throw new BusinessException("该日期已闭馆，请选择其他日期");
            }
            int dailyCapacity = museum.getDailyCapacity() == null ? 1200 : museum.getDailyCapacity();
            int slotCapacitySum = slotMapper.selectList(new LambdaQueryWrapper<VisitSlot>()
                            .eq(VisitSlot::getMuseumId, museum.getId())
                            .eq(VisitSlot::getVisitDate, visitDate)
                            .ne(VisitSlot::getStatus, Constants.STATUS_DELETED))
                    .stream()
                    .mapToInt(item -> item.getTotalCapacity() == null ? 0 : item.getTotalCapacity())
                    .sum();
            int basicQuota = Math.max(0, dailyCapacity - slotCapacitySum);
            long basicBooked = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                            .eq(Reservation::getMuseumId, museum.getId())
                            .eq(Reservation::getVisitDate, visitDate)
                            .isNull(Reservation::getSlotId)
                            .in(Reservation::getStatus, Constants.RESERVATION_PENDING_PAYMENT,
                                    Constants.RESERVATION_RESERVED, Constants.RESERVATION_CHECKED_IN))
                    .stream()
                    .mapToInt(Reservation::getVisitorCount)
                    .sum();
            if (basicBooked + count > basicQuota) {
                throw new BusinessException("该日期普通预约票余量不足，仅剩 "
                        + Math.max(0, basicQuota - basicBooked) + " 张");
            }
            for (VisitorRequest visitor : visitors) {
                long duplicated = reservationMapper.countBasicGuest(
                        museum.getId(), visitDate, visitor.getIdCard().toUpperCase());
                if (duplicated > 0) {
                    throw new BusinessException("身份证号 " + MaskUtil.idCard(visitor.getIdCard())
                            + " 已预约该馆当天的普通票，请勿重复预约");
                }
            }
        } else {
            if (request.getSlotId() == null) {
                throw new BusinessException("该票种需要选择入馆批次");
            }
            SlotVO slot = slotMapper.selectVoById(request.getSlotId());
            if (slot == null || slot.getStatus() == null || slot.getStatus() != Constants.STATUS_ENABLED) {
                throw new BusinessException("入馆批次不存在或已关闭");
            }
            LocalDateTime slotStart = slot.getVisitDate().atTime(slot.getStartTime());
            LocalDateTime slotEnd = slot.getVisitDate().atTime(slot.getEndTime());
            if (slot.getVisitDate().isBefore(LocalDate.now()) || slotEnd.isBefore(now)) {
                throw new BusinessException("该入馆批次已结束，请选择其他批次");
            }
            if (!now.isBefore(slotStart)) {
                throw new BusinessException("该入馆批次正在进行中，无法预约");
            }
            if (!ticketType.getMuseumId().equals(slot.getMuseumId())) {
                throw new BusinessException("票种与所选博物馆不一致");
            }
            if (MuseumScheduleUtils.isClosed(museum, slot.getVisitDate())) {
                throw new BusinessException("该日期为博物馆闭馆日，暂不可预约");
            }
            for (VisitorRequest visitor : visitors) {
                try {
                    SlotGuestLock lock = new SlotGuestLock();
                    lock.setSlotId(slot.getId());
                    lock.setIdCard(visitor.getIdCard().toUpperCase());
                    guestLockMapper.insert(lock);
                } catch (DuplicateKeyException e) {
                    throw new BusinessException("身份证号 " + MaskUtil.idCard(visitor.getIdCard())
                            + " 已预约该批次，请勿重复预约");
                }
            }
            int reserved = slotMapper.reserveStock(slot.getId(), count);
            if (reserved == 0) {
                throw new BusinessException("该批次余票不足，请选择其他批次");
            }
            visitDate = slot.getVisitDate();
            slotId = slot.getId();
            expireAt = slotEnd.plusMinutes(30);
        }

        Reservation reservation = new Reservation();
        reservation.setOrderNo(orderNo);
        reservation.setUserId(userId);
        reservation.setMuseumId(ticketType.getMuseumId());
        reservation.setExhibitionId(ticketType.getExhibitionId());
        reservation.setSlotId(slotId);
        reservation.setVisitDate(visitDate);
        reservation.setTicketTypeId(ticketType.getId());
        reservation.setVisitorCount(count);
        reservation.setUnitPrice(ticketType.getPrice());
        reservation.setTotalAmount(totalAmount);
        reservation.setContactName(request.getContactName());
        reservation.setContactPhone(request.getContactPhone());
        reservation.setRemark(request.getRemark());
        reservation.setStatus(free ? Constants.RESERVATION_RESERVED : Constants.RESERVATION_PENDING_PAYMENT);
        if (free) {
            reservation.setQrExpireAt(expireAt);
            reservation.setQrToken(qrTokenService.generate(orderNo, toInstant(expireAt)));
        }
        reservationMapper.insert(reservation);

        for (VisitorRequest visitorRequest : visitors) {
            Visitor visitor = new Visitor();
            visitor.setReservationId(reservation.getId());
            visitor.setSlotId(slotId);
            visitor.setName(visitorRequest.getName());
            visitor.setIdCard(visitorRequest.getIdCard().toUpperCase());
            visitor.setPhone(visitorRequest.getPhone());
            visitorMapper.insert(visitor);

            if (slotId != null) {
                guestLockMapper.update(null, new LambdaUpdateWrapper<SlotGuestLock>()
                        .eq(SlotGuestLock::getSlotId, slotId)
                        .eq(SlotGuestLock::getIdCard, visitorRequest.getIdCard().toUpperCase())
                        .isNull(SlotGuestLock::getReservationId)
                        .set(SlotGuestLock::getReservationId, reservation.getId()));
            }
        }

        PaymentOrder payment = new PaymentOrder();
        payment.setPaymentNo(OrderNoGenerator.nextPaymentNo());
        payment.setOrderNo(orderNo);
        payment.setReservationId(reservation.getId());
        payment.setUserId(userId);
        payment.setAmount(totalAmount);
        payment.setPayType(free ? "FREE" : "MOCK");
        payment.setStatus(free ? Constants.PAYMENT_PAID : Constants.PAYMENT_UNPAID);
        payment.setPaidTime(free ? LocalDateTime.now() : null);
        paymentOrderMapper.insert(payment);

        if (slotId != null) {
            slotService.evictAfterCommit(slotId);
        }
        return getDetailInternal(reservation.getId(), userId, false);
    }

    private LocalTime parseTime(String value, LocalTime fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        try {
            return LocalTime.parse(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    public PageResult<ReservationVO> myPage(Long userId, long page, long size, String status) {
        ReservationQuery query = new ReservationQuery();
        query.setUserId(userId);
        query.setStatus(status);
        return queryPage(page, size, query);
    }

    public PageResult<ReservationVO> adminPage(long page, long size, ReservationQuery query) {
        return queryPage(page, size, query);
    }

    private PageResult<ReservationVO> queryPage(long page, long size, ReservationQuery query) {
        Page<ReservationVO> result = new Page<>(page, Math.min(size, 100));
        reservationMapper.selectVoPage(result, query);
        result.getRecords().forEach(this::maskReservation);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public ReservationDetailVO getDetail(Long id, Long userId, boolean admin) {
        return getDetailInternal(id, userId, admin);
    }

    public ReservationDetailVO getDetailInternal(Long id, Long userId, boolean admin) {
        ReservationVO vo = reservationMapper.selectVoById(id);
        if (vo == null) {
            throw new BusinessException(404, "预约订单不存在");
        }
        if (!admin && !vo.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权查看该预约");
        }
        ReservationDetailVO detail = new ReservationDetailVO();
        BeanUtils.copyProperties(vo, detail);
        detail.setContactPhone(MaskUtil.phone(vo.getContactPhone()));

        List<Visitor> visitors = visitorMapper.selectList(new LambdaQueryWrapper<Visitor>()
                .eq(Visitor::getReservationId, id).orderByAsc(Visitor::getId));
        detail.setVisitors(visitors.stream().map(visitor -> {
            VisitorVO visitorVO = new VisitorVO();
            visitorVO.setId(visitor.getId());
            visitorVO.setName(visitor.getName());
            visitorVO.setIdCard(MaskUtil.idCard(visitor.getIdCard()));
            visitorVO.setPhone(MaskUtil.phone(visitor.getPhone()));
            return visitorVO;
        }).toList());

        PaymentOrder payment = paymentOrderMapper.selectByReservationId(id);
        if (payment != null) {
            detail.setPaymentNo(payment.getPaymentNo());
            detail.setPaymentStatus(payment.getStatus());
            detail.setPaymentPayType(payment.getPayType());
            detail.setPaymentPaidTime(payment.getPaidTime());
            detail.setPaymentRefundTime(payment.getRefundTime());
        }
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation != null && reservation.getQrToken() != null) {
            detail.setQrToken(reservation.getQrToken());
        }
        return detail;
    }

    public TicketVO getTicket(Long id, Long userId, boolean admin) {
        ReservationVO vo = reservationMapper.selectVoById(id);
        if (vo == null) {
            throw new BusinessException(404, "预约订单不存在");
        }
        if (!admin && !vo.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权查看该电子票");
        }
        if (!Constants.RESERVATION_RESERVED.equals(vo.getStatus())) {
            if (Constants.RESERVATION_PENDING_PAYMENT.equals(vo.getStatus())) {
                throw new BusinessException("订单尚未支付，请完成支付后再查看电子票");
            }
            throw new BusinessException("当前订单状态不可使用电子票");
        }
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation.getQrToken() == null || reservation.getQrExpireAt() == null) {
            throw new BusinessException("电子票正在生成，请稍后重试");
        }
        if (reservation.getQrExpireAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("电子票已过期");
        }
        TicketVO ticket = new TicketVO();
        ticket.setReservationId(id);
        ticket.setOrderNo(vo.getOrderNo());
        ticket.setStatus(vo.getStatus());
        ticket.setMuseumName(vo.getMuseumName());
        ticket.setExhibitionName(vo.getExhibitionName());
        ticket.setVisitDate(vo.getVisitDate());
        ticket.setStartTime(vo.getStartTime());
        ticket.setEndTime(vo.getEndTime());
        ticket.setVisitorCount(vo.getVisitorCount());
        ticket.setContactName(vo.getContactName());
        ticket.setExpireAt(reservation.getQrExpireAt());
        ticket.setTicketCode(reservation.getQrToken());
        ticket.setQrImage(qrCodeService.toBase64Png(reservation.getQrToken(), 360));
        return ticket;
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id, Long userId, boolean admin) {
        Reservation reservation = reservationMapper.selectForUpdate(id);
        if (reservation == null) {
            throw new BusinessException(404, "预约订单不存在");
        }
        if (!admin && !reservation.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权取消该预约");
        }
        if (!Constants.RESERVATION_PENDING_PAYMENT.equals(reservation.getStatus())
                && !Constants.RESERVATION_RESERVED.equals(reservation.getStatus())) {
            if (Constants.RESERVATION_CHECKED_IN.equals(reservation.getStatus())) {
                throw new BusinessException("订单已核销，不能取消");
            }
            if (Constants.RESERVATION_EXPIRED.equals(reservation.getStatus())) {
                throw new BusinessException("订单已过期，不能取消");
            }
            throw new BusinessException("当前订单状态不能取消");
        }
        if (!admin && reservation.getSlotId() != null) {
            VisitSlot slot = slotMapper.selectById(reservation.getSlotId());
            if (slot != null && LocalDateTime.now().isAfter(slot.getVisitDate().atTime(slot.getStartTime()))) {
                throw new BusinessException("参观时段已开始，不能取消预约");
            }
        }

        PaymentOrder payment = paymentOrderMapper.selectByReservationId(id);
        if (payment != null) {
            if (Constants.PAYMENT_PAID.equals(payment.getStatus())) {
                payment.setStatus(Constants.PAYMENT_REFUNDED);
                payment.setRefundTime(LocalDateTime.now());
            } else if (Constants.PAYMENT_UNPAID.equals(payment.getStatus())) {
                payment.setStatus(Constants.PAYMENT_CLOSED);
            }
            paymentOrderMapper.updateById(payment);
        }

        reservation.setStatus(Constants.RESERVATION_CANCELED);
        reservation.setCancelTime(LocalDateTime.now());
        reservationMapper.updateById(reservation);
        releaseSlotQuota(reservation);
    }

    private void releaseSlotQuota(Reservation reservation) {
        if (reservation.getSlotId() == null) {
            return;
        }
        slotMapper.releaseStock(reservation.getSlotId(), reservation.getVisitorCount());
        guestLockMapper.delete(new LambdaQueryWrapper<SlotGuestLock>()
                .eq(SlotGuestLock::getSlotId, reservation.getSlotId())
                .eq(SlotGuestLock::getReservationId, reservation.getId()));
        slotService.evictAfterCommit(reservation.getSlotId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void closePendingReservation(Long id) {
        Reservation reservation = reservationMapper.selectForUpdate(id);
        if (reservation == null || !Constants.RESERVATION_PENDING_PAYMENT.equals(reservation.getStatus())) {
            return;
        }
        reservation.setStatus(Constants.RESERVATION_CANCELED);
        reservation.setCancelTime(LocalDateTime.now());
        reservationMapper.updateById(reservation);

        PaymentOrder payment = paymentOrderMapper.selectByReservationId(id);
        if (payment != null && Constants.PAYMENT_UNPAID.equals(payment.getStatus())) {
            payment.setStatus(Constants.PAYMENT_CLOSED);
            paymentOrderMapper.updateById(payment);
        }
        releaseSlotQuota(reservation);
    }

    @Transactional(rollbackFor = Exception.class)
    public void expireReservation(Long id) {
        Reservation reservation = reservationMapper.selectForUpdate(id);
        if (reservation == null || !Constants.RESERVATION_RESERVED.equals(reservation.getStatus())) {
            return;
        }
        reservation.setStatus(Constants.RESERVATION_EXPIRED);
        reservationMapper.updateById(reservation);
        if (reservation.getSlotId() != null) {
            guestLockMapper.delete(new LambdaQueryWrapper<SlotGuestLock>()
                    .eq(SlotGuestLock::getSlotId, reservation.getSlotId())
                    .eq(SlotGuestLock::getReservationId, id));
            slotService.evictAfterCommit(reservation.getSlotId());
        }
    }

    public List<Reservation> findTimeoutPending() {
        return reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getStatus, Constants.RESERVATION_PENDING_PAYMENT)
                .lt(Reservation::getCreatedAt, LocalDateTime.now().minusMinutes(paymentTimeoutMinutes)));
    }

    public List<Reservation> findExpiredReserved() {
        return reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getStatus, Constants.RESERVATION_RESERVED)
                .isNotNull(Reservation::getQrExpireAt)
                .lt(Reservation::getQrExpireAt, LocalDateTime.now()));
    }

    public long getPaymentTimeoutMinutes() {
        return paymentTimeoutMinutes;
    }

    public Reservation findById(Long id) {
        return reservationMapper.selectById(id);
    }

    public String museumName(Long museumId) {
        Museum museum = museumMapper.selectById(museumId);
        return museum == null ? "" : museum.getName();
    }

    private void maskReservation(ReservationVO vo) {
        vo.setContactPhone(MaskUtil.phone(vo.getContactPhone()));
    }

    private java.time.Instant toInstant(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toInstant();
    }
}
