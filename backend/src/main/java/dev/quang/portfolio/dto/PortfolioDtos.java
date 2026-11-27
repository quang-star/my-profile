package dev.quang.portfolio.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

public final class PortfolioDtos {

    private PortfolioDtos() {}

    public record LocalizedTextDto(
            @NotNull String vi,
            @NotNull String en
    ) {}

    public record LocalizedListDto(
            @NotNull List<String> vi,
            @NotNull List<String> en
    ) {}

    public record SocialLinkDto(
            @NotBlank String id,
            @NotBlank String label,
            @NotBlank String handle,
            @NotBlank String url
    ) {}

    public record HighlightMetricDto(
            @NotBlank String value,
            @Valid @NotNull LocalizedTextDto label
    ) {}

    public record SkillGroupDto(
            String id,
            @Valid @NotNull LocalizedTextDto title,
            @Valid @NotNull LocalizedTextDto description,
            @NotBlank String accent,
            @NotNull List<String> items,
            Integer sortOrder
    ) {}

    public record ExperienceDto(
            String id,
            @NotBlank String company,
            @Valid @NotNull LocalizedTextDto role,
            @Valid @NotNull LocalizedTextDto period,
            @Valid @NotNull LocalizedTextDto location,
            @Valid @NotNull LocalizedTextDto type,
            @Valid @NotNull LocalizedTextDto summary,
            @Valid @NotNull LocalizedListDto achievements,
            @NotNull List<String> stack,
            Integer sortOrder
    ) {}

    public record EducationDto(
            String id,
            @NotBlank String school,
            @Valid @NotNull LocalizedTextDto degree,
            @Valid @NotNull LocalizedTextDto period,
            String gpa,
            @Valid @NotNull LocalizedTextDto details,
            Integer sortOrder
    ) {}

    public record ProjectDto(
            String id,
            @NotBlank String title,
            @NotBlank String category,
            boolean featured,
            @NotBlank String year,
            @Valid @NotNull LocalizedTextDto tagline,
            @Valid @NotNull LocalizedTextDto description,
            @Valid @NotNull LocalizedListDto highlights,
            @NotNull List<String> stack,
            String liveUrl,
            String repoUrl,
            String secondaryRepoUrl,
            Integer sortOrder
    ) {}

    public record ProfileUpdateRequest(
            @NotBlank String name,
            @NotBlank String initials,
            String avatarUrl,
            @NotBlank String handle,
            @Valid @NotNull LocalizedTextDto role,
            @Valid @NotNull LocalizedTextDto availability,
            @Valid @NotNull LocalizedTextDto location,
            @NotBlank String timezone,
            @NotNull String email,
            String phone,
            String cvPdfUrl,
            @Valid @NotNull LocalizedTextDto headline,
            @Valid @NotNull LocalizedTextDto bio,
            @Valid @NotNull LocalizedTextDto currentFocus,
            @Valid @NotNull List<HighlightMetricDto> metrics,
            @Valid @NotNull List<SocialLinkDto> socials,
            @Valid @NotNull LocalizedListDto languages,
            boolean isDraft
    ) {}

    public record PortfolioResponse(
            String name,
            String initials,
            String avatarUrl,
            String handle,
            LocalizedTextDto role,
            LocalizedTextDto availability,
            LocalizedTextDto location,
            String timezone,
            String email,
            String phone,
            String cvPdfUrl,
            LocalizedTextDto headline,
            LocalizedTextDto bio,
            LocalizedTextDto currentFocus,
            List<HighlightMetricDto> metrics,
            List<SocialLinkDto> socials,
            List<SkillGroupDto> skills,
            List<ExperienceDto> experiences,
            List<EducationDto> education,
            LocalizedListDto languages,
            List<ProjectDto> projects,
            boolean isDraft
    ) {}

    public record ContactRequest(
            @NotBlank String name,
            @NotBlank @Email String email,
            String subject,
            @NotBlank String message,
            String website
    ) {
        public ContactRequest(String name, String email, String subject, String message) {
            this(name, email, subject, message, null);
        }
    }

    public record ContactMessageDto(
            Long id,
            String name,
            String email,
            String subject,
            String message,
            boolean read,
            Instant createdAt
    ) {}

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {}

    public record LoginResponse(
            String token,
            String username,
            long expiresInMs
    ) {}
}
