package com.david.hlp.Spring.system.service.imp;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.david.hlp.Spring.system.service.PasswordService;

import lombok.RequiredArgsConstructor;

/**
 * 密码服务实现类
 *
 * @author david
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {
    private final PasswordEncoder passwordEncoder;

    /**
     * 对密码进行加密
     *
     * @param password 原始密码
     * @return 加密后的密码
     */
    @Override
    public String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

    /**
     * 验证原始密码与加密密码是否匹配
     *
     * @param rawPassword 原始密码
     * @param encodedPassword 加密后的密码
     * @return 如果匹配返回true，否则返回false
     */
    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
