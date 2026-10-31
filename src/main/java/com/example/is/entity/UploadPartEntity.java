package com.example.is.entity;

import jakarta.persistence.*;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_upload_part_number",
        columnNames = {"upload_id", "part_number"}
))
public class UploadPartEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "upload_id", nullable = false)
    private UploadFileEntity upload;

    @Column(name = "part_number", nullable = false)
    private int partNumber;

    // Null while the part is reserved but has not been confirmed by storage.
    @Column(nullable = true)
    private String etag;

    @Column(nullable = false)
    private long size;

    public Long getId() {
        return id;
    }

    public UploadFileEntity getUpload() {
        return upload;
    }

    public void setUpload(UploadFileEntity upload) {
        this.upload = upload;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(int partNumber) {
        this.partNumber = partNumber;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
