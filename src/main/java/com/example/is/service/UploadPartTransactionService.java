package com.example.is.service;

import com.example.is.entity.UploadFileEntity;
import com.example.is.entity.UploadFileStatus;
import com.example.is.entity.UploadPartEntity;
import com.example.is.exception.UnathorizedExcpetion;
import com.example.is.exception.UploadingExcpetion;
import com.example.is.repository.UploadFileRepository;
import com.example.is.repository.UploadPartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import software.amazon.awssdk.services.s3.model.CompletedPart;

@Service
public class UploadPartTransactionService {
    public record CompletionPlan(String objectKey, String s3UploadId, List<CompletedPart> parts) {}
    public record CleanupPlan(String objectKey, String s3UploadId) {}
    private final UploadFileRepository files;
    private final UploadPartRepository parts;

    public UploadPartTransactionService(UploadFileRepository files, UploadPartRepository parts) {
        this.files = files;
        this.parts = parts;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long reserve(int uploadId, String user, int number, long size) {
        UploadFileEntity file = files.findForUpdate(uploadId)
                .orElseThrow(() -> new UploadingExcpetion("Загрузка не найдена"));
        if (!file.getOwner().equals(user)) {
            throw new UnathorizedExcpetion("Файл не принадлежит пользователю");
        }
        if (file.getStatus() != UploadFileStatus.UPLOADING || file.getS3UploadId() == null) {
            throw new UploadingExcpetion("Multipart-загрузка недоступна");
        }
        if (parts.existsByUpload_IdAndEtagIsNull(uploadId)) {
            throw new UploadingExcpetion("Предыдущая часть ещё не завершена");
        }
        UploadPartEntity part = new UploadPartEntity();
        part.setUpload(file);
        part.setPartNumber(number);
        part.setSize(size);
        return parts.saveAndFlush(part).getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void confirm(int uploadId, Long partId, String etag, Instant expiresAt) {
        UploadFileEntity file = files.findForUpdate(uploadId)
                .orElseThrow(() -> new UploadingExcpetion("Загрузка не найдена"));
        if (file.getStatus() != UploadFileStatus.UPLOADING) {
            throw new UploadingExcpetion("Загрузка больше не активна");
        }
        UploadPartEntity part = parts.findById(partId)
                .orElseThrow(() -> new UploadingExcpetion("Часть не найдена"));
        part.setEtag(etag);
        file.setExpired(expiresAt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markForDeletion(int uploadId) {
        UploadFileEntity file = files.findForUpdate(uploadId)
                .orElseThrow(() -> new UploadingExcpetion("Загрузка не найдена"));
        if (file.getStatus() != UploadFileStatus.UPLOADING) {
            throw new UploadingExcpetion("Загрузка больше не активна");
        }
        file.setStatus(UploadFileStatus.TO_DELETE);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompletionPlan prepareCompletion(int uploadId, String user) {
        UploadFileEntity file = files.findForUpdate(uploadId).orElseThrow(() -> new UploadingExcpetion("Загрузка не найдена"));

        if (!file.getOwner().equals(user)) {
            throw new UnathorizedExcpetion("Файл не принадлежит пользователю");
        }

        if (file.getStatus() == UploadFileStatus.UPLOADED) {
            return null;
        }

        if (file.getStatus() != UploadFileStatus.UPLOADING) {
            throw new UploadingExcpetion("Загрузка недоступна для завершения: " + file.getStatus());
        }

        if (file.getS3UploadId() == null) {
            if (file.getChunkSize() != null || file.getExpectedSize() == null
                    || file.getSize() != file.getExpectedSize() || file.getSize() <= 0) {
                throw new UploadingExcpetion("Нет загруженного файла для завершения");
            }
            file.setStatus(UploadFileStatus.UPLOADED);
            file.setExpired(null);
            return null;
        }

        Long expected = file.getExpectedSize();
        Long chunkSize = file.getChunkSize();

        if (expected == null || expected <= 0 || chunkSize == null || chunkSize <= 0) {
            throw new UploadingExcpetion("Не задан план загрузки частей");
        }

        long count = (expected - 1) / chunkSize + 1;

        if (count > 10000) {
            throw new UploadingExcpetion("Слишком много частей");
        }

        List<UploadPartEntity> uploaded = parts.findByUpload_IdOrderByPartNumberAsc(uploadId);

        if (uploaded.size() != count) {
            throw new UploadingExcpetion("Загружены не все части");
        }

        for (int i = 0; i < uploaded.size(); i++) {
            UploadPartEntity part = uploaded.get(i);
            long expectedPartSize = i + 1 < count ? chunkSize : expected - (count - 1) * chunkSize;
            if (part.getPartNumber() != i + 1 || part.getEtag() == null || part.getEtag().isBlank()
                    || part.getSize() != expectedPartSize) {
                throw new UploadingExcpetion("Части отсутствуют, не подтверждены или имеют неверный размер");
            }
        }

        List<CompletedPart> completed = uploaded.stream()
                .map(part -> CompletedPart.builder().partNumber((int) part.getPartNumber()).eTag(part.getEtag()).build())
                .toList();
        file.setStatus(UploadFileStatus.COMPLETING);
        return new CompletionPlan(file.getObjectKey(), file.getS3UploadId(), completed);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finishCompletion(int uploadId) {
        UploadFileEntity file = files.findForUpdate(uploadId).orElseThrow(() -> new UploadingExcpetion("Загрузка не найдена"));
        if (file.getStatus() != UploadFileStatus.COMPLETING) {
            throw new UploadingExcpetion("Загрузка не находится в состоянии завершения");
        }
        file.setSize(file.getExpectedSize());
        file.setStatus(UploadFileStatus.UPLOADED);
        file.setExpired(null);
        parts.deleteByUpload_Id(uploadId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CleanupPlan claimExpired(int uploadId, Instant now) {
        var file = files.findForUpdate(uploadId).orElse(null);
        if (file == null || file.getExpired() == null || file.getExpired().isAfter(now)
                || (file.getStatus() != UploadFileStatus.UPLOADING && file.getStatus() != UploadFileStatus.TO_DELETE)) {
            return null;
        }
        file.setStatus(UploadFileStatus.TO_DELETE);
        return new CleanupPlan(file.getObjectKey(), file.getS3UploadId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteCleanedUpload(int uploadId) {
        var file = files.findForUpdate(uploadId).orElse(null);
        if (file != null && file.getStatus() == UploadFileStatus.TO_DELETE) {
            parts.deleteByUpload_Id(uploadId);
            files.delete(file);
        }
    }
}
