"""
==============================================================================
Tests Sprint 1 — DEVOPS-06 / RNF-1.1 | 6º Semestre UTP | Backend Minimal
Autor: Est. Tecnología en Desarrollo Software + Ing. Sistemas (UTP)
Experiencia: 2 años Python (pytest, FastAPI TestClient), 2 años JS/React (Jest, supertest),
             1 año PostgreSQL (mock DB), 2 años electrónica/Arduino (HU-01..HU-04)
Materia: DEVOPS-06 — CRUD mínimo eventos (HU-01..HU-04, RNF-2.2)
FOSS: pytest + FastAPI TestClient (MIT) — RNF-3.1, como Jest + supertest en Node
Analogía React/Jest: este archivo es `__tests__/health.test.ts` con
`describe('health', () => it('should return 200', ...))` — aquí con @pytest.fixture y
TestClient como supertest. Mock de DB es como mock de Prisma con jest.mock().
Analogía C: cada test es como validar que `processPinAttempt` no crashea sin hardware —
aquí validamos que endpoints no crashean sin Postgres real (mock).
Verifica que el backend minimal responde a los endpoints que CI usa (ci.yml:116 health check).
Sin Postgres real: mock de DB con MagicMock/AsyncMock (como mock de pg Pool en Node).
==============================================================================
"""

import uuid  # para generar UUIDs mock — como crypto.randomUUID() en JS
from datetime import datetime, timezone  # para timestamp mock — como new Date() en JS pero con TZ
from unittest.mock import AsyncMock, MagicMock  # mocks — como jest.fn() / jest.mock() en Jest

import pytest  # framework — como Jest
from fastapi.testclient import TestClient  # TestClient — como supertest en Node (simula HTTP sin levantar server)

from app.main import app  # FastAPI app — como import app from './main' en Express
from app.database import get_db  # Depends a mockear — como mock de pool.query en Node


# --------------------------------------------------------------------------
# Fixture mock_db_session — mock de AsyncSession sin Postgres real
# --------------------------------------------------------------------------
@pytest.fixture
def mock_db_session():
    """Mock de DB que simula INSERT y SELECT sin Postgres (como mock de Prisma en Jest)."""
    session = MagicMock()  # mock sync — como jest.fn() para session.add

    # fake_execute — simula db.execute(SELECT 1) en /health y SELECTs en list_* (events.py:51)
    async def fake_execute(*args, **kwargs):
        return None  # para /health, None es ok (no error) — ver test_health_ok

    session.execute = AsyncMock(side_effect=fake_execute)  # async mock — como jest.fn().mockResolvedValue(null)
    session.commit = AsyncMock()  # commit no hace nada — como mock de prisma.$transaction
    # refresh no hace nada — el objeto ya tiene id/timestamp por fake_add (como RETURNING * mockeado)
    session.refresh = AsyncMock()

    def fake_add(obj):
        """Simula autoincrement de DB: asigna id y timestamp si no existen (como autoGenerate en Prisma)."""
        # Si no tiene id (PK), genera UUID — como uuid_generate_v4() en init.sql:7 pero en Python
        if not getattr(obj, "id", None):
            obj.id = uuid.uuid4()  # como crypto.randomUUID() en JS
        # Si no tiene timestamp, pone NOW() — como server_default func.now() en models.py:27
        if not getattr(obj, "timestamp", None):
            obj.timestamp = datetime.now(timezone.utc)  # UTC — como new Date().toISOString() con TZ
        # Para AccessEvent acknowledged defaults — no aplica a AccessEvent, pero para SecurityEvent
        if hasattr(obj, "acknowledged") and obj.acknowledged is None:
            obj.acknowledged = False  # default false — como default en init.sql:32
        # Para SensorEvent metadata — default {} — como default=dict en models.py:47
        if hasattr(obj, "event_metadata") and obj.event_metadata is None:
            obj.event_metadata = {}  # evita null en JSONB

    session.add = MagicMock(side_effect=fake_add)  # add con side_effect fake_add — como jest.fn(fakeAdd)
    return session


# --------------------------------------------------------------------------
# Fixture client — TestClient con DB mockeada (como supertest(app) con mock DB)
# --------------------------------------------------------------------------
@pytest.fixture
def client(mock_db_session):
    """TestClient con get_db override — inyecta mock_db_session en lugar de Postgres real."""
    # override_get_db — reemplaza Depends(get_db) con mock (como jest.mock('./db', () => mockDb))
    async def override_get_db():
        yield mock_db_session  # yield mock — como `req.db = mockDb` en Express middleware mock

    app.dependency_overrides[get_db] = override_get_db  # inyecta mock — como app.use(mockMiddleware) en tests
    # Mockear query de listado: devolver lista vacía — como mock de findMany que retorna []
    mock_db_session.execute = AsyncMock(
        return_value=MagicMock(scalars=lambda: MagicMock(all=lambda: []))  # scalars().all() → [] — lista vacía
    )
    with TestClient(app) as c:  # TestClient — como supertest(app) — simula HTTP sin levantar uvicorn
        yield c  # entrega client para que tests hagan client.get/post (como request(app).get en supertest)
    app.dependency_overrides.clear()  # limpia override tras test — como afterEach(() => jest.clearAllMocks())


