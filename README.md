# MyProfile — Personal Portfolio & Visual CMS

A full-stack personal portfolio, interactive CV builder, and node-based visual CMS built with **Vue 3 + TypeScript + Tailwind CSS v4** on the frontend and **Java 21 + Spring Boot 3.4 + Spring Security (JWT) + PostgreSQL + Flyway** on the backend, packaged with **Docker Compose** and **Nginx**.

---

## Features

- **Bento Grid Portfolio & Bilingual UI**: Responsive desktop/tablet/mobile layout with instant Vietnamese (`vi`) and English (`en`) language switching.
- **Node-Based Visual CMS**:
  - Tree-structured content blocks (`section`, `grid`, `card`, `heading`, `text`, `badge-group`, `badge`, `icon`, `metric`, `link-button`, `image`, `list`, `divider`, `contact-form`).
  - Interactive canvas workspace with device viewport preview (`desktop` / `tablet` / `phone`), inline rich-text editing, node inspector, tag-tree outline editor, drag/reorder, batch save, and reset-to-default.
- **CV / Resume Preview & A4 PDF Export**: Built-in CV modal (`CvModal.vue`) formatted for clean A4 printing via `window.print()` and `@media print`.
- **Admin Mode (JWT Authenticated)**:
  - Open via **Admin** in the footer or `/#admin`.
  - Manage Profile, Skills, Education, Work Experience, Projects, and Visual Content Nodes persisted in PostgreSQL.
  - Upload images/assets (up to 10 MB) stored in the database and served via `/api/files/{id}`.
  - **Admin Inbox** to view, mark as read, and delete contact form submissions.
- **Offline Static Fallback**: Automatically falls back to static data in `src/data/profile.ts` when the backend API is unreachable.

---

## Tech Stack

| Layer | Technologies |
| --- | --- |
| **Frontend** | Vue 3, TypeScript, Vite, Tailwind CSS v4, Lucide Icons (`@lucide/vue`) |
| **Backend** | Java 21, Spring Boot 3.4.5, Spring Web, Spring Data JPA, Spring Security, JJWT (`0.12.6`), Bean Validation |
| **Database & Migrations** | PostgreSQL 16 (production/Docker), Flyway (`V1`–`V4`), H2 (integration tests) |
| **Containerization & Proxy** | Docker, Docker Compose, Nginx (`/api/` reverse proxy + SPA fallback) |

---

## Project Structure

```text
my-profile/
├── backend/
│   ├── src/main/java/dev/quang/portfolio/
│   │   ├── controller/       # Public, Auth, Admin Portfolio, Node, and File REST controllers
│   │   ├── dto/              # Request/Response records & DTOs
│   │   ├── entity/           # JPA entities (Profile, Skill, Experience, Education, Project, Node, File, Message)
│   │   ├── repository/       # Spring Data JPA repositories
│   │   ├── security/         # JWT filter, token service, Spring Security config
│   │   └── service/          # Business logic & initial data seeders
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/     # Flyway SQL migrations (V1__init_schema.sql .. V4__tag_nodes.sql)
│   ├── src/test/             # Spring Boot integration tests (H2 + Flyway)
│   ├── Dockerfile
│   └── pom.xml
├── src/
│   ├── components/           # Portfolio sections, Navbar, Footer, CvModal
│   │   ├── admin/            # Admin toolbar, login modal, inbox, and entity edit modals
│   │   └── nodes/            # Visual canvas workspace, node renderer, inspector, rich-text & tag-tree editors
│   ├── composables/          # State management (usePortfolio, useNodes, useCanvasLayout, useI18n)
│   ├── data/                 # Static fallback data (profile.ts)
│   ├── services/             # API client
│   └── types/                # TypeScript definitions (profile.ts, node.ts)
├── .env.example
├── docker-compose.yml
├── Dockerfile
├── nginx.conf
├── package.json
└── vite.config.ts
```

---

## Quick Start (Docker Compose)

