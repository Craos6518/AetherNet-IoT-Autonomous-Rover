"""
==============================================================================
Routers mínimos Sprint 1 — DEVOPS-06 | 6º Semestre UTP | HU-01..HU-04, RNF-2.2
Autor: Est. Tecnología en Desarrollo Software + Ing. Sistemas (UTP)
Experiencia: 2 años Python (FastAPI), 2 años JS/React (Express routers),
             1 año PostgreSQL (SELECT, INSERT, ORDER BY, LIMIT/OFFSET),
             2 años electrónica/Arduino (eventos HU-01..HU-04)
FOSS: FastAPI 0.111 + SQLAlchemy 2.0 async — RNF-3.1 (como Express + Prisma)
Analogía React/Express: este archivo es `routes/events.js` con
`router.post('/access-events', handler)` — aquí es @router.post con Depends(get_db).
Analogía C: cada handler es como `processPinAttempt` pero por HTTP — valida, INSERT, retorna.
Analogía PostgreSQL: cada handler es un `INSERT INTO access_events ... RETURNING *`
y `SELECT * FROM ... ORDER BY timestamp DESC LIMIT 50` — ORM lo traduce.
Endpoints habilitan HU-01 (access), HU-03 (sensor EMA), HU-02 (security), RF-3.1 (rover).
"""

from uuid import UUID  # para session_id en rover telemetry — como UUID en JS crypto.randomUUID()

from fastapi import APIRouter, Depends, Query, status  # APIRouter = Router en Express, Query = req.query, status = http codes
from sqlalchemy import desc, select  # desc = ORDER BY DESC, select = SELECT * FROM — como Prisma findMany
from sqlalchemy.ext.asyncio import AsyncSession  # sesión async — como db client en Node con async/await

from app.database import get_db  # Depends que inyecta sesión — como middleware que pone req.db
from app.models import AccessEvent, RoverTelemetry, SecurityEvent, SensorEvent  # ORM models — como Prisma models
from app.schemas import (  # Pydantic schemas — como Zod schemas para req.body y res
    AccessEventCreate,
    AccessEventOut,
    RoverTelemetryCreate,
    RoverTelemetryOut,
    SecurityEventCreate,
    SecurityEventOut,
    SensorEventCreate,
    SensorEventOut,
)

# Router con prefijo /api — como `const router = Router({prefix: '/api'})` en Express
# tags=["events"] agrupa en /docs Swagger — como group en Swagger UI
router = APIRouter(prefix="/api", tags=["events"])


# ---------------------------------------------------------------------------
# Access Events — HU-01, RF-2.2 / Tabla access_events (models.py:20, init.sql:6)
# Gateway ESP32 POST /api/access-events con pin_hash (gateway.ino:366) → App GET
# ---------------------------------------------------------------------------
@router.post("/access-events", response_model=AccessEventOut, status_code=status.HTTP_201_CREATED)
async def create_access_event(payload: AccessEventCreate, db: AsyncSession = Depends(get_db)):
    """
    POST /api/access-events — Crea evento acceso (como INSERT INTO access_events).
    Llamado por Gateway ESP32 (HTTPClient POST) tras PIN keypad/App — ver gateway.ino:378.
    Valida con AccessEventCreate (Zod-like) antes de INSERT — si falta pin_hash → 422.
    """
    # Crea objeto ORM — como `new AccessEvent({...})` en Prisma, pero con SQLAlchemy
    event = AccessEvent(
        user_id=payload.user_id,  # "keypad_user" o ID App
        pin_hash=payload.pin_hash,  # hash HEX, nunca PIN claro (keypad_control.cpp:99 djb2)
        success=payload.success,  # true/false
        source=payload.source,  # "keypad", "app", "bluetooth"
    )
    db.add(event)  # stage INSERT — como `prisma.accessEvent.create({data})` pero no ejecuta aún
    await db.commit()  # ejecuta INSERT + COMMIT — como `await prisma.$transaction()`
    await db.refresh(event)  # recarga id/timestamp generados por DB — como `RETURNING *` en SQL
    return event  # FastAPI serializa a AccessEventOut (ConfigDict from_attributes) — como res.json(event)


