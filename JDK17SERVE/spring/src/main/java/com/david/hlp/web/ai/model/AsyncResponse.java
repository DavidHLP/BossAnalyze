package com.david.hlp.web.ai.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AsyncResponse<T> {
    private String status; // "loading", "completed", "error"
    private String message;
    private T data;

    public static <T> AsyncResponse<T> loading(String message, String version) {
        return AsyncResponse.<T>builder()
                .status("loading")
                .message(message)
                .build();
    }

    public static <T> AsyncResponse<T> completed(T data, String version) {
        return AsyncResponse.<T>builder()
                .status("completed")
                .message("数据获取成功")
                .data(data)
                .build();
    }

    public static <T> AsyncResponse<T> error(String message, String version) {
        return AsyncResponse.<T>builder()
                .status("error")
                .message(message)
                .build();
    }
}