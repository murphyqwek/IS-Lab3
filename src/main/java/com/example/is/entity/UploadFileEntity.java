package com.example.is.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
public class UploadFileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String objectKey;

    @Column(nullable = false)
    private long size;

    private Long expectedSize;

    private Long chunkSize;

    private String s3UploadId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UploadFileStatus status;

    private Instant expired;

    @OneToMany(mappedBy = "upload", cascade = CascadeType.REMOVE)
    private List<UploadPartEntity> parts = new ArrayList<>();

    public Long getExpectedSize() {
        return expectedSize;
    }

    public void setExpectedSize(Long expectedSize) {
        this.expectedSize = expectedSize;
    }

    public Long getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Long chunkSize) {
        this.chunkSize = chunkSize;
    }

    public String getS3UploadId() {
        return s3UploadId;
    }

    public void setS3UploadId(String s3UploadId) {
        this.s3UploadId = s3UploadId;
    }

    public Instant getExpired() {
        return expired;
    }

    public void setExpired(Instant expired) {
        this.expired = expired;
    }

    public UploadFileStatus getStatus() {
        return status;
    }

    public void setStatus(UploadFileStatus status) {
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String filePath) {
        this.objectKey = filePath;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
