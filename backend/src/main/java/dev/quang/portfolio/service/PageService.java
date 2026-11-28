package dev.quang.portfolio.service;

import dev.quang.portfolio.dto.MultiTenantDtos.*;
import dev.quang.portfolio.dto.NodeDtos.ContentNodeDto;
import dev.quang.portfolio.dto.PortfolioDtos.ContactMessageDto;
import dev.quang.portfolio.dto.PortfolioDtos.ContactRequest;
import dev.quang.portfolio.entity.ContactMessageEntity;
import dev.quang.portfolio.entity.ContentNodeEntity;
import dev.quang.portfolio.entity.PageEntity;
import dev.quang.portfolio.entity.UserEntity;
import dev.quang.portfolio.repository.ContactMessageRepository;
import dev.quang.portfolio.repository.ContentNodeRepository;
import dev.quang.portfolio.repository.PageRepository;
import dev.quang.portfolio.repository.UserRepository;
import dev.quang.portfolio.security.AuthenticatedUser;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PageService {

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9-]{1,64}$");
    private static final Set<String> VALID_VISIBILITIES = Set.of("DRAFT", "PUBLIC", "UNLISTED");
    private static final Set<String> RESERVED_SLUGS = Set.of("edit");

    private final PageRepository pageRepository;
    private final UserRepository userRepository;
    private final ContentNodeRepository contentNodeRepository;
    private final ContactMessageRepository contactMessageRepository;
    private final NodeService nodeService;
    private final SystemDataSeeder systemDataSeeder;
    private final ViewCounterService viewCounterService;
    private final FeatureFlagsService featureFlagsService;

    public PageService(
            PageRepository pageRepository,
            UserRepository userRepository,
            ContentNodeRepository contentNodeRepository,
            ContactMessageRepository contactMessageRepository,
            NodeService nodeService,
            SystemDataSeeder systemDataSeeder,
            ViewCounterService viewCounterService,
            FeatureFlagsService featureFlagsService
    ) {
        this.pageRepository = pageRepository;
        this.userRepository = userRepository;
        this.contentNodeRepository = contentNodeRepository;
        this.contactMessageRepository = contactMessageRepository;
        this.nodeService = nodeService;
        this.systemDataSeeder = systemDataSeeder;
        this.viewCounterService = viewCounterService;
        this.featureFlagsService = featureFlagsService;
    }

    @Transactional(readOnly = true)
    public PublicPageResponse getPublicHome() {
        return getPublicHome(null);
    }

    @Transactional(readOnly = true)
    public PublicPageResponse getPublicHome(AuthenticatedUser currentUser) {
        PageEntity page = pageRepository.findFirstByHomeFeaturedTrueAndVisibilityIgnoreCase("PUBLIC")
                .or(() -> userRepository.findFirstByRoleOrderByCreatedAtAsc("SUPER_ADMIN")
                        .flatMap(admin -> pageRepository.findFirstByOwnerIdAndPrimaryTrueAndVisibilityIgnoreCase(admin.getId(), "PUBLIC")))
                .or(() -> pageRepository.findById(systemDataSeeder.getDefaultPageId())
                        .filter(p -> "PUBLIC".equalsIgnoreCase(p.getVisibility())))
                .orElseThrow(() -> new NoSuchElementException("Home page not found"));

        UserEntity owner = userRepository.findById(page.getOwnerId())
                .filter(u -> "ACTIVE".equalsIgnoreCase(u.getStatus()))
                .orElseThrow(() -> new NoSuchElementException("Page owner not found"));

        List<ContentNodeDto> nodes = nodeService.getTreeForPage(page.getId());
        return new PublicPageResponse(toSummary(page, 0, 0L), toOwner(owner), nodes);
    }

    @Transactional(readOnly = true)
    public PublicPageResponse getPublicUserPrimary(String username, AuthenticatedUser currentUser) {
        UserEntity owner = findActiveUserByUsername(username);
        PageEntity page = pageRepository.findFirstByOwnerIdAndPrimaryTrue(owner.getId())
                .orElseThrow(() -> new NoSuchElementException("Public page not found for user: " + username));

        boolean isPublic = "PUBLIC".equalsIgnoreCase(page.getVisibility());
        boolean canPreview = currentUser != null
                && (currentUser.id().equals(owner.getId()) || currentUser.isSuperAdmin());
        if (!isPublic && !canPreview) {
            throw new NoSuchElementException("Public page not found for user: " + username);
        }

        boolean incremented = isPublic && viewCounterService.recordPublicView(page, currentUser);

        List<ContentNodeDto> nodes = nodeService.getTreeForPage(page.getId());
        return new PublicPageResponse(toSummary(page, 0, incremented ? 1L : 0L), toOwner(owner), nodes);
    }

    @Transactional(readOnly = true)
    public PublicPageResponse getPublicUserSlug(String username, String slug, AuthenticatedUser currentUser) {
        UserEntity owner = findActiveUserByUsername(username);
        PageEntity page = pageRepository.findByOwnerIdAndSlug(owner.getId(), slug.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new NoSuchElementException("Page not found"));

        boolean incremented = false;
        if ("DRAFT".equalsIgnoreCase(page.getVisibility())) {
            boolean canPreview = currentUser != null
                    && (currentUser.id().equals(owner.getId()) || currentUser.isSuperAdmin());
            if (!canPreview) {
                throw new NoSuchElementException("Page not found");
            }
        } else {
            incremented = viewCounterService.recordPublicView(page, currentUser);
        }

        List<ContentNodeDto> nodes = nodeService.getTreeForPage(page.getId());
        return new PublicPageResponse(toSummary(page, 0, incremented ? 1L : 0L), toOwner(owner), nodes);
    }

    @Transactional(readOnly = true)
    public List<TemplateSummaryDto> getPublicTemplates() {
        List<PageEntity> templates = pageRepository.findByTemplateTrueAndVisibilityIgnoreCaseOrderByUpdatedAtDesc("PUBLIC");
        Map<String, UserEntity> owners = new HashMap<>();
        List<TemplateSummaryDto> result = new ArrayList<>();
        for (PageEntity page : templates) {
            UserEntity owner = owners.computeIfAbsent(
                    page.getOwnerId(),
                    id -> userRepository.findById(id).orElse(null)
            );
            if (owner == null || !"ACTIVE".equalsIgnoreCase(owner.getStatus())) {
                continue;
            }
            result.add(new TemplateSummaryDto(
                    page.getId(),
                    page.getSlug(),
                    page.getTitle(),
                    page.getDescription(),
                    owner.getUsername(),
                    page.getThemeJson(),
                    page.getUpdatedAt()
            ));
        }
        return result;
    }

    @Transactional
    public ContactMessageDto submitPageContact(String pageId, ContactRequest request) {
        if (request.website() != null && !request.website().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid contact submission");
        }
        PageEntity page = pageRepository.findById(pageId)
                .orElseThrow(() -> new NoSuchElementException("Page not found: " + pageId));
        if ("DRAFT".equalsIgnoreCase(page.getVisibility())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot submit contact to a draft page");
        }
        ContactMessageEntity entity = new ContactMessageEntity();
        entity.setPageId(page.getId());
        entity.setName(request.name().trim());
        entity.setEmail(request.email().trim());
        String subject = request.subject() == null || request.subject().isBlank()
                ? "[Portfolio] Message from " + request.name().trim()
                : request.subject().trim();
        entity.setSubject(subject);
        entity.setMessage(request.message().trim());
        entity.setRead(false);
        entity.setCreatedAt(Instant.now());
        return toMessageDto(contactMessageRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<PageSummaryDto> getMyPages(AuthenticatedUser currentUser) {
        return pageRepository.findByOwnerIdOrderByUpdatedAtDesc(currentUser.id()).stream()
                .map(page -> toSummary(page, contactMessageRepository.countByPageIdAndReadFalse(page.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageSummaryDto getMyPage(AuthenticatedUser currentUser, String pageId) {
        PageEntity page = requirePageOwnerOrSuperAdmin(pageId, currentUser);
        long unread = contactMessageRepository.countByPageIdAndReadFalse(page.getId());
        return toSummary(page, unread);
    }

    @Transactional
    public PageSummaryDto createPage(AuthenticatedUser currentUser, CreatePageRequest request) {
        UserEntity user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        enforcePageQuota(user);

        String slug = normalizeAndValidateSlug(request.slug());
        if (pageRepository.existsByOwnerIdAndSlug(user.getId(), slug)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug already exists on your account: " + slug);
        }

        String visibility = normalizeVisibility(request.visibility(), "DRAFT");
        boolean isFirstPage = pageRepository.countByOwnerId(user.getId()) == 0;
        Instant now = Instant.now();

        PageEntity page = new PageEntity();
        page.setId("page-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        page.setOwnerId(user.getId());
        page.setSlug(slug);
        page.setTitle(request.title().trim());
        page.setDescription(request.description() != null ? request.description().trim() : "");
        page.setVisibility(visibility);
        page.setPrimary(isFirstPage);
        page.setTemplate(false);
        page.setHomeFeatured(false);
        page.setThemeJson("{}");
        page.setViewCount(0L);
        page.setCreatedAt(now);
        page.setUpdatedAt(now);
        pageRepository.save(page);

        if (request.fromTemplateId() != null && !request.fromTemplateId().isBlank()) {
            FeatureFlagsDto flags = featureFlagsService.getFlags();
            featureFlagsService.requireFeature(flags.publicTemplatesEnabled(), currentUser, "Templates");
            PageEntity source = pageRepository.findById(request.fromTemplateId().trim())
                    .orElseThrow(() -> new NoSuchElementException("Template page not found"));
            boolean isPublicTemplate = source.isTemplate() && "PUBLIC".equalsIgnoreCase(source.getVisibility());
            if (!isPublicTemplate && !source.getOwnerId().equals(user.getId()) && !currentUser.isSuperAdmin()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to use this page as a template");
            }
            cloneNodes(source.getId(), page.getId());
        } else {
            createStarterNodes(page.getId(), page.getTitle());
        }

        return toSummary(page, 0);
    }

    @Transactional
    public PageSummaryDto duplicatePage(AuthenticatedUser currentUser, String pageId) {
        PageEntity source = requirePageOwnerOrSuperAdmin(pageId, currentUser);
        UserEntity user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        enforcePageQuota(user);

        String copySlug = generateUniqueCopySlug(user.getId(), source.getSlug());
        Instant now = Instant.now();

        PageEntity copy = new PageEntity();
        copy.setId("page-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        copy.setOwnerId(user.getId());
        copy.setSlug(copySlug);
        copy.setTitle((source.getTitle() + " (Copy)").substring(0, Math.min(160, source.getTitle().length() + 7)));
        copy.setDescription(source.getDescription());
        copy.setVisibility("DRAFT");
        copy.setPrimary(false);
        copy.setTemplate(false);
        copy.setHomeFeatured(false);
        copy.setCvPdfUrl(source.getCvPdfUrl());
        copy.setThemeJson(source.getThemeJson());
        copy.setViewCount(0L);
        copy.setCreatedAt(now);
        copy.setUpdatedAt(now);
        pageRepository.save(copy);

        cloneNodes(source.getId(), copy.getId());
        return toSummary(copy, 0);
    }

    @Transactional
    public PageSummaryDto updatePage(AuthenticatedUser currentUser, String pageId, UpdatePageRequest request) {
        PageEntity page = requirePageOwnerOrSuperAdmin(pageId, currentUser);

        String slug = normalizeAndValidateSlug(request.slug());
        if (!slug.equals(page.getSlug()) && pageRepository.existsByOwnerIdAndSlug(page.getOwnerId(), slug)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug already exists on this account: " + slug);
        }

        page.setTitle(request.title().trim());
        page.setSlug(slug);
        page.setDescription(request.description() != null ? request.description().trim() : "");
        if (request.visibility() != null && !request.visibility().isBlank()) {
            page.setVisibility(normalizeVisibility(request.visibility(), page.getVisibility()));
        }
        String cvPdfUrl = request.cvPdfUrl() != null && !request.cvPdfUrl().isBlank() ? request.cvPdfUrl().trim() : null;
        if (cvPdfUrl != null && !cvPdfUrl.matches("(?i)^(https?://|/api/files/).+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CV PDF URL must start with http://, https://, or /api/files/");
        }
        page.setCvPdfUrl(cvPdfUrl);
        if (request.themeJson() != null && !request.themeJson().isBlank()) {
            page.setThemeJson(request.themeJson());
        }
        if (Boolean.TRUE.equals(request.isPrimary())) {
            pageRepository.clearPrimaryForOwnerExcept(page.getOwnerId(), page.getId());
            page.setPrimary(true);
        } else if (Boolean.FALSE.equals(request.isPrimary()) && page.isPrimary()) {
            List<PageEntity> others = pageRepository.findByOwnerIdOrderByUpdatedAtDesc(page.getOwnerId()).stream()
                    .filter(p -> !p.getId().equals(page.getId()))
                    .toList();
            if (others.isEmpty()) {
                page.setPrimary(true);
            } else {
                page.setPrimary(false);
                PageEntity replacement = others.stream()
                        .filter(p -> "PUBLIC".equalsIgnoreCase(p.getVisibility()))
                        .findFirst()
                        .orElse(others.get(0));
                replacement.setPrimary(true);
                pageRepository.save(replacement);
            }
        }
        page.setUpdatedAt(Instant.now());
        pageRepository.save(page);

        long unread = contactMessageRepository.countByPageIdAndReadFalse(page.getId());
        return toSummary(page, unread);
    }

    @Transactional
    public void deletePage(AuthenticatedUser currentUser, String pageId) {
        PageEntity page = requirePageOwnerOrSuperAdmin(pageId, currentUser);
        boolean wasPrimary = page.isPrimary();
        String ownerId = page.getOwnerId();
        contactMessageRepository.deleteAllByPageId(page.getId());
        contentNodeRepository.clearParentIdsByPageId(page.getId());
        contentNodeRepository.deleteAllByPageId(page.getId());
        pageRepository.delete(page);
        pageRepository.flush();

        if (wasPrimary) {
            List<PageEntity> remaining = pageRepository.findByOwnerIdOrderByUpdatedAtDesc(ownerId);
            if (!remaining.isEmpty()) {
                PageEntity nextPrimary = remaining.stream()
                        .filter(p -> "PUBLIC".equalsIgnoreCase(p.getVisibility()))
                        .findFirst()
                        .orElse(remaining.get(0));
                nextPrimary.setPrimary(true);
                pageRepository.save(nextPrimary);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<ContactMessageDto> getPageMessages(AuthenticatedUser currentUser, String pageId) {
        requirePageOwnerOrSuperAdmin(pageId, currentUser);
        return contactMessageRepository.findByPageIdOrderByCreatedAtDesc(pageId).stream()
                .map(this::toMessageDto)
                .toList();
    }

    @Transactional
    public ContactMessageDto markMessageRead(AuthenticatedUser currentUser, Long messageId, boolean read) {
        ContactMessageEntity message = contactMessageRepository.findById(messageId)
                .orElseThrow(() -> new NoSuchElementException("Message not found: " + messageId));
        requireMessageAccess(message, currentUser);
        message.setRead(read);
        return toMessageDto(contactMessageRepository.save(message));
    }

    @Transactional
    public void deleteMessage(AuthenticatedUser currentUser, Long messageId) {
        ContactMessageEntity message = contactMessageRepository.findById(messageId)
                .orElseThrow(() -> new NoSuchElementException("Message not found: " + messageId));
        requireMessageAccess(message, currentUser);
        contactMessageRepository.delete(message);
    }

    private void requireMessageAccess(ContactMessageEntity message, AuthenticatedUser currentUser) {
        if (currentUser.isSuperAdmin()) {
            return;
        }
        if (message.getPageId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        requirePageOwnerOrSuperAdmin(message.getPageId(), currentUser);
    }

    private PageEntity requirePageOwnerOrSuperAdmin(String pageId, AuthenticatedUser currentUser) {
        PageEntity page = pageRepository.findById(pageId)
                .orElseThrow(() -> new NoSuchElementException("Page not found: " + pageId));
        if (!currentUser.isSuperAdmin() && !page.getOwnerId().equals(currentUser.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this page");
        }
        return page;
    }

    private UserEntity findActiveUserByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username.trim())
                .filter(u -> "ACTIVE".equalsIgnoreCase(u.getStatus()))
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));
    }

    private void enforcePageQuota(UserEntity user) {
        if ("SUPER_ADMIN".equalsIgnoreCase(user.getRole()) || user.getMaxPages() < 0) {
            return;
        }
        long currentCount = pageRepository.countByOwnerId(user.getId());
        if (currentCount >= user.getMaxPages()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Page limit reached (" + user.getMaxPages() + " pages max)"
            );
        }
    }

    private String normalizeAndValidateSlug(String rawSlug) {
        String slug = rawSlug == null ? "" : rawSlug.trim().toLowerCase(Locale.ROOT);
        if (!SLUG_PATTERN.matcher(slug).matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Slug must be 1-64 characters of lowercase letters, digits, and hyphens"
            );
        }
        if (RESERVED_SLUGS.contains(slug)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This slug is reserved: " + slug
            );
        }
        return slug;
    }

    private String normalizeVisibility(String rawVisibility, String defaultVisibility) {
        if (rawVisibility == null || rawVisibility.isBlank()) {
            return defaultVisibility;
        }
        String vis = rawVisibility.trim().toUpperCase(Locale.ROOT);
        if (!VALID_VISIBILITIES.contains(vis)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid visibility: " + rawVisibility);
        }
        return vis;
    }

    private String generateUniqueCopySlug(String ownerId, String baseSlug) {
        String prefix = baseSlug.length() > 54 ? baseSlug.substring(0, 54) : baseSlug;
        String candidate = prefix + "-copy";
        int suffix = 2;
        while (pageRepository.existsByOwnerIdAndSlug(ownerId, candidate)) {
            candidate = prefix + "-copy-" + suffix++;
        }
        return candidate;
    }

    private void cloneNodes(String sourcePageId, String targetPageId) {
        List<ContentNodeEntity> sourceNodes = contentNodeRepository.findByPageIdOrderBySortOrderAscIdAsc(sourcePageId);
        if (sourceNodes.isEmpty()) {
            return;
        }

        Map<String, String> idMap = new HashMap<>(sourceNodes.size());
        Map<String, List<ContentNodeEntity>> byParent = new LinkedHashMap<>();
        for (ContentNodeEntity node : sourceNodes) {
            idMap.put(node.getId(), "n-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
            String key = node.getParentId() == null ? "__ROOT__" : node.getParentId();
            byParent.computeIfAbsent(key, k -> new ArrayList<>()).add(node);
        }

        List<ContentNodeEntity> orderedClones = new ArrayList<>(sourceNodes.size());
        Deque<String> queue = new ArrayDeque<>();
        queue.add("__ROOT__");
        Set<String> visited = new HashSet<>();
        OffsetDateTime now = OffsetDateTime.now();

        while (!queue.isEmpty()) {
            String parentKey = queue.poll();
            for (ContentNodeEntity src : byParent.getOrDefault(parentKey, List.of())) {
                if (!visited.add(src.getId())) continue;
                ContentNodeEntity clone = new ContentNodeEntity();
                clone.setId(idMap.get(src.getId()));
                clone.setPageId(targetPageId);
                clone.setParentId(src.getParentId() != null ? idMap.get(src.getParentId()) : null);
                clone.setTag(src.getTag());
                clone.setSortOrder(src.getSortOrder());
                clone.setAttrsJson(src.getAttrsJson());
                clone.setContentVi(src.getContentVi());
                clone.setContentEn(src.getContentEn());
                clone.setUpdatedAt(now);
                orderedClones.add(clone);
                queue.add(src.getId());
            }
        }

        contentNodeRepository.saveAll(orderedClones);
        contentNodeRepository.flush();
    }

    private void createStarterNodes(String pageId, String title) {
        OffsetDateTime now = OffsetDateTime.now();
        String sectionId = "n-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        ContentNodeEntity section = new ContentNodeEntity();
        section.setId(sectionId);
        section.setPageId(pageId);
        section.setParentId(null);
        section.setTag("section");
        section.setSortOrder(0);
        section.setAttrsJson("{\"anchor\":\"about\",\"padding\":\"md\",\"bgColor\":\"transparent\"}");
        section.setContentVi("");
        section.setContentEn("");
        section.setUpdatedAt(now);
        contentNodeRepository.saveAndFlush(section);

        String safeTitle = title
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
        ContentNodeEntity heading = new ContentNodeEntity();
        heading.setId("n-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        heading.setPageId(pageId);
        heading.setParentId(sectionId);
        heading.setTag("heading");
        heading.setSortOrder(0);
        heading.setAttrsJson("{\"level\":\"h1\",\"fontSize\":\"3xl\",\"textColor\":\"#18181b\"}");
        heading.setContentVi(safeTitle);
        heading.setContentEn(safeTitle);
        heading.setUpdatedAt(now);
        contentNodeRepository.saveAndFlush(heading);
    }

    private PageSummaryDto toSummary(PageEntity page, long unreadMessagesCount) {
        return toSummary(page, unreadMessagesCount, 0L);
    }

    private PageSummaryDto toSummary(PageEntity page, long unreadMessagesCount, long viewCountDelta) {
        return new PageSummaryDto(
                page.getId(),
                page.getOwnerId(),
                page.getSlug(),
                page.getTitle(),
                page.getDescription(),
                page.getVisibility(),
                page.isPrimary(),
                page.isTemplate(),
                page.isHomeFeatured(),
                page.getCvPdfUrl(),
                page.getThemeJson(),
                page.getViewCount() + viewCountDelta,
                unreadMessagesCount,
                page.getCreatedAt(),
                page.getUpdatedAt()
        );
    }

    private PageOwnerDto toOwner(UserEntity user) {
        return new PageOwnerDto(user.getUsername(), user.getDisplayName(), user.getRole(), user.getAvatarUrl());
    }

    private ContactMessageDto toMessageDto(ContactMessageEntity entity) {
        return new ContactMessageDto(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getSubject(),
                entity.getMessage(),
                entity.isRead(),
                entity.getCreatedAt()
        );
    }
}
