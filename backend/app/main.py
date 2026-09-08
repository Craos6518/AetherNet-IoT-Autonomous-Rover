"""
==============================================================================
AetherNet FastAPI — Punto de entrada principal | 6º Semestre UTP
Autor: Est. Tecnología en Desarrollo Software + Ing. Sistemas (UTP)
Experiencia: 2 años Python (FastAPI), 2 años HTML/CSS/JS/React (fetch/CORS),
             1 año PostgreSQL (asyncpg), 2 años electrónica/Arduino (UART→HTTP)
Sprint: 1 / DEVOPS-01 + DEVOPS-06 — RNF-1.1, RF-2.1, RNF-2.2, RNF-3.1 FOSS
==============================================================================
QUÉ ES ESTO:
  Backend FastAPI que hace de "cerebro en la LAN" — recibe HTTP POST del
  Gateway ESP32 (ver firmware/gateway-esp32.ino:343 forwardAccessToBackend)
  y persiste en PostgreSQL para que App Kotlin y Node-RED consuman vía
  REST/MQTT. Es el `server.js` en Python pero con tipos y async.

  Si vienes de React/JS (2 años): FastAPI es Express en Python pero con
  validación automática (Pydantic = Zod) y docs auto-generados (/docs Swagger
  como Swagger UI en Node). `Depends(get_db)` es como middleware que inyecta DB.

  Si vienes de C/Arduino (1 año C, 2 años electrónica): este main.py es el
  setup()/loop() del servidor — lifespan crea tablas (como doorInit) y
  cada @app.get/post es como un handler de comando UART pero por HTTP.

Expone:
  GET  /              — info básica (como health de un ESP32, pero JSON)
  GET  /health        — health check con SELECT 1 a Postgres (usado por docker-compose y CI ci.yml:116)
  GET/POST /api/*     — CRUD eventos (HU-01..HU-04, ver routers/events.py:25)

Lifespan: crea tablas vía SQLAlchemy si no existen (complemento a init.sql).
          No bloquea si DB no está (para tests/CI sin Postgres real).
FOSS: FastAPI + SQLAlchemy + asyncpg — 100% open source, sin AWS/GCP (RNF-3.1).
"""

from contextlib import (
    asynccontextmanager,  # para lifespan async — como try/finally en JS pero para startup/shutdown
)

from fastapi import (  # FastAPI = Express, Depends = DI como useContext en React
    Depends,
    FastAPI,
)
from fastapi.middleware.cors import (
    CORSMiddleware,  # CORS — como cors() en Express, permite App Android en LAN
)
from sqlalchemy import text  # para SELECT 1 en health check (raw SQL)
from sqlalchemy.ext.asyncio import (
    AsyncSession,  # sesión async — como pool.query en pg Node pero async/await
)

# Importar modelos para que Base.metadata conozca las tablas — side-effect registra en metadata
# Si no importas, create_all() no crea nada porque Base no sabe qué tablas existen (como no importar modelos en Sequelize)
import app.models  # type: ignore[import]  # noqa — side-effect intencional, mypy lo ignora
from app.config import get_settings  # settings desde .env (como process.env en Node)
from app.database import (  # Base metadata, engine asyncpg, get_db Depends
    Base,
    engine,
    get_db,
)
from app.routers.events import (
    router as events_router,  # router /api/* — como app.use('/api', eventsRouter) en Express
)
from app.schemas import HealthResponse  # schema Pydantic para /health — como Zod schema

settings = get_settings()  # singleton cacheado con @lru_cache — lee .env una vez (como dotenv.config())


