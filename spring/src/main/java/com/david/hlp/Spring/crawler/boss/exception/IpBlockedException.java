package com.david.hlp.Spring.crawler.boss.exception;

/**
 * IP被封锁异常
 *
 * 当检测到IP被封锁时抛出此异常
 */
public class IpBlockedException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * 构造IP被封锁异常
     *
     * @param message 异常信息
     */
    public IpBlockedException(String message) {
        super(message);
    }

    /**
     * 构造IP被封锁异常
     *
     * @param message 异常信息
     * @param cause 原因
     */
    public IpBlockedException(String message, Throwable cause) {
        super(message, cause);
    }
}