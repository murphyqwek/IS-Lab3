package com.example.is.service;

import com.example.is.entity.UploadFileStatus;
import com.example.is.repository.UploadFileRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExpiredUploadCleanupWorker {
    private static final Logger log = LoggerFactory.getLogger(ExpiredUploadCleanupWorker.class);
    private final UploadFileRepository files;
    private final UploadPartTransactionService transactions;
    private final FileStorageService storage;

    public ExpiredUploadCleanupWorker(UploadFileRepository files, UploadPartTransactionService transactions,
                                      FileStorageService storage) {
        this.files = files;
        this.transactions = transactions;
        this.storage = storage;
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void cleanupExpired() {
        var ids = files.findExpiredIds(Instant.now(), List.of(UploadFileStatus.UPLOADING, UploadFileStatus.TO_DELETE));
        for (int id : ids) {
            try {
                var plan = transactions.claimExpired(id, Instant.now());
                if (plan == null) {
                    continue;
                }
                if (plan.s3UploadId() == null) {
                    storage.deleteFile(plan.objectKey());
                } else {
                    storage.cleanupMultipartUpload(plan.objectKey(), plan.s3UploadId());
                }
                transactions.deleteCleanedUpload(id);
            } catch (Exception e) {
                log.warn("Could not clean up expired upload {}; will retry", id, e);
            }
        }
    }
}
