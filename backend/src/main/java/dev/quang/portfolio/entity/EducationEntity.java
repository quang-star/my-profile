package dev.quang.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "education_items")
public class EducationEntity {

    @Id
    @Column(length = 80)
    private String id;

    @Column(nullable = false, length = 200)
    private String school;

    @Column(name = "degree_vi", nullable = false, length = 200)
    private String degreeVi;

    @Column(name = "degree_en", nullable = false, length = 200)
    private String degreeEn;

    @Column(name = "period_vi", nullable = false, length = 100)
    private String periodVi;

    @Column(name = "period_en", nullable = false, length = 100)
    private String periodEn;

    @Column(length = 40)
    private String gpa;

    @Column(name = "details_vi", nullable = false, columnDefinition = "TEXT")
    private String detailsVi;

    @Column(name = "details_en", nullable = false, columnDefinition = "TEXT")
    private String detailsEn;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }
    public String getDegreeVi() { return degreeVi; }
    public void setDegreeVi(String degreeVi) { this.degreeVi = degreeVi; }
    public String getDegreeEn() { return degreeEn; }
    public void setDegreeEn(String degreeEn) { this.degreeEn = degreeEn; }
    public String getPeriodVi() { return periodVi; }
    public void setPeriodVi(String periodVi) { this.periodVi = periodVi; }
    public String getPeriodEn() { return periodEn; }
    public void setPeriodEn(String periodEn) { this.periodEn = periodEn; }
    public String getGpa() { return gpa; }
    public void setGpa(String gpa) { this.gpa = gpa; }
    public String getDetailsVi() { return detailsVi; }
    public void setDetailsVi(String detailsVi) { this.detailsVi = detailsVi; }
    public String getDetailsEn() { return detailsEn; }
    public void setDetailsEn(String detailsEn) { this.detailsEn = detailsEn; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
