-- Esquema inicial para AetherNet IoT
-- Tabla de eventos de acceso y sensores

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS access_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id VARCHAR(64) NOT NULL,
    pin_hash VARCHAR(128) NOT NULL,
    success BOOLEAN NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    source VARCHAR(32) NOT NULL DEFAULT 'keypad' -- keypad, app, bluetooth
);

CREATE TABLE IF NOT EXISTS sensor_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sensor_id VARCHAR(64) NOT NULL,
    sensor_type VARCHAR(32) NOT NULL, -- laser, ultrasonic, ir, sound, rf
    value NUMERIC(10, 4) NOT NULL,
    filtered_value NUMERIC(10, 4),
    unit VARCHAR(16) NOT NULL, -- cm, db, boolean, rpm
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE TABLE IF NOT EXISTS security_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_type VARCHAR(32) NOT NULL, -- intrusion, access_denied, rf_failstop
    severity VARCHAR(16) NOT NULL DEFAULT 'medium', -- low, medium, high, critical
    description TEXT,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged_at TIMESTAMPTZ,
    acknowledged_by VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS rover_telemetry (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id UUID NOT NULL,
    left_motor_pwm SMALLINT NOT NULL,
    right_motor_pwm SMALLINT NOT NULL,
    ultrasonic_distance_cm NUMERIC(6, 2),
    ir_left BOOLEAN,
    ir_center BOOLEAN,
    ir_right BOOLEAN,
    rf_rssi SMALLINT,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Índices para consultas frecuentes
CREATE INDEX IF NOT EXISTS idx_access_events_timestamp ON access_events(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_access_events_user ON access_events(user_id);
CREATE INDEX IF NOT EXISTS idx_sensor_events_timestamp ON sensor_events(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_sensor_events_type ON sensor_events(sensor_type);
CREATE INDEX IF NOT EXISTS idx_security_events_timestamp ON security_events(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_security_events_type ON security_events(event_type);
CREATE INDEX IF NOT EXISTS idx_rover_telemetry_session ON rover_telemetry(session_id, timestamp);