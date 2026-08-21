package com.ledger.service.minio;

import com.ledger.config.minio.MinioProperties;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public String generateUploadPresignedUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(minioProperties.getBucketName())
                            .object(objectKey)
                            .expiry(minioProperties.getExpirySeconds(), TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            log.error("生成MinIO上传预签名URL失败: objectKey={}", objectKey, e);
            throw new RuntimeException("生成上传签名URL失败", e);
        }
    }

    public String getPublicUrl(String objectKey) {
        String endpoint = minioProperties.getEndpoint();
        if (!endpoint.endsWith("/")) {
            endpoint = endpoint + "/";
        }
        return endpoint + minioProperties.getBucketName() + "/" + objectKey;
    }

    public void deleteObject(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectKey)
                            .build()
            );
            log.info("MinIO删除对象成功: objectKey={}", objectKey);
        } catch (Exception e) {
            log.warn("MinIO删除对象失败: objectKey={}", objectKey, e);
        }
    }

    public void uploadObject(String objectKey, InputStream inputStream, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectKey)
                            .stream(inputStream, -1, 10485760)
                            .contentType(contentType != null ? contentType : "application/octet-stream")
                            .build()
            );
            log.info("MinIO上传对象成功: objectKey={}", objectKey);
        } catch (Exception e) {
            log.error("MinIO上传对象失败: objectKey={}", objectKey, e);
            throw new RuntimeException("上传文件到MinIO失败", e);
        }
    }
}
