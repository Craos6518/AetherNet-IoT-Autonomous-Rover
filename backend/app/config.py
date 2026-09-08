# =============================================================================
# backend/app/config.py — Configuración con Pydantic Settings | 6º Semestre UTP
# Autor: Est. Tecnología en Desarrollo Software + Ing. Sistemas (UTP)
# Experiencia: 2 años Python (Pydantic/FastAPI), 2 años JS/React (dotenv, process.env),
#              1 año PostgreSQL (DATABASE_URL), 2 años electrónica (secrets.h)
# Analogía React: este archivo es el `config.ts` que lee `.env` — como
# `import { DATABASE_URL } from './config'` en Node con dotenv.
# Analogía C/Arduino: como `secrets.h` en gateway-esp32 — credenciales fuera de código,
# pero aquí con validación de tipos y defaults (secrets.h usa #ifndef, aquí BaseSettings).
# Analogía PostgreSQL: DATABASE_URL es la misma que en `psql $DATABASE_URL` — asyncpg la usa.
# FOSS: pydantic-settings (MIT) — validación y parsing de env, FOSS RNF-3.1.
# Origen: DEVOPS-08 / DEVOPS-11 — .env.example y secrets.h pattern (no commitear secretos).
# =============================================================================
from functools import (
    lru_cache,  # cachea singleton — como memo() en React, evita re-leer .env cada vez
)

from pydantic_settings import (
    BaseSettings,  # BaseSettings lee env vars y .env auto — como Zod con env
    SettingsConfigDict,
)


class Settings(BaseSettings):
    """
    Settings validados — cada campo es una env var o default.
    Si falta DATABASE_URL y no hay default, Pydantic lanza error claro (fail-fast, como Zod parse).
    """
    # DATABASE_URL — asyncpg para SQLAlchemy async — como "postgresql://user:pass@host:5432/db" en Node pg
    # Default localhost para `pytest` sin Docker (sin .env) — CI corre sin Postgres real (mock en tests/test_health.py)
    # En Docker, docker-compose.yml sobrescribe con postgres:5432 (servicio Postgres)
    database_url: str = "postgresql+asyncpg://aethernet:changeme@localhost:5432/aethernet"
    # MQTT — Mosquitto broker LAN — mismos que en gateway-esp32 secrets.h y mosquitto.conf:4
    # localhost para pytest, mosquitto para Docker (ver .env.example:11 MQTT_BROKER_HOST=mosquitto)
    mqtt_broker_host: str = "localhost"  # default dev — en Docker es "mosquitto" (nombre servicio)
    mqtt_broker_port: int = 1883  # 1883 MQTT, 9001 WS — ver mosquitto.conf:4 listener 1883
    mqtt_username: str = ""  # vacío LAN Sprint 1 — allow_anonymous true (mosquitto.conf:6)
    mqtt_password: str = ""  # futuro: mosquitto_passwd con user/pass (acl.conf)
    # FastAPI host/port — como PORT en Express (process.env.PORT || 3000)
    api_host: str = "0.0.0.0"  # 0.0.0.0 escucha en todas las interfaces (para Docker)
    api_port: int = 8000  # 8000 FastAPI — ver Dockerfile:17 EXPOSE 8000 y gateway BACKEND_PORT 8000

    # Config como estudiante - simple, ignora variables extra de docker-compose (POSTGRES_USER etc)
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",  # ignora POSTGRES_USER, POSTGRES_PASSWORD etc que no son del backend
    )


# --------------------------------------------------------------------------
# Singleton cacheado — como `export const settings = new Settings()` singleton en JS,
# pero con lru_cache evita crear 10 instancias si 10 módulos hacen get_settings().
# --------------------------------------------------------------------------
@lru_cache  # cachea primera llamada — siguientes retornan mismo objeto (como singleton en Node)
def get_settings() -> Settings:
    """Retorna singleton Settings — lee .env solo una vez (como dotenv.config() una vez en Node)."""
    return Settings()  # Pydantic valida tipos aquí — si DATABASE_URL mal, lanza claro
