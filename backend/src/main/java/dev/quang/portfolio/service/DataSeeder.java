package dev.quang.portfolio.service;

import dev.quang.portfolio.dto.PortfolioDtos.*;
import dev.quang.portfolio.repository.ProfileRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final ProfileRepository profileRepository;
    private final PortfolioService portfolioService;

    public DataSeeder(ProfileRepository profileRepository, PortfolioService portfolioService) {
        this.profileRepository = profileRepository;
        this.portfolioService = portfolioService;
    }

    @Override
    public void run(String... args) {
        if (profileRepository.existsById(1L)) {
            return;
        }

        portfolioService.updateProfile(new ProfileUpdateRequest(
                "Nguyễn Kim Quang",
                "KQ",
                null,
                "@quang-star",
                new LocalizedTextDto("Java Backend Developer", "Java Backend Developer"),
                new LocalizedTextDto("Định hướng Java Backend", "Focused on Java Backend"),
                new LocalizedTextDto("Việt Nam", "Vietnam"),
                "UTC+7",
                "",
                null,
                null,
                new LocalizedTextDto(
                        "Xây dựng ứng dụng và kết nối các hệ thống bằng Java.",
                        "Building applications and connecting systems with Java."
                ),
                new LocalizedTextDto(
                        "Mình là Quang, theo đuổi hướng phát triển Java Backend. Qua các dự án, mình làm việc với Spring Boot, REST API, cơ sở dữ liệu quan hệ và tích hợp dịch vụ bên ngoài.",
                        "I am Quang, focused on Java backend development. My projects involve Spring Boot, REST APIs, relational databases, and external service integrations."
                ),
                new LocalizedTextDto(
                        "Java Backend, tích hợp hệ thống và ứng dụng AI vào sản phẩm.",
                        "Java backend, system integration, and applying AI to products."
                ),
                List.of(
                        new HighlightMetricDto("6", new LocalizedTextDto("Dự án GitHub", "GitHub projects")),
                        new HighlightMetricDto("3.61", new LocalizedTextDto("GPA / 4.0 (HAU)", "GPA / 4.0 (HAU)")),
                        new HighlightMetricDto("Java", new LocalizedTextDto("Trọng tâm chính", "Core focus")),
                        new HighlightMetricDto("4", new LocalizedTextDto("Nhóm kỹ năng", "Skill areas"))
                ),
                List.of(
                        new SocialLinkDto("github", "GitHub", "github.com/quang-star", "https://github.com/quang-star")
                ),
                new LocalizedListDto(List.of(), List.of()),
                true
        ));

        portfolioService.saveSkill("backend", new SkillGroupDto(
                "backend",
                new LocalizedTextDto("Backend & API", "Backend & APIs"),
                new LocalizedTextDto("Xây dựng API và xử lý nghiệp vụ với Java.", "Building APIs and application logic with Java."),
                "emerald",
                List.of("Java", "Spring Boot", "Spring Security", "REST API", "JWT"),
                0
        ));
        portfolioService.saveSkill("data", new SkillGroupDto(
                "data",
                new LocalizedTextDto("Cơ sở dữ liệu", "Databases"),
                new LocalizedTextDto("Làm việc với dữ liệu quan hệ trong các dự án ứng dụng.", "Working with relational data in application projects."),
                "indigo",
                List.of("MySQL", "PostgreSQL", "SQL"),
                1
        ));
        portfolioService.saveSkill("integration", new SkillGroupDto(
                "integration",
                new LocalizedTextDto("Tích hợp & AI", "Integration & AI"),
                new LocalizedTextDto("Kết nối dịch vụ xác thực và AI trong ứng dụng.", "Integrating authentication and AI services into applications."),
                "amber",
                List.of("Firebase Authentication", "Gemini API", "Python", "FastAPI"),
                2
        ));
        portfolioService.saveSkill("web", new SkillGroupDto(
                "web",
                new LocalizedTextDto("Web & triển khai", "Web & Deployment"),
                new LocalizedTextDto("Các công nghệ đã sử dụng trong những dự án web và công cụ.", "Technologies used across web and tooling projects."),
                "sky",
                List.of("Vue 3", "JavaScript", "PHP", "Laravel", "Git", "Oracle Cloud"),
                3
        ));

        portfolioService.saveEducation("education", new EducationDto(
                "education",
                "Đại học Kiến trúc Hà Nội",
                new LocalizedTextDto("Công nghệ thông tin", "Information Technology"),
                new LocalizedTextDto("2022 — Hiện tại", "2022 — Present"),
                "3.61 / 4.0",
                new LocalizedTextDto(
                        "Tập trung nền tảng lập trình hướng đối tượng, cơ sở dữ liệu quan hệ và phát triển hệ thống backend.",
                        "Focused on object-oriented programming, relational databases, and backend system development."
                ),
                0
        ));

        portfolioService.saveProject("recruitment", new ProjectDto(
                "recruitment",
                "Recruitment Management System",
                "fullstack",
                true,
                "2026",
                new LocalizedTextDto(
                        "Hệ thống tuyển dụng kết nối ứng viên và nhà tuyển dụng.",
                        "A recruitment system connecting candidates and recruiters."
                ),
                new LocalizedTextDto(
                        "Dự án quản lý hồ sơ ứng viên, tin tuyển dụng và đơn ứng tuyển; kết hợp xử lý CV và gợi ý phù hợp giữa ứng viên với công việc.",
                        "A project for candidate profiles, job postings, and applications, with CV processing and candidate-job matching."
                ),
                new LocalizedListDto(
                        List.of(
                                "REST API cho hồ sơ ứng viên, tin tuyển dụng và đơn ứng tuyển.",
                                "Xác thực JWT và phân quyền cho ứng viên và nhà tuyển dụng."
                        ),
                        List.of(
                                "REST APIs for candidate profiles, job postings, and applications.",
                                "JWT authentication and role-based access for candidates and recruiters."
                        )
                ),
                List.of("Java", "Spring Boot", "PostgreSQL", "Python", "FastAPI"),
                null,
                "https://github.com/quang-star/recruitment-system",
                null,
                0
        ));

        portfolioService.saveProject("finance", new ProjectDto(
                "finance",
                "Smart Personal Finance App",
                "fullstack",
                true,
                "2026",
                new LocalizedTextDto(
                        "Ứng dụng quản lý tài chính cá nhân và giao dịch thu chi.",
                        "A personal finance application for tracking income and expenses."
                ),
                new LocalizedTextDto(
                        "Ứng dụng quản lý thu chi sử dụng Spring Boot và MySQL, kết hợp xác thực Firebase và Gemini API trong luồng xử lý thông tin từ hóa đơn.",
                        "An income and expense application using Spring Boot and MySQL, with Firebase authentication and Gemini API in its receipt-processing workflow."
                ),
                new LocalizedListDto(
                        List.of(
                                "API và lưu trữ dữ liệu cho quản lý thu chi.",
                                "Tích hợp Firebase Authentication và Gemini API."
                        ),
                        List.of(
                                "APIs and data storage for income and expense management.",
                                "Integrates Firebase Authentication and Gemini API."
                        )
                ),
                List.of("Java", "Spring Boot", "MySQL", "Firebase", "Gemini API"),
                null,
                "https://github.com/quang-star/finace-app",
                null,
                1
        ));

        portfolioService.saveProject("ticket", new ProjectDto(
                "ticket",
                "Ticket — Event Booking",
                "fullstack",
                false,
                "2026",
                new LocalizedTextDto(
                        "Nền tảng đặt vé và quản lý sự kiện trực tuyến.",
                        "An online event booking and management platform."
                ),
                new LocalizedTextDto(
                        "Ứng dụng mô phỏng nền tảng đặt vé cho khách hàng, ban tổ chức và quản trị viên, với frontend Vue và backend Laravel.",
                        "A ticketing platform for customers, organizers, and administrators, with a Vue frontend and Laravel backend."
                ),
                new LocalizedListDto(
                        List.of(
                                "Tìm kiếm sự kiện, đặt vé và quản lý đơn hàng.",
                                "Tạo và kiểm duyệt sự kiện; tích hợp PayOS và mã QR."
                        ),
                        List.of(
                                "Event discovery, ticket booking, and order management.",
                                "Event creation and moderation, with PayOS and QR codes."
                        )
                ),
                List.of("Vue 3", "Laravel", "MySQL", "Pinia", "PayOS"),
                null,
                "https://github.com/quang-star/TicketFE",
                "https://github.com/quang-star/TicketBE",
                2
        ));

        portfolioService.saveProject("ai-sdlc", new ProjectDto(
                "ai-sdlc",
                "AI-SDLC",
                "tooling",
                false,
                "2026",
                new LocalizedTextDto(
                        "Dự án tìm hiểu quy trình phát triển phần mềm với AI.",
                        "Exploring AI-assisted software development workflows."
                ),
                new LocalizedTextDto(
                        "Dự án thử nghiệm điều phối AI trong quy trình phát triển phần mềm, với mã nguồn Python, workflow và bộ đánh giá.",
                        "An exploration of AI orchestration for software development, with Python code, workflows, and evaluations."
                ),
                new LocalizedListDto(
                        List.of(
                                "Tìm hiểu điều phối quy trình phát triển với AI.",
                                "Có workflow, công cụ đánh giá và kiểm thử."
                        ),
                        List.of(
                                "Explores AI-assisted development orchestration.",
                                "Includes workflows, evaluations, and tests."
                        )
                ),
                List.of("Python", "AI", "Workflows"),
                null,
                "https://github.com/quang-star/AI-SDLC",
                null,
                3
        ));

        portfolioService.saveProject("admission", new ProjectDto(
                "admission",
                "Online Admission",
                "fullstack",
                false,
                "2026",
                new LocalizedTextDto(
                        "Dự án tuyển sinh trực tuyến.",
                        "An online admissions project."
                ),
                new LocalizedTextDto(
                        "Dự án gồm frontend, backend và một dịch vụ RAG.",
                        "A project containing frontend, backend, and RAG service components."
                ),
                new LocalizedListDto(
                        List.of("Tách biệt frontend và backend.", "Có thành phần dịch vụ RAG."),
                        List.of("Separate frontend and backend.", "Includes a RAG service component.")
                ),
                List.of("Vue", "RAG"),
                null,
                "https://github.com/quang-star/OnlineAdmission",
                null,
                4
        ));

        portfolioService.saveProject("studio", new ProjectDto(
                "studio",
                "4ViewStudio",
                "fullstack",
                false,
                "2025",
                new LocalizedTextDto(
                        "Website đặt lịch cho studio.",
                        "A studio booking website."
                ),
                new LocalizedTextDto(
                        "Dự án website đặt lịch studio sử dụng Laravel.",
                        "A studio booking website built with Laravel."
                ),
                new LocalizedListDto(
                        List.of("Website đặt lịch studio.", "Ứng dụng xây dựng trên Laravel."),
                        List.of("Studio booking website.", "Built with Laravel.")
                ),
                List.of("PHP", "Laravel", "Blade"),
                null,
                "https://github.com/quang-star/4ViewStudio",
                null,
                5
        ));
    }
}
