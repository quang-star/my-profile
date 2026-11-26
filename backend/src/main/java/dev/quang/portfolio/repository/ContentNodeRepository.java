package dev.quang.portfolio.repository;

import dev.quang.portfolio.entity.ContentNodeEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentNodeRepository extends JpaRepository<ContentNodeEntity, String> {

    List<ContentNodeEntity> findByPageIdOrderBySortOrderAscIdAsc(String pageId);

    List<ContentNodeEntity> findByPageIdAndParentIdOrderBySortOrderAscIdAsc(String pageId, String parentId);

    List<ContentNodeEntity> findByParentIdOrderBySortOrderAscIdAsc(String parentId);

    long countByPageId(String pageId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select node from ContentNodeEntity node where node.pageId = :pageId order by node.id")
    List<ContentNodeEntity> lockCanvasNodesByPageId(@Param("pageId") String pageId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ContentNodeEntity n set n.pageId = :pageId where n.pageId is null")
    void assignNullPageId(@Param("pageId") String pageId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ContentNodeEntity n set n.parentId = null where n.pageId = :pageId")
    void clearParentIdsByPageId(@Param("pageId") String pageId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ContentNodeEntity n where n.pageId = :pageId")
    void deleteAllByPageId(@Param("pageId") String pageId);
}
