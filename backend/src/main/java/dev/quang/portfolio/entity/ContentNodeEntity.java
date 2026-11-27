package dev.quang.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "content_nodes")
public class ContentNodeEntity {

    @Id
    @Column(length = 80)
    private String id;

    @Column(name = "page_id", length = 80)
    private String pageId;

    @Column(name = "parent_id", length = 80)
    private String parentId;

    @Column(nullable = false, length = 50)
    private String tag;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "attrs_json", nullable = false, columnDefinition = "TEXT")
    private String attrsJson = "{}";

    @Column(name = "content_vi", nullable = false, columnDefinition = "TEXT")
    private String contentVi = "";

    @Column(name = "content_en", nullable = false, columnDefinition = "TEXT")
    private String contentEn = "";

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPageId() {
        return pageId;
    }

    public void setPageId(String pageId) {
        this.pageId = pageId;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getAttrsJson() {
        return attrsJson;
    }

    public void setAttrsJson(String attrsJson) {
        this.attrsJson = attrsJson;
    }

    public String getContentVi() {
        return contentVi;
    }

    public void setContentVi(String contentVi) {
        this.contentVi = contentVi;
    }

    public String getContentEn() {
        return contentEn;
    }

    public void setContentEn(String contentEn) {
        this.contentEn = contentEn;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
