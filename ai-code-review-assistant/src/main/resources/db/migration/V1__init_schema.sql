-- =============================================================================
-- V1__init_schema.sql
-- AI Code Review Assistant — Initial Database Schema
--
-- Tables:
--   1. review_sessions   — Stores each code review submission and its metadata.
--   2. review_feedback   — Stores individual feedback items produced by Gemini.
--
-- Relationship: review_feedback.session_id → review_sessions.id (CASCADE DELETE)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Table: review_sessions
-- -----------------------------------------------------------------------------
-- Each row represents one code review request submitted by a user.
-- The session tracks the full lifecycle from submission through AI analysis
-- to human review completion.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS review_sessions (
    id                        BIGINT          NOT NULL AUTO_INCREMENT,
    title                     VARCHAR(255)    NOT NULL                COMMENT 'User-provided or auto-generated session title',
    source_code               MEDIUMTEXT      NOT NULL                COMMENT 'Raw source code or Git unified diff',
    language                  VARCHAR(50)     NOT NULL                COMMENT 'Programming language (e.g., java, python)',
    submission_type           ENUM('SNIPPET', 'DIFF')
                                              NOT NULL DEFAULT 'SNIPPET'
                                                                      COMMENT 'Submission type: SNIPPET or DIFF',
    status                    ENUM('PENDING', 'IN_REVIEW', 'COMPLETED', 'FAILED')
                                              NOT NULL DEFAULT 'PENDING'
                                                                      COMMENT 'Session lifecycle state',

    -- Denormalized counters to avoid expensive COUNT queries on dashboards
    total_feedback_count      INT             NOT NULL DEFAULT 0      COMMENT 'Total feedback items in this session',
    accepted_count            INT             NOT NULL DEFAULT 0      COMMENT 'Number of accepted feedback items',
    rejected_count            INT             NOT NULL DEFAULT 0      COMMENT 'Number of rejected feedback items',

    -- Gemini API call metadata
    gemini_model              VARCHAR(100)    NULL                    COMMENT 'Gemini model identifier used for review',
    gemini_request_duration_ms BIGINT         NULL                    COMMENT 'Gemini API round-trip latency in ms',
    error_message             TEXT            NULL                    COMMENT 'Error detail when status = FAILED',

    -- Audit timestamps
    created_at                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_session_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- -----------------------------------------------------------------------------
-- Table: review_feedback
-- -----------------------------------------------------------------------------
-- Each row is one actionable feedback item (bug, security issue, readability
-- suggestion, etc.) produced by Gemini for a specific review session.
--
-- The `resolution` column implements the human-in-the-loop workflow:
-- items start as PENDING and are updated to ACCEPTED or REJECTED only by
-- explicit user action through the REST API.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS review_feedback (
    id                        BIGINT          NOT NULL AUTO_INCREMENT,
    session_id                BIGINT          NOT NULL                COMMENT 'FK to review_sessions.id',

    -- Classification
    category                  ENUM('BUG', 'EDGE_CASE', 'READABILITY', 'SECURITY', 'PERFORMANCE', 'BEST_PRACTICE', 'SYSTEM', 'OTHER')
                                              NOT NULL                COMMENT 'Feedback classification category',
    severity                  ENUM('CRITICAL', 'HIGH', 'WARNING', 'MEDIUM', 'LOW', 'INFO')
                                              NOT NULL                COMMENT 'Impact severity level',
    resolution                ENUM('PENDING', 'ACCEPTED', 'REJECTED')
                                              NOT NULL DEFAULT 'PENDING'
                                                                      COMMENT 'Human review decision',

    -- Content
    title                     VARCHAR(500)    NOT NULL                COMMENT 'Short feedback headline',
    description               TEXT            NOT NULL                COMMENT 'Detailed explanation from Gemini',

    -- Line references (nullable for non-line-specific feedback)
    line_start                INT             NULL                    COMMENT 'Start line in source code',
    line_end                  INT             NULL                    COMMENT 'End line in source code',

    -- Code snippets
    original_code             TEXT            NULL                    COMMENT 'Original code fragment being reviewed',
    suggested_code            TEXT            NULL                    COMMENT 'AI-suggested replacement code',

    -- Audit timestamps
    created_at                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_feedback_session_id (session_id),
    INDEX idx_feedback_resolution (resolution),

    CONSTRAINT fk_feedback_session
        FOREIGN KEY (session_id) REFERENCES review_sessions (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
