package dev.quang.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "skill_groups")
public class SkillGroupEntity {

    @Id
    @Column(length = 80)
    private String id;

    @Column(name = "title_vi", nullable = false, length = 150)
    private String titleVi;

    @Column(name = "title_en", nullable = false, length = 150)
    private String titleEn;

    @Column(name = "description_vi", nullable = false, columnDefinition = "TEXT")
    private String descriptionVi;

    @Column(name = "description_en", nullable = false, columnDefinition = "TEXT")
    private String descriptionEn;

    @Column(nullable = false, length = 32)
    private String accent = "emerald";

    @Column(name = "items_json", nullable = false, columnDefinition = "TEXT")
    private String itemsJson = "[]";

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitleVi() { return titleVi; }
    public void setTitleVi(String titleVi) { this.titleVi = titleVi; }
    public String getTitleEn() { return titleEn; }
    public void setTitleEn(String titleEn) { this.titleEn = titleEn; }
    public String getDescriptionVi() { return descriptionVi; }
    public void setDescriptionVi(String descriptionVi) { this.descriptionVi = descriptionVi; }
    public String getDescriptionEn() { return descriptionEn; }
    public void setDescriptionEn(String descriptionEn) { this.descriptionEn = descriptionEn; }
    public String getAccent() { return accent; }
    public void setAccent(String accent) { this.accent = accent; }
    public String getItemsJson() { return itemsJson; }
    public void setItemsJson(String itemsJson) { this.itemsJson = itemsJson; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
