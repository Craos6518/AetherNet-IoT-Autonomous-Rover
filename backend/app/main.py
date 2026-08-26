"""
AetherNet FastAPI — Punto de entrada principal.
Sprint 1 / DEVOPS-01 + DEVOPS-06 — RNF-1.1, RF-2.1, RNF-2.2.

Expone:
  GET  /              — info básica
  GET  /health        — health check (usado por docker-compose y CI en ci.yml:116)
  GET  /api/*         — CRUD mínimo de eventos (HU-01..HU-04)

Lifespan: crea tablas vía SQLAlchemy si no existen (complemento a init.sql).
No introduce dependencia de nube propietaria (RNF-3.1 FOSS).
"""

from contextlib import asynccontextmanager

from fastapi import Depends, FastAPI
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession

# Importar modelos para que Base.metadata conozca las tablas
import app.models  # noqa: F401  # mypy: ignore import side-effect
from app.config import get_settings
from app.database import Base, engine, get_db
from app.routers.events import router as events_router
from app.schemas import HealthResponse

settings = get_settings()


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Crear tablas si no existen (no rompe si init.sql ya las creó)
    try:
        async with engine.begin() as conn:
            await conn.run_sync(Base.metadata.create_all)
    except Exception:
        # Si DB no está disponible en build/test, no bloquear arranque
        pass
    yield
    # Cleanup opcional
    try:
        await engine.dispose()
    except Exception:
        pass


app = FastAPI(
    title="AetherNet IoT API",
    version="1.0.0-sprint1",
    description="Backend local FOSS para AetherNet IoT & Autonomous Rover — Sprint 1",
    lifespan=lifespan,
)

# CORS abierto para LAN (App Android, Node-RED en misma subred) — RNF prd.md:59
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Routers
app.include_router(events_router)


@app.get("/", tags=["system"])
async def root():
    return {
        "name": "AetherNet IoT API",
        "version": "1.0.0-sprint1",
        "docs": "/docs",
        "health": "/health",
    }


@app.get("/health", response_model=HealthResponse, tags=["system"])
async def health_check(db: AsyncSession = Depends(get_db)):
    """
    Health endpoint requerido por ci.yml:116 y docker-compose healthchecks.
    Verifica conectividad a Postgres con SELECT 1.
    """
    db_status = "ok"
    try:
        await db.execute(text("SELECT 1"))
    except Exception as exc:
        db_status = f"error: {type(exc).__name__}"

    return HealthResponse(status="ok" if db_status == "ok" else "degraded", database=db_status)
