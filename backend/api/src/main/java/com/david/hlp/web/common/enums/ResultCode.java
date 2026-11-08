package com.david.hlp.web.common.enums;

/**
 * 统一返回状态码枚举
 * 包含系统所有的状态码定义
 *
 * @author david
 */
public enum ResultCode {
    /** 成功状态码 */
    SUCCESS(200, "操作成功"),
    ERROR(500, "操作失败"),

    /** 客户端错误 4xx */
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权访问"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法错误"),

    /** 服务端错误 5xx */
    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),

    /** 自定义业务错误 10xx */
    USER_EXISTS(1001, "用户已存在"),
    INVALID_CREDENTIALS(1002, "用户名或密码错误"),
    CAPTCHA_ERROR(1003, "验证码错误"),
    CAPTCHA_EXPIRED(1004, "验证码过期"),
    USER_NOT_FOUND(1005, "用户不存在"),
    PASSWORD_ERROR(1006, "密码错误"),
    PARAM_ERROR(1007, "参数错误"),
    DUPLICATE_ERROR(1008, "数据重复"),
    REQUEST_ERROR(1009, "请求错误"),
    RESUME_NOT_FOUND(1010, "简历不存在"),
    LOCK_HAS_USED(1011, "已经被使用");

    private final Integer code;
    private final String message;

    /**
     * 构造方法
     *
     * @param code    状态码
     * @param message 状态信息
     */
    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 获取状态码
     *
     * @return 状态码
     */
    public Integer getCode() {
        return code;
    }

    /**
     * 获取状态信息
     *
     * @return 状态信息
     */
    public String getMessage() {
        return message;
    }
}
