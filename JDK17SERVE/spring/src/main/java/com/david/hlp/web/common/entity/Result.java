package com.david.hlp.web.common.entity;

import com.david.hlp.web.common.enums.ResultCode;
import lombok.ToString;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@ToString
@Accessors(chain = true)
public class Result<T> extends ResponseEntity<ResultBody<T>> {

    public Result(ResultBody<T> body, HttpStatus status) {
        super(body, status);
    }

    public Result(ResultBody<T> body, HttpHeaders headers, HttpStatus status) {
        super(body, headers, status);
    }

    public static <T> Result<T> success() {
        return new Result<>(
                ResultBody.<T>builder()
                        .code(ResultCode.SUCCESS.getCode())
                        .message(ResultCode.SUCCESS.getMessage())
                        .timestamp(System.currentTimeMillis())
                        .build(),
                HttpStatus.OK);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(
                ResultBody.<T>builder()
                        .code(ResultCode.SUCCESS.getCode())
                        .message(ResultCode.SUCCESS.getMessage())
                        .data(data)
                        .timestamp(System.currentTimeMillis())
                        .build(),
                HttpStatus.OK);
    }

    public static <T> Result<T> success(String message) {
        return new Result<>(
                ResultBody.<T>builder()
                        .code(ResultCode.SUCCESS.getCode())
                        .message(message)
                        .timestamp(System.currentTimeMillis())
                        .build(),
                HttpStatus.OK);
    }

    public static <T> Result<T> success(T data, String message) {
        return new Result<>(
                ResultBody.<T>builder()
                        .code(ResultCode.SUCCESS.getCode())
                        .message(message)
                        .data(data)
                        .timestamp(System.currentTimeMillis())
                        .build(),
                HttpStatus.OK);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(
                ResultBody.<T>builder()
                        .code(code)
                        .message(message)
                        .timestamp(System.currentTimeMillis())
                        .build(),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public static <T> Result<T> error(ResultCode code) {
        return new Result<>(
                ResultBody.<T>builder()
                        .code(code.getCode())
                        .message(code.getMessage())
                        .timestamp(System.currentTimeMillis())
                        .build(),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public static <T> Result<T> error(ResultCode code, String message) {
        return new Result<>(
                ResultBody.<T>builder()
                        .code(code.getCode())
                        .message(message)
                        .timestamp(System.currentTimeMillis())
                        .build(),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 返回文件响应
     * 
     * @param inputStream 文件输入流
     * @param contentType 文件类型
     * @param fileName    文件名
     * @param cacheMaxAge 缓存时间（秒）
     * @return 文件响应结果
     */
    public static Result<InputStreamResource> file(InputStreamResource inputStream, String contentType, String fileName,
            long cacheMaxAge) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDispositionFormData("inline", "\"" + fileName + "\"");
        headers.setCacheControl("max-age=" + cacheMaxAge);

        return new Result<>(
                ResultBody.<InputStreamResource>builder()
                        .code(ResultCode.SUCCESS.getCode())
                        .message(ResultCode.SUCCESS.getMessage())
                        .data(inputStream)
                        .timestamp(System.currentTimeMillis())
                        .build(),
                headers,
                HttpStatus.OK);
    }

    /**
     * 返回文件响应（默认缓存一年）
     * 
     * @param inputStream 文件输入流
     * @param contentType 文件类型
     * @param fileName    文件名
     * @return 文件响应结果
     */
    public static Result<InputStreamResource> file(InputStreamResource inputStream, String contentType,
            String fileName) {
        return file(inputStream, contentType, fileName, 31536000L);
    }
}