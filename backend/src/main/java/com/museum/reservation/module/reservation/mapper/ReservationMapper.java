package com.museum.reservation.module.reservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.museum.reservation.module.reservation.dto.ReservationQuery;
import com.museum.reservation.module.reservation.entity.Reservation;
import com.museum.reservation.module.reservation.vo.ReservationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {

    IPage<ReservationVO> selectVoPage(IPage<ReservationVO> page, @Param("query") ReservationQuery query);

    ReservationVO selectVoById(@Param("id") Long id);

    @Select("select * from reservation where id = #{id} for update")
    Reservation selectForUpdate(@Param("id") Long id);

    @Select("select * from reservation where order_no = #{orderNo}")
    Reservation selectByOrderNo(@Param("orderNo") String orderNo);
}