@router.get("/access-events", response_model=list[AccessEventOut])
async def list_access_events(
    # Query params con validación — como `req.query.limit` con Zod: limit 1..200 default 50 (como paginación en React Table)
    limit: int = Query(default=50, ge=1, le=200, description="cuántos eventos (paginación, como limit en SQL)"),
    offset: int = Query(default=0, ge=0, description="desde dónde (como OFFSET en SQL, para infinite scroll en React)"),
    db: AsyncSession = Depends(get_db),  # inyecta sesión — como `const db = req.db` en Express middleware
):
    """GET /api/access-events?limit=50&offset=0 — Lista accesos más recientes primero (DESC)."""
    # SELECT * FROM access_events ORDER BY timestamp DESC LIMIT 50 OFFSET 0 — como Prisma findMany con orderBy
    result = await db.execute(select(AccessEvent).order_by(desc(AccessEvent.timestamp)).limit(limit).offset(offset))
    return result.scalars().all()  # lista de AccessEvent ORM → FastAPI serializa a list[AccessEventOut]


# ---------------------------------------------------------------------------
# Sensor Events — RNF-2.1, HU-03 / Tabla sensor_events (EST-06, stats/ema_filter.py)
# ---------------------------------------------------------------------------
@router.post("/sensor-events", response_model=SensorEventOut, status_code=status.HTTP_201_CREATED)
async def create_sensor_event(payload: SensorEventCreate, db: AsyncSession = Depends(get_db)):
    """POST /api/sensor-events — Crea evento sensor con value raw y filtered_value EMA (stats)."""
    event = SensorEvent(
        sensor_id=payload.sensor_id,  # "hc-sr04-01"
        sensor_type=payload.sensor_type,  # "ultrasonic", "sound" — indexado para EST-06
        value=payload.value,  # raw — 42.5 cm
        filtered_value=payload.filtered_value,  # EMA α0.2 — 41.2 cm (stats/ema_filter.py:32)
        unit=payload.unit,  # "cm", "db"
        # event_metadata es atributo ORM mapeado a columna "metadata" JSONB — ver models.py:47
        # payload.metadata viene de Field(default_factory=dict) en schemas.py:53
        event_metadata=payload.metadata,  # {"alpha":0.2, "threshold":30} — extras flexibles
    )
    db.add(event)
    await db.commit()
    await db.refresh(event)
    return event


@router.get("/sensor-events", response_model=list[SensorEventOut])
async def list_sensor_events(
    # Filtro opcional por tipo — como `?sensor_type=ultrasonic` en React fetch (ver stats EST-06)
    sensor_type: str | None = None,  # None = todos, "ultrasonic" = solo HC-SR04
    limit: int = Query(default=50, ge=1, le=200),
    offset: int = Query(default=0, ge=0),
    db: AsyncSession = Depends(get_db),
):
    """GET /api/sensor-events?sensor_type=ultrasonic&limit=50 — Lista sensores, opcional filtrado por tipo."""
    # Construye query dinámico — como Prisma where: { sensor_type } si se da
    stmt = select(SensorEvent).order_by(desc(SensorEvent.timestamp))  # base ORDER BY DESC
    if sensor_type:
        stmt = stmt.where(SensorEvent.sensor_type == sensor_type)  # WHERE sensor_type = 'ultrasonic' (si ?sensor_type)
    stmt = stmt.limit(limit).offset(offset)  # LIMIT/OFFSET — paginación (como en React Table)
    result = await db.execute(stmt)
    return result.scalars().all()