1. **Create your environment file** from the template and update secrets (`APP_ADMIN_PASSWORD` and `APP_JWT_SECRET`, minimum 32 characters):
   ```bash
   cp .env.example .env
   ```

2. **Build and start all services** (`postgres`, `backend`, `frontend`):
   ```bash
   docker compose up --build -d
   ```

3. **Verify the deployment**:
   - **Portfolio Web (Nginx):** `http://localhost:3005`
   - **Backend Healthcheck:** `http://localhost:8080/api/health`
   - **Admin Mode:** Click **Admin** in the footer or visit `http://localhost:3005/#admin`

4. **Stop services**:
   ```bash
   docker compose down
   ```
   *(Add `-v` if you also want to remove the `pg_data` PostgreSQL volume.)*

---

## Environment Variables

| Variable | Default | Description |
| --- | --- | --- |
| `POSTGRES_DB` | `portfolio` | PostgreSQL database name |
| `POSTGRES_USER` | `portfolio` | PostgreSQL username |
| `POSTGRES_PASSWORD` | `portfolio_secret` | PostgreSQL password |
| `APP_ADMIN_USERNAME` | `admin` | Admin login username |
| `APP_ADMIN_PASSWORD` | `admin123` | Admin login password (change in `.env`!) |
| `APP_JWT_SECRET` | *(32+ char dev key)* | HMAC secret key for signing JWTs |
| `APP_JWT_EXPIRATION_MS` | `86400000` | JWT expiration in milliseconds (default: 24h) |
| `BACKEND_PORT` | `8080` | Host port mapped to backend container |
| `FRONTEND_PORT` | `3005` | Host port mapped to Nginx frontend container |
| `APP_CORS_ORIGINS` | `http://localhost:3005,...` | Comma-separated allowed CORS origins |

---

## Local Development

### Prerequisites
- **Node.js** 20+ & `npm`
- **Java 21** & **Maven**
- **PostgreSQL 16** (or run just the DB container via `docker compose up -d postgres`)

### Backend
```bash
cd backend

# Run tests (uses in-memory H2 + Flyway)
mvn test

# Run the Spring Boot server (connects to PostgreSQL on localhost:5432)
mvn spring-boot:run
```

### Frontend
```bash
# Install dependencies
npm install

# Start Vite dev server (proxies /api -> http://localhost:8080)
npm run dev

# Type-check only
npm run typecheck

# Production build (type-check + Vite bundle)
npm run build
```

*(On Windows PowerShell, use `npm.cmd` if `npm` script execution is restricted.)*

---

## API Overview

### Public Endpoints
| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/health` | Service health status |
| `GET` | `/api/portfolio` | Full public portfolio data |
| `POST` | `/api/contact` | Submit a contact form message |
| `GET` | `/api/nodes` | Fetch visual content node tree |
| `GET` | `/api/files/{id}` | Retrieve an uploaded file by ID |

### Authentication
| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/auth/login` | Authenticate admin and receive JWT |
| `GET` | `/api/auth/me` | Get current authenticated user info |

### Admin Endpoints *(Requires `Authorization: Bearer <token>`)*
| Area | Endpoints |
| --- | --- |
| **Portfolio** | `PUT /api/admin/profile` · `POST/PUT/DELETE /api/admin/skills` · `POST/PUT/DELETE /api/admin/education` · `POST/PUT/DELETE /api/admin/experiences` · `POST/PUT/DELETE /api/admin/projects` |
| **Visual Nodes** | `POST /api/admin/nodes` · `PUT /api/admin/nodes/{id}` · `DELETE /api/admin/nodes/{id}` · `PUT /api/admin/nodes/reorder` · `PUT /api/admin/nodes/changes` · `POST /api/admin/nodes/reset` |
| **Messages** | `GET /api/admin/messages` · `PATCH /api/admin/messages/{id}/read` · `DELETE /api/admin/messages/{id}` |
| **Files** | `POST /api/admin/upload` |
