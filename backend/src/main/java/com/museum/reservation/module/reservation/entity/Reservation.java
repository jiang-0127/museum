package com.museum.reservation.module.reservation.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("reservation")
public class Reservation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;
    private Long userId;
    private Long museumId;
    private Long exhibitionId;
    private Long slotId;
    private Long ticketTypeId;
    private Integer visitorCount;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private String contactName;
    private String contactPhone;
    private String status;
    private String qrToken;
    private LocalDateTime qrExpireAt;
    private String remark;
    private LocalDateTime cancelTime;
    private LocalDateTime checkinTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
