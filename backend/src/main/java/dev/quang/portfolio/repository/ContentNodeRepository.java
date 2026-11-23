package dev.quang.portfolio.repository;

import dev.quang.portfolio.entity.ContentNodeEntity;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ContentNodeRepository extends JpaRepository<ContentNodeEntity, String> {

    List<ContentNodeEntity> findAllByOrderBySortOrderAscIdAsc();

    List<ContentNodeEntity> findByParentIdOrderBySortOrderAscIdAsc(String parentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select node from ContentNodeEntity node order by node.id")
    List<ContentNodeEntity> lockCanvasNodes();
}
