# =============================================================================
# backend/app/models.py — Modelos SQLAlchemy | 6º Semestre UTP | RNF-2.2, HU-01..HU-04
# Autor: Est. Tecnología en Desarrollo Software + Ing. Sistemas (UTP)
# Experiencia: 1 año PostgreSQL (tablas, índices, UUID), 2 años Python (SQLAlchemy),
#              1 año C (structs), 2 años JS/React (tipos), 2 años electrónica (sensores)
# Analogía React: estos models son como los types/interfaces de Prisma/Drizzle —
# define la tabla en código y SQLAlchemy crea/migra la tabla en Postgres.
# Analogía C: cada class es un struct con columnas tipadas (como struct RoverTelemetry en rover-uno.ino:91)
# Analogía PostgreSQL: cada __tablename__ es CREATE TABLE en init.sql:6 — aquí en Python para ORM.
# FOSS: SQLAlchemy 2.0 + asyncpg + PostgreSQL 15 — RNF-3.1 sin RDS propietario.
# Orígenes: requirements.md RNF-2.2 (histórico), HU-01 (access), HU-02 (security), HU-03 (sensor), RF-3.1 (rover)
# =============================================================================
import uuid  # para default uuid4() — PK UUID v4 (como crypto.randomUUID() en JS)

from sqlalchemy import (
    JSON,  # tipo JSON — para event_metadata (como JSONB en Postgres, ver init.sql:23)
    Boolean,  # BOOLEAN — para success, acknowledged, ir_left (como bool en C)
    Column,  # columna — como field en Prisma
    DateTime,  # TIMESTAMPTZ — con timezone UTC (como Date en JS pero con TZ)
    Index,  # índice explícito — como @@index en Prisma (optimiza queries)
    Numeric,  # NUMERIC(precision, scale) — decimales exactos para cm/dB (no FLOAT impreciso)
    SmallInteger,  # SMALLINT — para PWM -255..255 y RSSI -120..0 (ahorra bytes vs INTEGER)
    String,  # VARCHAR(n) — texto con límite (como string @db.VarChar(64) en Prisma)
    Text,  # TEXT — texto largo sin límite (para description en security_events)
)
from sqlalchemy.dialects.postgresql import (
    UUID,  # UUID nativo Postgres — columna UUID (no VARCHAR)
)
from sqlalchemy.sql import (
    func,  # func.now() — NOW() en SQL (como Date.now() pero en DB)
)

from app.database import Base  # DeclarativeBase — clase base para todos los modelos ORM


# --------------------------------------------------------------------------
# AccessEvent — HU-01, RF-2.2 / Tabla access_events (init.sql:6, sprints.md:53)
# --------------------------------------------------------------------------
class AccessEvent(Base):
    """Evento de acceso por keypad o App — cada intento PIN (exitoso o no) queda aquí."""
    __tablename__ = "access_events"  # nombre tabla en Postgres — como model AccessEvent en Prisma

    # PK UUID v4 — aleatorio, no secuencial (mejor para distribuido que SERIAL, como id en Mongo)
    # default uuid4() genera en Python, no en DB (complementa init.sql uuid_generate_v4())
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    # user_id — quién intentó: "keypad_user" (físico) o ID App (futuro multiusuario MOV-04)
    # String(64) — VARCHAR 64, indexado para filtrar por usuario (ver idx_access_events_user_id)
    user_id = Column(String(64), nullable=False, index=True)  # NOT NULL — siempre hay origen
    # pin_hash — hash del PIN (djb2 en keypad_control.cpp:99, HEX) — nunca el PIN en claro
    # String(128) suficiente para SHA256 HEX (64) + margen, NOT NULL
    pin_hash = Column(String(128), nullable=False)
    # success — true si PIN correcto (desbloquea), false si falló (LED rojo 1s en MEGA)
    # Boolean — indexable para stats EST-06 (tasa éxito vs fallo)
    success = Column(Boolean, nullable=False)
    # timestamp — cuándo ocurrió — server_default NOW() en DB (como createdAt en Prisma)
    # TIMESTAMPTZ con zona (UTC) — crucial para t-Student y orden cronológico (DESC)
    # indexado DESC para list_access_events ORDER BY timestamp DESC (más reciente primero)
    timestamp = Column(DateTime(timezone=True), server_default=func.now(), nullable=False, index=True)
    # source — origen: "keypad" (físico), "app" (MQTT), "bluetooth" (RF-1.3 futuro)
    # String(32), default "keypad" — como enum en Prisma con default
    source = Column(String(32), nullable=False, default="keypad")

    # Índices explícitos — como @@index([timestamp(sort: Desc)]) en Prisma
    # Aceleran queries frecuentes (ver events.py:51 order_by desc)
    __table_args__ = (
        Index("idx_access_events_timestamp_desc", timestamp.desc()),  # para GET /api/access-events?limit=50 ORDER BY timestamp DESC
        Index("idx_access_events_user_id", user_id),  # para filtrar por usuario en dashboard App
    )


