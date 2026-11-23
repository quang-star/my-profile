package dev.quang.portfolio.repository;

import dev.quang.portfolio.entity.UploadedFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadedFileRepository extends JpaRepository<UploadedFileEntity, String> {
}
