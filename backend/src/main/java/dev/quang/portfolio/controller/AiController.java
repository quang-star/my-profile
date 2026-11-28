package dev.quang.portfolio.controller;

import dev.quang.portfolio.dto.AiDtos.*;
import dev.quang.portfolio.dto.MultiTenantDtos.FeatureFlagsDto;
import dev.quang.portfolio.security.AuthenticatedUser;
import dev.quang.portfolio.service.AiCvParserService;
import dev.quang.portfolio.service.FeatureFlagsService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiCvParserService aiCvParserService;
    private final FeatureFlagsService featureFlagsService;

    public AiController(AiCvParserService aiCvParserService, FeatureFlagsService featureFlagsService) {
        this.aiCvParserService = aiCvParserService;
        this.featureFlagsService = featureFlagsService;
    }

    @GetMapping("/status")
    public AiStatusDto getStatus(Authentication authentication) {
        AuthenticatedUser.require(authentication);
        return aiCvParserService.getPublicStatus();
    }

    @PostMapping("/parse-cv")
    public ParseCvResponse parseCv(
            Authentication authentication,
            @RequestBody ParseCvRequest request
    ) {
        AuthenticatedUser currentUser = AuthenticatedUser.require(authentication);
        FeatureFlagsDto flags = featureFlagsService.getFlags();
        featureFlagsService.requireFeature(flags.cvImportEnabled(), currentUser, "CV Import");
        return aiCvParserService.parseCv(request);
    }

    @PostMapping("/translate-doc")
    public TranslateDocResponse translateDoc(
            Authentication authentication,
            @RequestBody TranslateDocRequest request
    ) {
        AuthenticatedUser currentUser = AuthenticatedUser.require(authentication);
        FeatureFlagsDto flags = featureFlagsService.getFlags();
        featureFlagsService.requireFeature(flags.aiTranslateEnabled(), currentUser, "AI Translate");
        return aiCvParserService.translateDoc(request);
    }

    @PostMapping("/build-page")
    public BuildPageResponse buildPage(
            Authentication authentication,
            @RequestBody BuildPageRequest request
    ) {
        AuthenticatedUser currentUser = AuthenticatedUser.require(authentication);
        FeatureFlagsDto flags = featureFlagsService.getFlags();
        featureFlagsService.requireFeature(flags.aiCopilotEnabled(), currentUser, "AI Copilot");
        return aiCvParserService.buildPage(request);
    }
}
