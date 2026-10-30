package com.example.is.repository;

import com.example.is.entity.ImportHistory;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;

@Repository
public interface ImportHistoryRepository extends JpaRepository<ImportHistory, Long> {

    @Query("""
        SELECT h
        FROM ImportHistory h
        ORDER BY h.createdAt DESC, h.id DESC
    """)
    @QueryHints({
            @QueryHint(name = "org.hibernate.cacheable", value = "true"),
            @QueryHint(name = "org.hibernate.cacheRegion", value = "import-history")
    })
    List<ImportHistory> findFirstPage(Pageable pageable);

    @Query("""
        SELECT h
        FROM ImportHistory h
        WHERE
            h.createdAt < :cursorCreatedAt
            OR (
                h.createdAt = :cursorCreatedAt
                AND h.id < :cursorId
            )
        ORDER BY h.createdAt DESC, h.id DESC
    """)
    @QueryHints({
            @QueryHint(name = "org.hibernate.cacheable", value = "true"),
            @QueryHint(name = "org.hibernate.cacheRegion", value = "import-history")
    })
    List<ImportHistory> findNextPage(
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("""
        SELECT h
        FROM ImportHistory h
        WHERE h.username = :username
        ORDER BY h.createdAt DESC, h.id DESC
    """)
    @QueryHints({
            @QueryHint(name = "org.hibernate.cacheable", value = "true"),
            @QueryHint(name = "org.hibernate.cacheRegion", value = "import-history")
    })
    List<ImportHistory> findFirstPageUser(
            @Param("username") String username,
            Pageable pageable
    );

    @Query("""
        SELECT h
        FROM ImportHistory h
        WHERE
            h.username = :username
            AND (
                h.createdAt < :cursorCreatedAt
                OR (
                    h.createdAt = :cursorCreatedAt
                    AND h.id < :cursorId
                )
            )
        ORDER BY h.createdAt DESC, h.id DESC
    """)
    @QueryHints({
            @QueryHint(name = "org.hibernate.cacheable", value = "true"),
            @QueryHint(name = "org.hibernate.cacheRegion", value = "import-history")
    })
    List<ImportHistory> findNextPageUser(
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            @Param("username") String username,
            Pageable pageable
    );
}
