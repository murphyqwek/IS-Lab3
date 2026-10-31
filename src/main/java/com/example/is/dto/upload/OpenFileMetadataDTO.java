package com.example.is.dto.upload;

public record OpenFileMetadataDTO(
        int uploadId,
        boolean chunked,
        Long chunkSize,
        int chunkCount
) {
}
