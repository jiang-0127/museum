package com.museum.reservation.module.reservation.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("visitor")
public class Visitor {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reservationId;
    private Long slotId;
    private String name;
    private String idCard;
    private String phone;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
