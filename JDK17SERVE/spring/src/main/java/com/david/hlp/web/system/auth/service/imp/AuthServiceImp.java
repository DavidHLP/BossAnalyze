package com.david.hlp.web.system.auth.service.imp;

import org.springframework.stereotype.Service;
import org.springframework.aop.framework.AopContext;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.transaction.annotation.Transactional;

import com.david.hlp.web.common.enums.ResultCode;
import com.david.hlp.web.common.exception.BusinessException;
import com.david.hlp.web.system.auth.entity.auth.AuthUser;
import com.david.hlp.web.system.auth.entity.auth.LoginDTO;
import com.david.hlp.web.system.auth.entity.auth.RegistrationDTO;
import com.david.hlp.web.system.auth.entity.user.User;
import com.david.hlp.web.system.auth.mapper.PermissionMapper;
import com.david.hlp.web.system.auth.mapper.RoleMapper;
import com.david.hlp.web.system.auth.mapper.TokenMapper;
import com.david.hlp.web.system.auth.mapper.UserMapper;
import com.david.hlp.web.system.auth.service.AuthService;
import com.david.hlp.web.system.auth.token.Token;
import com.david.hlp.web.system.auth.token.TokenType;
import com.david.hlp.web.system.auth.utils.JwtService;
import com.david.hlp.commons.utils.RedisCacheHelper;
import com.david.hlp.web.common.enums.RedisKeyEnum;
import com.david.hlp.web.common.enums.RedisLockKeyEnum;

/**
 * 认证服务实现类
 *
 * @author david
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImp implements AuthService<LoginDTO, RegistrationDTO, Token> {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RoleMapper roleMapper;
    private final TokenMapper tokenMapper;
    private final RedisCacheHelper redisCacheHelper;
    private final PermissionMapper permissionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerUser(RegistrationDTO request) {
        String lockKey = RedisLockKeyEnum.REGISTER_LOCK_KEY.getKey() + request.getEmail();

        redisCacheHelper.executeWithLock(lockKey, () -> {
            String code = redisCacheHelper.getString(RedisKeyEnum.REGISTER_CODE_KEY.getKey() + request.getEmail());
            if (code == null) {
                log.warn("注册失败: 邮箱{}验证码已过期", request.getEmail());
                throw new BusinessException(ResultCode.CAPTCHA_EXPIRED);
            }
            if (!code.equals(request.getCode())) {
                log.warn("注册失败: 邮箱{}验证码错误", request.getEmail());
                throw new BusinessException(ResultCode.CAPTCHA_ERROR);
            }
            AuthServiceImp authServiceImp = (AuthServiceImp) AopContext.currentProxy();
            authServiceImp.addUser(request);
            redisCacheHelper.delete(RedisKeyEnum.REGISTER_CODE_KEY.getKey() + request.getEmail());
        });
    }

    /**
     * 演示用户注册
     *
     * @param request 注册请求对象
     * @throws BusinessException 当用户已存在时抛出异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addUser(RegistrationDTO request) {
        if (userMapper.getByEmailToUser(request.getEmail()) != null) {
            log.warn("注册失败: 邮箱{}已存在", request.getEmail());
            throw new BusinessException(ResultCode.USER_EXISTS);
        }

        User newUser = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .status(1)
                .roleId(getDefaultRoleId())
                .build();

        userMapper.insert(newUser);
    }

    /**
     * 用户登录
     *
     * @param request 登录请求对象
     * @return Token 登录成功后的令牌
     * @throws BusinessException 当用户不存在或密码错误时抛出异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Token login(LoginDTO request) {
        AuthUser user = userMapper.getByEmailToAuthUser(request.getEmail());
        if (user == null) {
            log.warn("登录失败: 邮箱{}不存在", request.getEmail());
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        user.setAuthorities(permissionMapper.listPermissionNamesByRoleId(user.getRoleId()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("登录失败: 用户ID{}密码错误", user.getUserId());
            throw new BusinessException(ResultCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtService.generateToken(user);
        Token token = Token.builder()
                .authUser(user)
                .userId(user.getUserId())
                .token(accessToken)
                .tokenType(TokenType.ACCESS)
                .build();

        try {
            tokenMapper.save(token);
        } catch (Exception e) {
            log.error("保存token失败: 用户ID={}", user.getUserId(), e);
            throw e;
        }
        return token;
    }

    /**
     * 获取默认角色ID
     *
     * @return 默认角色ID
     */
    @Override
    public Long getDefaultRoleId() {
        try {
            return roleMapper.getDefaultRoleId();
        } catch (Exception e) {
            log.error("获取默认角色ID失败", e);
            throw e;
        }
    }

    /**
     * 根据用户ID获取密码
     *
     * @param userId 用户ID
     * @return 用户密码
     */
    @Override
    public String getPassword(Long userId) {
        try {
            return userMapper.getPasswordById(userId);
        } catch (Exception e) {
            log.error("获取用户密码失败: 用户ID={}", userId, e);
            throw e;
        }
    }
}
