package dev.quang.portfolio.service;

import dev.quang.portfolio.dto.NodeDtos.SaveNodeRequest;
import dev.quang.portfolio.repository.ContentNodeRepository;
import java.util.List;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(2)
public class NodeDataSeeder implements CommandLineRunner {

    private final ContentNodeRepository nodeRepository;
    private final NodeService nodeService;

    public NodeDataSeeder(ContentNodeRepository nodeRepository, NodeService nodeService) {
        this.nodeRepository = nodeRepository;
        this.nodeService = nodeService;
    }

    @Override
    public void run(String... args) {
        if (nodeRepository.count() > 0) {
            return;
        }
        seedDefaultTree();
    }

    @Transactional
    public void resetDefaultTree() {
        nodeRepository.deleteAll();
        seedDefaultTree();
    }

    private void seedDefaultTree() {
        // 1. HERO / ABOUT SECTION
        save("sec-hero", null, "section", 0,
                Map.of("anchor", "about", "padding", "md", "bgColor", "transparent"),
                "", "");

        save("hero-main", "sec-hero", "card", 0,
                Map.of("bgColor", "#ffffff", "borderColor", "#e4e4e7", "rounded", "lg", "padding", "lg"),
                "", "");

        save("hero-status-row", "hero-main", "badge-group", 0,
                Map.of("gap", 2),
                "", "");
        save("hero-status-badge", "hero-status-row", "badge", 0,
                Map.of("bgColor", "#ecfdf5", "textColor", "#065f46", "borderColor", "#a7f3d0", "dotColor", "#16a34a"),
                "Sẵn sàng Intern / Fresher", "Open to opportunities");
        save("hero-loc-badge", "hero-status-row", "badge", 1,
                Map.of("mono", true, "bgColor", "#fafafa", "textColor", "#52525b", "borderColor", "#e4e4e7"),
                "Hà Nội, VN", "Ha Noi, VN");

        save("hero-title", "hero-main", "heading", 1,
                Map.of("level", "h1", "fontSize", "3xl", "textColor", "#18181b"),
                "Nguyễn Kim Quang", "Nguyen Kim Quang");

        save("hero-role", "hero-main", "text", 2,
                Map.of("fontSize", "sm", "textColor", "#047857"),
                "<p><strong>Software Developer Intern / Fresher — Java Backend &amp; Full-stack</strong></p>",
                "<p><strong>Software Developer Intern / Fresher — Java Backend &amp; Full-stack</strong></p>");

        save("hero-bio", "hero-main", "text", 3,
                Map.of("fontSize", "sm", "textColor", "#52525b"),
                "<p>Sinh viên năm cuối ngành Công nghệ thông tin tập trung vào phát triển backend với <span class=\"inline-tag\" data-color=\"emerald\">Java / Spring Boot</span>, <span class=\"inline-tag\" data-color=\"indigo\">Laravel</span>, <span class=\"inline-tag\" data-color=\"sky\">Vue.js</span> và kiến trúc RESTful API sạch. Mong muốn gia nhập đội ngũ kỹ thuật thực tế để xây dựng sản phẩm tin cậy và nâng cao tư duy thiết kế hệ thống.</p>",
                "<p>Final-year Information Technology student focused on backend and full-stack development (<span class=\"inline-tag\" data-color=\"emerald\">Java / Spring Boot</span>, <span class=\"inline-tag\" data-color=\"indigo\">Laravel</span>, <span class=\"inline-tag\" data-color=\"sky\">Vue.js</span>) and clean RESTful API architecture. Seeking a Software Developer Intern/Fresher role to build reliable products and sharpen system design thinking.</p>");

        save("hero-actions", "hero-main", "badge-group", 4,
                Map.of("gap", 2),
                "", "");
        save("hero-btn-github", "hero-actions", "link-button", 0,
                Map.of("href", "https://github.com/quang-star", "target", "_blank", "icon", "github", "bgColor", "#18181b", "textColor", "#ffffff", "borderColor", "#18181b"),
                "github.com/quang-star", "github.com/quang-star");
        save("hero-btn-email", "hero-actions", "link-button", 1,
                Map.of("href", "mailto:nguyenkimquang1612@gmail.com", "icon", "mail", "bgColor", "#fafafa", "textColor", "#3f3f46", "borderColor", "#e4e4e7"),
                "nguyenkimquang1612@gmail.com", "nguyenkimquang1612@gmail.com");
        save("hero-btn-contact", "hero-actions", "link-button", 2,
                Map.of("href", "#contact", "icon", "send", "bgColor", "#ecfdf5", "textColor", "#065f46", "borderColor", "#a7f3d0"),
                "Liên hệ", "Contact");

        // 2. SKILLS SECTION (Single key-value table card)
        save("sec-skills", null, "section", 1,
                Map.of("anchor", "skills", "padding", "sm", "bgColor", "transparent"),
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
                Map.of("anchor", "projects", "padding", "sm", "bgColor", "transparent"),
                "", "");
        save("proj-header", "sec-projects", "heading", 0,
                Map.of("level", "h2", "fontSize", "sm", "textColor", "#71717a"),
                "DỰ ÁN TIÊU BIỂU", "FEATURED PROJECTS");
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
                Map.of("anchor", "experience", "padding", "sm", "bgColor", "transparent"),
                "", "");
        save("exp-header", "sec-exp", "heading", 0,
                Map.of("level", "h2", "fontSize", "sm", "textColor", "#71717a"),
                "HỌC VẤN", "EDUCATION");
        save("edu-card", "sec-exp", "card", 1,
                Map.of("bgColor", "#ffffff", "borderColor", "#e4e4e7", "rounded", "lg", "padding", "md"),
                "", "");
        save("edu-title", "edu-card", "heading", 0,
                Map.of("level", "h3", "fontSize", "base", "textColor", "#18181b"),
                "Đại học Kiến trúc Hà Nội (HAU)", "Hanoi Architectural University (HAU)");
        save("edu-desc", "edu-card", "text", 1,
                Map.of("fontSize", "xs", "textColor", "#52525b"),
                "<p>Cử nhân Công nghệ thông tin · <span class=\"inline-tag\">2022 — Hiện tại</span></p>",
                "<p>Bachelor of Information Technology · <span class=\"inline-tag\">2022 — Present</span></p>");
        save("edu-badges", "edu-card", "badge-group", 2,
                Map.of("gap", 2),
                "", "");
        save("edu-gpa", "edu-badges", "badge", 0,
                Map.of("mono", true, "bgColor", "#fafafa", "textColor", "#18181b", "borderColor", "#e4e4e7"),
                "GPA: 3.61/4.0", "GPA: 3.61/4.0");
        save("edu-track", "edu-badges", "badge", 1,
                Map.of("mono", true, "bgColor", "#ecfdf5", "textColor", "#065f46", "borderColor", "#a7f3d0"),
                "Excellent Track", "Excellent Track");

