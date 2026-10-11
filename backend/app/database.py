# =============================================================================
# backend/app/database.py — Conexión PostgreSQL Async | 6º Semestre UTP
# Autor: Andres Felipe Martinez Henao
# Experiencia: 1 año PostgreSQL (psql, pools, asyncpg), 2 años Python (SQLAlchemy 2.0),
#              2 años JS/React (async/await, connection pools en Node pg)
# Analogía React/Node: este archivo es el `db.ts` con `new Pool({connectionString})`
# y `export const pool = new Pool(...)` — aquí es create_async_engine + async_sessionmaker.
# Analogía C/Arduino: como `Serial2.begin(BAUD)` — inicializa hardware (DB) una vez,
# luego `get_db()` es como `Serial.available()` pero para sesiones DB.
# FOSS: SQLAlchemy 2.0 + asyncpg (MIT) + PostgreSQL 15 — RNF-3.1, sin RDS.
# Origen: DEVOPS-01 docker-compose.yml — servicio postgres + backend.
# =============================================================================
from collections.abc import (
    AsyncGenerator,  # para tipar get_db yield — como Generator en TS
)

from sqlalchemy.ext.asyncio import (  # async engine — como Pool en pg Node pero async
    AsyncSession,
    async_sessionmaker,
    create_async_engine,
)
from sqlalchemy.orm import (
    DeclarativeBase,  # base para models — como BaseEntity en TypeORM
)

from app.config import (
    get_settings,  # lee DATABASE_URL desde .env (como process.env.DATABASE_URL en Node)
)

settings = get_settings()  # singleton cacheado — lee .env una vez (ver config.py:21)

# --------------------------------------------------------------------------
# Engine async — pool de conexiones a Postgres (como pool en Node pg)
# --------------------------------------------------------------------------
engine = create_async_engine(
    settings.database_url,  # "postgresql+asyncpg://aethernet:changeme@postgres:5432/aethernet" — mismo que psql $DATABASE_URL
    echo=False,  # no log SQL cada query — como pool con log: false en Node (en dev puedes poner True para debug)
    pool_pre_ping=True,  # ping antes de usar conexión — detecta conexiones muertas (como pool.query('SELECT 1') keepalive)
    pool_size=10,  # 10 conexiones en pool — como pool_size en Node (suficiente para 100 req/s LAN)
    max_overflow=20,  # hasta 20 extra si pool lleno — burst para picos (como max en pg Pool)
)
# Total max 30 conexiones — Postgres default max_connections 100, deja margen para psql y stats/ (psycopg2)

# --------------------------------------------------------------------------
# Session factory — crea sesiones por request (como pool.connect() en Node)
# --------------------------------------------------------------------------
async_session_maker = async_sessionmaker(
    engine,
    class_=AsyncSession,  # sesión async — como Client en pg Node pero con ORM
    expire_on_commit=False,  # no expira objetos tras commit — como keepAlive en pool (evita re-query)
)


# --------------------------------------------------------------------------
# Base — clase base para todos los models (como BaseEntity en TypeORM)
# --------------------------------------------------------------------------
class Base(DeclarativeBase):
    """Base para AccessEvent, SensorEvent, etc. — registra metadata para create_all() en main.py:36."""
    # vacío — solo marca herencia (como `class Base extends DeclarativeBase` en TS)


# --------------------------------------------------------------------------
# Depends get_db — inyección de sesión por request (como middleware en Express)
# --------------------------------------------------------------------------
async def get_db() -> AsyncGenerator[AsyncSession, None]:  # yield sesión — como Generator en TS
    """
    Depends para FastAPI — cada request obtiene su propia sesión (como req.db en Express).
    Uso: @app.get("/api") async def handler(db: AsyncSession = Depends(get_db))
    Es como `const client = await pool.connect()` en Node, pero con yield y auto-close.
    """
    async with async_session_maker() as session:  # abre sesión — como pool.connect()
        try:
            yield session  # entrega sesión al handler — como `req.db = session` en middleware
        finally:
            await session.close()  # siempre cierra — como `client.release()` en finally (evita leaks)


# --------------------------------------------------------------------------
# init_db — crea tablas si no existen (usado en lifespan main.py:36, alternativa a init.sql)
# --------------------------------------------------------------------------
async def init_db() -> None:
    """Crea tablas vía metadata — alternativa a init.sql para tests sin Docker (como migrate en Prisma)."""
    async with engine.begin() as conn:  # transacción DDL
        await conn.run_sync(Base.metadata.create_all)  # CREATE TABLE IF NOT EXISTS para cada modelo (idempotente)