# --------------------------------------------------------------------------
# SensorEvent — RNF-2.1, HU-03 / Tabla sensor_events (init.sql:15, EST-06)
# --------------------------------------------------------------------------
class SensorEvent(Base):
    """Evento sensor genérico — HC-SR04, KY-037, TCRT5000, láser — con valor raw y filtrado EMA."""
    __tablename__ = "sensor_events"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    # sensor_id — ej. "hc-sr04-01", "ky-037-01" — identifica hardware físico (como deviceId en IoT)
    sensor_id = Column(String(64), nullable=False)  # NOT NULL — siempre hay sensor
    # sensor_type — "ultrasonic" (HC-SR04), "sound" (KY-037), "ir" (TCRT5000), "laser" (KY-008), "rf"
    # indexado para filtrar por tipo en EST-06 (describe por tipo) y en GET /api/sensor-events?sensor_type=ultrasonic
    sensor_type = Column(String(32), nullable=False, index=True)
    # value — lectura cruda (raw) — ej. 42.5 cm del HC-SR04 sin filtrar
    # Numeric(10,4) — 10 dígitos totales, 4 decimales (precisión para EMA y t-Student, evita FLOAT error)
    value = Column(Numeric(10, 4), nullable=False)
    # filtered_value — lectura filtrada con EMA α=0.2 (stats/ema_filter.py:32, rover.ino:259)
    # Nullable porque puede llegar sin filtrar (raw only) — se calcula luego en stats/
    # Numeric(10,4) para comparar raw vs filt en KPI >85% (ver calculate_noise_reduction)
    filtered_value = Column(Numeric(10, 4))  # nullable — si no hay EMA aún
    # unit — "cm" (HC-SR04), "db" (KY-037), "boolean" (TCRT), etc. — para UI y conversión
    unit = Column(String(16), nullable=False)  # NOT NULL — siempre hay unidad
    timestamp = Column(DateTime(timezone=True), server_default=func.now(), nullable=False, index=True)
    # event_metadata — JSONB para extras flexibles: {"alpha":0.2, "rssi":-70, "threshold":30}
    # 'metadata' es palabra reservada en DeclarativeBase, así que atributo es event_metadata pero columna es "metadata"
    # (como mapear `field: column` en Prisma) — ver schemas.py:67 alias.
    event_metadata = Column("metadata", JSON, nullable=False, default=dict)  # JSONB en Postgres (init.sql:23)

    __table_args__ = (
        Index("idx_sensor_events_timestamp_desc", timestamp.desc()),  # ORDER BY timestamp DESC frecuente
        Index("idx_sensor_events_sensor_type", sensor_type),  # WHERE sensor_type='ultrasonic'
    )


