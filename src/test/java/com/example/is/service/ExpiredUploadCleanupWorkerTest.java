package com.example.is.service;

import com.example.is.repository.UploadFileRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ExpiredUploadCleanupWorkerTest {
    private final UploadFileRepository files = mock(UploadFileRepository.class);
    private final UploadPartTransactionService transactions = mock(UploadPartTransactionService.class);
    private final FileStorageService storage = mock(FileStorageService.class);
    private final ExpiredUploadCleanupWorker worker = new ExpiredUploadCleanupWorker(files, transactions, storage);

    @Test
    void abortsMultipartBeforeDeletingRecords() {
        when(files.findExpiredIds(any(), anyList())).thenReturn(List.of(1));
        when(transactions.claimExpired(eq(1), any())).thenReturn(new UploadPartTransactionService.CleanupPlan("key", "upload"));
        worker.cleanupExpired();
        var order = inOrder(storage, transactions);
        order.verify(storage).cleanupMultipartUpload("key", "upload");
        order.verify(transactions).deleteCleanedUpload(1);
    }

    @Test
    void leavesDatabaseRecordsWhenStorageFails() {
        when(files.findExpiredIds(any(), anyList())).thenReturn(List.of(1));
        when(transactions.claimExpired(eq(1), any())).thenReturn(new UploadPartTransactionService.CleanupPlan("key", "upload"));
        doThrow(new IllegalStateException("unavailable")).when(storage).cleanupMultipartUpload("key", "upload");
        worker.cleanupExpired();
        verify(transactions, never()).deleteCleanedUpload(anyInt());
    }

    @Test
    void removesWholeObjectBeforeDatabaseRecords() {
        when(files.findExpiredIds(any(), anyList())).thenReturn(List.of(2));
        when(transactions.claimExpired(eq(2), any())).thenReturn(new UploadPartTransactionService.CleanupPlan("key", null));
        worker.cleanupExpired();
        var order = inOrder(storage, transactions);
        order.verify(storage).deleteFile("key");
        order.verify(transactions).deleteCleanedUpload(2);
    }
}
