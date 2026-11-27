package dev.quang.portfolio.service;

import dev.quang.portfolio.entity.PageEntity;
import dev.quang.portfolio.entity.UploadedFileEntity;
import dev.quang.portfolio.entity.UserEntity;
import dev.quang.portfolio.repository.ContactMessageRepository;
import dev.quang.portfolio.repository.ContentNodeRepository;
import dev.quang.portfolio.repository.PageRepository;
import dev.quang.portfolio.repository.UploadedFileRepository;
import dev.quang.portfolio.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(1)
public class SystemDataSeeder implements CommandLineRunner {

    public static final String DEFAULT_ADMIN_ID = "user-super-admin";
    public static final String DEFAULT_PAGE_ID = "page-main";

    private final UserRepository userRepository;
    private final PageRepository pageRepository;
    private final ContentNodeRepository contentNodeRepository;
    private final ContactMessageRepository contactMessageRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public SystemDataSeeder(
            UserRepository userRepository,
            PageRepository pageRepository,
            ContentNodeRepository contentNodeRepository,
            ContactMessageRepository contactMessageRepository,
            UploadedFileRepository uploadedFileRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.username:admin}") String adminUsername,
            @Value("${app.admin.password:admin123}") String adminPassword
    ) {
        this.userRepository = userRepository;
        this.pageRepository = pageRepository;
        this.contentNodeRepository = contentNodeRepository;
        this.contactMessageRepository = contactMessageRepository;
        this.uploadedFileRepository = uploadedFileRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        ensureSystemSeeded();
    }

    @Transactional
    public String ensureSystemSeeded() {
        String normalizedUsername = adminUsername.trim();
        UserEntity superAdmin = userRepository.findByUsernameIgnoreCase(normalizedUsername)
                .or(() -> userRepository.findFirstByRoleOrderByCreatedAtAsc("SUPER_ADMIN"))
                .orElseGet(() -> {
                    UserEntity user = new UserEntity();
                    user.setId(DEFAULT_ADMIN_ID);
                    user.setUsername(normalizedUsername);
                    user.setEmail(normalizedUsername.toLowerCase(Locale.ROOT) + "@portfolio.local");
                    String hash = adminPassword.startsWith("$2")
                            ? adminPassword
                            : passwordEncoder.encode(adminPassword);
                    user.setPasswordHash(hash);
                    user.setDisplayName("Nguyễn Kim Quang");
                    user.setRole("SUPER_ADMIN");
                    user.setMaxPages(-1);
                    user.setMaxStorageBytes(-1L);
                    user.setStatus("ACTIVE");
                    user.setCreatedAt(Instant.now());
                    user.setUpdatedAt(Instant.now());
                    return userRepository.save(user);
                });

        List<PageEntity> adminPages = pageRepository.findByOwnerIdOrderByUpdatedAtDesc(superAdmin.getId());
        PageEntity defaultPage = adminPages.isEmpty()
                ? createDefaultPage(superAdmin.getId())
                : pageRepository.findFirstByHomeFeaturedTrue()
                        .or(() -> pageRepository.findFirstByOwnerIdAndPrimaryTrue(superAdmin.getId()))
                        .orElse(adminPages.get(0));

        if (DEFAULT_PAGE_ID.equals(defaultPage.getId())
                && (defaultPage.getThemeJson() == null || defaultPage.getThemeJson().isBlank() || "{}".equals(defaultPage.getThemeJson().trim()))) {
            defaultPage.setThemeJson("{\"category\":\"it-dev\",\"previewLayout\":\"bento\"}");
            if ("Default portfolio page".equals(defaultPage.getDescription())) {
                defaultPage.setDescription("Portfolio lập trình viên Java Backend & Full-stack — bố cục Bento gọn gàng, song ngữ VI/EN.");
            }
            pageRepository.save(defaultPage);
        }

        contentNodeRepository.assignNullPageId(defaultPage.getId());
        contactMessageRepository.assignNullPageId(defaultPage.getId());

        List<UploadedFileEntity> orphanFiles = uploadedFileRepository.findByOwnerIdIsNull();
        if (!orphanFiles.isEmpty()) {
            for (UploadedFileEntity file : orphanFiles) {
                file.setOwnerId(superAdmin.getId());
                if (file.getSizeBytes() == 0 && file.getData() != null) {
                    file.setSizeBytes(file.getData().length);
                }
            }
            uploadedFileRepository.saveAll(orphanFiles);
        }

        return defaultPage.getId();
    }

    private PageEntity createDefaultPage(String ownerId) {
        PageEntity page = new PageEntity();
        page.setId(DEFAULT_PAGE_ID);
        page.setOwnerId(ownerId);
        page.setSlug("main");
        page.setTitle("Nguyễn Kim Quang — Portfolio");
        page.setDescription("Portfolio lập trình viên Java Backend & Full-stack — bố cục Bento gọn gàng, song ngữ VI/EN.");
        page.setVisibility("PUBLIC");
        page.setPrimary(true);
        page.setTemplate(true);
        page.setHomeFeatured(true);
        page.setThemeJson("{\"category\":\"it-dev\",\"previewLayout\":\"bento\"}");
        page.setCreatedAt(Instant.now());
        page.setUpdatedAt(Instant.now());
        return pageRepository.save(page);
    }

    @Transactional
    public String getDefaultPageId() {
        return pageRepository.findFirstByHomeFeaturedTrue()
                .map(PageEntity::getId)
                .orElseGet(this::ensureSystemSeeded);
    }
}
