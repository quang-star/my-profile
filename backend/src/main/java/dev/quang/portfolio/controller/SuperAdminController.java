package dev.quang.portfolio.controller;

import dev.quang.portfolio.dto.AiDtos.*;
import dev.quang.portfolio.dto.MultiTenantDtos.*;
import dev.quang.portfolio.entity.PageEntity;
import dev.quang.portfolio.entity.UserEntity;
import dev.quang.portfolio.repository.PageRepository;
import dev.quang.portfolio.repository.UploadedFileRepository;
import dev.quang.portfolio.repository.UserRepository;
import dev.quang.portfolio.security.AuthenticatedUser;
import dev.quang.portfolio.service.AiCvParserService;
import dev.quang.portfolio.service.FeatureFlagsService;
import dev.quang.portfolio.service.HomeConfigService;
import dev.quang.portfolio.service.PageService;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/super-admin")
public class SuperAdminController {

    private static final Set<String> VALID_STATUSES = Set.of("ACTIVE", "BANNED");
    private static final Set<String> VALID_ROLES = Set.of("USER", "SUPER_ADMIN");
    private static final Set<String> VALID_VISIBILITIES = Set.of("DRAFT", "PUBLIC", "UNLISTED");

    private final UserRepository userRepository;
    private final PageRepository pageRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final PageService pageService;
    private final AiCvParserService aiCvParserService;
    private final HomeConfigService homeConfigService;
    private final FeatureFlagsService featureFlagsService;

    public SuperAdminController(
            UserRepository userRepository,
            PageRepository pageRepository,
            UploadedFileRepository uploadedFileRepository,
            PageService pageService,
            AiCvParserService aiCvParserService,
            HomeConfigService homeConfigService,
            FeatureFlagsService featureFlagsService
    ) {
        this.userRepository = userRepository;
        this.pageRepository = pageRepository;
        this.uploadedFileRepository = uploadedFileRepository;
        this.pageService = pageService;
        this.aiCvParserService = aiCvParserService;
        this.homeConfigService = homeConfigService;
        this.featureFlagsService = featureFlagsService;
    }

    @GetMapping("/overview")
    @Transactional(readOnly = true)
    public AdminOverviewDto overview() {
        List<UserEntity> users = userRepository.findAll();
        long activeUsers = users.stream().filter(u -> "ACTIVE".equalsIgnoreCase(u.getStatus())).count();
        long bannedUsers = users.stream().filter(u -> "BANNED".equalsIgnoreCase(u.getStatus())).count();
        long totalPages = pageRepository.count();
        long publicPages = pageRepository.countByVisibility("PUBLIC");
        long templatePages = pageRepository.countByTemplateTrue();
        long totalStorageBytes = uploadedFileRepository.sumAllSizeBytes();

        return new AdminOverviewDto(
                users.size(),
                activeUsers,
                bannedUsers,
                totalPages,
                publicPages,
                templatePages,
                totalStorageBytes
        );
    }

