CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(80) PRIMARY KEY,
    username VARCHAR(40) NOT NULL UNIQUE,
    email VARCHAR(200) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'USER',
    max_pages INT NOT NULL DEFAULT 3,
    max_storage_bytes BIGINT NOT NULL DEFAULT 20971520,
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS pages (
    id VARCHAR(80) PRIMARY KEY,
    owner_id VARCHAR(80) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    slug VARCHAR(80) NOT NULL,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(500) NOT NULL DEFAULT '',
    visibility VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    is_template BOOLEAN NOT NULL DEFAULT FALSE,
    is_home_featured BOOLEAN NOT NULL DEFAULT FALSE,
    cv_pdf_url VARCHAR(500),
    theme_json TEXT NOT NULL DEFAULT '{}',
    view_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_pages_owner_slug UNIQUE (owner_id, slug)
);

CREATE INDEX IF NOT EXISTS idx_pages_owner_updated ON pages (owner_id, updated_at);
CREATE INDEX IF NOT EXISTS idx_pages_template ON pages (is_template);
CREATE INDEX IF NOT EXISTS idx_pages_home_featured ON pages (is_home_featured);

ALTER TABLE content_nodes
    ADD COLUMN IF NOT EXISTS page_id VARCHAR(80) REFERENCES pages(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_content_nodes_page_parent_sort
    ON content_nodes (page_id, parent_id, sort_order);

ALTER TABLE contact_messages
    ADD COLUMN IF NOT EXISTS page_id VARCHAR(80) REFERENCES pages(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_contact_messages_page_created
    ON contact_messages (page_id, created_at);

ALTER TABLE uploaded_files
    ADD COLUMN IF NOT EXISTS owner_id VARCHAR(80) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE uploaded_files
    ADD COLUMN IF NOT EXISTS size_bytes BIGINT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_uploaded_files_owner
    ON uploaded_files (owner_id);
