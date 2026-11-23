CREATE TABLE IF NOT EXISTS content_nodes (
    id VARCHAR(80) PRIMARY KEY,
    parent_id VARCHAR(80) REFERENCES content_nodes(id) ON DELETE CASCADE,
    tag VARCHAR(50) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    attrs_json TEXT NOT NULL DEFAULT '{}',
    content_vi TEXT NOT NULL DEFAULT '',
    content_en TEXT NOT NULL DEFAULT '',
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_content_nodes_parent_sort
    ON content_nodes (parent_id, sort_order);
