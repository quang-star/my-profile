package dev.quang.portfolio.repository;

import dev.quang.portfolio.entity.EducationEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EducationRepository extends JpaRepository<EducationEntity, String> {
    List<EducationEntity> findAllByOrderBySortOrderAscIdAsc();
}