# --------------------------------------------------------------------------
# Tests — cada uno es un endpoint (como it('should ...') en Jest)
# --------------------------------------------------------------------------
def test_root(client):
    """GET / — info básica, sin DB (como ping del ESP32) — debe dar 200."""
    r = client.get("/")  # como request(app).get('/') en supertest
    assert r.status_code == 200  # como expect(res.status).toBe(200) en Jest
    assert r.json()["health"] == "/health"  # verifica campo health — como expect(res.body.health).toBe('/health')


def test_health_ok(client, mock_db_session):
    """GET /health — con DB mock ok, debe dar 200 y status ok/degraded (como health check en k8s)."""
    # Para /health, execute debe devolver algo sin error — simula SELECT 1 ok
    mock_db_session.execute = AsyncMock(return_value=None)  # None = SELECT 1 sin error
    r = client.get("/health")
    assert r.status_code == 200
    data = r.json()
    assert data["status"] in ("ok", "degraded")  # ok si DB ok, degraded si error (main.py:90)
    assert "database" in data  # siempre hay campo database — como health.database en JS
    assert data["version"] == "1.0.0-sprint1"  # versión Sprint 1 — como version en package.json


def test_create_access_event(client):
    """POST /api/access-events — HU-01 — crea evento acceso como Gateway POST (gateway.ino:378)."""
    r = client.post(
        "/api/access-events",
        json={"user_id": "keypad_user", "pin_hash": "abc123", "success": True, "source": "keypad"},  # payload HU-01
    )
    assert r.status_code == 201  # 201 Created — como res.status(201) en Express (events.py:31)
    data = r.json()
    assert data["user_id"] == "keypad_user"  # verifica payload eco — como expect(res.body.user_id).toBe(...)
    assert data["success"] is True
    assert "id" in data  # DB asignó UUID — como expect(res.body.id).toBeDefined() en Jest


def test_create_sensor_event(client):
    """POST /api/sensor-events — RNF-2.1, HU-03 — sensor con value y filtered EMA (stats)."""
    r = client.post(
        "/api/sensor-events",
        json={
            "sensor_id": "hc-sr04-01",  # deviceId — como sensorId en firmware
            "sensor_type": "ultrasonic",  # tipo — indexado para EST-06 (events.py:82 WHERE)
            "value": 42.5,  # raw — 42.5 cm sin filtrar
            "filtered_value": 41.2,  # EMA α0.2 — como ultrasonicEma en rover.ino:239
            "unit": "cm",  # unidad — como unit en SensorEvent models.py:44
            "metadata": {"alpha": 0.2},  # extras — como event_metadata JSONB en models.py:47
        },
    )
    assert r.status_code == 201
    assert r.json()["sensor_id"] == "hc-sr04-01"


def test_validation_rejects_invalid_payload(client):
    """Valida que Pydantic rechaza payloads inválidos con 422 (como Zod parse error)."""
    # Falta pin_hash — AccessEventCreate requiere pin_hash (Field(..., max_length=128) en schemas.py:28)
    r = client.post("/api/access-events", json={"user_id": "x", "success": True})
    assert r.status_code == 422  # 422 Unprocessable Entity — Pydantic valida (como Zod error 400/422)

    # Falta sensor_type — requerido (Field(...) en schemas.py:49)
    r = client.post("/api/sensor-events", json={"sensor_id": "s1", "value": 10, "unit": "cm"})
    assert r.status_code == 422  # también 422 — validación falla


def test_list_endpoints_return_empty(client):
    """GET list endpoints — sin datos, deben dar 200 y lista vacía [] (como findMany vacío en Prisma)."""
    r = client.get("/api/access-events")  # GET con limit/offset default 50/0 (events.py:47)
    assert r.status_code == 200
    assert isinstance(r.json(), list)  # lista — como expect(Array.isArray(res.body)).toBe(true) en Jest

    r = client.get("/api/sensor-events")
    assert r.status_code == 200  # 200 OK con [] — no 404 (recurso existe, solo vacío)

    r = client.get("/api/security-events")
    assert r.status_code == 200

    r = client.get("/api/rover/telemetry")
    assert r.status_code == 200  # rover telemetry también — RF-3.1 (events.py:141)



# Test agregado por estudiante Movil - verifica que /health no crashea si la DB esta caida
# Lo vi en un video de FastAPI health checks - debe devolver degraded no 500
def test_health_degraded_when_db_down(client, mock_db_session):
    """GET /health debe dar 200 degraded si DB lanza excepcion, no 500."""
    # Simulo que la DB esta caida - como cuando el postgres no esta levantado
    mock_db_session.execute = AsyncMock(side_effect=Exception("connection refused"))

    # Tengo que re-inyectar el mock que falla
    from app.database import get_db
    from app.main import app

    async def override_fail():
        yield mock_db_session

    app.dependency_overrides[get_db] = override_fail

    # Uso un cliente nuevo con el override que falla
    with TestClient(app) as c2:
        r = c2.get("/health")
        assert r.status_code == 200  # no debe ser 500
        assert r.json()["status"] == "degraded"  # debe avisar que esta degraded
        assert "error" in r.json()["database"]  # debe decir error

    app.dependency_overrides.clear()
    # Restauro el mock original para no afectar otros tests
    mock_db_session.execute = AsyncMock(
        return_value=MagicMock(scalars=lambda: MagicMock(all=lambda: []))
    )
