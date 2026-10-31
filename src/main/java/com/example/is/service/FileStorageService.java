package com.example.is.service;

import java.io.InputStream;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.CompletedPart;

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

    public String startMultipartUpload(String objectKey) {
        var response = storageClient.createMultipartUpload(request -> request
                .bucket(bucketName)
                .key(objectKey)
                .contentType("application/json")
        );

        return response.uploadId();
    }

    public String uploadPart(String objectKey, String s3UploadId, int partNumber, InputStream stream, long size) {
        return storageClient.uploadPart(request -> request.bucket(bucketName).key(objectKey)
                        .uploadId(s3UploadId).partNumber(partNumber),
                RequestBody.fromInputStream(stream, size)).eTag();
    }

    public void abortMultipartUpload(String objectKey, String s3UploadId) {
        storageClient.abortMultipartUpload(request -> request.bucket(bucketName).key(objectKey).uploadId(s3UploadId));
    }

    public void cleanupMultipartUpload(String objectKey, String s3UploadId) {
        try {
            abortMultipartUpload(objectKey, s3UploadId);
        } catch (S3Exception e) {
            if (e.awsErrorDetails() == null || !"NoSuchUpload".equals(e.awsErrorDetails().errorCode())) {
                throw e;
            }
            // An already-completed upload also has no upload ID. Never discard its DB record.
            try {
                storageClient.headObject(request -> request.bucket(bucketName).key(objectKey));
            } catch (S3Exception headError) {
                if (headError.statusCode() == 404) {
                    return;
                }
                throw headError;
            }
            throw new IllegalStateException("Multipart upload is absent, but its object exists: " + objectKey, e);
        }
    }

    public void completeMultipartUpload(String objectKey, String s3UploadId, List<CompletedPart> parts) {
        storageClient.completeMultipartUpload(request -> request.bucket(bucketName).key(objectKey)
                .uploadId(s3UploadId).multipartUpload(upload -> upload.parts(parts)));
    }

    public void uploadObject(String objectKey, InputStream inputStream, long size) {
        storageClient.putObject(
                request -> request.bucket(bucketName)
                        .key(objectKey)
                        .contentType("application/json"),
                RequestBody.fromInputStream(inputStream, size)
        );
    }

    public void deleteFile(String objectKey) {
        storageClient.deleteObject(request -> request.bucket(bucketName).key(objectKey));
    }
}
