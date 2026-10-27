package com.museum.reservation.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.museum.reservation.module.system.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
