package dev.quang.portfolio.service;

import dev.quang.portfolio.dto.NodeDtos.SaveNodeRequest;
import dev.quang.portfolio.entity.PageEntity;
import dev.quang.portfolio.entity.UserEntity;
import dev.quang.portfolio.repository.ContentNodeRepository;
import dev.quang.portfolio.repository.PageRepository;
import dev.quang.portfolio.repository.UserRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(2)
public class NodeDataSeeder implements CommandLineRunner {

    private final ContentNodeRepository nodeRepository;
    private final PageRepository pageRepository;
    private final UserRepository userRepository;
    private final NodeService nodeService;
    private final SystemDataSeeder systemDataSeeder;

    public NodeDataSeeder(
            ContentNodeRepository nodeRepository,
            PageRepository pageRepository,
            UserRepository userRepository,
            NodeService nodeService,
            SystemDataSeeder systemDataSeeder
    ) {
        this.nodeRepository = nodeRepository;
        this.pageRepository = pageRepository;
        this.userRepository = userRepository;
        this.nodeService = nodeService;
        this.systemDataSeeder = systemDataSeeder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (nodeRepository.count() == 0) {
            seedDefaultTreeForPage(systemDataSeeder.getDefaultPageId(), true);
        }
        ensureCuratedTemplatesSeeded();
    }

    @Transactional
    public void resetDefaultTreeForPage(String pageId, boolean preserveLegacyIds) {
        nodeRepository.clearParentIdsByPageId(pageId);
        nodeRepository.deleteAllByPageId(pageId);
        nodeRepository.flush();
        seedDefaultTreeForPage(pageId, preserveLegacyIds);
    }

    private static final ThreadLocal<SeedContext> SEED_CTX = new ThreadLocal<>();

    private void seedDefaultTreeForPage(String pageId, boolean preserveLegacyIds) {
        Map<String, String> idMap = preserveLegacyIds ? null : new HashMap<>();
        SEED_CTX.set(new SeedContext(pageId, idMap));
        try {
            seedNodes();
        } finally {
            SEED_CTX.remove();
        }
    }

