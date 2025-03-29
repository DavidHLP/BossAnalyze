package com.david.hlp.Spring.system.service;

/**
 * 密码服务接口
 *
 * @author david
 * @since 1.0
 */
public interface PasswordService {

    /**
     * 对密码进行加密
     *
     * @param password 原始密码
     * @return 加密后的密码
     */
    String encodePassword(String password);

    /**
     * 验证原始密码与加密密码是否匹配
     *
     * @param rawPassword 原始密码
     * @param encodedPassword 加密后的密码
     * @return 如果匹配返回true，否则返回false
     */
    boolean matches(String rawPassword, String encodedPassword);
}