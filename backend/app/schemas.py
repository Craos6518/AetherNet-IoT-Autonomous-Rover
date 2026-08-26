"""
Pydantic schemas para AetherNet IoT — Sprint 1 / DEVOPS-03, DEVOPS-06.
Valida payloads de access_events, sensor_events, security_events, rover_telemetry.
Orígenes: requirements.md RF-2.1, RNF-2.2, HU-01..HU-04.
"""

from datetime import datetime
from typing import Any
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field


# ---------------------------------------------------------------------------
# Health
# ---------------------------------------------------------------------------
class HealthResponse(BaseModel):
    status: str = "ok"
    database: str = "unknown"
    version: str = "1.0.0-sprint1"


# ---------------------------------------------------------------------------
# Access Events — HU-01, RF-2.2
# ---------------------------------------------------------------------------
class AccessEventCreate(BaseModel):
    user_id: str = Field(..., max_length=64, description="ID usuario / keypad_user")
    pin_hash: str = Field(..., max_length=128)
    success: bool
    source: str = Field(default="keypad", max_length=32)


class AccessEventOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    user_id: str
    pin_hash: str
    success: bool
    timestamp: datetime
    source: str


# ---------------------------------------------------------------------------
# Sensor Events — RNF-2.1, HU-03
# ---------------------------------------------------------------------------
class SensorEventCreate(BaseModel):
    sensor_id: str = Field(..., max_length=64)
    sensor_type: str = Field(..., max_length=32, description="laser, ultrasonic, ir, sound, rf")
    value: float
    filtered_value: float | None = None
    unit: str = Field(..., max_length=16, description="cm, db, boolean, etc")
    metadata: dict[str, Any] = Field(default_factory=dict)


class SensorEventOut(BaseModel):
    model_config = ConfigDict(from_attributes=True, populate_by_name=True)

    id: UUID
    sensor_id: str
    sensor_type: str
    value: float
    filtered_value: float | None = None
    unit: str
    timestamp: datetime
    # Mapea atributo ORM event_metadata -> campo API "metadata"
    metadata: dict[str, Any] = Field(default_factory=dict, validation_alias="event_metadata", serialization_alias="metadata")


# ---------------------------------------------------------------------------
# Security Events — HU-02, RF-2.3, RF-4.1
# ---------------------------------------------------------------------------
class SecurityEventCreate(BaseModel):
    event_type: str = Field(..., max_length=32, description="intrusion, access_denied, rf_failstop")
    severity: str = Field(default="medium", max_length=16)
    description: str | None = None


class SecurityEventOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    event_type: str
    severity: str
    description: str | None = None
    timestamp: datetime
    acknowledged: bool
    acknowledged_at: datetime | None = None
    acknowledged_by: str | None = None


# ---------------------------------------------------------------------------
# Rover Telemetry — RF-3.1, RF-3.3, HU-04
# ---------------------------------------------------------------------------
class RoverTelemetryCreate(BaseModel):
    session_id: UUID
    left_motor_pwm: int = Field(..., ge=-255, le=255)
    right_motor_pwm: int = Field(..., ge=-255, le=255)
    ultrasonic_distance_cm: float | None = Field(default=None, ge=0, le=500)
    ir_left: bool | None = None
    ir_center: bool | None = None
    ir_right: bool | None = None
    rf_rssi: int | None = Field(default=None, ge=-120, le=0)


class RoverTelemetryOut(BaseModel):
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
    timestamp: datetime
