package com.david.hlp.Spring.system.service;
import com.david.hlp.Spring.common.exception.BusinessException;
public interface AuthService<L , R, T> {
/**
     * 演示用户注册
     *
     * @param request 注册请求对象
     * @throws BusinessException 当用户已存在时抛出异常
     */
    public void addUser(R request) ;

    /**
     * 用户登录
     *
     * @param request 登录请求对象
     * @return Token 登录成功后的令牌
     * @throws BusinessException 当用户不存在或密码错误时抛出异常
     */
    public T login(L request);

    /**
     * 获取默认角色ID
     *
     * @return 默认角色ID
     */
    public Long getDefaultRoleId();

    /**
     * 根据用户ID获取密码
     *
     * @param userId 用户ID
     * @return 用户密码
     */
    public String getPassword(Long userId) ;

}
