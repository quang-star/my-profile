package dev.quang.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "projects")
public class ProjectEntity {

    @Id
    @Column(length = 80)
    private String id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 32)
    private String category = "fullstack";

    @Column(nullable = false)
    private boolean featured = false;

    @Column(name = "project_year", nullable = false, length = 64)
    private String year;

    @Column(name = "tagline_vi", nullable = false, columnDefinition = "TEXT")
    private String taglineVi;

    @Column(name = "tagline_en", nullable = false, columnDefinition = "TEXT")
    private String taglineEn;

    @Column(name = "description_vi", nullable = false, columnDefinition = "TEXT")
    private String descriptionVi;

    @Column(name = "description_en", nullable = false, columnDefinition = "TEXT")
    private String descriptionEn;

    @Column(name = "highlights_vi_json", nullable = false, columnDefinition = "TEXT")
    private String highlightsViJson = "[]";

    @Column(name = "highlights_en_json", nullable = false, columnDefinition = "TEXT")
    private String highlightsEnJson = "[]";

    @Column(name = "stack_json", nullable = false, columnDefinition = "TEXT")
    private String stackJson = "[]";

    @Column(name = "live_url", length = 500)
    private String liveUrl;

    @Column(name = "repo_url", length = 500)
    private String repoUrl;

    @Column(name = "secondary_repo_url", length = 500)
    private String secondaryRepoUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }
    public String getTaglineVi() { return taglineVi; }
    public void setTaglineVi(String taglineVi) { this.taglineVi = taglineVi; }
    public String getTaglineEn() { return taglineEn; }
    public void setTaglineEn(String taglineEn) { this.taglineEn = taglineEn; }
    public String getDescriptionVi() { return descriptionVi; }
    public void setDescriptionVi(String descriptionVi) { this.descriptionVi = descriptionVi; }
    public String getDescriptionEn() { return descriptionEn; }
    public void setDescriptionEn(String descriptionEn) { this.descriptionEn = descriptionEn; }
    public String getHighlightsViJson() { return highlightsViJson; }
    public void setHighlightsViJson(String highlightsViJson) { this.highlightsViJson = highlightsViJson; }
    public String getHighlightsEnJson() { return highlightsEnJson; }
    public void setHighlightsEnJson(String highlightsEnJson) { this.highlightsEnJson = highlightsEnJson; }
    public String getStackJson() { return stackJson; }
    public void setStackJson(String stackJson) { this.stackJson = stackJson; }
    public String getLiveUrl() { return liveUrl; }
    public void setLiveUrl(String liveUrl) { this.liveUrl = liveUrl; }
    public String getRepoUrl() { return repoUrl; }
    public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }
    public String getSecondaryRepoUrl() { return secondaryRepoUrl; }
    public void setSecondaryRepoUrl(String secondaryRepoUrl) { this.secondaryRepoUrl = secondaryRepoUrl; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
