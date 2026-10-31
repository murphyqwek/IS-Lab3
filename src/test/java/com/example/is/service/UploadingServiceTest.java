package com.example.is.service;

import com.example.is.entity.UploadFileEntity;
import com.example.is.entity.UploadFileStatus;
import com.example.is.exception.FileTooBigException;
import com.example.is.repository.UploadFileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UploadingServiceTest {
    private final UploadFileRepository repository = mock(UploadFileRepository.class);
    private final FileStorageService storage = mock(FileStorageService.class);
    private final UploadPartTransactionService transactions = mock(UploadPartTransactionService.class);
    private final UploadingService service = new UploadingService(storage, repository, transactions);
    private static final long MIB = 1024L * 1024;

    @Test
    void plansWholeFileAtChunkLimit() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.openFile("test.json", "alice", 32 * MIB);
        assertFalse(result.chunked());
        assertNull(result.chunkSize());
        assertEquals(0, result.chunkCount());
        verify(repository).save(argThat(file -> file.getStatus() == UploadFileStatus.UPLOADING
                && file.getExpectedSize() == 32 * MIB));
    }

    @Test
    void plansChunksIncludingSmallFinalPart() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(storage.startMultipartUpload(any())).thenReturn("s3-id");
        var result = service.openFile("test.json", "alice", 32 * MIB + 1);
        assertTrue(result.chunked());
        assertEquals(32 * MIB, result.chunkSize());
        assertEquals(2, result.chunkCount());
        var order = inOrder(storage, repository);
        order.verify(storage).startMultipartUpload(any());
        order.verify(repository).save(any());
    }

    @Test
    void abortsMultipartWhenSavingUploadFails() {
        when(storage.startMultipartUpload(any())).thenReturn("s3-id");
        when(repository.save(any())).thenThrow(new IllegalStateException("db unavailable"));

        assertThrows(IllegalStateException.class,
                () -> service.openFile("test.json", "alice", 32 * MIB + 1));

        verify(storage).abortMultipartUpload(any(), eq("s3-id"));
    }

    @Test
    void doesNotSaveWhenStartingMultipartFails() {
        when(storage.startMultipartUpload(any())).thenThrow(new IllegalStateException("storage unavailable"));

        assertThrows(IllegalStateException.class,
                () -> service.openFile("test.json", "alice", 32 * MIB + 1));

        verifyNoInteractions(repository);
    }

    @Test
    void rejectsOversizedFilesBeforeSaving() {
        assertThrows(FileTooBigException.class,
                () -> service.openFile("test.json", "alice", 200 * 1024 * MIB + 1));
        verifyNoInteractions(repository);
    }

    @Test
    void reservesUploadsAndConfirmsLastPart() throws Exception {
        UploadFileEntity file = new UploadFileEntity();
        file.setOwner("alice");
        file.setStatus(UploadFileStatus.UPLOADING);
        file.setExpectedSize(32 * MIB + 1);
        file.setChunkSize(32 * MIB);
        file.setS3UploadId("s3-id");
        file.setObjectKey("key");
        when(repository.findById(0)).thenReturn(Optional.of(file));
        MultipartFile chunk = mock(MultipartFile.class);
        when(chunk.getSize()).thenReturn(1L);
        when(chunk.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1}));
        when(transactions.reserve(0, "alice", 2, 1)).thenReturn(5L);
        when(storage.uploadPart(eq("key"), eq("s3-id"), eq(2), any(), eq(1L))).thenReturn("etag");

        service.uploadObjectOrChunk(chunk, "alice", 0, true, 2);

        var order = inOrder(transactions, storage);
        order.verify(transactions).reserve(0, "alice", 2, 1);
        order.verify(storage).uploadPart(eq("key"), eq("s3-id"), eq(2), any(), eq(1L));
        order.verify(transactions).confirm(eq(0), eq(5L), eq("etag"), any());
    }

    @Test
    void doesNotFinalizeDatabaseWhenStorageCompletionFails() {
        var plan = new UploadPartTransactionService.CompletionPlan("key", "s3-id", java.util.List.of());
        when(transactions.prepareCompletion(7, "alice")).thenReturn(plan);
        doThrow(new IllegalStateException("network failure"))
                .when(storage).completeMultipartUpload("key", "s3-id", plan.parts());

        assertThrows(IllegalStateException.class, () -> service.finishUpload("alice", 7));
        verify(transactions, never()).finishCompletion(7);
    }
}
