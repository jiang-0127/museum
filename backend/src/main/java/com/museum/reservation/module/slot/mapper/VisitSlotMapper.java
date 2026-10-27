package com.museum.reservation.module.slot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.museum.reservation.module.slot.entity.VisitSlot;
import com.museum.reservation.module.slot.vo.SlotVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface VisitSlotMapper extends BaseMapper<VisitSlot> {

    @Update("""
            UPDATE visit_slot
            SET booked_count = booked_count + #{count}, updated_at = NOW()
            WHERE id = #{slotId} AND status = 1 AND booked_count + #{count} <= total_capacity
            """)
    int reserveStock(@Param("slotId") Long slotId, @Param("count") int count);

    @Update("""
            UPDATE visit_slot
            SET booked_count = GREATEST(booked_count - #{count}, 0), updated_at = NOW()
            WHERE id = #{slotId}
            """)
    int releaseStock(@Param("slotId") Long slotId, @Param("count") int count);

    IPage<SlotVO> selectVoPage(IPage<SlotVO> page,
                               @Param("museumId") Long museumId,
                               @Param("exhibitionId") Long exhibitionId,
                               @Param("date") LocalDate date,
                               @Param("fromDate") LocalDate fromDate,
                               @Param("status") Integer status,
                               @Param("includePast") boolean includePast);

    List<SlotVO> selectVoList(@Param("museumId") Long museumId,
                              @Param("exhibitionId") Long exhibitionId,
                              @Param("date") LocalDate date,
                              @Param("fromDate") LocalDate fromDate,
                              @Param("status") Integer status,
                              @Param("includePast") boolean includePast);

    SlotVO selectVoById(@Param("id") Long id);

    @Select("select * from visit_slot where id = #{id} for update")
    VisitSlot selectForUpdate(@Param("id") Long id);
}
