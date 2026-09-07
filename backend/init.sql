-- =============================================================================
-- init.sql — Esquema PostgreSQL Inicial | 6º Semestre UTP | RNF-2.2, DEVOPS-03
-- Autor: Est. Tecnología en Desarrollo Software + Ing. Sistemas (UTP)
-- Experiencia: 1 año PostgreSQL (DDL, índices, UUID, JSONB), 2 años Python (SQLAlchemy),
--              2 años JS/React (migrations como Prisma), 1 año C (tipos)
-- Analogía React/Prisma: este archivo es `prisma/migrations/001_init.sql` — crea
-- tablas e índices en Postgres. SQLAlchemy models.py hace lo mismo en Python (ORM),
-- pero este SQL es el init para Docker Postgres (ejecutado al crear contenedor).
-- Analogía C: cada CREATE TABLE es como `struct` con columnas tipadas (ver models.py:20)
-- FOSS: PostgreSQL 15 + uuid-ossp (FOSS) — RNF-3.1, sin RDS propietario.
-- Ejecutado por: docker-compose.yml postgres service (volume ./init.sql:/docker-entrypoint-initdb.d/init.sql)
-- Complemento: backend/app/models.py y main.py lifespan create_all() — ambos idempotentes (IF NOT EXISTS).
-- =============================================================================

-- Extensión UUID — genera UUID v4 en DB (como crypto.randomUUID() en JS, pero en Postgres)
-- uuid_generate_v4() en DEFAULT necesita esta extensión (como importar uuid en Node)
-- IF NOT EXISTS no falla si ya existe (idempotente, como CREATE TABLE IF NOT EXISTS)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- --------------------------------------------------------------------------
-- access_events — HU-01, RF-2.2 / Modelo AccessEvent (models.py:20)
-- Cada intento PIN (keypad 4x4 o App) → Gateway HTTP POST → aquí (ver gateway.ino:343)
-- --------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS access_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),  -- PK — como id String @id @default(uuid()) en Prisma
    user_id VARCHAR(64) NOT NULL,  -- "keypad_user" o ID App — VARCHAR 64 (models.py:24 String(64))
    pin_hash VARCHAR(128) NOT NULL,  -- hash HEX (djb2 8 chars, pero 128 para SHA256 futuro) — nunca PIN claro
    success BOOLEAN NOT NULL,  -- true/false — como success en MEGA keypad_control.cpp:68
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),  -- con zona UTC — como Date.now() pero en DB (NOW() en Postgres)
    source VARCHAR(32) NOT NULL DEFAULT 'keypad' -- keypad, app, bluetooth (RF-1.3 futuro) — default keypad
);

-- --------------------------------------------------------------------------
-- sensor_events — RNF-2.1, HU-03 / Modelo SensorEvent (models.py:36, EST-06)
-- HC-SR04 (ultrasonic cm), KY-037 (sound db), TCRT5000 (ir boolean) — con value y filtered_value EMA
-- --------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sensor_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sensor_id VARCHAR(64) NOT NULL,  -- "hc-sr04-01", "ky-037-01" — deviceId IoT
    sensor_type VARCHAR(32) NOT NULL, -- laser, ultrasonic, ir, sound, rf — indexado para filtrar EST-06
    value NUMERIC(10, 4) NOT NULL,  -- raw — 42.5 cm sin filtrar — NUMERIC exacto (no FLOAT) para t-Student
    filtered_value NUMERIC(10, 4),  -- EMA α=0.2 — nullable si no hay filtro (stats/ema_filter.py:32)
    unit VARCHAR(16) NOT NULL, -- cm, db, boolean, rpm — para UI React y conversión
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb  -- JSONB — extras flexibles {"alpha":0.2, "threshold":30} — como JSON column en Prisma
);

-- --------------------------------------------------------------------------
-- security_events — HU-02, RF-2.3, RF-4.1 / Modelo SecurityEvent (models.py:55)
-- Láser KY-008 intrusión → aethernet/seguridad/intrusion → Node-RED → Telegram
-- --------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS security_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_type VARCHAR(32) NOT NULL, -- intrusion, access_denied, rf_failstop — indexado para dashboard
    severity VARCHAR(16) NOT NULL DEFAULT 'medium', -- low, medium, high, critical — como priority en Jira
    description TEXT,  -- "Laser interrupted at door" — TEXT sin límite (como String? en Prisma sin @db.VarChar)
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,  -- ¿admin vio alerta? — como read en notificaciones React
    acknowledged_at TIMESTAMPTZ,  -- cuándo se atendió — nullable hasta ack
    acknowledged_by VARCHAR(64)  -- quién — "admin" — nullable
);

-- --------------------------------------------------------------------------
-- rover_telemetry — RF-3.1, RF-3.3, HU-04 / Modelo RoverTelemetry (models.py:73)
-- Cada paquete RF del UNO (RoverCommand/RoverTelemetry struct en rover-uno.ino:84) por session_id
-- Gateway publishRoverTelemetry → MQTT aethernet/rover/telemetry → aquí (futuro) o HTTP POST directo
-- --------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rover_telemetry (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id UUID NOT NULL,  -- agrupa telemetría por sesión joystick — como sessionId en analytics (Prisma String @id)
    left_motor_pwm SMALLINT NOT NULL,  -- -255..255 — SmallInteger en models.py:78, como int16_t en C
    right_motor_pwm SMALLINT NOT NULL,
    ultrasonic_distance_cm NUMERIC(6, 2),  -- HC-SR04 EMA — 200.00 cm max — Numeric(6,2) en models.py:80
    ir_left BOOLEAN,  -- TCRT — true si <500 (borde) — nullable si sensor desconectado
    ir_center BOOLEAN,
    ir_right BOOLEAN,
    rf_rssi SMALLINT,  -- -120..0 dBm — placeholder -70 en rover.ino:243 (RF24 sin RSSI real)
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- --------------------------------------------------------------------------
-- Índices — Optimizan queries frecuentes (como @@index en Prisma, ver models.py:30)
-- Sin índices, SELECT ORDER BY timestamp DESC sería O(n) scan; con índice es O(log n)
-- --------------------------------------------------------------------------
-- Access — ORDER BY timestamp DESC (más reciente primero) en GET /api/access-events (events.py:51)
CREATE INDEX IF NOT EXISTS idx_access_events_timestamp ON access_events(timestamp DESC);
-- Por usuario — para dashboard App filtrar por keypad_user (models.py:32)
CREATE INDEX IF NOT EXISTS idx_access_events_user ON access_events(user_id);
-- Sensor — ORDER BY timestamp DESC y WHERE sensor_type (events.py:81)
CREATE INDEX IF NOT EXISTS idx_sensor_events_timestamp ON sensor_events(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_sensor_events_type ON sensor_events(sensor_type);
-- Security — ORDER BY timestamp DESC y WHERE event_type (events.py:112)
CREATE INDEX IF NOT EXISTS idx_security_events_timestamp ON security_events(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_security_events_type ON security_events(event_type);
-- Rover — compuesto (session_id, timestamp) para GET /api/rover/telemetry?session_id=X ORDER BY timestamp (events.py:148)
-- Índice compuesto — como @@index([session_id, timestamp]) en Prisma — optimiza filtro + orden por sesión
CREATE INDEX IF NOT EXISTS idx_rover_telemetry_session ON rover_telemetry(session_id, timestamp);
