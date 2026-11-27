package dev.quang.portfolio.repository;

import dev.quang.portfolio.entity.UploadedFileEntity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UploadedFileRepository extends JpaRepository<UploadedFileEntity, String> {

    interface UploadedFileSummary {
        String getId();
        String getFilename();
        String getContentType();
        long getSizeBytes();
        Instant getCreatedAt();
    }

    @Query("""
            select f.id as id, f.filename as filename, f.contentType as contentType,
                   f.sizeBytes as sizeBytes, f.createdAt as createdAt
            from UploadedFileEntity f
            where f.ownerId = :ownerId
            order by f.createdAt desc
            """)
    List<UploadedFileSummary> findSummariesByOwnerIdOrderByCreatedAtDesc(@Param("ownerId") String ownerId);

    List<UploadedFileEntity> findByOwnerIdIsNull();

    @Query("select coalesce(sum(f.sizeBytes), 0) from UploadedFileEntity f where f.ownerId = :ownerId")
    long sumSizeBytesByOwnerId(@Param("ownerId") String ownerId);

    @Query("select coalesce(sum(f.sizeBytes), 0) from UploadedFileEntity f")
    long sumAllSizeBytes();
}
