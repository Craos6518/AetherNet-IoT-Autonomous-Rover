"""
Routers mínimos Sprint 1 — DEVOPS-06.
Endpoints que habilitan HU-01..HU-04 y RNF-2.2.
"""

from uuid import UUID

from fastapi import APIRouter, Depends, Query, status
from sqlalchemy import desc, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.models import AccessEvent, RoverTelemetry, SecurityEvent, SensorEvent
from app.schemas import (
    AccessEventCreate,
    AccessEventOut,
    RoverTelemetryCreate,
    RoverTelemetryOut,
    SecurityEventCreate,
    SecurityEventOut,
    SensorEventCreate,
    SensorEventOut,
)

router = APIRouter(prefix="/api", tags=["events"])


# ---------------------------------------------------------------------------
# Access Events
# ---------------------------------------------------------------------------
@router.post("/access-events", response_model=AccessEventOut, status_code=status.HTTP_201_CREATED)
async def create_access_event(payload: AccessEventCreate, db: AsyncSession = Depends(get_db)):
    event = AccessEvent(
        user_id=payload.user_id,
        pin_hash=payload.pin_hash,
        success=payload.success,
        source=payload.source,
    )
    db.add(event)
    await db.commit()
    await db.refresh(event)
    return event


@router.get("/access-events", response_model=list[AccessEventOut])
async def list_access_events(
    limit: int = Query(default=50, ge=1, le=200),
    offset: int = Query(default=0, ge=0),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(select(AccessEvent).order_by(desc(AccessEvent.timestamp)).limit(limit).offset(offset))
    return result.scalars().all()


# ---------------------------------------------------------------------------
# Sensor Events
# ---------------------------------------------------------------------------
@router.post("/sensor-events", response_model=SensorEventOut, status_code=status.HTTP_201_CREATED)
async def create_sensor_event(payload: SensorEventCreate, db: AsyncSession = Depends(get_db)):
    event = SensorEvent(
        sensor_id=payload.sensor_id,
        sensor_type=payload.sensor_type,
        value=payload.value,
        filtered_value=payload.filtered_value,
        unit=payload.unit,
        event_metadata=payload.metadata,
    )
    db.add(event)
    await db.commit()
    await db.refresh(event)
    return event


@router.get("/sensor-events", response_model=list[SensorEventOut])
async def list_sensor_events(
    sensor_type: str | None = None,
    limit: int = Query(default=50, ge=1, le=200),
    offset: int = Query(default=0, ge=0),
    db: AsyncSession = Depends(get_db),
):
    stmt = select(SensorEvent).order_by(desc(SensorEvent.timestamp))
    if sensor_type:
        stmt = stmt.where(SensorEvent.sensor_type == sensor_type)
    stmt = stmt.limit(limit).offset(offset)
    result = await db.execute(stmt)
    return result.scalars().all()


# ---------------------------------------------------------------------------
# Security Events
# ---------------------------------------------------------------------------
@router.post("/security-events", response_model=SecurityEventOut, status_code=status.HTTP_201_CREATED)
async def create_security_event(payload: SecurityEventCreate, db: AsyncSession = Depends(get_db)):
    event = SecurityEvent(
        event_type=payload.event_type,
        severity=payload.severity,
        description=payload.description,
    )
    db.add(event)
    await db.commit()
    await db.refresh(event)
    return event


@router.get("/security-events", response_model=list[SecurityEventOut])
async def list_security_events(
    event_type: str | None = None,
    limit: int = Query(default=50, ge=1, le=200),
    offset: int = Query(default=0, ge=0),
    db: AsyncSession = Depends(get_db),
):
    stmt = select(SecurityEvent).order_by(desc(SecurityEvent.timestamp))
    if event_type:
        stmt = stmt.where(SecurityEvent.event_type == event_type)
    stmt = stmt.limit(limit).offset(offset)
    result = await db.execute(stmt)
    return result.scalars().all()


# ---------------------------------------------------------------------------
# Rover Telemetry
# ---------------------------------------------------------------------------
@router.post("/rover/telemetry", response_model=RoverTelemetryOut, status_code=status.HTTP_201_CREATED)
async def create_rover_telemetry(payload: RoverTelemetryCreate, db: AsyncSession = Depends(get_db)):
    entry = RoverTelemetry(
        session_id=payload.session_id,
        left_motor_pwm=payload.left_motor_pwm,
        right_motor_pwm=payload.right_motor_pwm,
        ultrasonic_distance_cm=payload.ultrasonic_distance_cm,
        ir_left=payload.ir_left,
        ir_center=payload.ir_center,
        ir_right=payload.ir_right,
        rf_rssi=payload.rf_rssi,
    )
    db.add(entry)
    await db.commit()
    await db.refresh(entry)
    return entry


@router.get("/rover/telemetry", response_model=list[RoverTelemetryOut])
async def list_rover_telemetry(
    session_id: UUID | None = None,
    limit: int = Query(default=50, ge=1, le=200),
    offset: int = Query(default=0, ge=0),
    db: AsyncSession = Depends(get_db),
):
    stmt = select(RoverTelemetry).order_by(desc(RoverTelemetry.timestamp))
    if session_id:
        stmt = stmt.where(RoverTelemetry.session_id == session_id)
    stmt = stmt.limit(limit).offset(offset)
    result = await db.execute(stmt)
    return result.scalars().all()
