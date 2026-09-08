"""
Tests de estudiante Movil - DEVOPS-06/07
Verifican que los 4 recursos POST/GET funcionen como los usa la App
Hecho con lo aprendido en videos de FastAPI + testing (16h)
"""

import uuid
from datetime import datetime, timezone
from unittest.mock import AsyncMock, MagicMock

import pytest
from fastapi.testclient import TestClient

from app.database import get_db
from app.main import app


# Fixture simple - lo copie de test_health.py y lo adapte
# No sabia que podia usar conftest.py, asi lo dejo aqui por ahora
@pytest.fixture
def mock_db_session():
    session = MagicMock()
    session.execute = AsyncMock(return_value=None)
    session.commit = AsyncMock()
    session.refresh = AsyncMock()

    def fake_add(obj):
        if not getattr(obj, "id", None):
            obj.id = uuid.uuid4()
        if not getattr(obj, "timestamp", None):
            obj.timestamp = datetime.now(timezone.utc)
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
    # Para los GET, devuelvo lista vacia
    mock_db_session.execute = AsyncMock(
        return_value=MagicMock(scalars=lambda: MagicMock(all=lambda: []))
    )
    with TestClient(app) as c:
        yield c
    app.dependency_overrides.clear()


# POST tests - lo que hace el Gateway y la App

def test_create_access_event_ok(client):
    # Esto es lo que manda el Gateway cuando alguien pone el PIN 1234
    r = client.post(
        "/api/access-events",
        json={"user_id": "keypad_user", "pin_hash": "7c78c98f", "success": True, "source": "keypad"},
    )
    assert r.status_code == 201
    data = r.json()
    assert data["user_id"] == "keypad_user"
    assert data["success"] is True
    assert "id" in data


def test_create_sensor_event_with_ema(client):
    # HC-SR04 con filtro EMA alpha 0.2 - lo vi en stats/ema_filter.py
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
    assert r.json()["value"] == 42.5


def test_create_security_event_critical(client):
    # Evento de intrusión laser - lo que dispara Telegram
    r = client.post(
        "/api/security-events",
        json={"event_type": "intrusion", "severity": "high", "description": "laser interrumpido"},
    )
    assert r.status_code == 201
    assert r.json()["event_type"] == "intrusion"
    assert r.json()["severity"] == "high"


def test_create_security_event_minimal(client):
    # Solo con event_type debe funcionar, severity default medium
    r = client.post("/api/security-events", json={"event_type": "intrusion"})
    assert r.status_code == 201
    assert r.json()["severity"] == "medium"


def test_create_rover_telemetry_full(client):
    # Telemetria del rover - lo que manda el UNO via RF
    sid = str(uuid.uuid4())
    r = client.post(
        "/api/rover/telemetry",
        json={
            "session_id": sid,
            "left_motor_pwm": 120,
            "right_motor_pwm": 120,
            "ultrasonic_distance_cm": 35.0,
            "ir_left": False,
            "ir_center": False,
            "ir_right": False,
            "rf_rssi": -65,
        },
    )
    assert r.status_code == 201
    assert r.json()["session_id"] == sid
    assert r.json()["left_motor_pwm"] == 120


# Validación 422 - Pydantic debe rechazar datos malos


def test_validation_422_access_missing_pin(client):
    # Falta pin_hash - debe dar 422
    r = client.post("/api/access-events", json={"user_id": "x", "success": True})
    assert r.status_code == 422


def test_validation_422_rover_pwm_out_of_range(client):
    # PWM fuera de rango - debe dar 422 (max 255)
    r = client.post(
        "/api/rover/telemetry",
        json={"session_id": str(uuid.uuid4()), "left_motor_pwm": 999, "right_motor_pwm": 0},
    )
    assert r.status_code == 422


def test_sensor_rejects_missing_unit(client):
    # Falta unit - debe dar 422
    r = client.post(
        "/api/sensor-events", json={"sensor_id": "s1", "sensor_type": "ultrasonic", "value": 10}
    )
    assert r.status_code == 422


# GET con paginación y filtros


def test_list_access_pagination(client):
    r = client.get("/api/access-events?limit=5&offset=10")
    assert r.status_code == 200
    assert isinstance(r.json(), list)


def test_list_sensor_filter_by_type(client):
    r = client.get("/api/sensor-events?sensor_type=ultrasonic&limit=10")
    assert r.status_code == 200


def test_list_security_filter_by_type(client):
    r = client.get("/api/security-events?event_type=intrusion")
    assert r.status_code == 200


def test_list_rover_filter_by_session(client):
    sid = uuid.uuid4()
    r = client.get(f"/api/rover/telemetry?session_id={sid}")
    assert r.status_code == 200


def test_list_limit_exceeds_200_gives_422(client):
    # Limit max es 200 - si pido 999 debe dar 422
    r = client.get("/api/access-events?limit=999")
    assert r.status_code == 422


def test_openapi_has_8_ops(client):
    # Verifica que existan los 8 endpoints (4 POST + 4 GET)
    r = client.get("/openapi.json")
    assert r.status_code == 200
    paths = r.json()["paths"]
    assert "/api/access-events" in paths
    assert "/api/sensor-events" in paths
    assert "/api/security-events" in paths
    assert "/api/rover/telemetry" in paths
    # Cada uno debe tener get y post
    for p in ["/api/access-events", "/api/sensor-events", "/api/security-events", "/api/rover/telemetry"]:
        assert "get" in paths[p]
        assert "post" in paths[p]
