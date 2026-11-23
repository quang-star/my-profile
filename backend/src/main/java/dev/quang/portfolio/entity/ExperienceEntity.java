package dev.quang.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "experiences")
public class ExperienceEntity {

    @Id
    @Column(length = 80)
    private String id;

    @Column(nullable = false, length = 150)
    private String company;

    @Column(name = "role_vi", nullable = false, length = 150)
    private String roleVi;

    @Column(name = "role_en", nullable = false, length = 150)
    private String roleEn;

    @Column(name = "period_vi", nullable = false, length = 100)
    private String periodVi;

    @Column(name = "period_en", nullable = false, length = 100)
    private String periodEn;

    @Column(name = "location_vi", nullable = false, length = 150)
    private String locationVi;

    @Column(name = "location_en", nullable = false, length = 150)
    private String locationEn;

    @Column(name = "type_vi", nullable = false, length = 80)
    private String typeVi;

    @Column(name = "type_en", nullable = false, length = 80)
    private String typeEn;

    @Column(name = "summary_vi", nullable = false, columnDefinition = "TEXT")
    private String summaryVi;

    @Column(name = "summary_en", nullable = false, columnDefinition = "TEXT")
    private String summaryEn;

    @Column(name = "achievements_vi_json", nullable = false, columnDefinition = "TEXT")
    private String achievementsViJson = "[]";

    @Column(name = "achievements_en_json", nullable = false, columnDefinition = "TEXT")
    private String achievementsEnJson = "[]";

    @Column(name = "stack_json", nullable = false, columnDefinition = "TEXT")
    private String stackJson = "[]";

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getRoleVi() { return roleVi; }
    public void setRoleVi(String roleVi) { this.roleVi = roleVi; }
    public String getRoleEn() { return roleEn; }
    public void setRoleEn(String roleEn) { this.roleEn = roleEn; }
    public String getPeriodVi() { return periodVi; }
    public void setPeriodVi(String periodVi) { this.periodVi = periodVi; }
    public String getPeriodEn() { return periodEn; }
    public void setPeriodEn(String periodEn) { this.periodEn = periodEn; }
    public String getLocationVi() { return locationVi; }
    public void setLocationVi(String locationVi) { this.locationVi = locationVi; }
    public String getLocationEn() { return locationEn; }
    public void setLocationEn(String locationEn) { this.locationEn = locationEn; }
    public String getTypeVi() { return typeVi; }
    public void setTypeVi(String typeVi) { this.typeVi = typeVi; }
    public String getTypeEn() { return typeEn; }
    public void setTypeEn(String typeEn) { this.typeEn = typeEn; }
    public String getSummaryVi() { return summaryVi; }
    public void setSummaryVi(String summaryVi) { this.summaryVi = summaryVi; }
    public String getSummaryEn() { return summaryEn; }
    public void setSummaryEn(String summaryEn) { this.summaryEn = summaryEn; }
    public String getAchievementsViJson() { return achievementsViJson; }
    public void setAchievementsViJson(String achievementsViJson) { this.achievementsViJson = achievementsViJson; }
    public String getAchievementsEnJson() { return achievementsEnJson; }
    public void setAchievementsEnJson(String achievementsEnJson) { this.achievementsEnJson = achievementsEnJson; }
    public String getStackJson() { return stackJson; }
    public void setStackJson(String stackJson) { this.stackJson = stackJson; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