# --------------------------------------------------------------------------
# Lifespan — startup/shutdown del servidor (como useEffect con cleanup en React)
# --------------------------------------------------------------------------
@asynccontextmanager
async def lifespan(app: FastAPI):
    """Crea tablas si no existen al arrancar (no rompe si init.sql ya las creó)."""
    # Intenta crear tablas vía SQLAlchemy metadata — idempotente (IF NOT EXISTS interno)
    # Complementa a init.sql (que usa uuid-ossp). Si ambos corren, no duplica.
    try:
        async with engine.begin() as conn:  # transacción DDL
            await conn.run_sync(Base.metadata.create_all)  # crea access_events, sensor_events, etc. si faltan
    except Exception:
        # Si DB no está disponible en build/test (ej. CI sin Postgres), no bloquear arranque
        # El endpoint /health reportará "degraded" en vez de crashear el server (graceful degradation)
        pass
    yield  # aquí corre el servidor (como return en useEffect)
    # Cleanup al apagar — cierra pool asyncpg (como pool.end() en Node)
    try:
        await engine.dispose()  # cierra conexiones — evita warnings en tests
    except Exception:
        pass


# --------------------------------------------------------------------------
# App FastAPI — el "servidor" (como const app = express() en Node)
# --------------------------------------------------------------------------
app = FastAPI(
    title="AetherNet IoT API",  # título en /docs Swagger — como name en package.json
    version="1.0.0-sprint1",  # versión Sprint 1 — se ve en / y /health
    description="Backend local FOSS para AetherNet IoT & Autonomous Rover — Sprint 1 (FOSS, sin cloud)",
    lifespan=lifespan,  # startup/shutdown hook — como app.listen con callback
)

# CORS abierto para LAN — App Android (Kotlin Retrofit), Node-RED, y cualquier host en 192.168.1.x
# RNF prd.md:59 — todo en misma subred, sin origen fijo. En prod restringirías a ["http://192.168.1.14:3000"]
# Como cors({origin: "*"}) en Express — permite fetch desde cualquier origen LAN
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # LAN abierta Sprint 1 — FOSS, sin auth compleja aún
    allow_credentials=True,  # permite cookies/auth si algún día usamos
    allow_methods=["*"],  # GET, POST, etc. — todos permitidos
    allow_headers=["*"],  # Content-Type, Authorization, etc.
)

# Routers — monta /api/* (como app.use('/api', router) en Express)
# events_router trae POST/GET access-events, sensor-events, security-events, rover/telemetry
app.include_router(events_router)  # prefijo /api ya en router (routers/events.py:25)


# --------------------------------------------------------------------------
# GET / — info básica (como GET / en Express que devuelve {name, version})
# --------------------------------------------------------------------------
@app.get("/", tags=["system"])  # tags agrupa en /docs Swagger — como group en Swagger UI
async def root():
    """Endpoint raíz — para probar que el servidor está vivo sin DB (como ping del ESP32)."""
    return {
        "name": "AetherNet IoT API",
        "version": "1.0.0-sprint1",
        "docs": "/docs",  # Swagger UI auto-generado — como /api-docs en Node con swagger-jsdoc
        "health": "/health",  # health check con DB — ver abajo
    }


# --------------------------------------------------------------------------
# GET /health — health check con DB (usado por docker-compose healthchecks y CI)
# --------------------------------------------------------------------------
@app.get("/health", response_model=HealthResponse, tags=["system"])
async def health_check(db: AsyncSession = Depends(get_db)):
    """
    Health endpoint requerido por ci.yml:116 y docker-compose healthchecks.
    Verifica conectividad a Postgres con SELECT 1 (como ping a DB).
    Si DB ok → status ok, si no → degraded (no 500, para que CI no falle por DB no lista).
    """
    db_status = "ok"
    try:
        await db.execute(text("SELECT 1"))  # ping DB — como pool.query('SELECT 1') en Node pg
    except Exception as exc:  # si DB cae o no hay conexión (ej. tests sin Postgres)
        db_status = f"error: {type(exc).__name__}"  # error con nombre excepción (como err.name en JS)

    # HealthResponse Pydantic — como Zod schema, valida y serializa JSON
    # status ok si DB ok, degraded si DB error — no 500 (graceful, como degraded en k8s)
    return HealthResponse(status="ok" if db_status == "ok" else "degraded", database=db_status)
