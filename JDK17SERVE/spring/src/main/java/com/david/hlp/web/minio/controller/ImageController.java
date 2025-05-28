package com.david.hlp.web.minio.controller;

import com.david.hlp.web.common.enums.ResultCode;
import com.david.hlp.web.common.result.Result;
import com.david.hlp.web.common.controller.BaseController;
import com.david.hlp.web.minio.model.ImageResponse;
import com.david.hlp.web.minio.service.MinioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

import lombok.RequiredArgsConstructor;

/**
 * 图片处理控制器
 * 
 * 图片访问说明：
 * 1. 上传图片后返回的URL格式为：/api/image/view/{fileName}
 * 2. 可以直接使用此相对路径访问图片，如：<img src="/api/image/view/my-image.jpg">
 * 3. 图片会通过viewImage接口提供，设置了正确的Content-Type
 * 4. URL简短且易于使用，避免了长预签名URL的问题
 */
@Slf4j
@RestController
@RequestMapping("/api/image")
@RequiredArgsConstructor
public class ImageController extends BaseController {

    private final MinioService minioService;

    /**
     * 上传图片
     *
     * @param file 图片文件
     * @return 上传结果
     */
    @PostMapping("/upload")
    public Result<ImageResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            // 检查文件是否为空
            if (file.isEmpty()) {
                return createErrorResult("文件为空");
            }

            // 检查文件是否为图片
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return createErrorResult("只允许上传图片文件");
            }

            // 获取原始文件名
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return createErrorResult("文件名不能为空");
            }
            // 构建新的文件名：用户ID_原始文件名
            String newFileName = getCurrentUserId() + "_" + originalFilename;

            // 上传文件到MinIO
            String fileName = minioService.uploadFile(file, newFileName);
            // 获取文件访问URL - 使用简短URL
            String fileUrl = minioService.getSimpleFileUrl(fileName);

            // 构建成功响应
            ImageResponse response = ImageResponse.builder()
                    .success(true)
                    .fileName(fileName)
                    .url(fileUrl)
                    .message("上传成功")
                    .build();

            return Result.<ImageResponse>builder()
                    .code(ResultCode.SUCCESS.getCode())
                    .message(ResultCode.SUCCESS.getMessage())
                    .data(response)
                    .build();
        } catch (Exception e) {
            log.error("上传图片失败: {}", e.getMessage());
            return createErrorResult("上传图片失败: " + e.getMessage());
        }
    }

    /**
     * 获取图片
     *
     * @param fileName 文件名
     * @return 图片资源
     */
    @GetMapping("/view/{fileName}")
    public ResponseEntity<InputStreamResource> viewImage(@PathVariable("fileName") String fileName) {
        try {
            InputStream inputStream = minioService.getObject(fileName);

            // 设置Content-Type
            String contentType = getContentTypeByFileName(fileName);

            // 构建HTTP响应，添加缓存控制
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=31536000") // 缓存一年
                    .body(new InputStreamResource(inputStream));
        } catch (Exception e) {
            log.error("获取图片失败 - 文件名: {}, 错误: {}", fileName, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 删除图片
     *
     * @param fileName 文件名
     * @return 删除结果
     */
    @DeleteMapping("/delete")
    public Result<ImageResponse> deleteImage(@RequestParam("fileName") String fileName) {
        try {
            minioService.deleteFile(fileName);

            ImageResponse response = ImageResponse.builder()
                    .success(true)
                    .message("删除成功")
                    .fileName(fileName)
                    .build();

            return Result.<ImageResponse>builder()
                    .code(ResultCode.SUCCESS.getCode())
                    .message(ResultCode.SUCCESS.getMessage())
                    .data(response)
                    .build();
        } catch (Exception e) {
            log.error("删除图片失败 - 文件名: {}, 错误: {}", fileName, e.getMessage());
            return createErrorResult("删除图片失败: " + e.getMessage());
        }
    }

    /**
     * 获取图片URL
     *
     * @param fileName 文件名
     * @return 图片URL
     */
    @GetMapping("/url/{fileName}")
    public Result<ImageResponse> getImageUrl(@PathVariable("fileName") String fileName) {
        try {
            // 使用简短URL而不是预签名URL
            String url = minioService.getFileUrl(fileName);

            ImageResponse response = ImageResponse.builder()
                    .success(true)
                    .url(url)
                    .fileName(fileName)
                    .build();

            return Result.<ImageResponse>builder()
                    .code(ResultCode.SUCCESS.getCode())
                    .message(ResultCode.SUCCESS.getMessage())
                    .data(response)
                    .build();
        } catch (Exception e) {
            log.error("获取图片URL失败 - 文件名: {}, 错误: {}", fileName, e.getMessage());
            return createErrorResult("获取图片URL失败: " + e.getMessage());
        }
    }

    /**
     * 创建错误响应
     *
     * @param errorMessage 错误信息
     * @return 错误响应
     */
    private Result<ImageResponse> createErrorResult(String errorMessage) {
        ImageResponse response = ImageResponse.builder()
                .success(false)
                .message(errorMessage)
                .build();

        return Result.<ImageResponse>builder()
                .code(ResultCode.ERROR.getCode())
                .message(ResultCode.ERROR.getMessage())
                .data(response)
                .build();
    }

    /**
     * 根据文件名判断ContentType
     *
     * @param fileName 文件名
     * @return 对应的ContentType
     */
    private String getContentTypeByFileName(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "webp":
                return "image/webp";
            case "svg":
                return "image/svg+xml";
            default:
                return "application/octet-stream";
        }
    }
}