    private record SeedContext(String pageId, Map<String, String> idMap) {
        String resolveId(String key) {
            if (key == null) return null;
            if (idMap == null) return key;
            return idMap.computeIfAbsent(key, k -> "n-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        }
    }

    private void seedNodes() {
        // 1. HERO / ABOUT SECTION
        save("sec-hero", null, "section", 0,
                Map.of("anchor", "about", "padding", "none", "bgColor", "transparent"),
                "", "");

        save("hero-main", "sec-hero", "card", 0,
                Map.of("bgColor", "#ffffff", "borderColor", "#e4e4e7", "rounded", "lg", "padding", "lg"),
                "", "");

        save("hero-top", "hero-main", "card", 0,
                Map.of(
                        "bgColor", "transparent",
                        "borderColor", "transparent",
                        "rounded", "none",
                        "padding", "none",
                        "stackDirection", "row",
                        "stackJustify", "space-between",
                        "stackWrap", true,
                        "gap", 3
                ),
                "", "");
        save("hero-identity", "hero-top", "card", 0,
                Map.of("bgColor", "transparent", "borderColor", "transparent", "rounded", "none", "padding", "none", "gap", 1),
                "", "");
        save("hero-title", "hero-identity", "heading", 0,
                Map.of("level", "h1", "fontSize", "3xl", "textColor", "#18181b"),
                "Nguyễn Kim Quang", "Nguyen Kim Quang");
        save("hero-role", "hero-identity", "text", 1,
                Map.of("fontSize", "sm", "textColor", "#1d4ed8"),
                "<p><strong>Software Developer Intern / Fresher · Java Backend &amp; Full-stack</strong></p>",
                "<p><strong>Software Developer Intern / Fresher · Java Backend &amp; Full-stack</strong></p>");
        save("hero-status-badge", "hero-top", "badge", 1,
                Map.of("bgColor", "#eff6ff", "textColor", "#1e40af", "borderColor", "#bfdbfe", "dotColor", "#2563eb"),
                "Sẵn sàng Intern / Fresher", "Open to opportunities");

        save("hero-bio", "hero-main", "text", 1,
                Map.of("fontSize", "sm", "textColor", "#52525b"),
                "<p>Sinh viên năm cuối ngành Công nghệ thông tin tập trung vào phát triển web backend và full-stack (Java / Spring Boot, Laravel, Vue.js, TypeScript) và kiến trúc RESTful API sạch. Mong muốn tìm kiếm vị trí Software Developer Intern/Fresher để xây dựng sản phẩm tin cậy, rèn luyện tư duy thiết kế hệ thống và đóng góp cho đội ngũ kỹ thuật thực tế.</p>",
                "<p>Final-year Information Technology student focused on backend and full-stack web development (Java / Spring Boot, Laravel, Vue.js, TypeScript) and clean RESTful API architecture. Seeking a Software Developer Intern/Fresher role to build reliable products, sharpen system design thinking, and contribute to real-world engineering teams.</p>");

        save("hero-divider", "hero-main", "divider", 2,
                Map.of("borderColor", "#f4f4f5"),
                "", "");

        save("hero-actions", "hero-main", "badge-group", 3,
                Map.of("gap", 3),
                "", "");
        save("hero-btn-email", "hero-actions", "link-button", 0,
                Map.of("href", "mailto:nguyenkimquang1612@gmail.com", "icon", "mail", "bgColor", "transparent", "textColor", "#52525b", "borderColor", "transparent"),
                "nguyenkimquang1612@gmail.com", "nguyenkimquang1612@gmail.com");
        save("hero-btn-github", "hero-actions", "link-button", 1,
                Map.of("href", "https://github.com/quang-star", "target", "_blank", "icon", "github", "bgColor", "transparent", "textColor", "#27272a", "borderColor", "transparent"),
                "github.com/quang-star", "github.com/quang-star");
        save("hero-phone", "hero-actions", "badge", 2,
                Map.of("mono", true, "icon", "phone", "bgColor", "transparent", "textColor", "#52525b", "borderColor", "transparent"),
                "0974213318", "0974213318");
        save("hero-loc-badge", "hero-actions", "badge", 3,
                Map.of("icon", "location", "bgColor", "transparent", "textColor", "#52525b", "borderColor", "transparent"),
                "Hà Nội, VN", "Ha Noi, VN");

        // 2. SKILLS SECTION (Single key-value table card)
        save("sec-skills", null, "section", 1,
                Map.of("anchor", "skills", "padding", "none", "bgColor", "transparent"),
                "", "");
        save("skills-header", "sec-skills", "heading", 0,
                Map.of("level", "h2", "fontSize", "sm", "textColor", "#71717a"),
                "KỸ NĂNG CHUYÊN MÔN", "TECHNICAL SKILLS");

        save("skills-table", "sec-skills", "card", 1,
                Map.of("bgColor", "#ffffff", "borderColor", "#e4e4e7", "rounded", "lg", "padding", "none", "divideY", true),
                "", "");

        createSkillRow("skill-backend", "skills-table", 0, "Backend", "Backend",
                List.of("Java", "Spring Boot", "Spring Security", "PHP", "Laravel", "RESTful API", "JWT"));
        createSkillRow("skill-frontend", "skills-table", 1, "Frontend", "Frontend",
                List.of("Vue.js", "TypeScript", "JavaScript", "Tailwind CSS", "HTML / CSS"));
        createSkillRow("skill-db", "skills-table", 2, "Cơ sở dữ liệu", "Databases",
                List.of("PostgreSQL", "MySQL", "MongoDB", "Flyway"));
        createSkillRow("skill-lang-ai", "skills-table", 3, "Ngôn ngữ & AI", "Languages & AI",
                List.of("Python", "FastAPI", "Gemini API", "C++", "C#"));
        createSkillRow("skill-tools", "skills-table", 4, "Công cụ & Cloud", "Tools & Cloud",
                List.of("Git / GitHub", "Docker", "Postman", "Firebase", "Oracle Cloud"));

        // 3. PROJECTS SECTION
        save("sec-projects", null, "section", 2,
                Map.of("anchor", "projects", "padding", "none", "bgColor", "transparent"),
                "", "");
        save("proj-header-row", "sec-projects", "card", 0,
                Map.of(
                        "bgColor", "transparent",
                        "borderColor", "transparent",
                        "rounded", "none",
                        "padding", "none",
                        "stackDirection", "row",
                        "stackJustify", "space-between",
                        "stackWrap", true,
                        "gap", 2
                ),
                "", "");
        save("proj-header", "proj-header-row", "heading", 0,
                Map.of("level", "h2", "fontSize", "sm", "textColor", "#71717a"),
                "DỰ ÁN TIÊU BIỂU", "FEATURED PROJECTS");
        save("proj-all-link", "proj-header-row", "link-button", 1,
                Map.of("href", "https://github.com/quang-star", "target", "_blank", "icon", "link", "bgColor", "transparent", "textColor", "#1d4ed8", "borderColor", "transparent"),
                "Tất cả kho lưu trữ", "All repositories");
        save("proj-grid", "sec-projects", "grid", 1,
                Map.of("cols", 2, "gap", 4),
                "", "");

        createProjectCard("proj-recruitment", "proj-grid", 0, "2026",
                "Recruitment Management System",
                "<p>Hệ thống tuyển dụng kết nối ứng viên và nhà tuyển dụng, phân quyền JWT và tích hợp xử lý CV / gợi ý phù hợp qua FastAPI.</p>",
                "<p>Recruitment platform connecting candidates and recruiters with JWT role-based access, CV processing, and AI matching via FastAPI.</p>",
                List.of("Java", "Spring Boot", "PostgreSQL", "Python", "FastAPI"),
                List.of(Map.of("label", "Source Code", "href", "https://github.com/quang-star/recruitment-system")));

        createProjectCard("proj-finance", "proj-grid", 1, "2026",
                "Smart Personal Finance App",
                "<p>Ứng dụng quản lý thu chi sử dụng Spring Boot và MySQL, tích hợp xác thực Firebase và Gemini API để trích xuất dữ liệu hóa đơn.</p>",
                "<p>Personal finance tracker built with Spring Boot and MySQL, integrating Firebase Auth and Gemini API for receipt processing.</p>",
                List.of("Java", "Spring Boot", "MySQL", "Firebase", "Gemini API"),
                List.of(Map.of("label", "Source Code", "href", "https://github.com/quang-star/finace-app")));

        createProjectCard("proj-4views", "proj-grid", 2, "Feb — May 2026",
                "4VIEWS SOCIAL",
                "<p>Mạng xã hội tách lớp xác thực, bảng tin, chat thời gian thực và quản trị với Laravel API, Express TypeScript Gateway, Vue.js và Socket.IO.</p>",
                "<p>Social networking platform with decoupled auth, timeline, real-time chat, and admin services using Laravel, Express/TS, Vue.js, and Socket.IO.</p>",
                List.of("Laravel", "Vue.js", "TypeScript", "Socket.IO", "MongoDB"),
                List.of(Map.of("label", "Source Code", "href", "https://github.com/quang-star")));

        createProjectCard("proj-ticket", "proj-grid", 3, "Oct 2025 — Jan 2026",
                "Ticket Booking Platform",
                "<p>Nền tảng đặt vé sự kiện cho người mua, ban tổ chức và admin. Tích hợp Laravel REST API, Vue.js, webhook PayOS và sinh mã QR.</p>",
                "<p>Event ticketing system for attendees, organizers, and admins featuring Laravel REST APIs, Vue.js, PayOS webhook verification, and QR tickets.</p>",
                List.of("Laravel", "Vue.js", "MySQL", "PayOS", "Firebase"),
                List.of(
                        Map.of("label", "GitHub FE", "href", "https://github.com/quang-star/TicketFE"),
                        Map.of("label", "GitHub BE", "href", "https://github.com/quang-star/TicketBE")
                ));

        // 4. EDUCATION SECTION
        save("sec-exp", null, "section", 3,
                Map.of("anchor", "experience", "padding", "none", "bgColor", "transparent"),
                "", "");
        save("exp-header", "sec-exp", "heading", 0,
                Map.of("level", "h2", "fontSize", "sm", "textColor", "#71717a"),
                "HỌC VẤN", "EDUCATION");
        save("edu-card", "sec-exp", "card", 1,
                Map.of(
                        "bgColor", "#ffffff",
                        "borderColor", "#e4e4e7",
                        "rounded", "lg",
                        "padding", "md",
                        "stackDirection", "row",
                        "stackJustify", "space-between",
                        "stackWrap", true,
                        "gap", 3
                ),
                "", "");
        save("edu-info", "edu-card", "card", 0,
                Map.of("bgColor", "transparent", "borderColor", "transparent", "rounded", "none", "padding", "none", "gap", 1),
                "", "");
        save("edu-title", "edu-info", "heading", 0,
                Map.of("level", "h3", "fontSize", "base", "textColor", "#18181b"),
                "Đại học Kiến trúc Hà Nội", "Hanoi Architectural University");
        save("edu-desc", "edu-info", "text", 1,
                Map.of("fontSize", "xs", "textColor", "#52525b"),
                "<p>Cử nhân Công nghệ thông tin · 2022 — Hiện tại</p>",
                "<p>Bachelor of Information Technology · 2022 — Present</p>");
        save("edu-badges", "edu-card", "badge-group", 1,
                Map.of("gap", 2),
                "", "");
        save("edu-gpa", "edu-badges", "badge", 0,
                Map.of("mono", true, "bgColor", "#fafafa", "textColor", "#18181b", "borderColor", "#e4e4e7"),
                "GPA: 3.61/4.0", "GPA: 3.61/4.0");
        save("edu-track", "edu-badges", "badge", 1,
                Map.of("mono", true, "bgColor", "#eff6ff", "textColor", "#1e40af", "borderColor", "#bfdbfe"),
                "Excellent Track", "Excellent Track");

        // 5. CONTACT SECTION
        save("sec-contact", null, "section", 4,
                Map.of("anchor", "contact", "padding", "none", "bgColor", "transparent"),
                "", "");
        save("contact-header", "sec-contact", "heading", 0,
                Map.of("level", "h2", "fontSize", "sm", "textColor", "#71717a"),
                "LIÊN HỆ", "CONTACT");
        save("contact-card", "sec-contact", "card", 1,
                Map.of("bgColor", "#ffffff", "borderColor", "#e4e4e7", "rounded", "lg", "padding", "lg"),
                "", "");
        save("contact-desc", "contact-card", "text", 0,
                Map.of("fontSize", "sm", "textColor", "#52525b"),
                "<p>Sẵn sàng trao đổi về cơ hội thực tập, vị trí Fresher hoặc dự án hợp tác qua email và các kênh bên dưới.</p>",
                "<p>Open to discussing internship, fresher roles, or collaboration opportunities via email or the links below.</p>");
        save("contact-links", "contact-card", "badge-group", 1,
                Map.of("gap", 3),
                "", "");
        save("contact-email-btn", "contact-links", "link-button", 0,
                Map.of("href", "mailto:nguyenkimquang1612@gmail.com", "icon", "mail", "bgColor", "#1d4ed8", "textColor", "#ffffff", "borderColor", "#1d4ed8", "rounded", "lg"),
                "nguyenkimquang1612@gmail.com", "nguyenkimquang1612@gmail.com");
        save("contact-github-btn", "contact-links", "link-button", 1,
                Map.of("href", "https://github.com/quang-star", "target", "_blank", "icon", "github", "bgColor", "#fafafa", "textColor", "#18181b", "borderColor", "#e4e4e7", "rounded", "lg"),
                "github.com/quang-star", "github.com/quang-star");
        save("contact-phone-btn", "contact-links", "link-button", 2,
                Map.of("href", "tel:0974213318", "icon", "phone", "bgColor", "#fafafa", "textColor", "#18181b", "borderColor", "#e4e4e7", "rounded", "lg"),
                "0974213318", "0974213318");
    }

    private void createSkillRow(
            String id,
            String parentId,
            int order,
            String labelVi,
            String labelEn,
            List<String> items
    ) {
        save(id, parentId, "card", order,
                Map.of(
                        "bgColor", "transparent",
                        "borderColor", "transparent",
                        "rounded", "none",
                        "padding", "sm",
                        "stackDirection", "row",
                        "stackWrap", true,
                        "gap", 4
                ),
                "", "");
        save(id + "-label", id, "text", 0,
                Map.of("fontSize", "xs", "textColor", "#27272a", "width", "9rem"),
                "<p><strong>" + labelVi + "</strong></p>",
                "<p><strong>" + labelEn + "</strong></p>");
        save(id + "-badges", id, "badge-group", 1,
                Map.of("gap", 2),
                "", "");
        for (int i = 0; i < items.size(); i++) {
            save(id + "-b-" + i, id + "-badges", "badge", i,
                    Map.of("mono", true, "bgColor", "#fafafa", "textColor", "#3f3f46", "borderColor", "#e4e4e7"),
                    items.get(i), items.get(i));
        }
    }

    private void createProjectCard(
            String id,
            String parentId,
            int order,
            String period,
            String title,
            String descVi,
            String descEn,
            List<String> stack,
            List<Map<String, String>> links
    ) {
        save(id, parentId, "card", order,
                Map.of("bgColor", "#ffffff", "borderColor", "#e4e4e7", "rounded", "lg", "padding", "md"),
                "", "");
        save(id + "-meta", id, "badge-group", 0,
                Map.of("gap", 2),
                "", "");
        save(id + "-type", id + "-meta", "badge", 0,
                Map.of("bgColor", "#eff6ff", "textColor", "#1e40af", "borderColor", "#bfdbfe"),
                "Full-stack", "Full-stack");
        save(id + "-year", id + "-meta", "badge", 1,
                Map.of("mono", true, "bgColor", "#fafafa", "textColor", "#71717a", "borderColor", "#e4e4e7"),
                period, period);

        save(id + "-title", id, "heading", 1,
                Map.of("level", "h3", "fontSize", "base", "textColor", "#18181b"),
                title, title);
        save(id + "-desc", id, "text", 2,
                Map.of("fontSize", "sm", "textColor", "#52525b"),
                descVi, descEn);
        save(id + "-stack", id, "badge-group", 3,
                Map.of("gap", 2),
                "", "");
        for (int i = 0; i < stack.size(); i++) {
            save(id + "-s-" + i, id + "-stack", "badge", i,
                    Map.of("mono", true, "bgColor", "#f4f4f5", "textColor", "#3f3f46", "borderColor", "#e4e4e7"),
                    stack.get(i), stack.get(i));
        }
        save(id + "-links", id, "badge-group", 4,
                Map.of("gap", 2),
                "", "");
        for (int i = 0; i < links.size(); i++) {
            Map<String, String> link = links.get(i);
            save(id + "-l-" + i, id + "-links", "link-button", i,
                    Map.of(
                            "href", link.get("href"),
                            "target", "_blank",
                            "icon", "github",
                            "bgColor", "#fafafa",
                            "textColor", "#18181b",
                            "borderColor", "#e4e4e7"
                    ),
                    link.get("label"), link.get("label"));
        }
    }

    private void save(
            String id,
            String parentId,
            String tag,
            int sortOrder,
            Map<String, Object> attrs,
            String contentVi,
            String contentEn
    ) {
        SeedContext ctx = SEED_CTX.get();
        String resolvedId = ctx.resolveId(id);
        String resolvedParentId = ctx.resolveId(parentId);
        nodeService.saveNodeForPage(
                ctx.pageId(),
                resolvedId,
                new SaveNodeRequest(resolvedId, resolvedParentId, tag, sortOrder, attrs, contentVi, contentEn, null)
        );
    }

    private void ensureCuratedTemplatesSeeded() {
        UserEntity owner = userRepository.findFirstByRoleOrderByCreatedAtAsc("SUPER_ADMIN").orElse(null);
        if (owner == null) {
            return;
        }
        String ownerId = owner.getId();

        seedCuratedTemplateIfAbsent(
                "tpl-ats-fresher",
                ownerId,
                "cv-ats-fresher",
                "CV 1 Trang Chuẩn ATS — Sinh viên / Fresher",
                "Bố cục 1 cột tối giản, dễ đọc cho máy lọc ATS và nhà tuyển dụng tuyển thực tập sinh & fresher.",
                "{\"category\":\"ats\",\"previewLayout\":\"ats\"}",
                this::seedAtsFresherTemplate
        );

        seedCuratedTemplateIfAbsent(
                "tpl-product-designer",
                ownerId,
                "product-designer",
                "UI/UX & Product Designer Portfolio",
                "Tập trung vào Case Study, số liệu tác động thiết kế, Design System và trải nghiệm người dùng.",
                "{\"category\":\"design\",\"previewLayout\":\"design\"}",
                this::seedProductDesignerTemplate
        );

        seedCuratedTemplateIfAbsent(
                "tpl-business-marketing",
                ownerId,
                "business-marketing",
                "Kinh doanh & Growth Marketing CV",
                "Làm nổi bật KPI tăng trưởng, chiến dịch tiêu biểu, kỹ năng phân tích số liệu và kinh nghiệm thực chiến.",
                "{\"category\":\"business\",\"previewLayout\":\"business\"}",
                this::seedBusinessMarketingTemplate
        );
    }

    private void seedCuratedTemplateIfAbsent(
            String pageId,
            String ownerId,
            String slug,
            String title,
            String description,
            String themeJson,
            Runnable nodesSeeder
    ) {
        if (pageRepository.existsById(pageId) || pageRepository.existsByOwnerIdAndSlug(ownerId, slug)) {
            return;
        }
        Instant now = Instant.now();
        PageEntity page = new PageEntity();
        page.setId(pageId);
        page.setOwnerId(ownerId);
        page.setSlug(slug);
        page.setTitle(title);
        page.setDescription(description);
        page.setVisibility("PUBLIC");
        page.setPrimary(false);
        page.setTemplate(true);
        page.setHomeFeatured(false);
        page.setThemeJson(themeJson);
        page.setViewCount(0L);
        page.setCreatedAt(now);
        page.setUpdatedAt(now);
        pageRepository.saveAndFlush(page);

        SEED_CTX.set(new SeedContext(pageId, new HashMap<>()));
        try {
            nodesSeeder.run();
        } finally {
            SEED_CTX.remove();
        }
    }

    private void seedAtsFresherTemplate() {
        save("ats-sec-header", null, "section", 0,
                Map.of("anchor", "about", "canvasLabel", "Thông tin cá nhân", "navLabel", "Giới thiệu", "padding", "none", "bgColor", "transparent"),
                "", "");
        save("ats-header-card", "ats-sec-header", "card", 0,
                Map.of("bgColor", "#ffffff", "borderColor", "#e4e4e7", "rounded", "lg", "padding", "lg", "gap", 2),
                "", "");
        save("ats-name", "ats-header-card", "heading", 0,
                Map.of("level", "h1", "fontSize", "2xl", "textColor", "#18181b"),
                "Nguyễn Văn A", "Nguyen Van A");
        save("ats-role", "ats-header-card", "text", 1,
                Map.of("fontSize", "sm", "textColor", "#1d4ed8"),
                "<p><strong>Sinh viên năm cuối Công nghệ Thông tin · Thực tập sinh Software Engineer</strong></p>",
                "<p><strong>Final-year IT Student · Software Engineering Intern / Fresher</strong></p>");
        save("ats-contact-row", "ats-header-card", "badge-group", 2,
                Map.of("gap", 2), "", "");
        save("ats-email", "ats-contact-row", "link-button", 0,
                Map.of("href", "mailto:nguyenvana@example.com", "icon", "mail", "bgColor", "#fafafa", "textColor", "#18181b", "borderColor", "#e4e4e7"),
                "nguyenvana@example.com", "nguyenvana@example.com");
        save("ats-phone", "ats-contact-row", "badge", 1,
                Map.of("mono", true, "icon", "phone", "bgColor", "#fafafa", "textColor", "#3f3f46", "borderColor", "#e4e4e7"),
                "0912 345 678", "0912 345 678");
        save("ats-loc", "ats-contact-row", "badge", 2,
                Map.of("icon", "location", "bgColor", "#fafafa", "textColor", "#3f3f46", "borderColor", "#e4e4e7"),
                "TP. Hồ Chí Minh", "Ho Chi Minh City");
        save("ats-summary", "ats-header-card", "text", 3,
                Map.of("fontSize", "sm", "textColor", "#52525b"),
                "<p>Sinh viên năm cuối GPA 3.5/4.0 có nền tảng vững về Java, Spring Boot, SQL và React/Vue. Đã hoàn thành 2 dự án web đầy đủ quy trình REST API, bảo mật JWT và triển khai Docker. Sẵn sàng làm việc toàn thời gian cho vị trí Thực tập sinh / Fresher.</p>",
                "<p>Final-year CS/IT student (GPA 3.5/4.0) with strong fundamentals in Java, Spring Boot, SQL, and React/Vue. Built 2 full-stack web applications with REST APIs, JWT auth, and Docker deployment. Available full-time for Intern / Fresher roles.</p>");

        save("ats-sec-edu", null, "section", 1,
                Map.of("anchor", "education", "canvasLabel", "Học vấn", "navLabel", "Học vấn", "padding", "none", "bgColor", "transparent"),
                "", "");
        save("ats-edu-title", "ats-sec-edu", "heading", 0,
                Map.of("level", "h2", "fontSize", "sm", "textColor", "#71717a"),
                "HỌC VẤN", "EDUCATION");
        save("ats-edu-card", "ats-sec-edu", "card", 1,
                Map.of("bgColor", "#ffffff", "borderColor", "#e4e4e7", "rounded", "lg", "padding", "md", "gap", 2),
                "", "");
        save("ats-edu-school", "ats-edu-card", "heading", 0,
                Map.of("level", "h3", "fontSize", "base", "textColor", "#18181b"),
                "Đại học Công nghệ Thông tin — ĐHQG TP.HCM (2022 — 2026)",
                "University of Information Technology — VNU-HCM (2022 — 2026)");
        save("ats-edu-detail", "ats-edu-card", "list", 1,
                Map.of("fontSize", "sm", "textColor", "#52525b"),
                "<ul><li>Chuyên ngành: Kỹ thuật Phần mềm · GPA tích lũy: <strong>3.52 / 4.0</strong></li><li>Môn học tiêu biểu: Cấu trúc dữ liệu &amp; Giải thuật, Cơ sở dữ liệu, Mạng máy tính, Công nghệ phần mềm.</li></ul>",
                "<ul><li>Major: Software Engineering · Cumulative GPA: <strong>3.52 / 4.0</strong></li><li>Relevant Coursework: Data Structures &amp; Algorithms, Databases, Computer Networks, Software Engineering.</li></ul>");

        save("ats-sec-skills", null, "section", 2,
                Map.of("anchor", "skills", "canvasLabel", "Kỹ năng", "navLabel", "Kỹ năng", "padding", "none", "bgColor", "transparent"),
                "", "");
        save("ats-skills-title", "ats-sec-skills", "heading", 0,
                Map.of("level", "h2", "fontSize", "sm", "textColor", "#71717a"),
                "KỸ NĂNG CHUYÊN MÔN", "SKILLS");
        save("ats-skills-card", "ats-sec-skills", "card", 1,
                Map.of("bgColor", "#ffffff", "borderColor", "#e4e4e7", "rounded", "lg", "padding", "md"),
                "", "");
        save("ats-skills-list", "ats-skills-card", "list", 0,
                Map.of("fontSize", "sm", "textColor", "#3f3f46"),
                "<ul><li><strong>Ngôn ngữ &amp; Backend:</strong> Java, TypeScript, JavaScript, SQL, Spring Boot, RESTful API.</li><li><strong>Frontend &amp; Công cụ:</strong> Vue.js, Tailwind CSS, Git/GitHub, Docker, PostgreSQL, Postman.</li><li><strong>Ngoại ngữ:</strong> Tiếng Anh đọc hiểu tài liệu chuyên ngành tốt (TOEIC 750 / IELTS 6.5).</li></ul>",
                "<ul><li><strong>Languages &amp; Backend:</strong> Java, TypeScript, JavaScript, SQL, Spring Boot, RESTful APIs.</li><li><strong>Frontend &amp; Tools:</strong> Vue.js, Tailwind CSS, Git/GitHub, Docker, PostgreSQL, Postman.</li><li><strong>Languages:</strong> Fluent technical English reading &amp; communication (TOEIC 750 / IELTS 6.5).</li></ul>");

        save("ats-sec-proj", null, "section", 3,
                Map.of("anchor", "projects", "canvasLabel", "Dự án & Kinh nghiệm", "navLabel", "Dự án", "padding", "none", "bgColor", "transparent"),
                "", "");
        save("ats-proj-title", "ats-sec-proj", "heading", 0,
                Map.of("level", "h2", "fontSize", "sm", "textColor", "#71717a"),
                "DỰ ÁN &amp; HOẠT ĐỘNG", "PROJECTS &amp; EXPERIENCE");
        save("ats-proj-card", "ats-sec-proj", "card", 1,
                Map.of("bgColor", "#ffffff", "borderColor", "#e4e4e7", "rounded", "lg", "padding", "md", "gap", 3),
                "", "");
        save("ats-p1-title", "ats-proj-card", "heading", 0,
                Map.of("level", "h3", "fontSize", "base", "textColor", "#18181b"),
                "Hệ thống Quản lý Thư viện &amp; Mượn Trả Sách (09/2025 — 12/2025)",
                "Library &amp; Book Lending Management System (Sep 2025 — Dec 2025)");
        save("ats-p1-bullets", "ats-proj-card", "list", 1,
                Map.of("fontSize", "sm", "textColor", "#52525b"),
                "<ul><li>Phát triển Backend bằng Spring Boot &amp; PostgreSQL phục vụ tìm kiếm sách, đặt mượn và nhắc hạn tự động.</li><li>Thiết kế phân quyền JWT cho Thủ thư và Sinh viên; viết 25+ integration tests đạt độ phủ 80%.</li></ul>",
                "<ul><li>Built Spring Boot &amp; PostgreSQL backend supporting catalog search, lending workflows, and due reminders.</li><li>Implemented JWT role-based access for librarians and students; wrote 25+ integration tests (80% coverage).</li></ul>");
        save("ats-p2-title", "ats-proj-card", "heading", 2,
                Map.of("level", "h3", "fontSize", "base", "textColor", "#18181b"),
                "Cộng tác viên CLB Lập trình — Mentor môn Nhập môn Lập trình (2024 — 2025)",
                "Programming Club Mentor — Intro to Programming (2024 — 2025)");
        save("ats-p2-bullets", "ats-proj-card", "list", 3,
                Map.of("fontSize", "sm", "textColor", "#52525b"),
                "<ul><li>Hướng dẫn 40+ sinh viên năm nhất ôn tập thuật toán cơ bản, Git và sửa lỗi code hàng tuần.</li><li>Đạt giải Ba cuộc thi Hackathon cấp trường năm 2025.</li></ul>",
                "<ul><li>Mentored 40+ freshmen in weekly sessions on core algorithms, Git workflows, and debugging.</li><li>Won 3rd Prize at the 2025 University Hackathon.</li></ul>");
    }

    private void seedProductDesignerTemplate() {
        save("des-sec-hero", null, "section", 0,
                Map.of("anchor", "about", "canvasLabel", "Giới thiệu", "navLabel", "Giới thiệu", "padding", "none", "bgColor", "transparent"),
                "", "");
        save("des-hero-card", "des-sec-hero", "card", 0,
                Map.of("bgColor", "#ffffff", "borderColor", "#e4e4e7", "rounded", "2xl", "padding", "lg", "gap", 3),
                "", "");
        save("des-badges", "des-hero-card", "badge-group", 0,
                Map.of("gap", 2), "", "");
        save("des-status", "des-badges", "badge", 0,
                Map.of("bgColor", "#eff6ff", "textColor", "#1d4ed8", "borderColor", "#bfdbfe", "dotColor", "#2563eb"),
                "Sẵn sàng hợp tác dự án", "Available for Product Roles");
        save("des-loc", "des-badges", "badge", 1,
                Map.of("icon", "location", "bgColor", "#fafafa", "textColor", "#52525b", "borderColor", "#e4e4e7"),
                "Hà Nội · Remote", "Hanoi · Remote");
        save("des-name", "des-hero-card", "heading", 1,
                Map.of("level", "h1", "fontSize", "3xl", "textColor", "#18181b"),
                "Nguyễn Thị B", "Nguyen Thi B");
        save("des-role", "des-hero-card", "text", 2,
                Map.of("fontSize", "base", "textColor", "#1d4ed8"),
                "<p><strong>Product Designer · UI/UX &amp; Design Systems</strong></p>",
                "<p><strong>Product Designer · UI/UX &amp; Design Systems</strong></p>");
        save("des-bio", "des-hero-card", "text", 3,
                Map.of("fontSize", "sm", "textColor", "#52525b"),
                "<p>Hơn 3 năm thiết kế sản phẩm SaaS và ứng dụng di động từ nghiên cứu người dùng, wireframe đến Design System chuẩn hóa. Tập trung vào trải nghiệm mạch lạc, giảm ma sát thao tác và tăng tỷ lệ chuyển đổi.</p>",
                "<p>3+ years designing B2B SaaS and mobile products from user research and flows to scalable Design Systems. Focused on clarity, accessibility, and measurable conversion lift.</p>");
        save("des-cta-row", "des-hero-card", "badge-group", 4,
                Map.of("gap", 2), "", "");
        save("des-btn-mail", "des-cta-row", "link-button", 0,
                Map.of("href", "mailto:nguyenthib@example.com", "icon", "mail", "bgColor", "#1d4ed8", "textColor", "#ffffff", "borderColor", "#1d4ed8", "rounded", "lg"),
                "nguyenthib@example.com", "nguyenthib@example.com");
        save("des-btn-port", "des-cta-row", "link-button", 1,
                Map.of("href", "https://dribbble.com", "target", "_blank", "icon", "globe", "bgColor", "#ffffff", "textColor", "#18181b", "borderColor", "#e4e4e7", "rounded", "lg"),
                "Dribbble / Behance", "Dribbble / Behance");

        save("des-sec-metrics", null, "section", 1,
                Map.of("canvasLabel", "Số liệu tác động", "padding", "none", "bgColor", "transparent"),
                "", "");
        save("des-metrics-grid", "des-sec-metrics", "grid", 0,
                Map.of("cols", 4, "gap", 4), "", "");
        save("des-m1", "des-metrics-grid", "metric", 0,
                Map.of("value", "3+", "valueColor", "#1d4ed8", "labelColor", "#64748b", "bgColor", "#ffffff", "borderColor", "#e4e4e7", "borderWidth", "1", "borderStyle", "solid"),
                "Năm kinh nghiệm", "Years experience");
        save("des-m2", "des-metrics-grid", "metric", 1,
                Map.of("value", "+28%", "valueColor", "#1d4ed8", "labelColor", "#64748b", "bgColor", "#ffffff", "borderColor", "#e4e4e7", "borderWidth", "1", "borderStyle", "solid"),
                "Tỷ lệ hoàn tất onboarding", "Onboarding completion");
        save("des-m3", "des-metrics-grid", "metric", 2,
                Map.of("value", "120+", "valueColor", "#1d4ed8", "labelColor", "#64748b", "bgColor", "#ffffff", "borderColor", "#e4e4e7", "borderWidth", "1", "borderStyle", "solid"),
                "Component trong Design System", "Design system components");
        save("des-m4", "des-metrics-grid", "metric", 3,
                Map.of("value", "12", "valueColor", "#1d4ed8", "labelColor", "#64748b", "bgColor", "#ffffff", "borderColor", "#e4e4e7", "borderWidth", "1", "borderStyle", "solid"),
                "Sản phẩm đã ra mắt", "Shipped products");

        save("des-sec-cases", null, "section", 2,
                Map.of("anchor", "case-studies", "canvasLabel", "Case Studies", "navLabel", "Case Studies", "padding", "none", "bgColor", "transparent"),
                "", "");
        save("des-cases-title", "des-sec-cases", "heading", 0,
                Map.of("level", "h2", "fontSize", "sm", "textColor", "#71717a"),
                "CASE STUDY TIÊU BIỂU", "SELECTED CASE STUDIES");
        save("des-cases-grid", "des-sec-cases", "grid", 1,
                Map.of("cols", 2, "gap", 4), "", "");

        save("des-c1", "des-cases-grid", "card", 0,
                Map.of("bgColor", "#ffffff", "borderColor", "#e4e4e7", "rounded", "2xl", "padding", "md", "gap", 2),
                "", "");
        save("des-c1-tag", "des-c1", "badge", 0,
                Map.of("bgColor", "#eff6ff", "textColor", "#1d4ed8", "borderColor", "#bfdbfe"),
                "FinTech · Mobile App", "FinTech · Mobile App");
        save("des-c1-title", "des-c1", "heading", 1,
                Map.of("level", "h3", "fontSize", "lg", "textColor", "#18181b"),
                "Tái thiết kế luồng Chuyển tiền &amp; Quản lý Ví",
                "Redesigning Instant Transfer &amp; Wallet Experience");
        save("des-c1-desc", "des-c1", "text", 2,
                Map.of("fontSize", "sm", "textColor", "#52525b"),
                "<p>Phỏng vấn 18 người dùng thực tế, rút gọn luồng chuyển khoản từ 6 bước xuống 3 bước, giảm 35% tỷ lệ bỏ ngang và tăng điểm hài lòng CSAT lên 4.8/5.</p>",
                "<p>Conducted 18 user interviews, streamlined the transfer flow from 6 steps to 3, cutting drop-off by 35% and raising CSAT to 4.8/5.</p>");

        save("des-c2", "des-cases-grid", "card", 1,
                Map.of("bgColor", "#ffffff", "borderColor", "#e4e4e7", "rounded", "2xl", "padding", "md", "gap", 2),
                "", "");
        save("des-c2-tag", "des-c2", "badge", 0,
                Map.of("bgColor", "#eff6ff", "textColor", "#1d4ed8", "borderColor", "#bfdbfe"),
                "B2B SaaS · Design System", "B2B SaaS · Design System");
        save("des-c2-title", "des-c2", "heading", 1,
                Map.of("level", "h3", "fontSize", "lg", "textColor", "#18181b"),
                "Xây dựng Design System đa thương hiệu trên Figma",
                "Multi-brand Figma Design System &amp; Tokens");
        save("des-c2-desc", "des-c2", "text", 2,
                Map.of("fontSize", "sm", "textColor", "#52525b"),
                "<p>Chuẩn hóa Figma Variables, Typography và 120+ component đồng bộ với Tailwind CSS giúp đội Frontend bàn giao tính năng nhanh hơn 40%.</p>",
                "<p>Standardized Figma Variables, typography, and 120+ components synced with Tailwind tokens, accelerating FE handoff by 40%.</p>");

        save("des-sec-tools", null, "section", 3,
                Map.of("anchor", "skills", "canvasLabel", "Công cụ & Quy trình", "navLabel", "Kỹ năng", "padding", "none", "bgColor", "transparent"),
                "", "");
        save("des-tools-title", "des-sec-tools", "heading", 0,
                Map.of("level", "h2", "fontSize", "sm", "textColor", "#71717a"),
                "CÔNG CỤ &amp; CHUYÊN MÔN", "TOOLS &amp; EXPERTISE");
        save("des-tools-card", "des-sec-tools", "card", 1,
                Map.of("bgColor", "#ffffff", "borderColor", "#e4e4e7", "rounded", "2xl", "padding", "md"),
                "", "");
        save("des-tools-badges", "des-tools-card", "badge-group", 0,
                Map.of("gap", 2), "", "");
        List<String> desTools = List.of("Figma", "Design Tokens", "User Research", "Usability Testing", "Prototyping", "Wireframing", "HTML / CSS", "WCAG Accessibility");
        for (int i = 0; i < desTools.size(); i++) {
            save("des-t-" + i, "des-tools-badges", "badge", i,
                    Map.of("mono", true, "bgColor", "#fafafa", "textColor", "#3f3f46", "borderColor", "#e4e4e7"),
                    desTools.get(i), desTools.get(i));
        }
    }

    private void seedBusinessMarketingTemplate() {
        save("biz-sec-hero", null, "section", 0,
                Map.of("anchor", "about", "canvasLabel", "Giới thiệu", "navLabel", "Giới thiệu", "padding", "none", "bgColor", "transparent"),
                "", "");
        save("biz-hero-card", "biz-sec-hero", "card", 0,
                Map.of("bgColor", "#ffffff", "borderColor", "#e4e4e7", "rounded", "lg", "padding", "lg", "gap", 3),
                "", "");
        save("biz-name", "biz-hero-card", "heading", 0,
                Map.of("level", "h1", "fontSize", "3xl", "textColor", "#18181b"),
                "Nguyễn Văn C", "Nguyen Van C");
        save("biz-role", "biz-hero-card", "text", 1,
                Map.of("fontSize", "sm", "textColor", "#1d4ed8"),
                "<p><strong>Growth Marketing &amp; Business Development Specialist</strong></p>",
                "<p><strong>Growth Marketing &amp; Business Development Specialist</strong></p>");
        save("biz-bio", "biz-hero-card", "text", 2,
                Map.of("fontSize", "sm", "textColor", "#52525b"),
                "<p>Chuyên viên Tăng trưởng &amp; Phát triển Kinh doanh với 4 năm kinh nghiệm triển khai chiến dịch Performance Marketing, tối ưu phễu chuyển đổi B2B/B2C và mở rộng đối tác chiến lược.</p>",
                "<p>Growth &amp; Business Development specialist with 4 years of experience scaling Performance Marketing campaigns, optimizing B2B/B2C funnels, and closing strategic partnerships.</p>");
        save("biz-contact", "biz-hero-card", "badge-group", 3,
                Map.of("gap", 2), "", "");
        save("biz-mail", "biz-contact", "link-button", 0,
                Map.of("href", "mailto:nguyenvanc@example.com", "icon", "mail", "bgColor", "#1d4ed8", "textColor", "#ffffff", "borderColor", "#1d4ed8", "rounded", "lg"),
                "nguyenvanc@example.com", "nguyenvanc@example.com");
        save("biz-linkedin", "biz-contact", "link-button", 1,
                Map.of("href", "https://linkedin.com", "target", "_blank", "icon", "linkedin", "bgColor", "#fafafa", "textColor", "#18181b", "borderColor", "#e4e4e7", "rounded", "lg"),
                "LinkedIn Profile", "LinkedIn Profile");

        save("biz-sec-kpi", null, "section", 1,
                Map.of("canvasLabel", "Thành tích KPI", "padding", "none", "bgColor", "transparent"),
                "", "");
        save("biz-kpi-grid", "biz-sec-kpi", "grid", 0,
                Map.of("cols", 4, "gap", 4), "", "");
        save("biz-k1", "biz-kpi-grid", "metric", 0,
                Map.of("value", "3.8x", "valueColor", "#1d4ed8", "labelColor", "#64748b", "bgColor", "#ffffff", "borderColor", "#e4e4e7", "borderWidth", "1", "borderStyle", "solid"),
                "ROAS trung bình", "Average ROAS");
        save("biz-k2", "biz-kpi-grid", "metric", 1,
                Map.of("value", "+145%", "valueColor", "#1d4ed8", "labelColor", "#64748b", "bgColor", "#ffffff", "borderColor", "#e4e4e7", "borderWidth", "1", "borderStyle", "solid"),
                "Tăng trưởng Lead MQL", "MQL Lead Growth");
        save("biz-k3", "biz-kpi-grid", "metric", 2,
                Map.of("value", "30+", "valueColor", "#1d4ed8", "labelColor", "#64748b", "bgColor", "#ffffff", "borderColor", "#e4e4e7", "borderWidth", "1", "borderStyle", "solid"),
                "Đối tác doanh nghiệp", "Enterprise partners");
        save("biz-k4", "biz-kpi-grid", "metric", 3,
                Map.of("value", "-22%", "valueColor", "#1d4ed8", "labelColor", "#64748b", "bgColor", "#ffffff", "borderColor", "#e4e4e7", "borderWidth", "1", "borderStyle", "solid"),
                "Giảm chi phí CAC", "Lowered CAC");

        save("biz-sec-exp", null, "section", 2,
                Map.of("anchor", "experience", "canvasLabel", "Kinh nghiệm & Chiến dịch", "navLabel", "Kinh nghiệm", "padding", "none", "bgColor", "transparent"),
                "", "");
        save("biz-exp-title", "biz-sec-exp", "heading", 0,
                Map.of("level", "h2", "fontSize", "sm", "textColor", "#71717a"),
                "KINH NGHIỆM &amp; CHIẾN DỊCH NỔI BẬT", "EXPERIENCE &amp; CAMPAIGNS");
        save("biz-exp-grid", "biz-sec-exp", "grid", 1,
                Map.of("cols", 2, "gap", 4), "", "");

        save("biz-e1", "biz-exp-grid", "card", 0,
                Map.of("bgColor", "#ffffff", "borderColor", "#e4e4e7", "rounded", "lg", "padding", "md", "gap", 2),
                "", "");
        save("biz-e1-badge", "biz-e1", "badge", 0,
                Map.of("bgColor", "#eff6ff", "textColor", "#1d4ed8", "borderColor", "#bfdbfe"),
                "2024 — Hiện tại", "2024 — Present");
        save("biz-e1-title", "biz-e1", "heading", 1,
                Map.of("level", "h3", "fontSize", "base", "textColor", "#18181b"),
                "Senior Growth Executive · NovaTech Commerce",
                "Senior Growth Executive · NovaTech Commerce");
        save("biz-e1-list", "biz-e1", "list", 2,
                Map.of("fontSize", "sm", "textColor", "#52525b"),
                "<ul><li>Quản lý ngân sách quảng cáo 400 triệu/tháng trên Google Ads, Meta &amp; TikTok với ROAS 3.8x.</li><li>Xây dựng phễu CRM HubSpot tự động nuôi dưỡng khách hàng B2B, tăng 42% tỷ lệ chốt đơn.</li></ul>",
                "<ul><li>Managed $16k/month paid media across Google, Meta &amp; TikTok achieving 3.8x blended ROAS.</li><li>Built automated HubSpot CRM nurturing sequences, lifting B2B deal close rate by 42%.</li></ul>");

        save("biz-e2", "biz-exp-grid", "card", 1,
                Map.of("bgColor", "#ffffff", "borderColor", "#e4e4e7", "rounded", "lg", "padding", "md", "gap", 2),
                "", "");
        save("biz-e2-badge", "biz-e2", "badge", 0,
                Map.of("bgColor", "#fafafa", "textColor", "#3f3f46", "borderColor", "#e4e4e7"),
                "2022 — 2024", "2022 — 2024");
        save("biz-e2-title", "biz-e2", "heading", 1,
                Map.of("level", "h3", "fontSize", "base", "textColor", "#18181b"),
                "Business Development Executive · CloudVN",
                "Business Development Executive · CloudVN");
        save("biz-e2-list", "biz-e2", "list", 2,
                Map.of("fontSize", "sm", "textColor", "#52525b"),
                "<ul><li>Phát triển mới 30+ khách hàng SMEs sử dụng gói giải pháp doanh nghiệp thường niên.</li><li>Phối hợp đội Product thiết kế tài liệu Sales Deck &amp; Case Study rút ngắn chu kỳ bán hàng.</li></ul>",
                "<ul><li>Onboarded 30+ SME accounts on annual enterprise SaaS subscriptions.</li><li>Partnered with Product to create sales decks and ROI case studies, shortening sales cycles.</li></ul>");

        save("biz-sec-skills", null, "section", 3,
                Map.of("anchor", "skills", "canvasLabel", "Kỹ năng chuyên môn", "navLabel", "Kỹ năng", "padding", "none", "bgColor", "transparent"),
                "", "");
        save("biz-skills-title", "biz-sec-skills", "heading", 0,
                Map.of("level", "h2", "fontSize", "sm", "textColor", "#71717a"),
                "KỸ NĂNG &amp; CÔNG CỤ", "SKILLS &amp; TOOLS");
        save("biz-skills-card", "biz-sec-skills", "card", 1,
                Map.of("bgColor", "#ffffff", "borderColor", "#e4e4e7", "rounded", "lg", "padding", "md"),
                "", "");
        save("biz-skills-badges", "biz-skills-card", "badge-group", 0,
                Map.of("gap", 2), "", "");
        List<String> bizSkills = List.of("Google Analytics 4", "Performance Ads", "SEO / Content", "HubSpot CRM", "B2B Sales", "A/B Testing", "SQL / Looker Studio", "Excel Modeling");
        for (int i = 0; i < bizSkills.size(); i++) {
            save("biz-s-" + i, "biz-skills-badges", "badge", i,
                    Map.of("mono", true, "bgColor", "#fafafa", "textColor", "#3f3f46", "borderColor", "#e4e4e7"),
                    bizSkills.get(i), bizSkills.get(i));
        }
    }
}
