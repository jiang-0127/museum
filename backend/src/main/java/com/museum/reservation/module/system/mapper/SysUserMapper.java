package com.museum.reservation.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.museum.reservation.module.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("""
            SELECT r.role_code
            FROM sys_role r
            JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
            """)
    List<String> findRoleCodes(@Param("userId") Long userId);

    @Select("""
            SELECT r.role_name
            FROM sys_role r
            JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
            """)
    List<String> findRoleNames(@Param("userId") Long userId);
}
