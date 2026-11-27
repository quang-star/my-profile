package dev.quang.portfolio.repository;

import dev.quang.portfolio.entity.PageEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PageRepository extends JpaRepository<PageEntity, String> {

    List<PageEntity> findByOwnerIdOrderByUpdatedAtDesc(String ownerId);

    Optional<PageEntity> findByOwnerIdAndSlug(String ownerId, String slug);

    Optional<PageEntity> findFirstByOwnerIdAndPrimaryTrue(String ownerId);

    Optional<PageEntity> findFirstByOwnerIdAndPrimaryTrueAndVisibilityIgnoreCase(String ownerId, String visibility);

    Optional<PageEntity> findFirstByHomeFeaturedTrue();

    Optional<PageEntity> findFirstByHomeFeaturedTrueAndVisibilityIgnoreCase(String visibility);

    List<PageEntity> findByTemplateTrueOrderByUpdatedAtDesc();

    List<PageEntity> findByTemplateTrueAndVisibilityIgnoreCaseOrderByUpdatedAtDesc(String visibility);

    List<PageEntity> findAllByOrderByUpdatedAtDesc();

    long countByOwnerId(String ownerId);

    long countByVisibility(String visibility);

    long countByTemplateTrue();

    long countByTemplateTrueAndVisibilityIgnoreCase(String visibility);

    boolean existsByOwnerIdAndSlug(String ownerId, String slug);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PageEntity p set p.primary = false where p.ownerId = :ownerId and p.id <> :keepId")
    void clearPrimaryForOwnerExcept(@Param("ownerId") String ownerId, @Param("keepId") String keepId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PageEntity p set p.homeFeatured = false where p.id <> :keepId")
    void clearHomeFeaturedExcept(@Param("keepId") String keepId);

    @Modifying
    @Query("update PageEntity p set p.viewCount = p.viewCount + 1 where p.id = :id")
    void incrementViewCount(@Param("id") String id);
}