    @GetMapping("/users")
    @Transactional(readOnly = true)
    public List<AdminUserDto> listUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toAdminUserDto)
                .toList();
    }

    @PatchMapping("/users/{userId}")
    @Transactional
    public AdminUserDto updateUser(
            Authentication authentication,
            @PathVariable String userId,
            @RequestBody AdminUpdateUserRequest request
    ) {
        AuthenticatedUser current = AuthenticatedUser.require(authentication);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));

        if (request.status() != null && !request.status().isBlank()) {
            String nextStatus = request.status().trim().toUpperCase(Locale.ROOT);
            if (!VALID_STATUSES.contains(nextStatus)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + request.status());
            }
            if (current.id().equals(user.getId()) && !"ACTIVE".equals(nextStatus)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot ban your own account");
            }
            user.setStatus(nextStatus);
        }

        if (request.role() != null && !request.role().isBlank()) {
            String nextRole = request.role().trim().toUpperCase(Locale.ROOT);
            if (!VALID_ROLES.contains(nextRole)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + request.role());
            }
            if (current.id().equals(user.getId()) && !"SUPER_ADMIN".equals(nextRole)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot demote your own account");
            }
            user.setRole(nextRole);
        }

        if (request.maxPages() != null) {
            if (request.maxPages() < -1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "maxPages must be >= -1");
            }
            user.setMaxPages(request.maxPages());
        }

        if (request.maxStorageBytes() != null) {
            if (request.maxStorageBytes() < -1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "maxStorageBytes must be >= -1");
            }
            user.setMaxStorageBytes(request.maxStorageBytes());
        }

        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        return toAdminUserDto(user);
    }

    @GetMapping("/pages")
    @Transactional(readOnly = true)
    public List<AdminPageDto> listPages(
            @RequestParam(required = false) String visibility,
            @RequestParam(required = false) Boolean template,
            @RequestParam(required = false) String username
    ) {
        Map<String, String> usernames = new HashMap<>();
        String visFilter = visibility != null && !visibility.isBlank() ? visibility.trim().toUpperCase(Locale.ROOT) : null;
        String userFilter = username != null && !username.isBlank() ? username.trim().toLowerCase(Locale.ROOT) : null;

        return pageRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(p -> visFilter == null || visFilter.equalsIgnoreCase(p.getVisibility()))
                .filter(p -> template == null || template.booleanValue() == p.isTemplate())
                .map(p -> {
                    String ownerUsername = usernames.computeIfAbsent(
                            p.getOwnerId(),
                            id -> userRepository.findById(id).map(UserEntity::getUsername).orElse("unknown")
                    );
                    return new AdminPageDto(
                            p.getId(),
                            p.getOwnerId(),
                            ownerUsername,
                            p.getSlug(),
                            p.getTitle(),
                            p.getVisibility(),
                            p.isPrimary(),
                            p.isTemplate(),
                            p.isHomeFeatured(),
                            p.getViewCount(),
                            p.getUpdatedAt()
                    );
                })
                .filter(dto -> userFilter == null || dto.ownerUsername().toLowerCase(Locale.ROOT).contains(userFilter))
                .toList();
    }

    @PatchMapping("/pages/{pageId}")
    @Transactional
    public AdminPageDto updatePage(
            @PathVariable String pageId,
            @RequestBody AdminUpdatePageRequest request
    ) {
        PageEntity page = pageRepository.findById(pageId)
                .orElseThrow(() -> new NoSuchElementException("Page not found: " + pageId));

        if (request.isTemplate() != null) {
            page.setTemplate(request.isTemplate());
        }
        if (request.isHomeFeatured() != null) {
            if (Boolean.TRUE.equals(request.isHomeFeatured())) {
                pageRepository.clearHomeFeaturedExcept(page.getId());
                page.setHomeFeatured(true);
            } else {
                page.setHomeFeatured(false);
            }
        }
        if (request.visibility() != null && !request.visibility().isBlank()) {
            String vis = request.visibility().trim().toUpperCase(Locale.ROOT);
            if (!VALID_VISIBILITIES.contains(vis)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid visibility: " + request.visibility());
            }
            page.setVisibility(vis);
        }

        page.setUpdatedAt(Instant.now());
        pageRepository.save(page);

        String ownerUsername = userRepository.findById(page.getOwnerId())
                .map(UserEntity::getUsername)
                .orElse("unknown");
        return new AdminPageDto(
                page.getId(),
                page.getOwnerId(),
                ownerUsername,
                page.getSlug(),
                page.getTitle(),
                page.getVisibility(),
                page.isPrimary(),
                page.isTemplate(),
                page.isHomeFeatured(),
                page.getViewCount(),
                page.getUpdatedAt()
        );
    }

    @DeleteMapping("/pages/{pageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePage(Authentication authentication, @PathVariable String pageId) {
        pageService.deletePage(AuthenticatedUser.require(authentication), pageId);
    }

    @GetMapping("/ai-config")
    public AdminAiConfigDto getAiConfig() {
        return aiCvParserService.getAdminConfig();
    }

    @PutMapping("/ai-config")
    public AdminAiConfigDto updateAiConfig(@RequestBody UpdateAiConfigRequest request) {
        return aiCvParserService.updateAdminConfig(request);
    }

    @PostMapping("/ai-config/test")
    public TestAiConfigResponse testAiConfig(@RequestBody(required = false) TestAiConfigRequest request) {
        return aiCvParserService.testAndFetchModels(request);
    }

    @GetMapping("/home-config")
    public HomeConfigDto getHomeConfig() {
        return homeConfigService.getConfig();
    }

    @PutMapping("/home-config")
    public HomeConfigDto updateHomeConfig(@RequestBody HomeConfigDto request) {
        return homeConfigService.updateConfig(request);
    }

    @GetMapping("/feature-flags")
    public FeatureFlagsDto getFeatureFlags() {
        return featureFlagsService.getFlags();
    }

    @PutMapping("/feature-flags")
    public FeatureFlagsDto updateFeatureFlags(@RequestBody FeatureFlagsDto request) {
        return featureFlagsService.updateFlags(request);
    }

    private AdminUserDto toAdminUserDto(UserEntity user) {
        long usedPages = pageRepository.countByOwnerId(user.getId());
        long usedStorage = uploadedFileRepository.sumSizeBytesByOwnerId(user.getId());
        return new AdminUserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.getStatus(),
                user.getMaxPages(),
                user.getMaxStorageBytes(),
                usedPages,
                usedStorage,
                user.getCreatedAt()
        );
    }
}
