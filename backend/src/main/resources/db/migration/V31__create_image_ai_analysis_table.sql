-- V31: Create image_ai_analysis table for AI-assisted image understanding
CREATE TABLE image_ai_analysis (
    id UUID PRIMARY KEY,
    media_id UUID NOT NULL,
    issue_id UUID NOT NULL,
    provider VARCHAR(50) NOT NULL,
    model VARCHAR(100) NOT NULL,
    model_version VARCHAR(50) NOT NULL,
    calculation_version VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    likely_category VARCHAR(50) NULL,
    category_confidence INT NULL CHECK (category_confidence >= 0 AND category_confidence <= 100),
    visual_problem_types JSONB NOT NULL DEFAULT '[]',
    image_quality VARCHAR(30) NULL,
    quality_issues JSONB NOT NULL DEFAULT '[]',
    relevance VARCHAR(30) NULL,
    visual_severity_signals JSONB NOT NULL DEFAULT '[]',
    safety_concern VARCHAR(30) NULL,
    sensitive_visual_content_detected BOOLEAN NOT NULL DEFAULT FALSE,
    summary VARCHAR(500) NULL,
    error_message VARCHAR(500) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_image_ai_media FOREIGN KEY (media_id) REFERENCES media(id) ON DELETE CASCADE,
    CONSTRAINT fk_image_ai_issue FOREIGN KEY (issue_id) REFERENCES issues(id) ON DELETE CASCADE,
    CONSTRAINT uq_image_ai_media UNIQUE (media_id)
);

CREATE INDEX idx_image_ai_issue_id ON image_ai_analysis(issue_id);
CREATE INDEX idx_image_ai_status ON image_ai_analysis(status);
CREATE INDEX idx_image_ai_likely_category ON image_ai_analysis(likely_category);
