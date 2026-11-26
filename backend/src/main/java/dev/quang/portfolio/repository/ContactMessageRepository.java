package dev.quang.portfolio.repository;

import dev.quang.portfolio.entity.ContactMessageEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContactMessageRepository extends JpaRepository<ContactMessageEntity, Long> {
    List<ContactMessageEntity> findAllByOrderByCreatedAtDesc();

    List<ContactMessageEntity> findByPageIdOrderByCreatedAtDesc(String pageId);

    long countByPageIdAndReadFalse(String pageId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ContactMessageEntity m set m.pageId = :pageId where m.pageId is null")
    void assignNullPageId(@Param("pageId") String pageId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ContactMessageEntity m where m.pageId = :pageId")
    void deleteAllByPageId(@Param("pageId") String pageId);
}
