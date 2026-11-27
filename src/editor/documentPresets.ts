import type { JSONContent } from '@tiptap/core'

export type DocPresetCategory = 'page' | 'cv'

export interface DocBlockPreset {
  id: string
  category: DocPresetCategory
  icon: string
  vi: string
  en: string
  descVi: string
  descEn: string
  build: (isVi: boolean) => JSONContent
}

export const DOC_BLOCK_PRESETS: DocBlockPreset[] = [
  {
    id: 'box-card',
    category: 'page',
    icon: 'square',
    vi: 'Khung nội dung (Card)',
    en: 'Content Box / Card',
    descVi: 'Nhét tự do chữ, ảnh, bảng hoặc cột vào trong 1 khung có nền & viền.',
    descEn: 'Nest text, images, tables or columns inside a styled card.',
    build: (isVi) => ({
      type: 'boxBlock',
      attrs: {
        bgColor: '#ffffff',
        textColor: '',
        borderColor: '#e4e4e7',
        borderWidth: 1,
        borderSide: 'all',
        radius: 12,
        shadow: 'sm',
        paddingX: 20,
        paddingY: 16,
        marginBottom: 14,
        widthPct: 100,
        align: 'left',
      },
      content: [
        {
          type: 'heading',
          attrs: { level: 3 },
          content: [{ type: 'text', text: isVi ? 'Tiêu đề khung mới' : 'New Box Title' }],
        },
        {
          type: 'paragraph',
          content: [{ type: 'text', text: isVi ? 'Gõ nội dung hoặc chèn thêm ảnh, danh sách, bảng vào trong khung này...' : 'Type content or insert lists, images, tables inside...' }],
        },
      ],
    }),
  },
  {
    id: 'banner-header',
    category: 'page',
    icon: 'sparkles',
    vi: 'Hero Banner đầu trang (Màu nổi)',
    en: 'Hero Header Banner',
    descVi: 'Khối giới thiệu nổi bật với nền màu đậm và thông tin liên hệ.',
    descEn: 'Bold header banner with dark/accent background and contact info.',
    build: (isVi) => ({
      type: 'boxBlock',
      attrs: {
        bgColor: '#1e293b',
        textColor: '#f8fafc',
        borderColor: 'transparent',
        borderWidth: 0,
        borderSide: 'all',
        radius: 14,
        shadow: 'sm',
        paddingX: 24,
        paddingY: 20,
        marginBottom: 16,
        widthPct: 100,
        align: 'left',
      },
      content: [
        {
          type: 'heading',
          attrs: { level: 1 },
          content: [{ type: 'text', text: isVi ? 'Họ và Tên' : 'Full Name', marks: [{ type: 'textStyle', attrs: { color: '#ffffff', fontSize: '28px' } }] }],
        },
        {
          type: 'paragraph',
          content: [{ type: 'text', text: isVi ? 'Vị trí · Chuyên môn chính' : 'Role · Key Expertise', marks: [{ type: 'bold' }, { type: 'textStyle', attrs: { color: '#38bdf8' } }] }],
        },
        {
          type: 'paragraph',
          content: [{ type: 'text', text: 'Email: email@example.com  ·  SĐT: 0900 000 000  ·  LinkedIn / GitHub', marks: [{ type: 'textStyle', attrs: { color: '#cbd5e1', fontSize: '13px' } }] }],
        },
      ],
    }),
  },
  {
    id: 'callout-left',
    category: 'page',
    icon: 'quote',
    vi: 'Khối nhấn viền trái (Callout)',
    en: 'Left-accent Callout',
    descVi: 'Thích hợp cho Giới thiệu, Ghi chú hoặc Quote nổi bật.',
    descEn: 'Great for Bio summary, Highlight or Quote block.',
    build: (isVi) => ({
      type: 'boxBlock',
      attrs: {
        bgColor: '#f8fafc',
        textColor: '',
        borderColor: '#0f766e',
        borderWidth: 4,
        borderSide: 'left',
        radius: 8,
        shadow: 'none',
        paddingX: 18,
        paddingY: 14,
        marginBottom: 14,
        widthPct: 100,
        align: 'left',
      },
      content: [
        {
          type: 'paragraph',
          content: [{ type: 'text', text: isVi ? 'Tóm tắt điểm mạnh nổi bật, số năm kinh nghiệm và định hướng công việc của bạn.' : 'Summarize your key strengths, years of experience and career focus.' }],
        },
      ],
    }),
  },
  {
    id: 'cols-topcv',
    category: 'page',
    icon: 'columns-2',
    vi: 'Chia 2 cột (34% / 66%)',
    en: '2 Columns (34% / 66%)',
    descVi: 'Cột trái có nền nhẹ cho Sidebar/Liên hệ, cột phải cho Nội dung chính.',
    descEn: 'Tinted left sidebar for Info/Contact, right column for Main content.',
    build: (isVi) => ({
      type: 'columnsBlock',
      attrs: { widths: [34, 66], gap: 20, marginBottom: 16, stackMobile: true },
      content: [
        {
          type: 'columnCell',
          attrs: { bgColor: '#f8fafc', textColor: '', borderColor: '#e2e8f0', paddingX: 14, paddingY: 14, radius: 12 },
          content: [
            { type: 'heading', attrs: { level: 3 }, content: [{ type: 'text', text: isVi ? 'Cột trái' : 'Left column' }] },
            { type: 'paragraph', content: [{ type: 'text', text: isVi ? 'Thêm thông tin liên hệ, kỹ năng, giới thiệu...' : 'Add contact info, skills, summary...' }] },
          ],
        },
        {
          type: 'columnCell',
          attrs: { bgColor: 'transparent', textColor: '', borderColor: 'transparent', paddingX: 8, paddingY: 6, radius: 10 },
          content: [
            { type: 'heading', attrs: { level: 3 }, content: [{ type: 'text', text: isVi ? 'Cột phải' : 'Right column' }] },
            { type: 'paragraph', content: [{ type: 'text', text: isVi ? 'Thêm kinh nghiệm làm việc, dự án...' : 'Add experience, projects...' }] },
          ],
        },
      ],
    }),
  },
  {
    id: 'cols-equal-2',
    category: 'page',
    icon: 'columns-2',
    vi: 'Chia 2 cột đều (50% / 50%)',
    en: '2 Equal Columns (50% / 50%)',
    descVi: 'Hai cột bằng nhau cho Dự án, Thẻ showcase hoặc So sánh 2 mục.',
    descEn: 'Two equal columns for Projects, Showcase or side-by-side info.',
    build: (isVi) => ({
      type: 'columnsBlock',
      attrs: { widths: [50, 50], gap: 16, marginBottom: 16, stackMobile: true },
      content: [
        {
          type: 'columnCell',
          attrs: { bgColor: '#ffffff', textColor: '', borderColor: '#e4e4e7', paddingX: 14, paddingY: 14, radius: 12 },
          content: [
            { type: 'heading', attrs: { level: 3 }, content: [{ type: 'text', text: isVi ? 'Cột 1' : 'Column 1' }] },
            { type: 'paragraph', content: [{ type: 'text', text: isVi ? 'Nội dung cột 1...' : 'Column 1 content...' }] },
          ],
        },
        {
          type: 'columnCell',
          attrs: { bgColor: '#ffffff', textColor: '', borderColor: '#e4e4e7', paddingX: 14, paddingY: 14, radius: 12 },
          content: [
            { type: 'heading', attrs: { level: 3 }, content: [{ type: 'text', text: isVi ? 'Cột 2' : 'Column 2' }] },
            { type: 'paragraph', content: [{ type: 'text', text: isVi ? 'Nội dung cột 2...' : 'Column 2 content...' }] },
          ],
        },
      ],
    }),
  },
  {
    id: 'cols-3',
    category: 'page',
    icon: 'columns-3',
    vi: 'Chia 3 cột (33% / 33% / 34%)',
    en: '3 Columns (33% / 33% / 34%)',
    descVi: 'Phù hợp cho 3 thẻ số liệu, kỹ năng hoặc dịch vụ.',
    descEn: 'Great for 3 stat cards, skill groups or highlights.',
    build: (isVi) => ({
      type: 'columnsBlock',
      attrs: { widths: [33, 33, 34], gap: 14, marginBottom: 16, stackMobile: true },
      content: [
        {
          type: 'columnCell',
          attrs: { bgColor: '#ffffff', borderColor: '#e4e4e7', paddingX: 14, paddingY: 14, radius: 12 },
          content: [{ type: 'paragraph', content: [{ type: 'text', text: isVi ? 'Mục 1' : 'Item 1', marks: [{ type: 'bold' }] }] }],
        },
        {
          type: 'columnCell',
          attrs: { bgColor: '#ffffff', borderColor: '#e4e4e7', paddingX: 14, paddingY: 14, radius: 12 },
          content: [{ type: 'paragraph', content: [{ type: 'text', text: isVi ? 'Mục 2' : 'Item 2', marks: [{ type: 'bold' }] }] }],
        },
        {
          type: 'columnCell',
          attrs: { bgColor: '#ffffff', borderColor: '#e4e4e7', paddingX: 14, paddingY: 14, radius: 12 },
          content: [{ type: 'paragraph', content: [{ type: 'text', text: isVi ? 'Mục 3' : 'Item 3', marks: [{ type: 'bold' }] }] }],
        },
      ],
    }),
  },
  {
    id: 'skill-bar',
    category: 'page',
    icon: 'bar-chart-2',
    vi: 'Thanh mức độ kỹ năng (%)',
    en: 'Skill Progress Bar (%)',
    descVi: 'Thanh kéo trực quan thể hiện mức độ thành thạo.',
    descEn: 'Visual progress bar with editable skill label & %.',
    build: (isVi) => ({
      type: 'skillBarBlock',
      attrs: { label: isVi ? 'Kỹ năng chuyên môn' : 'Core Skill', level: 85, color: '', showPercent: true, marginBottom: 10 },
    }),
  },
  {
    id: 'badge-list',
    category: 'page',
    icon: 'tag',
    vi: 'Nhóm nhãn kỹ năng (Tags)',
    en: 'Skill Badges / Tags',
    descVi: 'Hiển thị danh sách công nghệ, từ khóa dạng pill gọn gàng.',
    descEn: 'Compact pill badges for skills and keywords.',
    build: () => ({
      type: 'badgeListBlock',
      attrs: { items: ['JavaScript', 'TypeScript', 'Vue 3', 'Docker'], align: 'left', marginBottom: 10 },
    }),
  },
  {
    id: 'link-buttons',
    category: 'page',
    icon: 'link',
    vi: 'Nút liên kết (Email / GitHub)',
    en: 'Action Link Buttons',
    descVi: 'Các nút bấm dẫn tới Email, LinkedIn, GitHub hoặc Demo.',
    descEn: 'Clickable buttons linking to Email, LinkedIn or GitHub.',
    build: (isVi) => ({
      type: 'linkButtonBlock',
      attrs: {
        items: [
          { label: isVi ? 'Liên hệ Email' : 'Email Me', href: 'mailto:you@example.com', variant: 'primary' },
          { label: 'GitHub', href: 'https://github.com', variant: 'outline' },
        ],
        align: 'left',
        marginBottom: 10,
      },
    }),
  },
  {
    id: 'cv-classic-header',
    category: 'cv',
    icon: 'user',
    vi: 'Đầu trang CV cổ điển (Căn giữa)',
    en: 'Classic CV Header (Centered)',
    descVi: 'Họ tên lớn, chức danh và dòng liên hệ căn giữa kiểu chuẩn ATS.',
    descEn: 'Centered name, role and contact info line for classic ATS CVs.',
    build: (isVi) => ({
      type: 'boxBlock',
      attrs: {
        bgColor: 'transparent',
        textColor: '',
        borderColor: 'transparent',
        borderWidth: 0,
        borderSide: 'all',
        radius: 0,
        shadow: 'none',
        paddingX: 0,
        paddingY: 4,
        marginBottom: 12,
        widthPct: 100,
        align: 'left',
      },
      content: [
        {
          type: 'heading',
          attrs: { level: 1, textAlign: 'center' },
          content: [{ type: 'text', text: isVi ? 'NGUYỄN VĂN A' : 'FULL NAME', marks: [{ type: 'textStyle', attrs: { fontSize: '28px' } }] }],
        },
        {
          type: 'paragraph',
          attrs: { textAlign: 'center' },
          content: [{ type: 'text', text: isVi ? 'Backend Developer Intern' : 'Software Engineer', marks: [{ type: 'textStyle', attrs: { fontSize: '15px' } }] }],
        },
        {
          type: 'paragraph',
          attrs: { textAlign: 'center' },
          content: [{ type: 'text', text: '0900 000 000  ·  email@example.com  ·  github.com/username  ·  Ha Noi', marks: [{ type: 'textStyle', attrs: { fontSize: '12px' } }] }],
        },
      ],
    }),
  },
  {
    id: 'cv-section-heading',
    category: 'cv',
    icon: 'minus',
    vi: 'Mục Tiêu đề + Đường kẻ ngang',
    en: 'Section Heading + Divider',
    descVi: '1 khung gọn gồm tiêu đề in hoa và đường kẻ ngang chỉnh màu/độ dày trực tiếp.',
    descEn: 'Single clean section frame with uppercase heading and editable horizontal divider.',
    build: (isVi) => ({
      type: 'boxBlock',
      attrs: {
        bgColor: 'transparent',
        textColor: '',
        borderColor: 'transparent',
        borderWidth: 0,
        borderSide: 'all',
        radius: 0,
        shadow: 'none',
        paddingX: 0,
        paddingY: 4,
        marginTop: 0,
        marginBottom: 12,
        widthPct: 100,
        align: 'left',
      },
      content: [
        {
          type: 'heading',
          attrs: { level: 2, textAlign: 'left' },
          content: [{ type: 'text', text: isVi ? 'KINH NGHIỆM / DỰ ÁN' : 'EXPERIENCE / PROJECTS', marks: [{ type: 'bold' }, { type: 'textStyle', attrs: { fontSize: '14px' } }] }],
        },
        {
          type: 'horizontalRule',
          attrs: { color: '#111827', thickness: 1, lineStyle: 'solid', widthPct: 100, align: 'left', marginTop: 2, marginBottom: 6 },
        },
        {
          type: 'paragraph',
          content: [{ type: 'text', text: isVi ? 'Nhập nội dung mục tại đây...' : 'Type section content here...' }],
        },
      ],
    }),
  },
  {
    id: 'divider-line',
    category: 'page',
    icon: 'minus',
    vi: 'Đường kẻ ngang (Divider)',
    en: 'Horizontal Divider',
    descVi: 'Đường kẻ phân cách bấm trực tiếp để chỉnh màu, độ dày, nét đứt và độ dài.',
    descEn: 'Directly clickable horizontal line with color, thickness, dashed/solid & width.',
    build: () => ({
      type: 'horizontalRule',
      attrs: { color: '', thickness: 1, lineStyle: 'solid', widthPct: 100, align: 'left', marginTop: 4, marginBottom: 8 },
    }),
  },
  {
    id: 'cv-title-date',
    category: 'cv',
    icon: 'columns-2',
    vi: 'Dòng Tiêu đề trái | Thời gian phải',
    en: 'Title Left | Date Right Row',
    descVi: '2 cột không viền cho Tên trường / Công ty / Dự án bên trái và Ngày tháng bên phải.',
    descEn: 'Borderless 2-column row for Institution/Project on left and Date on right.',
    build: (isVi) => ({
      type: 'columnsBlock',
      attrs: { widths: [74, 26], gap: 8, marginTop: 4, marginBottom: 2, stackMobile: false },
      content: [
        {
          type: 'columnCell',
          attrs: { bgColor: 'transparent', textColor: '', borderColor: 'transparent', paddingX: 0, paddingY: 0, radius: 0 },
          content: [
            {
              type: 'paragraph',
              attrs: { textAlign: 'left' },
              content: [{ type: 'text', text: isVi ? 'TÊN CÔNG TY / DỰ ÁN' : 'COMPANY / PROJECT NAME', marks: [{ type: 'bold' }] }],
            },
          ],
        },
        {
          type: 'columnCell',
          attrs: { bgColor: 'transparent', textColor: '', borderColor: 'transparent', paddingX: 0, paddingY: 0, radius: 0 },
          content: [
            {
              type: 'paragraph',
              attrs: { textAlign: 'right' },
              content: [{ type: 'text', text: isVi ? '01/2025 – Hiện tại' : 'Jan 2025 – Present' }],
            },
          ],
        },
      ],
    }),
  },
  {
    id: 'cv-skills-table',
    category: 'cv',
    icon: 'table',
    vi: 'Bảng Kỹ năng 2 cột (Nhóm | Chi tiết)',
    en: '2-Column Skills Table',
    descVi: 'Bảng gạch ngang mảnh chuẩn ATS — di chuột vào từng dòng để Thêm/Xóa/Đảo nhanh.',
    descEn: 'Clean hairline-bordered skills table with hover row controls.',
    build: (isVi) => ({
      type: 'table',
      content: [
        {
          type: 'tableRow',
          content: [
            { type: 'tableCell', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Backend' }] }] },
            { type: 'tableCell', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Java, Spring Boot, RESTful API, PostgreSQL' }] }] },
          ],
        },
        {
          type: 'tableRow',
          content: [
            { type: 'tableCell', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Frontend' }] }] },
            { type: 'tableCell', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Vue.js, React, TypeScript, Tailwind CSS' }] }] },
          ],
        },
        {
          type: 'tableRow',
          content: [
            { type: 'tableCell', content: [{ type: 'paragraph', content: [{ type: 'text', text: isVi ? 'Công cụ' : 'Tools' }] }] },
            { type: 'tableCell', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Docker, Git/GitHub, Linux, Postman' }] }] },
          ],
        },
      ],
    }),
  },
  {
    id: 'exp-section',
    category: 'cv',
    icon: 'briefcase',
    vi: 'Mục Kinh nghiệm làm việc',
    en: 'Work Experience Section',
    descVi: 'Tiêu đề + chức danh, thời gian và danh sách thành tích.',
    descEn: 'Heading + role, dates and bulleted achievements.',
    build: (isVi) => ({
      type: 'boxBlock',
      attrs: { bgColor: 'transparent', borderColor: '#e4e4e7', borderWidth: 0, radius: 0, paddingX: 0, paddingY: 4, marginBottom: 14 },
      content: [
        { type: 'heading', attrs: { level: 2 }, content: [{ type: 'text', text: isVi ? 'KINH NGHIỆM LÀM VIỆC' : 'WORK EXPERIENCE' }] },
        {
          type: 'paragraph',
          content: [
            { type: 'text', text: isVi ? 'Vị trí công việc · Tên công ty' : 'Job Title · Company Name', marks: [{ type: 'bold' }] },
            { type: 'text', text: isVi ? '   |   01/2023 — Hiện tại' : '   |   01/2023 — Present' },
          ],
        },
        {
          type: 'bulletList',
          content: [
            { type: 'listItem', content: [{ type: 'paragraph', content: [{ type: 'text', text: isVi ? 'Mô tả trách nhiệm chính và kết quả đo lường được.' : 'Describe main responsibilities and measurable results.' }] }] },
            { type: 'listItem', content: [{ type: 'paragraph', content: [{ type: 'text', text: isVi ? 'Công nghệ hoặc quy trình đã triển khai thành công.' : 'Technologies or workflows delivered.' }] }] },
          ],
        },
      ],
    }),
  },
]
