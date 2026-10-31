package com.example.is.entity;

import jakarta.persistence.*;

@Entity
public class UploadedFileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(nullable = false)
    private User owner;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String objectKey;

    @Column(nullable = false)
    private long size;

    public int getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
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
