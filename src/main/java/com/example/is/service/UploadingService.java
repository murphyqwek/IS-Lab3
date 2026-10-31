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

    public UploadingService(FileStorageService fileStorageService, UploadFileRepository uploadFileRepository) {
        this.fileStorageService = fileStorageService;
        this.uploadFileRepository = uploadFileRepository;
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
        UploadFileEntity uploadFileEntity = new UploadFileEntity();

        uploadFileEntity.setFileName(filename);
        uploadFileEntity.setOwner(user);
        uploadFileEntity.setStatus(chunked ? UploadFileStatus.CREATING : UploadFileStatus.UPLOADING);
        uploadFileEntity.setExpired(Instant.now().plus(UPLOAD_TIMEOUT));
        uploadFileEntity.setObjectKey(getObjectKey(user));
        uploadFileEntity.setExpectedSize(fileSize);
        uploadFileEntity.setChunkSize(chunked ? CHUNK_SIZE : null);

        UploadFileEntity saved = uploadFileRepository.save(uploadFileEntity);
        return new OpenFileMetadataDTO(saved.getId(), chunked, saved.getChunkSize(), chunkCount);
    }

    public void uploadObjectOrChunk(MultipartFile file, String user, int uploadId, boolean isChunk) {
        var uploadFile = getUploadFile(user, uploadId);

        if(uploadFile.getStatus() != UploadFileStatus.UPLOADING) {
            throw new UploadingExcpetion("Файл уже загружен");
        }

        if (file.isEmpty()) {
            throw new UploadingExcpetion("Файл или часть не должны быть пустыми");
        }

        if (isChunk) {
            throw new UnsupportedOperationException("Загрузка частей пока не реализована");
        }
        if (uploadFile.getS3UploadId() != null) {
            throw new UploadingExcpetion("Для этой загрузки уже начат multipart upload");
        }
        if (uploadFile.getSize() > 0) {
            throw new UploadingExcpetion("Файл уже загружен");
        }
        if (uploadFile.getExpectedSize() != null && uploadFile.getExpectedSize() != file.getSize()) {
            throw new UploadingExcpetion("Размер файла не совпадает с ожидаемым");
        }

        try {
            uploadObject(uploadFile, file);
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

    private void uploadObject(UploadFileEntity uploadFile, MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            fileStorageService.uploadObject(uploadFile.getObjectKey(), inputStream, file.getSize());
        }
        uploadFile.setSize(file.getSize());
    }

    public void finishUpload(String user, int uploadId) {
        var uploadFile = getUploadFile(user, uploadId);

        if (uploadFile.getStatus() != UploadFileStatus.UPLOADING || uploadFile.getSize() == 0) {
            throw new UploadingExcpetion("Нет загруженного файла для завершения");
        }
        uploadFile.setStatus(UploadFileStatus.UPLOADED);
        uploadFile.setExpired(null);
        uploadFileRepository.save(uploadFile);
    }
}
