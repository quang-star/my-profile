package dev.quang.portfolio.repository;

import dev.quang.portfolio.entity.ExperienceEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperienceRepository extends JpaRepository<ExperienceEntity, String> {
    List<ExperienceEntity> findAllByOrderBySortOrderAscIdAsc();
}
