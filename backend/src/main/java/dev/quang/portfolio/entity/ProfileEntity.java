package dev.quang.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "profile_info")
public class ProfileEntity {

    @Id
    private Long id = 1L;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 12)
    private String initials;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(nullable = false, length = 100)
    private String handle;

    @Column(name = "role_vi", nullable = false, length = 200)
    private String roleVi;

    @Column(name = "role_en", nullable = false, length = 200)
    private String roleEn;

    @Column(name = "availability_vi", nullable = false, length = 255)
    private String availabilityVi;

    @Column(name = "availability_en", nullable = false, length = 255)
    private String availabilityEn;

    @Column(name = "location_vi", nullable = false, length = 150)
    private String locationVi;

    @Column(name = "location_en", nullable = false, length = 150)
    private String locationEn;

    @Column(nullable = false, length = 32)
    private String timezone;

    @Column(nullable = false, length = 200)
    private String email = "";

    @Column(length = 60)
    private String phone;

    @Column(name = "cv_pdf_url", length = 500)
    private String cvPdfUrl;

    @Column(name = "headline_vi", nullable = false, columnDefinition = "TEXT")
    private String headlineVi;

    @Column(name = "headline_en", nullable = false, columnDefinition = "TEXT")
    private String headlineEn;

    @Column(name = "bio_vi", nullable = false, columnDefinition = "TEXT")
    private String bioVi;

    @Column(name = "bio_en", nullable = false, columnDefinition = "TEXT")
    private String bioEn;

    @Column(name = "current_focus_vi", nullable = false, columnDefinition = "TEXT")
    private String currentFocusVi;

    @Column(name = "current_focus_en", nullable = false, columnDefinition = "TEXT")
    private String currentFocusEn;

    @Column(name = "metrics_json", nullable = false, columnDefinition = "TEXT")
    private String metricsJson = "[]";

    @Column(name = "socials_json", nullable = false, columnDefinition = "TEXT")
    private String socialsJson = "[]";

    @Column(name = "languages_vi_json", nullable = false, columnDefinition = "TEXT")
    private String languagesViJson = "[]";

    @Column(name = "languages_en_json", nullable = false, columnDefinition = "TEXT")
    private String languagesEnJson = "[]";

    @Column(name = "is_draft", nullable = false)
    private boolean draft = false;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getInitials() { return initials; }
    public void setInitials(String initials) { this.initials = initials; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getHandle() { return handle; }
    public void setHandle(String handle) { this.handle = handle; }
    public String getRoleVi() { return roleVi; }
    public void setRoleVi(String roleVi) { this.roleVi = roleVi; }
    public String getRoleEn() { return roleEn; }
    public void setRoleEn(String roleEn) { this.roleEn = roleEn; }
    public String getAvailabilityVi() { return availabilityVi; }
    public void setAvailabilityVi(String availabilityVi) { this.availabilityVi = availabilityVi; }
    public String getAvailabilityEn() { return availabilityEn; }
    public void setAvailabilityEn(String availabilityEn) { this.availabilityEn = availabilityEn; }
    public String getLocationVi() { return locationVi; }
    public void setLocationVi(String locationVi) { this.locationVi = locationVi; }
    public String getLocationEn() { return locationEn; }
    public void setLocationEn(String locationEn) { this.locationEn = locationEn; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCvPdfUrl() { return cvPdfUrl; }
    public void setCvPdfUrl(String cvPdfUrl) { this.cvPdfUrl = cvPdfUrl; }
    public String getHeadlineVi() { return headlineVi; }
    public void setHeadlineVi(String headlineVi) { this.headlineVi = headlineVi; }
    public String getHeadlineEn() { return headlineEn; }
    public void setHeadlineEn(String headlineEn) { this.headlineEn = headlineEn; }
    public String getBioVi() { return bioVi; }
    public void setBioVi(String bioVi) { this.bioVi = bioVi; }
    public String getBioEn() { return bioEn; }
    public void setBioEn(String bioEn) { this.bioEn = bioEn; }
    public String getCurrentFocusVi() { return currentFocusVi; }
    public void setCurrentFocusVi(String currentFocusVi) { this.currentFocusVi = currentFocusVi; }
    public String getCurrentFocusEn() { return currentFocusEn; }
    public void setCurrentFocusEn(String currentFocusEn) { this.currentFocusEn = currentFocusEn; }
    public String getMetricsJson() { return metricsJson; }
    public void setMetricsJson(String metricsJson) { this.metricsJson = metricsJson; }
    public String getSocialsJson() { return socialsJson; }
    public void setSocialsJson(String socialsJson) { this.socialsJson = socialsJson; }
    public String getLanguagesViJson() { return languagesViJson; }
    public void setLanguagesViJson(String languagesViJson) { this.languagesViJson = languagesViJson; }
    public String getLanguagesEnJson() { return languagesEnJson; }
    public void setLanguagesEnJson(String languagesEnJson) { this.languagesEnJson = languagesEnJson; }
    public boolean isDraft() { return draft; }
    public void setDraft(boolean draft) { this.draft = draft; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
