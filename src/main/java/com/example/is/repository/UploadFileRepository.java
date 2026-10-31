package com.example.is.repository;

import com.example.is.entity.UploadFileEntity;
import com.example.is.entity.UploadFileStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadFileRepository extends JpaRepository<UploadFileEntity, Integer> {
}
