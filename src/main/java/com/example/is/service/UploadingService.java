package com.example.is.service;

import com.example.is.entity.UploadFileEntity;
import com.example.is.entity.UploadFileStatus;
import com.example.is.dto.upload.OpenFileMetadataDTO;
import com.example.is.exception.FileTooBigException;
import com.example.is.exception.UnathorizedExcpetion;
import com.example.is.exception.UploadingExcpetion;
import com.example.is.repository.UploadFileRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class UploadingService {
    private static final Duration UPLOAD_TIMEOUT = Duration.ofMinutes(5);
    private static final long CHUNK_SIZE = 32L * 1024 * 1024;
    private static final long MAX_FILE_SIZE = 200L * 1024 * 1024 * 1024;
    private final FileStorageService fileStorageService;
    private final UploadFileRepository uploadFileRepository;

    private final UploadPartTransactionService partTransactions;

    public UploadingService(FileStorageService fileStorageService, UploadFileRepository uploadFileRepository, UploadPartTransactionService partTransactions) {
        this.fileStorageService = fileStorageService;
        this.uploadFileRepository = uploadFileRepository;
        this.partTransactions = partTransactions;
    }

    private String getObjectKey(String user) {
        return "/user/" + user + "/" + UUID.randomUUID().toString();
    }

    private UploadFileEntity getUploadFile(String user, int uploadId) {
        var uploadFile = uploadFileRepository.findById(uploadId).orElseThrow(() -> new UploadingExcpetion("Нет файла с id" +  uploadId));

        if(!uploadFile.getOwner().equals(user)) {
            throw new UnathorizedExcpetion("Файл не принадлежит пользователю");
        }

        return uploadFile;
    }


    public OpenFileMetadataDTO openFile(String filename, String user, long fileSize) {
        if (filename == null || filename.isBlank() || fileSize <= 0) {
            throw new IllegalArgumentException("Имя и положительный размер файла обязательны");
        }
        if (fileSize > MAX_FILE_SIZE) {
            throw new FileTooBigException();
        }

        boolean chunked = fileSize > CHUNK_SIZE;
        int chunkCount = chunked ? (int) ((fileSize - 1) / CHUNK_SIZE + 1) : 0;
        var objectKey = getObjectKey(user);

        UploadFileEntity uploadFileEntity = new UploadFileEntity();

        uploadFileEntity.setFileName(filename);
        uploadFileEntity.setOwner(user);
        uploadFileEntity.setStatus(UploadFileStatus.UPLOADING);
        uploadFileEntity.setExpired(Instant.now().plus(UPLOAD_TIMEOUT));
        uploadFileEntity.setObjectKey(objectKey);
        uploadFileEntity.setExpectedSize(fileSize);
        uploadFileEntity.setChunkSize(chunked ? CHUNK_SIZE : null);

        if (chunked) {
            var s3UploadId = fileStorageService.startMultipartUpload(objectKey);
            uploadFileEntity.setS3UploadId(s3UploadId);
        }
        UploadFileEntity saved;
        try {
            saved = uploadFileRepository.save(uploadFileEntity);
        } catch (RuntimeException e) {
            if (chunked) {
                try {
                    fileStorageService.abortMultipartUpload(objectKey, uploadFileEntity.getS3UploadId());
                } catch (Exception abortError) {
                    e.addSuppressed(abortError);
                }
            }
            throw e;
        }
        return new OpenFileMetadataDTO(saved.getId(), chunked, saved.getChunkSize(), chunkCount);
    }

    private void checkChunkId(UploadFileEntity uploadFile, long chunkId) {
        Long expectedSize = uploadFile.getExpectedSize();
        Long chunkSize = uploadFile.getChunkSize();

        if (expectedSize == null || expectedSize <= 0 || chunkSize == null || chunkSize <= 0) {
            throw new UploadingExcpetion("Не задан план загрузки частей");
        }

        long chunkCount = (expectedSize - 1) / chunkSize + 1;

        if (chunkId < 1 || chunkId > chunkCount) {
            throw new UploadingExcpetion("Недопустимый номер части");
        }
    }

    public void uploadObjectOrChunk(MultipartFile file, String user, int uploadId, boolean isChunk, int chunkId) {
        var uploadFile = getUploadFile(user, uploadId);

        if(uploadFile.getStatus() != UploadFileStatus.UPLOADING) {
            throw new UploadingExcpetion("Файл уже загружен");
        }

        if (file.isEmpty()) {
            throw new UploadingExcpetion("Файл или часть не должны быть пустыми");
        }

        if (isChunk) {
            checkChunkId(uploadFile, chunkId);

            if (uploadFile.getS3UploadId() == null) {
                throw new UploadingExcpetion("Multipart-загрузка не начата");
            }
            long count = (uploadFile.getExpectedSize() - 1) / uploadFile.getChunkSize() + 1;
            long expectedPartSize = chunkId < count ? uploadFile.getChunkSize() : uploadFile.getExpectedSize() - (count - 1) * uploadFile.getChunkSize();
            if (file.getSize() != expectedPartSize) {
                throw new UploadingExcpetion("Неверный размер части");
            }
            uploadChunk(uploadFile, file, chunkId, user);
            return;
        }

        else {
            if (uploadFile.getChunkSize() != null || uploadFile.getS3UploadId() != null) {
                throw new UploadingExcpetion("Для этой загрузки нужны части");
            }
            if (uploadFile.getSize() > 0) {
                throw new UploadingExcpetion("Файл уже загружен");
            }

            if (uploadFile.getExpectedSize() != null && uploadFile.getExpectedSize() != file.getSize()) {
                throw new UploadingExcpetion("Размер файла не совпадает с ожидаемым");
            }
        }

        try {
            try (InputStream inputStream = file.getInputStream()) {
                fileStorageService.uploadObject(uploadFile.getObjectKey(), inputStream, file.getSize());
            }

            uploadFile.setSize(file.getSize());
        } catch (Exception e) {
            try {
                uploadFile.setStatus(UploadFileStatus.TO_DELETE);
                uploadFileRepository.save(uploadFile);
            } catch (Exception markError) {
                e.addSuppressed(markError);
            }
            try {
                fileStorageService.deleteFile(uploadFile.getObjectKey());
                uploadFileRepository.delete(uploadFile);
            } catch (Exception cleanupError) {
                e.addSuppressed(cleanupError);
            }
            throw new UploadingExcpetion("Не удалось сохранить файл", e);
        }
        uploadFile.setExpired(Instant.now().plus(UPLOAD_TIMEOUT));
        uploadFileRepository.save(uploadFile);
    }

    private void uploadChunk(UploadFileEntity uploadFile, MultipartFile file, int chunkId, String user) {
        Long partId = partTransactions.reserve(uploadFile.getId(), user, chunkId, file.getSize());
        String etag;
        try (InputStream stream = file.getInputStream()) {
            etag = fileStorageService.uploadPart(uploadFile.getObjectKey(), uploadFile.getS3UploadId(), chunkId, stream, file.getSize());
            if (etag == null || etag.isBlank()) {
                throw new UploadingExcpetion("Хранилище не вернуло ETag");
            }
        } catch (Exception e) {
            try {
                partTransactions.markForDeletion(uploadFile.getId());
                fileStorageService.abortMultipartUpload(uploadFile.getObjectKey(), uploadFile.getS3UploadId());
            } catch (Exception cleanupError) {
                e.addSuppressed(cleanupError);
            }
            throw new UploadingExcpetion("Не удалось загрузить часть", e);
        }

        partTransactions.confirm(uploadFile.getId(), partId, etag, Instant.now().plus(UPLOAD_TIMEOUT));
    }

    public void finishUpload(String user, int uploadId) {
        var plan = partTransactions.prepareCompletion(uploadId, user);
        if (plan == null) {
            return;
        }
        fileStorageService.completeMultipartUpload(plan.objectKey(), plan.s3UploadId(), plan.parts());
        partTransactions.finishCompletion(uploadId);
    }
}
