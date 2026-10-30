package com.example.is.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.time.Instant;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ImportHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private Integer importedObjectsCount;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportHistoryStatus status;

    @PrePersist
    private void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getImportedObjectsCount() {
        return importedObjectsCount;
    }

    public void setImportedObjectsCount(Integer importedObjectsCount) {
        this.importedObjectsCount = importedObjectsCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setStatus(ImportHistoryStatus status) {
        this.status = status;
    }

    public ImportHistoryStatus getStatus() {
        return status;
    }
}