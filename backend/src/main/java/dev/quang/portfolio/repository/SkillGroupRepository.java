package dev.quang.portfolio.repository;

import dev.quang.portfolio.entity.SkillGroupEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillGroupRepository extends JpaRepository<SkillGroupEntity, String> {
    List<SkillGroupEntity> findAllByOrderBySortOrderAscIdAsc();
}
