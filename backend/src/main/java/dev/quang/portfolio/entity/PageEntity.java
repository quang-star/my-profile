package dev.quang.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "pages")
public class PageEntity {

    @Id
    @Column(length = 80)
    private String id;

    @Column(name = "owner_id", nullable = false, length = 80)
    private String ownerId;

    @Column(nullable = false, length = 80)
    private String slug;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 500)
    private String description = "";

    @Column(nullable = false, length = 24)
    private String visibility = "DRAFT";

    @Column(name = "is_primary", nullable = false)
    private boolean primary = false;

    @Column(name = "is_template", nullable = false)
    private boolean template = false;

    @Column(name = "is_home_featured", nullable = false)
    private boolean homeFeatured = false;

    @Column(name = "cv_pdf_url", length = 500)
    private String cvPdfUrl;

    @Column(name = "theme_json", nullable = false, columnDefinition = "TEXT")
    private String themeJson = "{}";

    @Column(name = "view_count", nullable = false)
    private long viewCount = 0L;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public boolean isPrimary() { return primary; }
    public void setPrimary(boolean primary) { this.primary = primary; }

    public boolean isTemplate() { return template; }
    public void setTemplate(boolean template) { this.template = template; }

    public boolean isHomeFeatured() { return homeFeatured; }
    public void setHomeFeatured(boolean homeFeatured) { this.homeFeatured = homeFeatured; }

    public String getCvPdfUrl() { return cvPdfUrl; }
    public void setCvPdfUrl(String cvPdfUrl) { this.cvPdfUrl = cvPdfUrl; }

    public String getThemeJson() { return themeJson; }
    public void setThemeJson(String themeJson) { this.themeJson = themeJson; }

    public long getViewCount() { return viewCount; }
    public void setViewCount(long viewCount) { this.viewCount = viewCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
