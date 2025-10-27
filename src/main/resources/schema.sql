-- SQLite schema for Ruuvitag measurements

-- Measurements table stores all telemetry data
CREATE TABLE IF NOT EXISTS measurements (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id TEXT NOT NULL,
    mac_address TEXT NOT NULL,
    measurement_type TEXT NOT NULL,
    timestamp INTEGER NOT NULL,  -- Unix timestamp in milliseconds
    value REAL NOT NULL,
    created_at INTEGER DEFAULT (strftime('%s', 'now') * 1000)
);

-- Index for efficient querying by user, MAC address, and measurement type
CREATE INDEX IF NOT EXISTS idx_measurements_user_mac_type
    ON measurements(user_id, mac_address, measurement_type);

-- Index for efficient time range queries
CREATE INDEX IF NOT EXISTS idx_measurements_timestamp
    ON measurements(timestamp);

-- Composite index for the most common query pattern
CREATE INDEX IF NOT EXISTS idx_measurements_query
    ON measurements(user_id, mac_address, measurement_type, timestamp);
