CREATE TABLE job (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    type            TEXT NOT NULL,        -- SCAN_SKU | NOTIFY_SKU_DISABLED | ...
    dedupe_key      TEXT NOT NULL,
    payload_json    TEXT,
    status          TEXT NOT NULL DEFAULT 'PENDING', -- PENDING | RUNNING | DONE | FAILED
    priority        INTEGER NOT NULL DEFAULT 0,
    run_at_ms       BIGINT NOT NULL,
    attempts        INTEGER NOT NULL DEFAULT 0,
    last_error      TEXT,
    created_at_ms   BIGINT NOT NULL,
    updated_at_ms   BIGINT NOT NULL,
    UNIQUE(type, dedupe_key)
);

CREATE INDEX idx_job_type_runat ON job(type, run_at_ms DESC);
CREATE INDEX ix_job_status_time ON job(status, run_at_ms);
