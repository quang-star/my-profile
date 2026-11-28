package dev.quang.portfolio.dto;

import dev.quang.portfolio.dto.NodeDtos.ContentNodeDto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class MultiTenantDtos {

    private MultiTenantDtos() {}

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 32) String username,
            @NotBlank @Email @Size(max = 200) String email,
            @NotBlank @Size(min = 8, max = 128) String password,
            @Size(max = 120) String displayName
    ) {}

    public record UserProfileDto(
            String id,
            String username,
            String email,
            String displayName,
            String avatarUrl,
            boolean hasPassword,
            boolean googleLinked,
            String role,
            String status,
            int maxPages,
            long maxStorageBytes,
            long usedPages,
            long usedStorageBytes
    ) {}

    public record AuthResponse(
            String token,
            String username,
            long expiresInMs,
            UserProfileDto user,
            boolean isNewUser
    ) {
        public AuthResponse(String token, String username, long expiresInMs, UserProfileDto user) {
            this(token, username, expiresInMs, user, false);
        }
    }

    public record GoogleConfigDto(
            boolean enabled,
            String clientId
    ) {}

    public record GoogleLoginRequest(
            @NotBlank String credential,
            @Size(max = 64) String preferredUsername
    ) {}

    public record UpdateProfileRequest(
            @Size(max = 64) String username,
            @Size(max = 120) String displayName,
            @Size(max = 500) String avatarUrl,
            Boolean clearAvatar
    ) {
        public UpdateProfileRequest(String username, String displayName, String avatarUrl) {
            this(username, displayName, avatarUrl, null);
        }
    }

    public record ChangePasswordRequest(
            String currentPassword,
            @NotBlank @Size(min = 8, max = 128) String newPassword
    ) {}

    public record ForgotPasswordRequest(
            @NotBlank @Email @Size(max = 200) String email
    ) {}

    public record ForgotPasswordResponse(
            String message,
            boolean smtpConfigured
    ) {}

    public record ResetPasswordRequest(
            @NotBlank @Size(min = 16, max = 200) String token,
            @NotBlank @Size(min = 8, max = 128) String newPassword
    ) {}

    public record PageOwnerDto(
            String username,
            String displayName,
            String role,
            String avatarUrl
    ) {}

    public record PageSummaryDto(
            String id,
            String ownerId,
            String slug,
            String title,
            String description,
            String visibility,
            boolean isPrimary,
            boolean isTemplate,
            boolean isHomeFeatured,
            String cvPdfUrl,
            String themeJson,
            long viewCount,
            long unreadMessagesCount,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record PublicPageResponse(
            PageSummaryDto page,
            PageOwnerDto owner,
            List<ContentNodeDto> nodes
    ) {}

    public record TemplateSummaryDto(
            String id,
            String slug,
            String title,
            String description,
            String ownerUsername,
            String themeJson,
            Instant updatedAt
    ) {}

    public record CreatePageRequest(
            @NotBlank @Size(max = 160) String title,
            @NotBlank @Size(max = 80) String slug,
            @Size(max = 500) String description,
            String visibility,
            String fromTemplateId
    ) {}

    public record UpdatePageRequest(
            @NotBlank @Size(max = 160) String title,
            @NotBlank @Size(max = 80) String slug,
            @Size(max = 500) String description,
            String visibility,
            Boolean isPrimary,
            @Size(max = 500) String cvPdfUrl,
            String themeJson
    ) {}

    public record UploadedFileDto(
            String id,
            String url,
            String filename,
            String contentType,
            long sizeBytes,
            int usageCount,
            Instant createdAt
    ) {}

    public record MyFilesResponse(
            List<UploadedFileDto> files,
            long usedStorageBytes,
            long maxStorageBytes
    ) {}

    public record AdminOverviewDto(
            long totalUsers,
            long activeUsers,
            long bannedUsers,
            long totalPages,
            long publicPages,
            long templatePages,
            long totalStorageBytes
    ) {}

    public record AdminUserDto(
            String id,
            String username,
            String email,
            String displayName,
            String role,
            String status,
            int maxPages,
            long maxStorageBytes,
            long usedPages,
            long usedStorageBytes,
            Instant createdAt
    ) {}

    public record AdminUpdateUserRequest(
            String status,
            String role,
            Integer maxPages,
            Long maxStorageBytes
    ) {}

    public record AdminPageDto(
            String id,
            String ownerId,
            String ownerUsername,
            String slug,
            String title,
            String visibility,
            boolean isPrimary,
            boolean isTemplate,
            boolean isHomeFeatured,
            long viewCount,
            Instant updatedAt
    ) {}

    public record AdminUpdatePageRequest(
            Boolean isTemplate,
            Boolean isHomeFeatured,
            String visibility
    ) {}

    public record HomeConfigDto(
            String heroPreview,
            String featured,
            String features,
            String templates,
            String howItWorks,
            String bottomCta
    ) {}

    public record FeatureFlagsDto(
            Boolean registrationEnabled,
            Boolean googleLoginEnabled,
            Boolean aiCopilotEnabled,
            Boolean aiTranslateEnabled,
            Boolean cvImportEnabled,
            Boolean pdfExportEnabled,
            Boolean mediaUploadEnabled,
            Boolean publicTemplatesEnabled,
            Boolean publicContactEnabled,
            Boolean maintenanceMode,
            String maintenanceMessage
    ) {}
}
