package com.david.hlp.Spring.common.result;

import com.david.hlp.Spring.common.enums.ResultCode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> success() {
        return Result.<T>builder()
                .code(ResultCode.SUCCESS.getCode())
                .message(ResultCode.SUCCESS.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(ResultCode.SUCCESS.getCode())
                .message(ResultCode.SUCCESS.getMessage())
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> Result<T> success(String message) {
        return Result.<T>builder()
                .code(ResultCode.SUCCESS.getCode())
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> Result<T> error(Integer code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> Result<T> error(ResultCode code) {
        return Result.<T>builder()
                .code(code.getCode())
                .message(code.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> Result<T> error(ResultCode code, String customMessage) {
        return Result.<T>builder()
                .code(code.getCode())
                .message(customMessage)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