# ---------------------------------------------------------------------------
# Security Events — HU-02, RF-2.3, RF-4.1 / Tabla security_events (Node-RED → Telegram)
# ---------------------------------------------------------------------------
@router.post("/security-events", response_model=SecurityEventOut, status_code=status.HTTP_201_CREATED)
async def create_security_event(payload: SecurityEventCreate, db: AsyncSession = Depends(get_db)):
    """POST /api/security-events — Crea evento seguridad (intrusión láser KY-008 → Telegram)."""
    event = SecurityEvent(
        event_type=payload.event_type,  # "intrusion", "access_denied", "rf_failstop"
        severity=payload.severity,  # "low", "medium", "high", "critical" — default medium
        description=payload.description,  # "Laser interrupted at door"
    )
    db.add(event)
    await db.commit()
    await db.refresh(event)
    return event


@router.get("/security-events", response_model=list[SecurityEventOut])
async def list_security_events(
    event_type: str | None = None,  # filtro opcional — como ?event_type=intrusion
    limit: int = Query(default=50, ge=1, le=200),
    offset: int = Query(default=0, ge=0),
    db: AsyncSession = Depends(get_db),
):
    """GET /api/security-events?event_type=intrusion — Lista seguridad, opcional por tipo."""
    stmt = select(SecurityEvent).order_by(desc(SecurityEvent.timestamp))
    if event_type:
        stmt = stmt.where(SecurityEvent.event_type == event_type)
    stmt = stmt.limit(limit).offset(offset)
    result = await db.execute(stmt)
    return result.scalars().all()


# ---------------------------------------------------------------------------
# Rover Telemetry — RF-3.1, RF-3.3, HU-04 / Tabla rover_telemetry (session_id + timestamp)
# POST cada paquete RF del UNO — para replay, EST-05 t-Student y dashboard Rover
# ---------------------------------------------------------------------------
@router.post("/rover/telemetry", response_model=RoverTelemetryOut, status_code=status.HTTP_201_CREATED)
async def create_rover_telemetry(payload: RoverTelemetryCreate, db: AsyncSession = Depends(get_db)):
    """POST /api/rover/telemetry — Crea telemetría Rover por sesión (como INSERT INTO rover_telemetry)."""
    entry = RoverTelemetry(
        session_id=payload.session_id,  # UUID sesión — agrupa telemetría por run joystick (como sessionId en analytics)
        left_motor_pwm=payload.left_motor_pwm,  # -255..255 — igual que RoverCommand left_pwm en gateway/r.ino
        right_motor_pwm=payload.right_motor_pwm,
        ultrasonic_distance_cm=payload.ultrasonic_distance_cm,  # HC-SR04 EMA α0.2 — 42.5 cm
        ir_left=payload.ir_left,  # TCRT — true si borde (como bool en C rover.ino:240)
        ir_center=payload.ir_center,
        ir_right=payload.ir_right,
        rf_rssi=payload.rf_rssi,  # -70 dBm placeholder — RF24 no da RSSI real (rover.ino:243)
    )
    db.add(entry)
    await db.commit()
    await db.refresh(entry)
    return entry


@router.get("/rover/telemetry", response_model=list[RoverTelemetryOut])
async def list_rover_telemetry(
    session_id: UUID | None = None,  # filtro por sesión — como ?session_id=uuid para replay ruta
    limit: int = Query(default=50, ge=1, le=200),
    offset: int = Query(default=0, ge=0),
    db: AsyncSession = Depends(get_db),
):
    """GET /api/rover/telemetry?session_id=uuid&limit=50 — Lista telemetría, opcional por sesión."""
    # Índice compuesto (session_id, timestamp) en models.py:88 — optimiza este query por sesión ordenado
    stmt = select(RoverTelemetry).order_by(desc(RoverTelemetry.timestamp))
    if session_id:
        stmt = stmt.where(RoverTelemetry.session_id == session_id)  # WHERE session_id = ?
    stmt = stmt.limit(limit).offset(offset)
    result = await db.execute(stmt)
    return result.scalars().all()
