package dev.quang.portfolio.controller;

import dev.quang.portfolio.dto.PortfolioDtos.*;
import dev.quang.portfolio.service.PortfolioService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminPortfolioController {

    private final PortfolioService portfolioService;

    public AdminPortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @PutMapping("/profile")
    public PortfolioResponse updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return portfolioService.updateProfile(request);
    }

    @PostMapping("/skills")
    @ResponseStatus(HttpStatus.CREATED)
    public SkillGroupDto createSkill(@Valid @RequestBody SkillGroupDto dto) {
        return portfolioService.saveSkill(null, dto);
    }

    @PutMapping("/skills/{id}")
    public SkillGroupDto updateSkill(@PathVariable String id, @Valid @RequestBody SkillGroupDto dto) {
        return portfolioService.saveSkill(id, dto);
    }

    @DeleteMapping("/skills/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSkill(@PathVariable String id) {
        portfolioService.deleteSkill(id);
    }

    @PostMapping("/education")
    @ResponseStatus(HttpStatus.CREATED)
    public EducationDto createEducation(@Valid @RequestBody EducationDto dto) {
        return portfolioService.saveEducation(null, dto);
    }

    @PutMapping("/education/{id}")
    public EducationDto updateEducation(@PathVariable String id, @Valid @RequestBody EducationDto dto) {
        return portfolioService.saveEducation(id, dto);
    }

    @DeleteMapping("/education/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEducation(@PathVariable String id) {
        portfolioService.deleteEducation(id);
    }

    @PostMapping("/experiences")
    @ResponseStatus(HttpStatus.CREATED)
    public ExperienceDto createExperience(@Valid @RequestBody ExperienceDto dto) {
        return portfolioService.saveExperience(null, dto);
    }

    @PutMapping("/experiences/{id}")
    public ExperienceDto updateExperience(@PathVariable String id, @Valid @RequestBody ExperienceDto dto) {
        return portfolioService.saveExperience(id, dto);
    }

    @DeleteMapping("/experiences/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExperience(@PathVariable String id) {
        portfolioService.deleteExperience(id);
    }

    @PostMapping("/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDto createProject(@Valid @RequestBody ProjectDto dto) {
        return portfolioService.saveProject(null, dto);
    }

    @PutMapping("/projects/{id}")
    public ProjectDto updateProject(@PathVariable String id, @Valid @RequestBody ProjectDto dto) {
        return portfolioService.saveProject(id, dto);
    }

    @DeleteMapping("/projects/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(@PathVariable String id) {
        portfolioService.deleteProject(id);
    }

    @GetMapping("/messages")
    public List<ContactMessageDto> getMessages() {
        return portfolioService.getAllMessages();
    }

    @PatchMapping("/messages/{id}/read")
    public ContactMessageDto markMessageRead(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Boolean> body
    ) {
        boolean read = body == null || body.getOrDefault("read", true);
        return portfolioService.markMessageRead(id, read);
    }

    @DeleteMapping("/messages/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMessage(@PathVariable Long id) {
        portfolioService.deleteMessage(id);
    }
}
