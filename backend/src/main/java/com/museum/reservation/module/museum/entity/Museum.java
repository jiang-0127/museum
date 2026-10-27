package com.museum.reservation.module.museum.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("museum")
public class Museum {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String address;
    private String description;
    private String openTime;
    private String closeTime;
    private String phone;
    private String coverUrl;
    private Integer status;
    private Integer sort;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
