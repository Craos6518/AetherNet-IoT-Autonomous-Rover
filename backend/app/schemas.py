"""
==============================================================================
Pydantic Schemas AetherNet IoT | 6º Semestre UTP | Sprint 1 / DEVOPS-03, DEVOPS-06
Autor: Andres Felipe Martinez Henao
Experiencia: 2 años Python (Pydantic), 2 años JS/React (Zod, TypeScript),
             1 año PostgreSQL (tipos VARCHAR, NUMERIC, JSONB), 2 años electrónica
Materia: TS4D3 Estadística — validación para sensor_events (EMA) y t-Student
FOSS: Pydantic 2.7 (MIT) — validación runtime, FOSS RNF-3.1 (como Zod en JS)
Analogía React/TS: estos schemas son como `z.object({ user_id: z.string().max(64) })`
en Zod — validan payload HTTP antes de tocar DB y generan docs Swagger auto.
Analogía C: cada Create es como struct validado (como RoverCommand con checksum pero con Field constraints).
Analogía PostgreSQL: Field(max_length=64) = VARCHAR(64) en models.py:24 y init.sql:8 — mantiene coherencia.
Orígenes: requirements.md RF-2.1 (gateway), RNF-2.2 (histórico), HU-01..HU-04 (BDD).
==============================================================================
"""

from datetime import datetime  # para timestamp — como Date en JS pero con TZ
from typing import Any  # para metadata JSON flexible — como Record<string, any> en TS
from uuid import UUID  # para id/session_id — como crypto.randomUUID() en JS

from pydantic import (  # BaseModel = Zod schema, Field = z.string().max()
    BaseModel,
    ConfigDict,
    Field,
)


# ---------------------------------------------------------------------------
# Health — GET /health (main.py:79, ci.yml:116)
# ---------------------------------------------------------------------------
class HealthResponse(BaseModel):
    """Schema para /health — como { status: "ok", database: "ok" } en JS."""
    status: str = "ok"  # "ok" o "degraded" — como health en k8s
    database: str = "unknown"  # "ok" o "error: ..." — ver main.py:85 SELECT 1
    version: str = "1.0.0-sprint1"  # versión Sprint 1 — como version en package.json


# ---------------------------------------------------------------------------
# Access Events — HU-01, RF-2.2 / Tabla access_events (models.py:20, init.sql:6)
# Gateway ESP32 POST /api/access-events con {"user_id","pin_hash","success"} (gateway.ino:366)
# ---------------------------------------------------------------------------
class AccessEventCreate(BaseModel):
    """Payload POST /api/access-events — valida antes de INSERT (como Zod parse para req.body)."""
    # Field(..., max_length=64) — ... = requerido (como z.string().min(1)), max 64 como VARCHAR(64) en models.py:24
    user_id: str = Field(..., max_length=64, description="ID usuario / keypad_user (físico o App)")
    pin_hash: str = Field(..., max_length=128, description="hash HEX del PIN (djb2 en keypad_control.cpp:99, nunca PIN claro)")
    success: bool  # true/false — como success en MEGA processPinAttempt (models.py:26)
    source: str = Field(default="keypad", max_length=32, description="origen: keypad, app, bluetooth (RF-1.3 futuro)")


class AccessEventOut(BaseModel):
    """Respuesta GET /api/access-events — lo que devuelve la API (como type Response en TS)."""
    # ConfigDict(from_attributes=True) — permite crear desde ORM object (AccessEvent) directamente
    # Como `plainToInstance` en class-transformer — mapea Column a field sin dict manual
    model_config = ConfigDict(from_attributes=True)

    id: UUID  # PK — como id en Prisma, UUID v4
    user_id: str
    pin_hash: str
    success: bool
    timestamp: datetime  # TIMESTAMPTZ — como Date en JS pero con zona
    source: str


# ---------------------------------------------------------------------------
# Sensor Events — RNF-2.1, HU-03 / Tabla sensor_events (models.py:36, EST-06)
# HC-SR04 (ultrasonic cm), KY-037 (sound db), TCRT5000 (ir boolean) — con EMA filtrado
# ---------------------------------------------------------------------------
class SensorEventCreate(BaseModel):
    """Payload POST /api/sensor-events — para telemetría con valor raw y filtrado EMA."""
    sensor_id: str = Field(..., max_length=64, description="ej. hc-sr04-01, ky-037-01 — deviceId IoT")
    sensor_type: str = Field(..., max_length=32, description="laser, ultrasonic, ir, sound, rf — indexado para EST-06")
    value: float  # valor crudo — ej. 42.5 cm del HC-SR04 sin filtrar (Numeric(10,4) en models.py:42)
    filtered_value: float | None = None  # valor EMA α=0.2 — nullable si no hay filtro aún (stats/ema_filter.py:32)
    unit: str = Field(..., max_length=16, description="cm (HC-SR04), db (KY-037), boolean (TCRT), etc.")
    # metadata — extras flexibles: {"alpha":0.2, "threshold":30, "rssi":-70} — JSONB en Postgres (init.sql:23)
    # default_factory=dict evita mutable default (como () => ({}) en JS) — cada instancia su dict
    metadata: dict[str, Any] = Field(default_factory=dict)


