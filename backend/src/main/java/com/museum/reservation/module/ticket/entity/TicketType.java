package com.museum.reservation.module.ticket.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ticket_type")
public class TicketType {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long museumId;
    private String name;
    private BigDecimal price;
    private String ticketKind;
    private Long exhibitionId;
    private String audienceType;
    private String tagline;
    private String audience;
    private String description;
    private String benefits;
    private String rules;
    private Integer status;
    private Integer sort;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
