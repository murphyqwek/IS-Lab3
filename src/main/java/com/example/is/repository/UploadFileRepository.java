package com.example.is.repository;

import com.example.is.entity.UploadFileEntity;
import com.example.is.entity.UploadFileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.time.Instant;
import java.util.List;

public interface UploadFileRepository extends JpaRepository<UploadFileEntity, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from UploadFileEntity f where f.id = :id")
    Optional<UploadFileEntity> findForUpdate(@Param("id") int id);

    @Query("select f.id from UploadFileEntity f where f.expired <= :now and f.status in :statuses order by f.expired")
    List<Integer> findExpiredIds(@Param("now") Instant now, @Param("statuses") List<UploadFileStatus> statuses);
}
