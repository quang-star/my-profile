import type { ProfileData } from '@/types/profile'

// Bản nháp tổng hợp từ CV và GitHub; xác nhận các chi tiết còn thiếu trước khi chia sẻ.
export const isSampleProfile = true

export const profileData: ProfileData = {
  name: 'Nguyễn Kim Quang',
  initials: 'KQ',
  handle: '@quang-star',
  role: { vi: 'Java Backend Developer', en: 'Java Backend Developer' },
  availability: { vi: 'Định hướng Java Backend', en: 'Focused on Java Backend' },
  location: { vi: 'Việt Nam', en: 'Vietnam' },
  timezone: 'UTC+7',
  email: '',
  headline: {
    vi: 'Xây dựng ứng dụng và kết nối các hệ thống bằng Java.',
    en: 'Building applications and connecting systems with Java.',
  },
  bio: {
    vi: 'Mình là Quang, theo đuổi hướng phát triển Java Backend. Qua các dự án, mình làm việc với Spring Boot, REST API, cơ sở dữ liệu quan hệ và tích hợp dịch vụ bên ngoài.',
    en: 'I am Quang, focused on Java backend development. My projects involve Spring Boot, REST APIs, relational databases, and external service integrations.',
  },
  currentFocus: {
    vi: 'Java Backend, tích hợp hệ thống và ứng dụng AI vào sản phẩm.',
    en: 'Java backend, system integration, and applying AI to products.',
  },
  metrics: [
    {
      value: '6',
      label: { vi: 'Dự án GitHub', en: 'GitHub projects' },
    },
    {
      value: '3.61',
      label: { vi: 'GPA / 4.0 (HAU)', en: 'GPA / 4.0 (HAU)' },
    },
    {
      value: 'Java',
      label: { vi: 'Trọng tâm chính', en: 'Core focus' },
    },
    {
      value: '4',
      label: { vi: 'Nhóm kỹ năng', en: 'Skill areas' },
    },
  ],
  socials: [{ id: 'github', label: 'GitHub', handle: 'github.com/quang-star', url: 'https://github.com/quang-star' }],
  skills: [
    {
      id: 'backend', title: { vi: 'Backend & API', en: 'Backend & APIs' },
      description: { vi: 'Xây dựng API và xử lý nghiệp vụ với Java.', en: 'Building APIs and application logic with Java.' },
      accent: 'emerald', items: ['Java', 'Spring Boot', 'Spring Security', 'REST API', 'JWT'],
    },
    {
      id: 'data', title: { vi: 'Cơ sở dữ liệu', en: 'Databases' },
      description: { vi: 'Làm việc với dữ liệu quan hệ trong các dự án ứng dụng.', en: 'Working with relational data in application projects.' },
      accent: 'indigo', items: ['MySQL', 'PostgreSQL', 'SQL'],
    },
    {
      id: 'integration', title: { vi: 'Tích hợp & AI', en: 'Integration & AI' },
      description: { vi: 'Kết nối dịch vụ xác thực và AI trong ứng dụng.', en: 'Integrating authentication and AI services into applications.' },
      accent: 'amber', items: ['Firebase Authentication', 'Gemini API', 'Python', 'FastAPI'],
    },
    {
      id: 'web', title: { vi: 'Web & triển khai', en: 'Web & Deployment' },
      description: { vi: 'Các công nghệ đã sử dụng trong những dự án web và công cụ.', en: 'Technologies used across web and tooling projects.' },
      accent: 'sky', items: ['Vue 3', 'JavaScript', 'PHP', 'Laravel', 'Git', 'Oracle Cloud'],
    },
  ],
  experiences: [],
  education: [{
    id: 'education',
    school: 'Đại học Kiến trúc Hà Nội',
    degree: { vi: 'Công nghệ thông tin', en: 'Information Technology' },
    period: { vi: '2022 — Hiện tại', en: '2022 — Present' },
    gpa: '3.61 / 4.0',
    details: {
      vi: 'Tập trung nền tảng lập trình hướng đối tượng, cơ sở dữ liệu quan hệ và phát triển hệ thống backend.',
      en: 'Focused on object-oriented programming, relational databases, and backend system development.',
    },
  }],
  languages: { vi: [], en: [] },
  projects: [
    {
      id: 'recruitment', title: 'Recruitment Management System', category: 'fullstack', featured: true, year: '2026',
      tagline: { vi: 'Hệ thống tuyển dụng kết nối ứng viên và nhà tuyển dụng.', en: 'A recruitment system connecting candidates and recruiters.' },
      description: { vi: 'Dự án quản lý hồ sơ ứng viên, tin tuyển dụng và đơn ứng tuyển; kết hợp xử lý CV và gợi ý phù hợp giữa ứng viên với công việc.', en: 'A project for candidate profiles, job postings, and applications, with CV processing and candidate-job matching.' },
      highlights: { vi: ['REST API cho hồ sơ ứng viên, tin tuyển dụng và đơn ứng tuyển.', 'Xác thực JWT và phân quyền cho ứng viên và nhà tuyển dụng.'], en: ['REST APIs for candidate profiles, job postings, and applications.', 'JWT authentication and role-based access for candidates and recruiters.'] },
      stack: ['Java', 'Spring Boot', 'PostgreSQL', 'Python', 'FastAPI'],
      repoUrl: 'https://github.com/quang-star/recruitment-system',
    },
    {
      id: 'finance', title: 'Smart Personal Finance App', category: 'fullstack', featured: true, year: '2026',
      tagline: { vi: 'Ứng dụng quản lý tài chính cá nhân và giao dịch thu chi.', en: 'A personal finance application for tracking income and expenses.' },
      description: { vi: 'Ứng dụng quản lý thu chi sử dụng Spring Boot và MySQL, kết hợp xác thực Firebase và Gemini API trong luồng xử lý thông tin từ hóa đơn.', en: 'An income and expense application using Spring Boot and MySQL, with Firebase authentication and Gemini API in its receipt-processing workflow.' },
      highlights: { vi: ['API và lưu trữ dữ liệu cho quản lý thu chi.', 'Tích hợp Firebase Authentication và Gemini API.'], en: ['APIs and data storage for income and expense management.', 'Integrates Firebase Authentication and Gemini API.'] },
      stack: ['Java', 'Spring Boot', 'MySQL', 'Firebase', 'Gemini API'],
      repoUrl: 'https://github.com/quang-star/finace-app',
    },
    {
      id: 'ticket', title: 'Ticket — Event Booking', category: 'fullstack', year: '2026',
      tagline: { vi: 'Nền tảng đặt vé và quản lý sự kiện trực tuyến.', en: 'An online event booking and management platform.' },
      description: { vi: 'Ứng dụng mô phỏng nền tảng đặt vé cho khách hàng, ban tổ chức và quản trị viên, với frontend Vue và backend Laravel.', en: 'A ticketing platform for customers, organizers, and administrators, with a Vue frontend and Laravel backend.' },
      highlights: { vi: ['Tìm kiếm sự kiện, đặt vé và quản lý đơn hàng.', 'Tạo và kiểm duyệt sự kiện; tích hợp PayOS và mã QR.'], en: ['Event discovery, ticket booking, and order management.', 'Event creation and moderation, with PayOS and QR codes.'] },
      stack: ['Vue 3', 'Laravel', 'MySQL', 'Pinia', 'PayOS'],
      repoUrl: 'https://github.com/quang-star/TicketFE', secondaryRepoUrl: 'https://github.com/quang-star/TicketBE',
    },
    {
      id: 'ai-sdlc', title: 'AI-SDLC', category: 'tooling', year: '2026',
      tagline: { vi: 'Dự án tìm hiểu quy trình phát triển phần mềm với AI.', en: 'Exploring AI-assisted software development workflows.' },
      description: { vi: 'Dự án thử nghiệm điều phối AI trong quy trình phát triển phần mềm, với mã nguồn Python, workflow và bộ đánh giá.', en: 'An exploration of AI orchestration for software development, with Python code, workflows, and evaluations.' },
      highlights: { vi: ['Tìm hiểu điều phối quy trình phát triển với AI.', 'Có workflow, công cụ đánh giá và kiểm thử.'], en: ['Explores AI-assisted development orchestration.', 'Includes workflows, evaluations, and tests.'] },
      stack: ['Python', 'AI', 'Workflows'], repoUrl: 'https://github.com/quang-star/AI-SDLC',
    },
    {
      id: 'admission', title: 'Online Admission', category: 'fullstack', year: '2026',
      tagline: { vi: 'Dự án tuyển sinh trực tuyến.', en: 'An online admissions project.' },
      description: { vi: 'Dự án gồm frontend, backend và một dịch vụ RAG.', en: 'A project containing frontend, backend, and RAG service components.' },
      highlights: { vi: ['Tách biệt frontend và backend.', 'Có thành phần dịch vụ RAG.'], en: ['Separate frontend and backend.', 'Includes a RAG service component.'] },
      stack: ['Vue', 'RAG'], repoUrl: 'https://github.com/quang-star/OnlineAdmission',
    },
    {
      id: 'studio', title: '4ViewStudio', category: 'fullstack', year: '2025',
      tagline: { vi: 'Website đặt lịch cho studio.', en: 'A studio booking website.' },
      description: { vi: 'Dự án website đặt lịch studio sử dụng Laravel.', en: 'A studio booking website built with Laravel.' },
      highlights: { vi: ['Website đặt lịch studio.', 'Ứng dụng xây dựng trên Laravel.'], en: ['Studio booking website.', 'Built with Laravel.'] },
      stack: ['PHP', 'Laravel', 'Blade'], repoUrl: 'https://github.com/quang-star/4ViewStudio',
    },
  ],
}
