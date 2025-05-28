package com.david.hlp.web.minio.service;

import com.david.hlp.web.minio.config.MinioConfig;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private static final int DEFAULT_URL_EXPIRY_DAYS = 7;

    private final MinioClient minioClient;

    private final MinioConfig minioConfig;

    /**
     * 初始化存储桶
     * 
     * @param bucketName 存储桶名称
     */
    public void createBucket(String bucketName) {
        try {
            if (!StringUtils.hasText(bucketName)) {
                throw new IllegalArgumentException("存储桶名称不能为空");
            }

            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());

            if (!bucketExists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception e) {
            log.error("创建存储桶 [{}] 失败: {}", bucketName, e.getMessage(), e);
            throw new RuntimeException("创建存储桶失败", e);
        }
    }

    /**
     * 上传文件（使用自定义文件名）
     *
     * @param file     要上传的文件
     * @param fileName 自定义文件名
     * @return 文件名
     */
    public String uploadFile(MultipartFile file, String fileName) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if (!StringUtils.hasText(fileName)) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        try {
            // 检查默认存储桶是否存在
            String bucketName = minioConfig.getBucketName();
            createBucket(bucketName);

            // 上传文件到MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .contentType(file.getContentType())
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .build());

            return fileName;
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    /**
     * 获取文件访问URL(完整的预签名URL)
     * 
     * @param fileName 文件名
     * @return 文件访问URL
     */
    public String getFileUrl(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        try {
            String presignedUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileName)
                            .method(Method.GET)
                            .expiry(DEFAULT_URL_EXPIRY_DAYS, TimeUnit.DAYS)
                            .build());

            return presignedUrl;
        } catch (Exception e) {
            log.error("获取文件 [{}] URL失败: {}", fileName, e.getMessage(), e);
            throw new RuntimeException("获取文件URL失败", e);
        }
    }

    /**
     * 获取简短的文件URL (相对路径，不是预签名URL)
     *
     * @param fileName 文件名
     * @return 简短的文件URL
     */
    public String getSimpleFileUrl(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        return "/api/image/view/" + fileName;
    }

    /**
     * 获取文件
     * 
     * @param fileName 文件名
     * @return 文件输入流
     */
    public InputStream getObject(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        try {
            InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileName)
                            .build());

            return inputStream;
        } catch (Exception e) {
            log.error("获取文件 [{}] 失败: {}", fileName, e.getMessage(), e);
            throw new RuntimeException("获取文件失败", e);
        }
    }

    /**
     * 删除文件
     * 
     * @param fileName 文件名
     */
    public void deleteFile(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileName)
                            .build());

        } catch (Exception e) {
            log.error("删除文件 [{}] 失败: {}", fileName, e.getMessage(), e);
            throw new RuntimeException("删除文件失败", e);
        }
    }
}