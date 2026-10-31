package com.example.is.service;

import com.example.is.entity.UploadFileEntity;
import com.example.is.entity.UploadFileStatus;
import com.example.is.exception.FileTooBigException;
import com.example.is.repository.UploadFileRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UploadingServiceTest {
    private final UploadFileRepository repository = mock(UploadFileRepository.class);
    private final UploadingService service = new UploadingService(mock(FileStorageService.class), repository);
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
        var result = service.openFile("test.json", "alice", 32 * MIB + 1);
        assertTrue(result.chunked());
        assertEquals(32 * MIB, result.chunkSize());
        assertEquals(2, result.chunkCount());
        verify(repository).save(argThat(file -> file.getStatus() == UploadFileStatus.CREATING));
    }

    @Test
    void rejectsOversizedFilesBeforeSaving() {
        assertThrows(FileTooBigException.class,
                () -> service.openFile("test.json", "alice", 200 * 1024 * MIB + 1));
        verifyNoInteractions(repository);
    }
}
