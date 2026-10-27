package com.museum.reservation.module.checkin.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("checkin_record")
public class CheckinRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reservationId;
    private String orderNo;
    private LocalDateTime checkinTime;
    private String gate;
    private Long operatorId;
    private String operatorName;
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
