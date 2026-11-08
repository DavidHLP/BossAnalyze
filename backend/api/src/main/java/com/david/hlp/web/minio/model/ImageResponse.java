package com.david.hlp.web.minio.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图片操作通用响应类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageResponse {
    private boolean success;
    private String message;
    private String fileName;
    private String url;
    // 可以根据需要添加其他字段
}