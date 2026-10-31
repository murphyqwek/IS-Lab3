package com.example.is.service;

import java.io.InputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class FileStorageService {
    private final S3Client storageClient;
    private final String bucketName;

    public FileStorageService(S3Client storageClient, @Value("${minio.bucket}") String bucketName) {
        this.storageClient = storageClient;
        this.bucketName = bucketName;

        try {
            storageClient.headBucket(request -> request.bucket(bucketName));
        } catch (S3Exception e) {
            if (e.statusCode() != 404) {
                throw e;
            }
            storageClient.createBucket(request -> request.bucket(bucketName));
        }
    }

    public void uploadObject(String objectKey, InputStream inputStream, long size) {
        storageClient.putObject(
                request -> request.bucket(bucketName)
                        .key(objectKey)
                        .contentType("application/octet-stream"),
                RequestBody.fromInputStream(inputStream, size)
        );
    }

    public void deleteFile(String objectKey) {
        storageClient.deleteObject(request -> request.bucket(bucketName).key(objectKey));
    }
}
