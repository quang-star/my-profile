package dev.quang.portfolio.repository;

import dev.quang.portfolio.entity.ProjectEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<ProjectEntity, String> {
    List<ProjectEntity> findAllByOrderBySortOrderAscIdAsc();
}
