package com.museum.reservation.module.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.museum.reservation.common.BusinessException;
import com.museum.reservation.common.Constants;
import com.museum.reservation.config.CacheService;
import com.museum.reservation.module.auth.dto.LoginRequest;
import com.museum.reservation.module.auth.dto.ProfileUpdateRequest;
import com.museum.reservation.module.auth.dto.RegisterRequest;
import com.museum.reservation.module.auth.vo.LoginVO;
import com.museum.reservation.module.auth.vo.UserVO;
import com.museum.reservation.module.system.entity.SysRole;
import com.museum.reservation.module.system.entity.SysUser;
import com.museum.reservation.module.system.entity.SysUserRole;
import com.museum.reservation.module.system.mapper.SysRoleMapper;
import com.museum.reservation.module.system.mapper.SysUserMapper;
import com.museum.reservation.module.system.mapper.SysUserRoleMapper;
import com.museum.reservation.security.JwtTokenProvider;
import com.museum.reservation.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_LOGIN_FAILURES = 6;

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final CacheService cacheService;

    @Transactional
    public UserVO register(RegisterRequest request) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (exists > 0) {
            throw new BusinessException("用户名已存在，请更换后重试");
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

        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, Constants.ROLE_VISITOR));
        if (role == null) {
            throw new BusinessException(500, "系统角色未初始化，请联系管理员");
        }
        userRoleMapper.insert(new SysUserRole(user.getId(), role.getId()));
        return toUserVO(user);
    }

    public LoginVO login(LoginRequest request) {
        String username = request.getUsername().trim();
        String limitKey = "login:fail:" + username.toLowerCase();
        if (cacheService.current(limitKey) >= MAX_LOGIN_FAILURES) {
            throw new BusinessException(429, "登录失败次数过多，请 10 分钟后再试");
        }
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            // 仅在凭证校验失败时累加，正常登录不会被计入失败次数
            cacheService.increment(limitKey, Duration.ofMinutes(10));
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != Constants.STATUS_ENABLED) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }
        List<String> roles = userMapper.findRoleCodes(user.getId());
        List<String> roleNames = userMapper.findRoleNames(user.getId());
        LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), user.getRealName(), roles);
        String token = tokenProvider.generate(loginUser);

        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, user.getId())
                .set(SysUser::getLastLoginTime, LocalDateTime.now()));

        cacheService.delete(limitKey);
        user.setLastLoginTime(LocalDateTime.now());
        UserVO userVO = toUserVO(user);
        userVO.setRoles(roles);
        userVO.setRoleNames(roleNames);
        return new LoginVO(token, userVO);
    }

    public UserVO me(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在，请重新登录");
        }
        return toUserVO(user);
    }

    @Transactional
    public UserVO updateProfile(Long userId, ProfileUpdateRequest request) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在，请重新登录");
        }
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setIdCard(request.getIdCard() == null ? null : request.getIdCard().toUpperCase());
        user.setEmail(request.getEmail());
        user.setAvatar(request.getAvatar());
        userMapper.updateById(user);
        return toUserVO(user);
    }

    private UserVO toUserVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setIdCard(user.getIdCard());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setRoles(userMapper.findRoleCodes(user.getId()));
        vo.setRoleNames(userMapper.findRoleNames(user.getId()));
        return vo;
    }
}