class SensorEventOut(BaseModel):
    """Respuesta GET /api/sensor-events — mapea event_metadata (columna) ↔ metadata (API)."""
    # from_attributes + populate_by_name — permite alias event_metadata ↔ metadata (models.py:47)
    model_config = ConfigDict(from_attributes=True, populate_by_name=True)

    id: UUID
    sensor_id: str
    sensor_type: str
    value: float
    filtered_value: float | None = None
    unit: str
    timestamp: datetime
    # Mapea atributo ORM event_metadata (columna "metadata") → campo API "metadata"
    # validation_alias = lee de event_metadata en DB, serialization_alias = escribe como metadata en JSON
    # Como `@Column({name: "metadata"}) event_metadata` en TypeORM con alias en DTO
    metadata: dict[str, Any] = Field(default_factory=dict, validation_alias="event_metadata", serialization_alias="metadata")


# ---------------------------------------------------------------------------
# Security Events — HU-02, RF-2.3, RF-4.1 / Tabla security_events (models.py:55)
# Láser KY-008 intrusión → Telegram vía Node-RED (aethernet/seguridad/intrusion)
# ---------------------------------------------------------------------------
class SecurityEventCreate(BaseModel):
    """Payload POST /api/security-events — evento seguridad con severidad."""
    event_type: str = Field(..., max_length=32, description="intrusion (láser), access_denied, rf_failstop (HU-04)")
    severity: str = Field(default="medium", max_length=16, description="low, medium, high, critical — como priority en Jira")
    description: str | None = None  # texto libre — "Laser interrupted at door" (Text en models.py:61)


class SecurityEventOut(BaseModel):
    """Respuesta GET /api/security-events — con acknowledged para workflow admin."""
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    event_type: str
    severity: str
    description: str | None = None
    timestamp: datetime
    acknowledged: bool  # ¿admin vio alerta? — como read en notificaciones React (models.py:63)
    acknowledged_at: datetime | None = None  # cuándo se marcó visto — nullable hasta ack
    acknowledged_by: str | None = None  # quién — "admin" o user_id


# ---------------------------------------------------------------------------
# Rover Telemetry — RF-3.1, RF-3.3, HU-04 / Tabla rover_telemetry (models.py:73)
# Cada paquete RF del UNO (PWM + HC-SR04 EMA + TCRT + RSSI) por session_id
# Gateway publishRoverTelemetry → MQTT → backend (futuro) o directo HTTP POST aquí
# ---------------------------------------------------------------------------
class RoverTelemetryCreate(BaseModel):
    """Payload POST /api/rover/telemetry — telemetría RF por sesión (como struct RoverTelemetry en rover-uno.ino:91)."""
    session_id: UUID  # agrupa telemetría por run — como sessionId en tracking (ver models.py:77)
    # ge=-255, le=255 — valida PWM igual que rover.ino:69 constrain(-255,255) y C int16_t
    left_motor_pwm: int = Field(..., ge=-255, le=255, description="PWM izq -255..255 (SmallInteger, int16_t en C)")
    right_motor_pwm: int = Field(..., ge=-255, le=255, description="PWM der")
    # ge=0, le=500 — distancia 0-500cm, valida antes de Numeric(6,2) en models.py:80
    ultrasonic_distance_cm: float | None = Field(default=None, ge=0, le=500, description="HC-SR04 EMA α0.2, 0-200cm típico")
    ir_left: bool | None = None  # TCRT — true si <500 (borde) — como bool en C rover.ino:240
    ir_center: bool | None = None
    ir_right: bool | None = None
    # ge=-120, le=0 — RSSI dBm negativo (como WiFi signal en dBm) — placeholder -70 en rover.ino:243
    rf_rssi: int | None = Field(default=None, ge=-120, le=0, description="RSSI dBm -120..0")


class RoverTelemetryOut(BaseModel):
    """Respuesta GET /api/rover/telemetry — con timestamp para replay y EST-05 t-Student."""
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    session_id: UUID
    left_motor_pwm: int
    right_motor_pwm: int
    ultrasonic_distance_cm: float | None = None
    ir_left: bool | None = None
    ir_center: bool | None = None
    ir_right: bool | None = None
    rf_rssi: int | None = None
    timestamp: datetime  # para ORDER BY timestamp y latencias por sesión (EST-05 RF vs WiFi)
