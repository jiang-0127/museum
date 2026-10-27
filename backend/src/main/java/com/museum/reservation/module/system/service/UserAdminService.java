package com.museum.reservation.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.museum.reservation.common.BusinessException;
import com.museum.reservation.common.Constants;
import com.museum.reservation.common.MaskUtil;
import com.museum.reservation.common.PageResult;
import com.museum.reservation.module.auth.vo.UserVO;
import com.museum.reservation.module.system.dto.UserCreateRequest;
import com.museum.reservation.module.system.entity.SysRole;
import com.museum.reservation.module.system.entity.SysUser;
import com.museum.reservation.module.system.entity.SysUserRole;
import com.museum.reservation.module.system.mapper.SysRoleMapper;
import com.museum.reservation.module.system.mapper.SysUserMapper;
import com.museum.reservation.module.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public PageResult<UserVO> page(long page, long size, String keyword, Integer status, String roleCode) {
        Page<SysUser> result = userMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SysUser>()
                        .and(StringUtils.hasText(keyword), q -> q.like(SysUser::getUsername, keyword)
                                .or().like(SysUser::getRealName, keyword)
                                .or().like(SysUser::getPhone, keyword))
                        .eq(status != null, SysUser::getStatus, status)
                        .orderByDesc(SysUser::getCreatedAt));
        List<UserVO> records = result.getRecords().stream()
                .map(this::toVO)
                .filter(vo -> !StringUtils.hasText(roleCode)
                        || (vo.getRoles() != null && vo.getRoles().contains(roleCode)))
                .toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    public List<SysRole> roles() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
    }

    @Transactional
    public UserVO create(UserCreateRequest request) {
        if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername())) > 0) {
            throw new BusinessException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setIdCard(request.getIdCard() == null ? null : request.getIdCard().toUpperCase());
        user.setEmail(request.getEmail());
        user.setStatus(Constants.STATUS_ENABLED);
        userMapper.insert(user);
        replaceRoles(user.getId(), request.getRoles());
        return toVO(user);
    }

    @Transactional
    public void updateStatus(Long id, Integer status, Long currentUserId) {
        if (id.equals(currentUserId) && status != null && status != Constants.STATUS_ENABLED) {
            throw new BusinessException("不能禁用当前登录账号");
        }
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Transactional
    public void assignRoles(Long id, List<String> roleCodes, Long currentUserId) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (id.equals(currentUserId) && !roleCodes.contains(Constants.ROLE_SYSTEM_ADMIN)) {
            throw new BusinessException("不能移除当前账号的系统管理员角色");
        }
        replaceRoles(id, roleCodes);
    }

    @Transactional
    public void resetPassword(Long id, String password) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setPasswordHash(passwordEncoder.encode(password));
        userMapper.updateById(user);
    }

    private void replaceRoles(Long userId, List<String> roleCodes) {
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getRoleCode, roleCodes));
        if (roles.size() != roleCodes.stream().distinct().count()) {
            throw new BusinessException("包含无效的角色编码");
        }
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        for (SysRole role : roles) {
            userRoleMapper.insert(new SysUserRole(userId, role.getId()));
        }
    }

    private UserVO toVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(MaskUtil.phone(user.getPhone()));
        vo.setIdCard(MaskUtil.idCard(user.getIdCard()));
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        vo.setRoles(userMapper.findRoleCodes(user.getId()));
        vo.setRoleNames(userMapper.findRoleNames(user.getId()));
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
