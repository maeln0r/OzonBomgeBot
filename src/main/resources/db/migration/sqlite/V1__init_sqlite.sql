-- 1. Отслеживаемые товары (пользовательский слой)
CREATE TABLE tracked_item (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    chat_id         BIGINT NOT NULL,
    user_id         BIGINT,
    sku             BIGINT NOT NULL,
    title           TEXT,
    start_price     BIGINT,
    last_price      BIGINT,
    availability    TEXT NOT NULL DEFAULT 'UNKNOWN', -- AVAILABLE | OUT_OF_STOCK | UNKNOWN | LONG_OUT_OF_STOCK
    threshold_type  TEXT,                            -- PERCENT | PRICE
    threshold_value BIGINT,
    created_at_ms   BIGINT NOT NULL,
    updated_at_ms   BIGINT NOT NULL,
    UNIQUE(chat_id, sku)
);

CREATE INDEX idx_tracked_item_chat ON tracked_item(chat_id);
CREATE INDEX idx_tracked_item_sku ON tracked_item(sku);
CREATE INDEX idx_tracked_item_avail ON tracked_item(availability);

-- 2. Глобальная история по SKU (факт сканирования)
CREATE TABLE price_history (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    sku             BIGINT NOT NULL,
    price           BIGINT,
    availability    TEXT NOT NULL DEFAULT 'UNKNOWN',              -- тут тоже храним состояние на момент скана
    created_at_ms   BIGINT NOT NULL
);

CREATE INDEX idx_price_history_sku_time ON price_history(sku, created_at_ms DESC);
CREATE INDEX idx_price_history_sku_avail_time ON price_history(sku, availability, created_at_ms DESC);
