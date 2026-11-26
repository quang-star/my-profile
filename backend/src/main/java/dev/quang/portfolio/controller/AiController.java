package dev.quang.portfolio.controller;

import dev.quang.portfolio.dto.AiDtos.*;
import dev.quang.portfolio.security.AuthenticatedUser;
import dev.quang.portfolio.service.AiCvParserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiCvParserService aiCvParserService;

    public AiController(AiCvParserService aiCvParserService) {
        this.aiCvParserService = aiCvParserService;
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
        AuthenticatedUser.require(authentication);
        return aiCvParserService.parseCv(request);
    }

    @PostMapping("/translate-doc")
    public TranslateDocResponse translateDoc(
            Authentication authentication,
            @RequestBody TranslateDocRequest request
    ) {
        AuthenticatedUser.require(authentication);
        return aiCvParserService.translateDoc(request);
    }

    @PostMapping("/build-page")
    public BuildPageResponse buildPage(
            Authentication authentication,
            @RequestBody BuildPageRequest request
    ) {
        AuthenticatedUser.require(authentication);
        return aiCvParserService.buildPage(request);
    }
}
