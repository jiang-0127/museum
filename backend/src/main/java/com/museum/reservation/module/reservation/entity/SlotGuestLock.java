package com.museum.reservation.module.reservation.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("slot_guest_lock")
public class SlotGuestLock {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long slotId;
    private String idCard;
    private Long reservationId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
