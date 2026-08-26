"""
Tests Sprint 1 — DEVOPS-06 / RNF-1.1
Verifica que el backend minimal responde a los endpoints que el CI usa (ci.yml:116).
Sin Postgres real: mock de DB.
"""

import uuid
from datetime import datetime, timezone
from unittest.mock import AsyncMock, MagicMock

import pytest
from fastapi.testclient import TestClient

from app.main import app
from app.database import get_db


@pytest.fixture
def mock_db_session():
    session = MagicMock()

    async def fake_execute(*args, **kwargs):
        return None

    session.execute = AsyncMock(side_effect=fake_execute)
    session.commit = AsyncMock()
    # refresh no hace nada, el objeto ya tiene id/timestamp por side_effect de add
    session.refresh = AsyncMock()

    def fake_add(obj):
        # Simula autoincrement de DB: asigna id y timestamp si no existen
        if not getattr(obj, "id", None):
            obj.id = uuid.uuid4()
        if not getattr(obj, "timestamp", None):
            obj.timestamp = datetime.now(timezone.utc)
        # Para AccessEvent acknowledged defaults
        if hasattr(obj, "acknowledged") and obj.acknowledged is None:
            obj.acknowledged = False
        if hasattr(obj, "event_metadata") and obj.event_metadata is None:
            obj.event_metadata = {}

    session.add = MagicMock(side_effect=fake_add)
    return session


@pytest.fixture
def client(mock_db_session):
    async def override_get_db():
        yield mock_db_session

    app.dependency_overrides[get_db] = override_get_db
    # Mockear la query de listado: devolver lista vacía
    mock_db_session.execute = AsyncMock(
        return_value=MagicMock(scalars=lambda: MagicMock(all=lambda: []))
    )
    with TestClient(app) as c:
        yield c
    app.dependency_overrides.clear()


def test_root(client):
    r = client.get("/")
    assert r.status_code == 200
    assert r.json()["health"] == "/health"


def test_health_ok(client, mock_db_session):
    # Para /health, execute debe devolver algo sin error
    mock_db_session.execute = AsyncMock(return_value=None)
    r = client.get("/health")
    assert r.status_code == 200
    data = r.json()
    assert data["status"] in ("ok", "degraded")
    assert "database" in data
    assert data["version"] == "1.0.0-sprint1"


def test_create_access_event(client):
    r = client.post(
        "/api/access-events",
        json={"user_id": "keypad_user", "pin_hash": "abc123", "success": True, "source": "keypad"},
    )
    assert r.status_code == 201
    data = r.json()
    assert data["user_id"] == "keypad_user"
    assert data["success"] is True
    assert "id" in data


def test_create_sensor_event(client):
    r = client.post(
        "/api/sensor-events",
        json={
            "sensor_id": "hc-sr04-01",
            "sensor_type": "ultrasonic",
            "value": 42.5,
            "filtered_value": 41.2,
            "unit": "cm",
            "metadata": {"alpha": 0.2},
        },
    )
    assert r.status_code == 201
    assert r.json()["sensor_id"] == "hc-sr04-01"


def test_validation_rejects_invalid_payload(client):
    r = client.post("/api/access-events", json={"user_id": "x", "success": True})
    assert r.status_code == 422

    r = client.post("/api/sensor-events", json={"sensor_id": "s1", "value": 10, "unit": "cm"})
    assert r.status_code == 422


def test_list_endpoints_return_empty(client):
    r = client.get("/api/access-events")
    assert r.status_code == 200
    assert isinstance(r.json(), list)

    r = client.get("/api/sensor-events")
    assert r.status_code == 200

    r = client.get("/api/security-events")
    assert r.status_code == 200

    r = client.get("/api/rover/telemetry")
    assert r.status_code == 200