        // 5. CONTACT SECTION
        save("sec-contact", null, "section", 4,
                Map.of("anchor", "contact", "padding", "sm", "bgColor", "transparent"),
                "", "");
        save("contact-header", "sec-contact", "heading", 0,
                Map.of("level", "h2", "fontSize", "sm", "textColor", "#71717a"),
                "LIÊN HỆ", "CONTACT");
        save("contact-card", "sec-contact", "card", 1,
                Map.of("bgColor", "#ffffff", "borderColor", "#e4e4e7", "rounded", "lg", "padding", "lg"),
                "", "");
        save("contact-desc", "contact-card", "text", 0,
                Map.of("fontSize", "sm", "textColor", "#52525b"),
                "<p>Gửi tin nhắn trực tiếp qua form bên dưới hoặc liên hệ qua <a href=\"mailto:nguyenkimquang1612@gmail.com\">nguyenkimquang1612@gmail.com</a>.</p>",
                "<p>Send a direct message below or reach out via <a href=\"mailto:nguyenkimquang1612@gmail.com\">nguyenkimquang1612@gmail.com</a>.</p>");
        save("contact-form-node", "contact-card", "contact-form", 1,
                Map.of("accentColor", "#047857"),
                "", "");
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
                Map.of("bgColor", "#ecfdf5", "textColor", "#065f46", "borderColor", "#a7f3d0"),
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
        nodeService.saveNode(id, new SaveNodeRequest(id, parentId, tag, sortOrder, attrs, contentVi, contentEn, null));
    }
}