# --------------------------------------------------------------------------
# SecurityEvent — HU-02, RF-2.3, RF-4.1 / Tabla security_events (init.sql:26)
# --------------------------------------------------------------------------
class SecurityEvent(Base):
    """Evento seguridad — intrusión láser, fail-stop Rover, etc. — dispara Telegram vía Node-RED."""
    __tablename__ = "security_events"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    # event_type — "intrusion" (láser KY-008), "access_denied" (PIN fallido ×N), "rf_failstop" (HU-04)
    # indexado para filtrar por tipo en dashboard seguridad
    event_type = Column(String(32), nullable=False, index=True)
    # severity — "low", "medium" (default), "high", "critical" — para priorizar en Telegram/Node-RED
    # Como priority en un ticket de Jira — medium es default (no crítico pero notifica)
    severity = Column(String(16), nullable=False, default="medium")
    # description — texto libre: "Laser interrupted at door" — TEXT sin límite
    description = Column(Text)  # nullable — puede ser solo event_type sin detalle
    timestamp = Column(DateTime(timezone=True), server_default=func.now(), nullable=False, index=True)
    # acknowledged — ¿el admin vio/atendió la alerta? — como read en notificaciones React
    acknowledged = Column(Boolean, nullable=False, default=False)  # default false — no leído al crear
    acknowledged_at = Column(DateTime(timezone=True))  # cuándo se marcó como visto — nullable hasta ack
    acknowledged_by = Column(String(64))  # quién lo atendió — "admin" o user_id — nullable

    __table_args__ = (
        Index("idx_security_events_timestamp_desc", timestamp.desc()),
        Index("idx_security_events_event_type", event_type),
    )


# --------------------------------------------------------------------------
# RoverTelemetry — RF-3.1, RF-3.3, HU-04 / Tabla rover_telemetry (init.sql:37, docs/architecture.md:31)
# --------------------------------------------------------------------------
class RoverTelemetry(Base):
    """Telemetría Rover — cada paquete RF del UNO (PWM + sensores + RSSI) por sesión."""
    __tablename__ = "rover_telemetry"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    # session_id — UUID de la sesión (ej. joystick conectado) — agrupa telemetría por run
    # indexado con timestamp para queries por sesión ordenadas (ver idx_rover_telemetry_session_timestamp)
    session_id = Column(UUID(as_uuid=True), nullable=False, index=True)
    # left/right_motor_pwm — -255..255 (SmallInteger) — igual que RoverCommand left_pwm en gateway/r.ino:92
    # SmallInteger (-32768..32767) suficiente y ahorra bytes vs INTEGER (como int16_t en C)
    left_motor_pwm = Column(SmallInteger, nullable=False)  # -255 reversa .. 255 adelante
    right_motor_pwm = Column(SmallInteger, nullable=False)
    # ultrasonic_distance_cm — distancia HC-SR04 ya filtrada con EMA α=0.2 en UNO (ultrasonicEma)
    # Numeric(6,2) — 6 dígitos, 2 decimales (0.00 .. 9999.99 cm, suficiente para 0-200 cm)
    ultrasonic_distance_cm = Column(Numeric(6, 2))  # nullable — si HC-SR04 dio timeout (raw 0)
    # ir_left/center/right — Boolean — true si TCRT detecta borde (valor <500) — como bool en C rover.ino:240
    ir_left = Column(Boolean)  # nullable — si sensor desconectado
    ir_center = Column(Boolean)
    ir_right = Column(Boolean)
    # rf_rssi — intensidad RF -120..0 dBm (SmallInteger) — placeholder -70 en rover.ino:243 (RF24 no da RSSI real)
    rf_rssi = Column(SmallInteger)  # nullable — si no hay nRF
    timestamp = Column(DateTime(timezone=True), server_default=func.now(), nullable=False, index=True)

    __table_args__ = (
        # Índice compuesto session_id + timestamp — para GET /api/rover/telemetry?session_id=X ORDER BY timestamp
        # Optimiza queries de stats EST-05 t-Student (latencias por sesión) y replay de ruta
        Index("idx_rover_telemetry_session_timestamp", session_id, timestamp),
    )
