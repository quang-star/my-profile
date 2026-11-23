package dev.quang.portfolio.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.quang.portfolio.dto.PortfolioDtos.*;
import dev.quang.portfolio.entity.*;
import dev.quang.portfolio.repository.*;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioService {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
    private static final TypeReference<List<HighlightMetricDto>> METRIC_LIST = new TypeReference<>() {};
    private static final TypeReference<List<SocialLinkDto>> SOCIAL_LIST = new TypeReference<>() {};

    private final ProfileRepository profileRepository;
    private final SkillGroupRepository skillGroupRepository;
    private final ExperienceRepository experienceRepository;
    private final EducationRepository educationRepository;
    private final ProjectRepository projectRepository;
    private final ContactMessageRepository contactMessageRepository;
    private final ObjectMapper objectMapper;

    public PortfolioService(
            ProfileRepository profileRepository,
            SkillGroupRepository skillGroupRepository,
            ExperienceRepository experienceRepository,
            EducationRepository educationRepository,
            ProjectRepository projectRepository,
            ContactMessageRepository contactMessageRepository,
            ObjectMapper objectMapper
    ) {
        this.profileRepository = profileRepository;
        this.skillGroupRepository = skillGroupRepository;
        this.experienceRepository = experienceRepository;
        this.educationRepository = educationRepository;
        this.projectRepository = projectRepository;
        this.contactMessageRepository = contactMessageRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PortfolioResponse getPortfolio() {
        ProfileEntity profile = profileRepository.findById(1L)
                .orElseThrow(() -> new NoSuchElementException("Profile not initialized"));

        List<SkillGroupDto> skills = skillGroupRepository.findAllByOrderBySortOrderAscIdAsc()
                .stream().map(this::toDto).toList();
        List<ExperienceDto> experiences = experienceRepository.findAllByOrderBySortOrderAscIdAsc()
                .stream().map(this::toDto).toList();
        List<EducationDto> education = educationRepository.findAllByOrderBySortOrderAscIdAsc()
                .stream().map(this::toDto).toList();
        List<ProjectDto> projects = projectRepository.findAllByOrderBySortOrderAscIdAsc()
                .stream().map(this::toDto).toList();

        return new PortfolioResponse(
                profile.getName(),
                profile.getInitials(),
                profile.getAvatarUrl(),
                profile.getHandle(),
                new LocalizedTextDto(profile.getRoleVi(), profile.getRoleEn()),
                new LocalizedTextDto(profile.getAvailabilityVi(), profile.getAvailabilityEn()),
                new LocalizedTextDto(profile.getLocationVi(), profile.getLocationEn()),
                profile.getTimezone(),
                profile.getEmail(),
                profile.getPhone(),
                profile.getCvPdfUrl(),
                new LocalizedTextDto(profile.getHeadlineVi(), profile.getHeadlineEn()),
                new LocalizedTextDto(profile.getBioVi(), profile.getBioEn()),
                new LocalizedTextDto(profile.getCurrentFocusVi(), profile.getCurrentFocusEn()),
                readJson(profile.getMetricsJson(), METRIC_LIST),
                readJson(profile.getSocialsJson(), SOCIAL_LIST),
                skills,
                experiences,
                education,
                new LocalizedListDto(
                        readJson(profile.getLanguagesViJson(), STRING_LIST),
                        readJson(profile.getLanguagesEnJson(), STRING_LIST)
                ),
                projects,
                profile.isDraft()
        );
    }

    @Transactional
    public PortfolioResponse updateProfile(ProfileUpdateRequest request) {
        ProfileEntity entity = profileRepository.findById(1L).orElseGet(ProfileEntity::new);
        entity.setId(1L);
        entity.setName(request.name().trim());
        entity.setInitials(request.initials().trim());
        entity.setAvatarUrl(blankToNull(request.avatarUrl()));
        entity.setHandle(request.handle().trim());
        entity.setRoleVi(request.role().vi().trim());
        entity.setRoleEn(request.role().en().trim());
        entity.setAvailabilityVi(request.availability().vi().trim());
        entity.setAvailabilityEn(request.availability().en().trim());
        entity.setLocationVi(request.location().vi().trim());
        entity.setLocationEn(request.location().en().trim());
        entity.setTimezone(request.timezone().trim());
        entity.setEmail(request.email().trim());
        entity.setPhone(blankToNull(request.phone()));
        entity.setCvPdfUrl(blankToNull(request.cvPdfUrl()));
        entity.setHeadlineVi(request.headline().vi().trim());
        entity.setHeadlineEn(request.headline().en().trim());
        entity.setBioVi(request.bio().vi().trim());
        entity.setBioEn(request.bio().en().trim());
        entity.setCurrentFocusVi(request.currentFocus().vi().trim());
        entity.setCurrentFocusEn(request.currentFocus().en().trim());
        entity.setMetricsJson(writeJson(request.metrics()));
        entity.setSocialsJson(writeJson(request.socials()));
        entity.setLanguagesViJson(writeJson(request.languages().vi()));
        entity.setLanguagesEnJson(writeJson(request.languages().en()));
        entity.setDraft(request.isDraft());
        entity.setUpdatedAt(Instant.now());
        profileRepository.save(entity);
        return getPortfolio();
    }

    @Transactional
    public SkillGroupDto saveSkill(String id, SkillGroupDto dto) {
        String resolvedId = resolveId(id, dto.id(), "skill");
        SkillGroupEntity entity = skillGroupRepository.findById(resolvedId).orElseGet(SkillGroupEntity::new);
        boolean isNew = entity.getId() == null;
        entity.setId(resolvedId);
        entity.setTitleVi(dto.title().vi().trim());
        entity.setTitleEn(dto.title().en().trim());
        entity.setDescriptionVi(dto.description().vi().trim());
        entity.setDescriptionEn(dto.description().en().trim());
        entity.setAccent(dto.accent().trim());
        entity.setItemsJson(writeJson(cleanList(dto.items())));
        entity.setSortOrder(dto.sortOrder() != null ? dto.sortOrder() : (isNew ? (int) skillGroupRepository.count() : entity.getSortOrder()));
        return toDto(skillGroupRepository.save(entity));
    }

    @Transactional
    public void deleteSkill(String id) {
        if (!skillGroupRepository.existsById(id)) {
            throw new NoSuchElementException("Skill group not found: " + id);
        }
        skillGroupRepository.deleteById(id);
    }

    @Transactional
    public EducationDto saveEducation(String id, EducationDto dto) {
        String resolvedId = resolveId(id, dto.id(), "edu");
        EducationEntity entity = educationRepository.findById(resolvedId).orElseGet(EducationEntity::new);
        boolean isNew = entity.getId() == null;
        entity.setId(resolvedId);
        entity.setSchool(dto.school().trim());
        entity.setDegreeVi(dto.degree().vi().trim());
        entity.setDegreeEn(dto.degree().en().trim());
        entity.setPeriodVi(dto.period().vi().trim());
        entity.setPeriodEn(dto.period().en().trim());
        entity.setGpa(blankToNull(dto.gpa()));
        entity.setDetailsVi(dto.details().vi().trim());
        entity.setDetailsEn(dto.details().en().trim());
        entity.setSortOrder(dto.sortOrder() != null ? dto.sortOrder() : (isNew ? (int) educationRepository.count() : entity.getSortOrder()));
        return toDto(educationRepository.save(entity));
    }

    @Transactional
    public void deleteEducation(String id) {
        if (!educationRepository.existsById(id)) {
            throw new NoSuchElementException("Education item not found: " + id);
        }
        educationRepository.deleteById(id);
    }

    @Transactional
    public ExperienceDto saveExperience(String id, ExperienceDto dto) {
        String resolvedId = resolveId(id, dto.id(), "exp");
        ExperienceEntity entity = experienceRepository.findById(resolvedId).orElseGet(ExperienceEntity::new);
        boolean isNew = entity.getId() == null;
        entity.setId(resolvedId);
        entity.setCompany(dto.company().trim());
        entity.setRoleVi(dto.role().vi().trim());
        entity.setRoleEn(dto.role().en().trim());
        entity.setPeriodVi(dto.period().vi().trim());
        entity.setPeriodEn(dto.period().en().trim());
        entity.setLocationVi(dto.location().vi().trim());
        entity.setLocationEn(dto.location().en().trim());
        entity.setTypeVi(dto.type().vi().trim());
        entity.setTypeEn(dto.type().en().trim());
        entity.setSummaryVi(dto.summary().vi().trim());
        entity.setSummaryEn(dto.summary().en().trim());
        entity.setAchievementsViJson(writeJson(cleanList(dto.achievements().vi())));
        entity.setAchievementsEnJson(writeJson(cleanList(dto.achievements().en())));
        entity.setStackJson(writeJson(cleanList(dto.stack())));
        entity.setSortOrder(dto.sortOrder() != null ? dto.sortOrder() : (isNew ? (int) experienceRepository.count() : entity.getSortOrder()));
        return toDto(experienceRepository.save(entity));
    }

    @Transactional
    public void deleteExperience(String id) {
        if (!experienceRepository.existsById(id)) {
            throw new NoSuchElementException("Experience item not found: " + id);
        }
        experienceRepository.deleteById(id);
    }

    @Transactional
    public ProjectDto saveProject(String id, ProjectDto dto) {
        String resolvedId = resolveId(id, dto.id(), "proj");
        ProjectEntity entity = projectRepository.findById(resolvedId).orElseGet(ProjectEntity::new);
        boolean isNew = entity.getId() == null;
        entity.setId(resolvedId);
        entity.setTitle(dto.title().trim());
        entity.setCategory(dto.category().trim());
        entity.setFeatured(dto.featured());
        entity.setYear(dto.year().trim());
        entity.setTaglineVi(dto.tagline().vi().trim());
        entity.setTaglineEn(dto.tagline().en().trim());
        entity.setDescriptionVi(dto.description().vi().trim());
        entity.setDescriptionEn(dto.description().en().trim());
        entity.setHighlightsViJson(writeJson(cleanList(dto.highlights().vi())));
        entity.setHighlightsEnJson(writeJson(cleanList(dto.highlights().en())));
        entity.setStackJson(writeJson(cleanList(dto.stack())));
        entity.setLiveUrl(blankToNull(dto.liveUrl()));
        entity.setRepoUrl(blankToNull(dto.repoUrl()));
        entity.setSecondaryRepoUrl(blankToNull(dto.secondaryRepoUrl()));
        entity.setSortOrder(dto.sortOrder() != null ? dto.sortOrder() : (isNew ? (int) projectRepository.count() : entity.getSortOrder()));
        return toDto(projectRepository.save(entity));
    }

    @Transactional
    public void deleteProject(String id) {
        if (!projectRepository.existsById(id)) {
            throw new NoSuchElementException("Project not found: " + id);
        }
        projectRepository.deleteById(id);
    }

    @Transactional
    public ContactMessageDto createContactMessage(ContactRequest request) {
        ContactMessageEntity entity = new ContactMessageEntity();
        entity.setName(request.name().trim());
        entity.setEmail(request.email().trim());
        String subject = request.subject() == null || request.subject().isBlank()
                ? "[Portfolio] Message from " + request.name().trim()
                : request.subject().trim();
        entity.setSubject(subject);
        entity.setMessage(request.message().trim());
        entity.setRead(false);
        entity.setCreatedAt(Instant.now());
        return toDto(contactMessageRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<ContactMessageDto> getAllMessages() {
        return contactMessageRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public ContactMessageDto markMessageRead(Long id, boolean read) {
        ContactMessageEntity entity = contactMessageRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Message not found: " + id));
        entity.setRead(read);
        return toDto(contactMessageRepository.save(entity));
    }

    @Transactional
    public void deleteMessage(Long id) {
        if (!contactMessageRepository.existsById(id)) {
            throw new NoSuchElementException("Message not found: " + id);
        }
        contactMessageRepository.deleteById(id);
    }

    private SkillGroupDto toDto(SkillGroupEntity entity) {
        return new SkillGroupDto(
                entity.getId(),
                new LocalizedTextDto(entity.getTitleVi(), entity.getTitleEn()),
                new LocalizedTextDto(entity.getDescriptionVi(), entity.getDescriptionEn()),
                entity.getAccent(),
                readJson(entity.getItemsJson(), STRING_LIST),
                entity.getSortOrder()
        );
    }

    private ExperienceDto toDto(ExperienceEntity entity) {
        return new ExperienceDto(
                entity.getId(),
                entity.getCompany(),
                new LocalizedTextDto(entity.getRoleVi(), entity.getRoleEn()),
                new LocalizedTextDto(entity.getPeriodVi(), entity.getPeriodEn()),
                new LocalizedTextDto(entity.getLocationVi(), entity.getLocationEn()),
                new LocalizedTextDto(entity.getTypeVi(), entity.getTypeEn()),
                new LocalizedTextDto(entity.getSummaryVi(), entity.getSummaryEn()),
                new LocalizedListDto(
                        readJson(entity.getAchievementsViJson(), STRING_LIST),
                        readJson(entity.getAchievementsEnJson(), STRING_LIST)
                ),
                readJson(entity.getStackJson(), STRING_LIST),
                entity.getSortOrder()
        );
    }

    private EducationDto toDto(EducationEntity entity) {
        return new EducationDto(
                entity.getId(),
                entity.getSchool(),
                new LocalizedTextDto(entity.getDegreeVi(), entity.getDegreeEn()),
                new LocalizedTextDto(entity.getPeriodVi(), entity.getPeriodEn()),
                entity.getGpa(),
                new LocalizedTextDto(entity.getDetailsVi(), entity.getDetailsEn()),
                entity.getSortOrder()
        );
    }

    private ProjectDto toDto(ProjectEntity entity) {
        return new ProjectDto(
                entity.getId(),
                entity.getTitle(),
                entity.getCategory(),
                entity.isFeatured(),
                entity.getYear(),
                new LocalizedTextDto(entity.getTaglineVi(), entity.getTaglineEn()),
                new LocalizedTextDto(entity.getDescriptionVi(), entity.getDescriptionEn()),
                new LocalizedListDto(
                        readJson(entity.getHighlightsViJson(), STRING_LIST),
                        readJson(entity.getHighlightsEnJson(), STRING_LIST)
                ),
                readJson(entity.getStackJson(), STRING_LIST),
                entity.getLiveUrl(),
                entity.getRepoUrl(),
                entity.getSecondaryRepoUrl(),
                entity.getSortOrder()
        );
    }

    private ContactMessageDto toDto(ContactMessageEntity entity) {
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

    private String resolveId(String pathId, String bodyId, String prefix) {
        if (pathId != null && !pathId.isBlank()) return pathId.trim();
        if (bodyId != null && !bodyId.isBlank()) return bodyId.trim();
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private List<String> cleanList(List<String> input) {
        if (input == null) return List.of();
        return input.stream()
                .map(s -> s == null ? "" : s.trim())
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    public String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize JSON", e);
        }
    }

    private <T> T readJson(String raw, TypeReference<T> typeRef) {
        try {
            return objectMapper.readValue(raw == null || raw.isBlank() ? "[]" : raw, typeRef);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize JSON", e);
        }
    }
}
