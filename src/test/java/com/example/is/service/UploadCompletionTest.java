package com.example.is.service;

import com.example.is.entity.UploadFileEntity;
import com.example.is.entity.UploadFileStatus;
import com.example.is.entity.UploadPartEntity;
import com.example.is.exception.UploadingExcpetion;
import com.example.is.repository.UploadFileRepository;
import com.example.is.repository.UploadPartRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UploadCompletionTest {
    private final UploadFileRepository files = mock(UploadFileRepository.class);
    private final UploadPartRepository parts = mock(UploadPartRepository.class);
    private final UploadPartTransactionService service = new UploadPartTransactionService(files, parts);

    private UploadFileEntity file() {
        var file = new UploadFileEntity();
        file.setOwner("alice");
        file.setStatus(UploadFileStatus.UPLOADING);
        file.setExpectedSize(11L);
        file.setChunkSize(10L);
        file.setObjectKey("key");
        file.setS3UploadId("s3-id");
        when(files.findForUpdate(1)).thenReturn(Optional.of(file));
        return file;
    }

    private UploadPartEntity part(long number, long size, String etag) {
        var part = new UploadPartEntity();
        part.setPartNumber(number);
        part.setSize(size);
        part.setEtag(etag);
        return part;
    }

    @Test
    void validatesPartsBeforeEnteringCompleting() {
        var file = file();
        when(parts.findByUpload_IdOrderByPartNumberAsc(1)).thenReturn(List.of(part(1, 10, "a"), part(2, 1, "b")));
        var plan = service.prepareCompletion(1, "alice");
        assertEquals(UploadFileStatus.COMPLETING, file.getStatus());
        assertEquals(List.of(1, 2), plan.parts().stream().map(p -> p.partNumber()).toList());
        assertEquals(List.of("a", "b"), plan.parts().stream().map(p -> p.eTag()).toList());
    }

    @Test
    void rejectsUnconfirmedPartWithoutChangingState() {
        var file = file();
        when(parts.findByUpload_IdOrderByPartNumberAsc(1)).thenReturn(List.of(part(1, 10, "a"), part(2, 1, null)));
        assertThrows(UploadingExcpetion.class, () -> service.prepareCompletion(1, "alice"));
        assertEquals(UploadFileStatus.UPLOADING, file.getStatus());
    }

    @Test
    void finishesAndRemovesPartsAfterStorageSuccess() {
        var file = file();
        file.setStatus(UploadFileStatus.COMPLETING);
        service.finishCompletion(1);
        assertEquals(UploadFileStatus.UPLOADED, file.getStatus());
        assertEquals(11, file.getSize());
        assertNull(file.getExpired());
        verify(parts).deleteByUpload_Id(1);
    }
}
