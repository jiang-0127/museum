package com.museum.reservation.module.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.museum.reservation.module.payment.entity.PaymentOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {

    @Select("select * from payment_order where order_no = #{orderNo} order by id desc limit 1")
    PaymentOrder selectByOrderNo(@Param("orderNo") String orderNo);

    @Select("select * from payment_order where reservation_id = #{reservationId} order by id desc limit 1")
    PaymentOrder selectByReservationId(@Param("reservationId") Long reservationId);
}
