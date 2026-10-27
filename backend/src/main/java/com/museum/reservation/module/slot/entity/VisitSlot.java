package com.museum.reservation.module.slot.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("visit_slot")
public class VisitSlot {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long museumId;
    private Long exhibitionId;
    private LocalDate visitDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer totalCapacity;
    private Integer bookedCount;
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
