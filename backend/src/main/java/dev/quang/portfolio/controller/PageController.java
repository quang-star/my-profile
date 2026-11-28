package dev.quang.portfolio.controller;

import dev.quang.portfolio.dto.MultiTenantDtos.*;
import dev.quang.portfolio.dto.PortfolioDtos.ContactMessageDto;
import dev.quang.portfolio.dto.PortfolioDtos.ContactRequest;
import dev.quang.portfolio.security.AuthenticatedUser;
import dev.quang.portfolio.service.FeatureFlagsService;
import dev.quang.portfolio.service.HomeConfigService;
import dev.quang.portfolio.service.PageService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PageController {

    private final PageService pageService;
    private final HomeConfigService homeConfigService;
    private final FeatureFlagsService featureFlagsService;

    public PageController(
            PageService pageService,
            HomeConfigService homeConfigService,
            FeatureFlagsService featureFlagsService
    ) {
        this.pageService = pageService;
        this.homeConfigService = homeConfigService;
        this.featureFlagsService = featureFlagsService;
    }

    @GetMapping("/public/feature-flags")
    public FeatureFlagsDto getPublicFeatureFlags() {
        return featureFlagsService.getFlags();
    }

    @GetMapping("/public/home-config")
    public HomeConfigDto getPublicHomeConfig() {
        return homeConfigService.getConfig();
    }

    @GetMapping("/public/home")
    public PublicPageResponse getPublicHome(Authentication authentication) {
        return pageService.getPublicHome(AuthenticatedUser.optional(authentication));
    }

    @GetMapping("/public/u/{username}")
    public PublicPageResponse getPublicUserPrimary(
            @PathVariable String username,
            Authentication authentication
    ) {
        return pageService.getPublicUserPrimary(username, AuthenticatedUser.optional(authentication));
    }

    @GetMapping("/public/u/{username}/{slug}")
    public PublicPageResponse getPublicUserSlug(
            @PathVariable String username,
            @PathVariable String slug,
            Authentication authentication
    ) {
        return pageService.getPublicUserSlug(username, slug, AuthenticatedUser.optional(authentication));
    }

    @GetMapping("/public/templates")
    public List<TemplateSummaryDto> getPublicTemplates() {
        return pageService.getPublicTemplates();
    }

    @PostMapping("/public/pages/{pageId}/contact")
    @ResponseStatus(HttpStatus.CREATED)
    public ContactMessageDto submitPageContact(
            Authentication authentication,
            @PathVariable String pageId,
            @Valid @RequestBody ContactRequest request
    ) {
        FeatureFlagsDto flags = featureFlagsService.getFlags();
        featureFlagsService.requireFeature(flags.publicContactEnabled(), AuthenticatedUser.optional(authentication), "Public contact form");
        return pageService.submitPageContact(pageId, request);
    }

    @GetMapping("/my/pages")
    public List<PageSummaryDto> getMyPages(Authentication authentication) {
        return pageService.getMyPages(AuthenticatedUser.require(authentication));
    }

    @GetMapping("/my/pages/{pageId}")
    public PageSummaryDto getMyPage(Authentication authentication, @PathVariable String pageId) {
        return pageService.getMyPage(AuthenticatedUser.require(authentication), pageId);
    }

    @PostMapping("/my/pages")
    @ResponseStatus(HttpStatus.CREATED)
    public PageSummaryDto createPage(
            Authentication authentication,
            @Valid @RequestBody CreatePageRequest request
    ) {
        return pageService.createPage(AuthenticatedUser.require(authentication), request);
    }

    @PostMapping("/my/pages/{pageId}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    public PageSummaryDto duplicatePage(
            Authentication authentication,
            @PathVariable String pageId
    ) {
        return pageService.duplicatePage(AuthenticatedUser.require(authentication), pageId);
    }

    @PutMapping("/my/pages/{pageId}")
    public PageSummaryDto updatePage(
            Authentication authentication,
            @PathVariable String pageId,
            @Valid @RequestBody UpdatePageRequest request
    ) {
        return pageService.updatePage(AuthenticatedUser.require(authentication), pageId, request);
    }

    @DeleteMapping("/my/pages/{pageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePage(
            Authentication authentication,
            @PathVariable String pageId
    ) {
        pageService.deletePage(AuthenticatedUser.require(authentication), pageId);
    }

    @GetMapping("/my/pages/{pageId}/messages")
    public List<ContactMessageDto> getPageMessages(
            Authentication authentication,
            @PathVariable String pageId
    ) {
        return pageService.getPageMessages(AuthenticatedUser.require(authentication), pageId);
    }

    @PatchMapping("/my/messages/{id}/read")
    public ContactMessageDto markMessageRead(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Boolean> body
    ) {
        boolean read = body == null || body.getOrDefault("read", true);
        return pageService.markMessageRead(AuthenticatedUser.require(authentication), id, read);
    }

    @DeleteMapping("/my/messages/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMessage(
            Authentication authentication,
            @PathVariable Long id
    ) {
        pageService.deleteMessage(AuthenticatedUser.require(authentication), id);
    }
}
