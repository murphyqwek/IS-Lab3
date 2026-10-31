package com.example.is.repository;

import com.example.is.entity.UploadPartEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UploadPartRepository extends JpaRepository<UploadPartEntity, Long> {
    boolean existsByUpload_IdAndEtagIsNull(int uploadId);
    List<UploadPartEntity> findByUpload_IdOrderByPartNumberAsc(int uploadId);
    void deleteByUpload_Id(int uploadId);
}
