# Backend — AetherNet FastAPI + PostgreSQL + Mosquitto

> **Autor:** Estudiante 6º semestre Tecnología en Desarrollo de Software + 6º semestre Ingeniería de Sistemas y Computación (UTP)  
> **Experiencia:** 2 años electrónica y Arduino | 1 año Programación C | 2 años Python | 2 años HTML/CSS/JS/React | 1 año PostgreSQL  
> **Stack:** Python 3.12, FastAPI 0.111, SQLAlchemy 2.0 async, PostgreSQL 16, Mosquitto 2.0, Docker Compose — 100% FOSS (RNF-3.1)  
> **Sprint:** 1 ✅ (DEVOPS-01/03/06) — CRUD mínimo `HU-01..HU-04`, `RNF-2.2`, `health` CI | Sprint 2+ ⏳ (auth, WS, t-Student)

---

## 0. Cómo leer este backend si vienes de web/electrónica/C

Si vienes de **React/JS (2 años)**, piensa en este backend como un `Express` en Python: `FastAPI` es `Express`, `Pydantic` es `Zod`, `SQLAlchemy` es `Prisma`, `PostgreSQL` es la DB (como `pg`), y `Mosquitto` es el `WebSocket server` (MQTT). `Depends(get_db)` es un middleware que inyecta `req.db` como en Express.

Si vienes de **C/Arduino (1 año C, 2 años electrónica)**, este `backend/` es el `main.py` del servidor — `lifespan` crea tablas como `doorInit()` crea el servo, y cada `@router.post("/api/access-events")` es como `processPinAttempt` pero por HTTP: valida JSON, hace `INSERT`, retorna 201. El `Gateway ESP32` le hace `HTTPClient POST` igual que harías `fetch()` en JS, pero desde C.

Si vienes de **PostgreSQL (1 año)**, aquí está todo lo que usas en `psql`: `init.sql` crea tablas con `TIMESTAMPTZ` y `JSONB`, `models.py` mapea `Column(Numeric, Boolean, UUID, Index)` a esas tablas, y `events.py` hace `SELECT ... ORDER BY timestamp DESC LIMIT 50` con ORM. Los índices `idx_*` aceleran `GET /api/*?limit=50` como haría un `EXPLAIN ANALYZE`.

Este README cubre **todo** lo que hay en `backend/` archivo por archivo con `ruta:línea`.

---

## 1. Mapa de carpetas

```
backend/
├── app/
│   ├── __init__.py          # 0 líneas — marca paquete Python (como index.ts vacío)
│   ├── main.py              # 132 líneas — FastAPI app, lifespan, CORS, / y /health
│   ├── config.py            # 51 líneas — BaseSettings lee .env (como process.env con Zod)
│   ├── database.py          # 72 líneas — engine asyncpg pool 10+20, Base, get_db Depends
│   ├── models.py            # 159 líneas — 4 modelos ORM (access/sensor/security/rover)
│   ├── schemas.py           # 153 líneas — Pydantic Zod-like para validar POST y serializar GET
│   └── routers/
│       ├── __init__.py      # package routers (como routes/index.ts)
│       └── events.py        # 188 líneas — 8 endpoints CRUD /api/* (HU-01..HU-04, RNF-2.2)
├── tests/
│   └── test_health.py       # 162 líneas — 7 tests pytest con TestClient + mock DB (sin Postgres real)
├── mosquitto/
│   ├── config/
│   │   ├── mosquitto.conf   # 33 líneas — broker 1883/9001, persistence, limits, log
│   │   └── acl.conf         # ACL aethernet/# + $SYS/# (RNF-3.1 LAN)
│   ├── data/mosquitto.db    # persistencia Mosquitto (volume)
│   └── log/mosquitto.log    # logs Mosquitto (volume)
├── Dockerfile               # 50 líneas — python:3.12-slim + gcc/libpq + pip + uvicorn 8000
├── init.sql                 # 97 líneas — DDL 4 tablas + 7 índices (uuid-ossp, TIMESTAMPTZ, JSONB)
├── .env.example             # 19 líneas — plantilla DATABASE_URL + MQTT (copiar a .env gitignored)
├── .env                     # gitignored — secretos reales LAN (como secrets.h en firmware)
├── requirements.txt         # 12 deps pinneadas (FastAPI 0.111, asyncpg 0.29, Pydantic 2.7...)
└── pyproject.toml           # ruff + mypy + pytest config (como eslint+tsc+jest.config)
```

Root del repo aporta `docker-compose.yml:1` (3 servicios `postgres`, `mosquitto`, `fastapi`) y CI `.github/workflows/ci.yml:20` (`backend-test`, `docker-build`).

---

## 2. Visión general — dónde vive el backend

```
┌─────────────────┐  UART 38400   ┌──────────────┐  HTTP POST 8000   ┌──────────────────────────┐
│ MEGA 2560       │──────────────►│ Gateway ESP32│─────────────────►│ Backend FastAPI (Docker) │
│ keypad 4x4      │  ACCESS:JSON  │ WiFi         │  /api/access-events│  POST /api/access-events │
│ servo MG90S     │               │ HTTPClient   │  201 + PostgreSQL │  → access_events (UUID)  │
└─────────────────┘               └──────┬───────┘                   └────────────┬─────────────┘
                                         │ MQTT 1883/9001                       │ SQLAlchemy async
                                         ▼                                      ▼
                                  ┌──────────────┐                        ┌─────────────┐
                                  │ Mosquitto    │◄──────────────────────►│ PostgreSQL  │
                                  │ aethernet/#  │   MQTT pub/sub         │ 4 tablas    │
                                  │ ACL aethernet│                        │ + índices   │
                                  └──────┬───────┘                        └──────┬──────┘
                                         │                                     │
                                  ┌──────▼───────┐                      ┌──────▼──────┐
                                  │ App Kotlin   │                      │ stats/      │
                                  │ Node-RED     │                      │ t-Student   │
                                  └──────────────┘                      └─────────────┘
```

**Rol del backend (arquitectura.md:31):** no es el cerebro que mueve motores (eso es el Rover), ni el que decide acceso (eso es el MEGA Edge). Es **coordinación + histórico**: recibe eventos por HTTP (más rápido que MQTT bridge para HU-01, decisión 2026-08-26), persiste en PostgreSQL, y deja que App/Node-RED/stats consuman por REST o MQTT.

Por qué **HTTP POST** para HU-01 y **MQTT** para lo demás (arquitectura.md:63): el cerrojo debe quedar registrado en BD aunque Mosquitto esté caído; `HTTP 201` lo garantiza, MQTT requiere que backend esté suscrito. Es como hacer `fetch(..., {method: 'POST'})` con retry vs. `socket.emit` que se pierde si no hay listener.

---

## 3. `init.sql:1` — Esquema PostgreSQL (97 líneas, 4 tablas + 7 índices)

> **Analogía Prisma:** este `init.sql` es `prisma/migrations/001_init.sql` — DDL que crea tablas cuando `docker-compose up` levanta `postgres:16-alpine` (montado en `/docker-entrypoint-initdb.d/init.sql:ro` en `docker-compose.yml:11`).

**Extensión** `init.sql:4` `CREATE EXTENSION "uuid-ossp"` habilita `uuid_generate_v4()` para `DEFAULT` en cada `id`. Sin esto, el `DEFAULT uuid_generate_v4()` fallaría.

**4 tablas (cada una = un modelo en `models.py` + un par POST/GET en `routers/events.py`):**

| Tabla `init.sql` | Modelo `models.py` | Endpoint `events.py` | HU/RF | Columnas clave |
|---|---|---|---|---|
| `access_events:6` | `AccessEvent:20` | `POST/GET /api/access-events:31` | HU-01 RF-2.2 | `user_id VARCHAR(64) NOT NULL INDEX`, `pin_hash VARCHAR(128) NOT NULL`, `success BOOLEAN`, `timestamp TIMESTAMPTZ DEFAULT NOW() INDEX DESC`, `source VARCHAR(32) DEFAULT 'keypad'` |
| `sensor_events:15` | `SensorEvent:36` | `POST/GET /api/sensor-events:58` | RNF-2.1 HU-03 | `sensor_id VARCHAR(64)`, `sensor_type VARCHAR(32) INDEX` (laser/ultrasonic/ir/sound/rf), `value NUMERIC(10,4) NOT NULL`, `filtered_value NUMERIC(10,4)` (EMA α0.2 `stats/ema_filter.py:32`), `unit VARCHAR(16)`, `metadata JSONB DEFAULT '{}'`, `timestamp` |
| `security_events:26` | `SecurityEvent:55` | `POST/GET /api/security-events:92` | HU-02 RF-2.3 RF-4.1 | `event_type VARCHAR(32) INDEX` (intrusion/access_denied/rf_failstop), `severity VARCHAR(16) DEFAULT 'medium'` (low/medium/high/critical), `description TEXT`, `acknowledged BOOLEAN DEFAULT FALSE`, `acknowledged_at/by` |
| `rover_telemetry:37` | `RoverTelemetry:73` | `POST/GET /api/rover/telemetry:123` | RF-3.1 RF-3.3 HU-04 | `session_id UUID INDEX`, `left/right_motor_pwm SMALLINT NOT NULL` (-255..255 `int16_t` C), `ultrasonic_distance_cm NUMERIC(6,2)`, `ir_left/center/right BOOLEAN`, `rf_rssi SMALLINT` (-120..0 dBm placeholder -70 `rover-uno.ino:243`) |

**Tipos elegidos con criterio (1 año PostgreSQL):**

- `TIMESTAMPTZ` no `TIMESTAMP`: guarda zona UTC. Crucial para ordenar `ORDER BY timestamp DESC` sin ambigüedad y para t-Student por ventana temporal.
- `NUMERIC(10,4)` no `FLOAT`: decimales exactos para `cm`/`db` y KPI >85% (evita error binario de `FLOAT`). `Numeric(10,4)` en `models.py:42` → `Max 999999.9999`, suficiente para 0-500 cm.
- `NUMERIC(6,2)` para `ultrasonic_distance_cm`: 6 dígitos, 2 decimales (0.00-9999.99), optimiza storage vs `10,4`.
- `SMALLINT` para `PWM`/`RSSI`: -32768..32767, ahorra 2 bytes vs `INTEGER` por fila (como `int16_t` en C `rover-uno.ino:85`).
- `JSONB` para `metadata`: binario indexable, no `JSON` texto. Guarda `{"alpha":0.2, "threshold":30, "rssi":-70}` flexible sin alterar esquema (como `Json?` en Prisma).
- `UUID` PK `DEFAULT uuid_generate_v4()`: aleatorio v4, mejor para distribuido que `SERIAL` secuencial (como `crypto.randomUUID()` en JS vs autoincrement).

**7 índices** `init.sql:50` (ver `models.py:30` `Index`):

```sql
CREATE INDEX idx_access_events_timestamp ON access_events(timestamp DESC);   -- GET /api/access-events ORDER BY timestamp DESC (events.py:51)
CREATE INDEX idx_access_events_user ON access_events(user_id);              -- dashboard App filtro por usuario
CREATE INDEX idx_sensor_events_timestamp ON sensor_events(timestamp DESC);
CREATE INDEX idx_sensor_events_type ON sensor_events(sensor_type);          -- WHERE sensor_type='ultrasonic' (EST-06)
CREATE INDEX idx_security_events_timestamp ON security_events(timestamp DESC);
CREATE INDEX idx_security_events_type ON security_events(event_type);
CREATE INDEX idx_rover_telemetry_session ON rover_telemetry(session_id, timestamp); -- compuesto: filtro + orden por sesión (events.py:148)
```

Sin índices, `SELECT ... ORDER BY timestamp DESC LIMIT 50` escanea toda la tabla `O(n)`; con índice `DESC` es `O(log n)` (como `@@index([timestamp(sort: Desc)])` en Prisma).

---

## 4. `app/models.py:1` — ORM SQLAlchemy 2.0 (159 líneas)

> **Analogía TypeORM/Prisma:** cada `class AccessEvent(Base)` es un `model AccessEvent` en `schema.prisma` — `Column(String(64))` equivale a `String @db.VarChar(64)`, `Index(...)` a `@@index([timestamp(sort: Desc)])`.

**Patrones clave:**

- `Base(DeclarativeBase):23` vacío — clase base que registra `metadata` para `Base.metadata.create_all()` en `main.py:36` (lifespan). Si no importas `app.models` en `main.py:22`, `create_all` no crea nada.
- `id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4:23)` genera UUID en Python (`uuid.uuid4()` como `crypto.randomUUID()` en JS), complementa `init.sql:7` `uuid_generate_v4()` en DB. Ambos son idempotentes.
- `server_default=func.now():27` mapea a `DEFAULT NOW()` en `init.sql:11` — `NOW()` en Postgres, no en Python (así `timestamp` es consistente aunque App mande reloj desfasado).
- `event_metadata = Column("metadata", JSON, default=dict:47)` resuelve colisión: atributo Python `event_metadata` pero columna SQL `"metadata"` (palabra reservada en `DeclarativeBase`). En `schemas.py:67` `validation_alias="event_metadata"` hace el puente.

**Comparativa tipo a tipo (PostgreSQL 1 año + C 1 año):**

| Columna `models.py` | Tipo PG `init.sql` | Tipo C firmware | Tipo React/TS |
|---|---|---|---|
| `String(64)` `user_id:24` | `VARCHAR(64)` | `char[64]` | `string` `z.string().max(64)` |
| `Boolean` `success:26` | `BOOLEAN` | `bool` | `boolean` |
| `SmallInteger` `left_motor_pwm:78` | `SMALLINT` | `int16_t` `RoverTelemetry:78` | `number` `Field(ge=-255, le=255)` |
| `Numeric(10,4)` `value:42` | `NUMERIC(10,4)` | `float` `ultrasonicEma` | `number` |
| `JSON` `event_metadata:47` | `JSONB` | — | `Record<string, any>` |

---

## 5. `app/schemas.py:1` — Validación Pydantic (153 líneas, Zod de Python)

> **Analogía Zod:** `class AccessEventCreate(BaseModel)` con `Field(..., max_length=64)` es `z.object({ user_id: z.string().max(64) })` — valida `req.body` antes de `INSERT` y genera docs Swagger en `/docs`.

**8 schemas (Create/Out por tabla):**

| Schema `schemas.py` | Uso | Validación clave |
|---|---|---|
| `HealthResponse:17` | `GET /health` `main.py:78` | `status: "ok" | "degraded"`, `database: "ok" | "error: ..."`, `version: "1.0.0-sprint1"` |
| `AccessEventCreate:26` | `POST /api/access-events` `events.py:31` | `user_id max 64` `pin_hash max 128` `success bool` `source default "keypad" max 32` — `...` = requerido (como `z.string().min(1)`) |
| `AccessEventOut:33` | `GET /api/access-events` | `ConfigDict(from_attributes=True)` — permite `return AccessEvent` ORM directo (como `plainToInstance` en `class-transformer`) |
| `SensorEventCreate:47` | `POST /api/sensor-events` | `sensor_type` `value float` `filtered_value float|None` `unit` `metadata dict default_factory=dict` (evita mutable default como `() => ({})` en JS) |
| `SensorEventOut:56` | `GET /api/sensor-events` | `populate_by_name=True` + `validation_alias="event_metadata"` ↔ `serialization_alias="metadata"` — mapea `models.py:47` `event_metadata` ↔ API `metadata` (como `@Column({name: "metadata"})` en TypeORM) |
| `SecurityEventCreate:73` | `POST /api/security-events` | `event_type` `severity default "medium"` `description optional` |
| `SecurityEventOut:79` | `GET /api/security-events` | `acknowledged bool` `acknowledged_at/by optional` |
| `RoverTelemetryCreate:95` | `POST /api/rover/telemetry` | `session_id UUID` `left/right_motor_pwm ge=-255 le=255` (igual que `constrain(-255,255)` en `rover-uno.ino:271`/C `int16_t`) `ultrasonic_distance_cm ge=0 le=500` `rf_rssi ge=-120 le=0` (como `Field` constraints en Zod) |

**Coherencia `Field` ↔ `Column` ↔ `init.sql`:**

- `Field(max_length=64)` en `schemas.py:27` ↔ `String(64)` en `models.py:24` ↔ `VARCHAR(64)` en `init.sql:8`. Si cambias uno, cambia los tres o tendrás `422` (Pydantic) vs `ERROR value too long` (Postgres).

---

## 6. `app/routers/events.py:1` — 8 Endpoints CRUD (188 líneas)

> **Analogía Express:** `APIRouter(prefix="/api")` es `Router({prefix: '/api'})`, `@router.post("/access-events")` es `router.post('/access-events', handler)`, `Depends(get_db)` es `req.db` middleware.

**8 handlers (POST 201 + GET lista):**

| Método `events.py` | Ruta | Tabla | HU | Qué hace (como SQL) |
|---|---|---|---|---|
| `POST:31` `create_access_event` | `/api/access-events` | `access_events` | HU-01 | `INSERT INTO access_events (user_id, pin_hash, success, source) VALUES (...) RETURNING *` — llamado por `gateway.ino:378` `HTTPClient POST` |
| `GET:45` `list_access_events` | `/api/access-events?limit=50&offset=0` | `access_events` | HU-01 | `SELECT * FROM access_events ORDER BY timestamp DESC LIMIT 50 OFFSET 0` (`desc()` = `DESC`, `Query(limit ge=1 le=200)` paginación como `React Table` `limit`/`offset` para infinite scroll) |
| `POST:58` `create_sensor_event` | `/api/sensor-events` | `sensor_events` | HU-03 | `INSERT` con `value` raw + `filtered_value` EMA `alpha 0.2` (`stats/ema_filter.py:32` ↔ `value`/`filtered_value` aquí) + `event_metadata=payload.metadata` |
| `GET:74` `list_sensor_events` | `/api/sensor-events?sensor_type=ultrasonic` | `sensor_events` | HU-03 | `SELECT ... WHERE sensor_type='ultrasonic' ORDER BY timestamp DESC` (filtro opcional para `EST-06` descriptivo por tipo) |
| `POST:92` `create_security_event` | `/api/security-events` | `security_events` | HU-02 | `INSERT` `event_type` `severity` `description` (láser `intrusion` → `Node-RED` → `Telegram` `RF-4.1`) |
| `GET:105` `list_security_events` | `/api/security-events?event_type=intrusion` | `security_events` | HU-02 | `SELECT ... WHERE event_type='intrusion'` |
| `POST:123` `create_rover_telemetry` | `/api/rover/telemetry` | `rover_telemetry` | RF-3.1 | `INSERT` por `session_id` (agrupa run joystick como `sessionId` en analytics) `left/right_motor_pwm` + `ultrasonic` + `ir_*` + `rf_rssi` |
| `GET:141` `list_rover_telemetry` | `/api/rover/telemetry?session_id=uuid` | `rover_telemetry` | RF-3.1 | `SELECT ... WHERE session_id=uuid ORDER BY timestamp DESC` (índice compuesto `session_id,timestamp` en `models.py:88` optimiza) |

**Patrón común POST (ej. `events.py:31`):**

```python
event = AccessEvent(user_id=payload.user_id, ...)  # new AccessEvent({...}) en Prisma
db.add(event)                                      # stage INSERT (prisma create sin execute)
await db.commit()                                  # COMMIT (prisma $transaction)
await db.refresh(event)                            # RETURNING * (recarga id/timestamp generados)
return event                                       # FastAPI serializa a AccessEventOut (from_attributes)
```

**Patrón común GET (ej. `events.py:51`):**

```python
result = await db.execute(select(AccessEvent).order_by(desc(...)).limit(limit).offset(offset))
return result.scalars().all()  # list ORM → FastAPI serializa a list[Out] (como Prisma findMany)
```

---

## 7. `app/config.py:1` (51 líneas) + `app/database.py:1` (72 líneas) — Config y Pool

### `config.py:1` — `pydantic-settings` como `dotenv` + `Zod`

```python
class Settings(BaseSettings):
    database_url: str = "postgresql+asyncpg://aethernet:changeme@localhost:5432/aethernet"  # para pytest sin Docker
    mqtt_broker_host: str = "localhost"  # en Docker es "mosquitto" (nombre servicio, ver .env.example:11)
    api_host: str = "0.0.0.0"  # 0.0.0.0 para Docker (mapeo host:8000)
```

- `BaseSettings` lee `env vars` + `.env` automáticamente (como `dotenv.config()` + `Zod` validación). Si `DATABASE_URL` no existe y no hay default, lanza error claro (fail-fast).
- `@lru_cache get_settings():20` singleton cacheado — primera llamada lee `.env` (como `dotenv.config()` una vez), siguientes retornan mismo objeto (como `memo()` en React, evita 10 lecturas si 10 módulos importan).

**Coherencia env:** `DATABASE_URL` en `.env.example:9` ↔ `database_url` en `config.py:7` ↔ `DATABASE_URL` en `docker-compose.yml:42`. En Docker, `host` es `postgres` (nombre servicio), no `localhost`.

### `database.py:1` — Pool asyncpg 10+20

```python
engine = create_async_engine(settings.database_url, echo=False, pool_pre_ping=True, pool_size=10, max_overflow=20)
async_session_maker = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)
```

- `create_async_engine` es `new Pool({connectionString, max: 10})` en Node `pg` pero async. `pool_size 10` + `max_overflow 20` = máx 30 conexiones (Postgres default `max_connections 100`, deja margen para `psql` y `stats/` `psycopg2`).
- `pool_pre_ping=True` hace `SELECT 1` keepalive antes de usar conexión muerta (como `pool.query('SELECT 1')` en Node healthcheck).
- `expire_on_commit=False` no expira objetos tras `commit` (como `keepAlive` en `pg` Pool, evita re-query).

**`get_db():27` Depends:**

```python
async def get_db() -> AsyncSession:
    async with async_session_maker() as session:
        try:
            yield session  # inyecta en handler (como req.db en Express middleware)
        finally:
            await session.close()  # siempre cierra, evita leaks (como client.release() en finally)
```

Uso: `async def handler(db: AsyncSession = Depends(get_db))` — FastAPI inyecta sesión por request (como `const db = req.db`).

**`Base:23` vacío** — `class Base(DeclarativeBase): pass` registra `metadata` para `create_all()` en `main.py:36` lifespan (complementa `init.sql`).

---

## 8. `app/main.py:1` (132 líneas) — App, Lifespan, CORS, `/` y `/health`

**Lifespan `main.py:31` startup/shutdown (como `useEffect` con cleanup en React):**

```python
@asynccontextmanager
async def lifespan(app: FastAPI):
    try:
        async with engine.begin() as conn:
            await conn.run_sync(Base.metadata.create_all)  # CREATE TABLE IF NOT EXISTS para cada model (idempotente)
    except Exception:
        pass  # si DB no está (CI sin Postgres), no bloquear arranque — /health dará degraded
    yield  # aquí corre el servidor
    try:
        await engine.dispose()  # cleanup al apagar (como pool.end() en Node)
    except: pass
```

Complementa `init.sql`: ambos son idempotentes (`IF NOT EXISTS` / `create_all` si ya existe no duplica). Si uno ya creó tablas, el otro no hace nada.

**CORS `main.py:56` LAN (prd.md:59 misma subred):**

```python
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_credentials=True, allow_methods=["*"], allow_headers=["*"])
```

Como `cors({origin: "*"})` en Express — permite `fetch` desde App Android (`Retrofit`), `Node-RED`, y cualquier host `192.168.1.x`. Sprint 1 LAN trust, en prod restringirías a `["http://192.168.1.14:3000"]`.

**`GET /:68` root** — `{name, version, docs: "/docs", health: "/health"}` (como `GET /` en Express que devuelve `{name, version}`).

**`GET /health:78` con DB ping:**

```python
async def health_check(db: AsyncSession = Depends(get_db)):
    try:
        await db.execute(text("SELECT 1"))  # ping DB (como pool.query('SELECT 1') en Node)
    except Exception as exc:
        db_status = f"error: {type(exc).__name__}"
    return HealthResponse(status="ok" if db_status=="ok" else "degraded", database=db_status)
```

Usado por `docker-compose.yml healthcheck` y `ci.yml:149` `curl -sf http://localhost:8000/health | grep '"status":"ok"'`. `degraded` no `500` — graceful como `k8s` `degraded` (no crashea CI si DB tarda).

---

## 9. Docker + `docker-compose.yml:1` (62 líneas)

**`Dockerfile:1` `python:3.12-slim`:**

```dockerfile
FROM python:3.12-slim
WORKDIR /app
RUN apt-get update && apt-get install gcc libpq-dev && rm -rf /var/lib/apt/lists/*  # compila psycopg2
COPY requirements.txt .  # cache: si no cambia, reusa pip install (como npm ci cache)
RUN pip install --no-cache-dir -r requirements.txt
COPY ./app ./app
EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]  # como node server.js
```

**`docker-compose.yml:1` 3 servicios `aethernet-net` bridge:**

| Servicio | Imagen | Puertos | Env | Volumes | Healthcheck |
|---|---|---|---|---|---|
| `postgres:2` | `postgres:16-alpine` | `5432:5432` | `POSTGRES_USER/DB/PASSWORD` desde `.env` | `postgres_data:/var/lib/postgresql/data` + `./backend/init.sql:/docker-entrypoint-initdb.d/init.sql:ro` | `pg_isready -U aethernet` 10s×5 |
| `mosquitto:22` | `eclipse-mosquitto:2.0` | `1883:1883` `9001:9001` | — | `mosquitto.conf:ro` `acl.conf:ro` `data/` `log/` | — |
| `fastapi:36` | `build ./backend` | `8000:8000` | `DATABASE_URL` `postgresql+asyncpg://...@postgres:5432/...` (host `postgres` no `localhost`) `MQTT_*` | build context `./backend` | — (CI espera `curl /health`) |

`depends_on:50` `postgres: condition: service_healthy` espera a que `pg_isready` pase antes de arrancar FastAPI (evita `connection refused`).

---

## 10. Mosquitto — `mosquitto.conf:1` + `acl.conf:1`

**`mosquitto.conf:1` broker config (como `vite.config.ts` para MQTT):**

```ini
listener 1883
protocol mqtt
allow_anonymous true  # LAN Sprint 1, sin auth (verdadero en ACL prod)
listener 9001
protocol websockets   # App MQTT over WS (MQTT.js en React Native)

persistence true
persistence_location /mosquitto/data/
persistence_file mosquitto.db  # como WAL en Postgres (persiste QoS 1/2)
log_dest file /mosquitto/log/mosquitto.log + stdout (docker logs)
message_size_limit 102400  # 100KB max (como bodyParser limit en Express)
```

**`acl.conf:1` topics (como `GRANT` en Postgres / `RBAC` en API):**

```ini
topic readwrite aethernet/#  # todos pub/sub en aethernet/* (Gateway, App, Node-RED, backend) — como GRANT
topic readwrite $SYS/#       # stats broker Mosquitto (como /metrics en Prometheus)
```

Topics `architecture.md:60`: `aethernet/rover/#` (comando/telemetría `RF-3.1`), `aethernet/access/#` (MEGA `HU-01`), `aethernet/seguridad/#` (láser `HU-02`), `aethernet/system/#` (heartbeat), `aethernet/sensor/#` (HU-03). `#` es wildcard como `/*` en Express, `aethernet/` es namespace como `/api/`.

Activación: descomentar `acl_file` en `mosquitto.conf:33` y `allow_anonymous false` para prod con `mosquitto_passwd`.

---

## 11. `requirements.txt:1` (31 líneas) + `pyproject.toml:1` (16 líneas) + `tests/test_health.py:1`

**`requirements.txt:1` pinneado (como `package-lock.json`):**

```
fastapi==0.111.0      # Express en Python + Swagger /docs
uvicorn[standard]==0.30.1 # ASGI server (uvloop, httptools) — FOSS
sqlalchemy[asyncio]==2.0.30 # ORM async — Prisma/Drizzle
asyncpg==0.29.0       # driver PG async — pg en Node pero nativo async (1 año exp)
pydantic==2.7.4       # Zod Python
paho-mqtt==2.1.0      # mqtt.js Python
pytest==8.2.2 + pytest-asyncio + httpx  # Jest + supertest
```

Pin exacto = `lib_deps = ArduinoJson@6.21.3` en `platformio.ini` — evita rotura con update.

**`pyproject.toml:1` lint/type/test (como `eslint.config` + `tsconfig` + `jest.config`):**

```toml
[tool.ruff.lint] ignore = ["B008", "S110", "BLE001"]  # B008 falso positivo FastAPI Depends (como eslint-disable para DI válido)
[tool.pytest.ini_options] asyncio_mode = "auto" testpaths = ["tests"] pythonpath = ["."]
[tool.mypy] python_version = "3.12" ignore_missing_imports = true  # como skipLibCheck TS
```

**`tests/test_health.py:1` 7 tests `pytest` con `TestClient` + `MagicMock` (162 líneas):**

| Test `test_health.py` | Qué valida (como `Jest` `supertest`) |
|---|---|
| `test_root:61` `GET /` | `200` + `health=="/health"` |
| `test_health_ok:67` `GET /health` | `200` `status ok|degraded` `database` `version 1.0.0-sprint1` (mock `SELECT 1`) |
| `test_create_access_event:78` `POST /api/access-events` | `201` `user_id` `success` `id` UUID (HU-01 `gateway.ino:343`) |
| `test_create_sensor_event:90` `POST /api/sensor-events` | `201` `sensor_id hc-sr04-01` `value 42.5` `filtered 41.2` (HU-03) |
| `test_validation_rejects_invalid_payload:106` | `422` si falta `pin_hash` o `sensor_type` (Pydantic Zod error) |
| `test_list_endpoints_return_empty:114` `GET /api/*` | `200` `[]` para 4 listas (access/sensor/security/rover) sin datos (como `findMany` vacío) |

**Mocks `test_health.py:18`:** `mock_db_session` `MagicMock` con `AsyncMock execute/commit/refresh` + `fake_add` que asigna `uuid4()` y `NOW()` como `init.sql` y `models.py` (simula `RETURNING *` sin Postgres real). `client` `dependency_overrides[get_db]` inyecta mock como `jest.mock('./db')` (como `req.db = mockDb` en Express). Sin Postgres real — CI `backend-test` corre sin Docker.

---

## 12. CI y Flujo Firmware ↔ Backend ↔ Stats

**CI `.github/workflows/ci.yml:20` `backend-test`:**

```yaml
backend-test: runs-on ubuntu-latest
  setup-python 3.12 cache pip
  pip install -r requirements.txt
  ruff check app/ + mypy app/ + pytest -v --tb=short  # 6 tests
```

**CI `docker-build:117` integración real:**

```bash
docker compose build --no-cache && docker compose up -d
# espera postgres healthy (pg_isready) + curl /health 30×2s
curl -X POST /api/access-events -d '{"user_id":"ci_user","pin_hash":"abc123","success":true}' | grep ci_user
curl -X POST /api/sensor-events -d '{"sensor_id":"ci-hc-sr04","value":42.5}' | grep ci-hc-sr04
curl -X POST /api/security-events -d '{"event_type":"intrusion"}'
curl -X POST /api/rover/telemetry -d '{"session_id":uuid,"left_motor_pwm":120}' | grep session
curl http://localhost:8000/api/access-events?limit=5 | grep "\["
```

**Flujo vivo (arquitectura.md:72):**

```
MEGA keypad "1234#" → keypad_control.cpp:67 processPinAttempt → uart_protocol.cpp:60 ACCESS:JSON → Serial2 38400
  → Gateway gateway.ino:286 handleMegaUart → forwardAccessToBackend:343 HTTP POST http://192.168.1.14:8000/api/access-events
    → FastAPI events.py:31 create_access_event → models.py:20 AccessEvent → asyncpg → PostgreSQL access_events
      → App Kotlin GET /api/access-events?limit=5 (Retrofit) → RecyclerView histórico
      → stats/ EST-10 db_extract.py (futuro) SELECT * FROM access_events → Pandas → t-Student RF vs WiFi EST-05
```

**HTML/CSS/JS/React 2 años:** `POST /api/access-events` es `fetch('http://backend:8000/api/access-events', {method:'POST', body: JSON.stringify({user_id, pin_hash})})` pero desde `gateway.ino` con `HTTPClient`. `GET /api/sensor-events?sensor_type=ultrasonic` es `fetch('/api/sensor-events?sensor_type=ultrasonic').then(r=>r.json()).then(data=>setSensorData(data))` en React con `useEffect`.

**C 1 año:** `pin_hash String(128)` en `Schemas/Models` es el `String hashPin()` `djb2 HEX` en `keypad_control.cpp:99` — nunca el PIN. `SmallInteger -255..255` en `RoverTelemetry` es `int16_t left_pwm` en `rover-uno.ino:85`.

**PostgreSQL 1 año:** `asyncpg` + `SQLAlchemy async` es `pg Pool` + `Prisma` en Node pero async (`await db.commit()` como `await prisma.$transaction`). `NUMERIC(10,4)` guarda `42.5` exacto para `t-Student` sin error `FLOAT`.

---

## 13. Instalación y Uso

```bash
# 1. Env (copia plantilla, no commitear .env)
cd backend
cp .env.example .env  # edita POSTGRES_PASSWORD (cambia changeme_secure_password_here)
# 2. Docker (3 servicios)
cd ..  # repo raíz
docker compose build --no-cache
docker compose up -d        # postgres + mosquitto + fastapi
docker compose ps           # 3 healthy
docker compose logs -f fastapi  # uvicorn 8000
# 3. Probar
curl -sf http://localhost:8000/health | jq
curl -sf http://localhost:8000/ | jq
curl -sf -X POST http://localhost:8000/api/access-events -H "Content-Type: application/json" \
  -d '{"user_id":"keypad_user","pin_hash":"7c78c98f","success":true,"source":"keypad"}' | jq
curl -sf "http://localhost:8000/api/access-events?limit=5" | jq
curl -sf -X POST http://localhost:8000/api/sensor-events -H "Content-Type: application/json" \
  -d '{"sensor_id":"hc-sr04-01","sensor_type":"ultrasonic","value":42.5,"filtered_value":40.1,"unit":"cm","metadata":{"alpha":0.2}}' | jq
# 4. Docs Swagger
open http://localhost:8000/docs  # Swagger UI auto-generado (como swagger-jsdoc)
# 5. Tests sin Docker (mock DB)
cd backend
pip install -r requirements.txt
pytest -v --tb=short        # 6 passed (sin Postgres real)
ruff check app/ && mypy app/
# 6. Stats usa esta DB (Sprint 4)
# stats/db_extract.py (futuro EST-10) hará SELECT * FROM sensor_events WHERE sensor_type='ultrasonic'
# para Pandas + t-Student (stats/ema_filter.py ya prototipado offline)
```

**Puertos LAN** `docker-compose.yml:13/26/48`: `5432` Postgres, `1883` MQTT, `9001` WS, `8000` FastAPI — todos en `aethernet-net` bridge `192.168.1.x` (misma subred `prd.md:59`).

---

## 14. Configuración y Secretos

- `.env` gitignored (como `secrets.h` en firmware) — `.env.example` commiteable con `changeme` dummy.
- `config.py:7` lee `DATABASE_URL` con `asyncpg` — formato `postgresql+asyncpg://user:pass@host:5432/db`. En Docker `host=postgres` (servicio), en local `host=localhost`.
- `MQTT_BROKER_HOST` `mosquitto` en Docker (servicio), `localhost` en local sin Docker. `allow_anonymous true` `mosquitto.conf:6` LAN Sprint 1 — futuro `mosquitto_passwd` + `acl.conf`.
- Gateway `secrets.h.example` ↔ `backend/.env.example` — misma LAN `192.168.1.14` `AetherNet-LAN` / `FELIPE` / `changeme`.

---

## 15. Roadmap Backend

| Área | Estado | Origen |
|---|---|---|
| CRUD 4 tablas `access/sensor/security/rover` POST/GET `limit/offset` | ✅ Sprint 1 `events.py:25` | `HU-01..HU-04` `RNF-2.2` `DEVOPS-06` |
| `GET /health` con `SELECT 1` + `lifespan create_all` | ✅ `main.py:31` `tests/test_health.py:67` | `ci.yml:116` `docker-compose healthcheck` |
| `Dockerfile` `python:3.12-slim` + `docker-compose` 3 servicios | ✅ `docker-compose.yml:1` | `RNF-1.1` `prd.md:30` |
| `Mosquitto` `1883/9001` `acl aethernet/#` | ✅ `mosquitto.conf:1` `acl.conf:14` | `RNF-1.1` `architecture.md:63` |
| Tests 7 `pytest` + `ruff` + `mypy` CI verde | ✅ `tests/test_health.py:1` `pyproject.toml:1` | `RNF-1.2` |
| `init.sql` `uuid-ossp` + 7 índices | ✅ `init.sql:1` | `models.py:30` `events.py:51` |
| Auth JWT / RBAC `user_id` multiusuario `MOV-04` | ⏳ Sprint 2 | `config.h:48 VALID_PIN hardcodeado → DB` |
| `WebSockets` `WS` push dashboard App `RF-1.1` | ⏳ Sprint 2 | `architecture.md:64` `MQTT+WS` |
| `Node-RED` `aethernet/seguridad/intrusion` → `Telegram` `RF-4.1` | ⏳ Sprint 3 | `automation/` `architecture.md:92` |
| `stats/ EST-10` `db_extract.py` `SELECT sensor_events` → `Pandas` | ⏳ Sprint 4 | `docs/roadmap.md` `EST-10` |
| `stats/ EST-05` `t-Student RF vs WiFi` latencia `<50ms` `prd.md:49` | ⏳ Sprint 4 | `RNF-2.2` |
| Tuya/bombillo `RF-4.2` | ❌ cancelado `ADR-001` | `RNF-3.1` violación `R-01` |

---

## 16. Referencias cruzadas (para evaluadores)

- `docs/prd.md:30` In-Scope `Docker FastAPI+Postgres+Mosquitto` + `KPI` `prd.md:49` latencia `<50ms` MQTT / `prd.md:51` filtro `>85%` (aquí `sensor_events.filtered_value`)
- `docs/requirements.md:21` `RF-2.1` Gateway `→ HTTP POST` `requirements.md:42` `RNF-1.1` `docker-compose` `requirements.md:44` `RNF-2.2` histórico
- `docs/architecture.md:31` Backend `FastAPI+Mosquitto+PostgreSQL` `architecture.md:63` `HTTP POST` vs `MQTT` `architecture.md:72` flujo `EMA → MQTT → PostgreSQL → stats`
- `docs/sprints.md` Sprint 1 `DEVOPS-01/03/06` `sprints.md:53` estado verificado `curl /api/access-events` + `firmware/gateway-esp32.ino:343` `forwardAccessToBackend`
- `docs/roadmap.md:86` conocimiento `PostgreSQL`/`FastAPI`/`Docker` para desplegar `backend/`
- `firmware/mega-access/src/keypad_control.cpp:99` `hashPin djb2` → `pin_hash:128` aquí (nunca PIN claro)
- `stats/ema_filter.py:32` `EMA α0.2` ↔ `sensor_events.filtered_value` aquí (mismo cálculo `rover-uno.ino:259` portado a Python)
- `.github/workflows/ci.yml:20` `backend-test` + `ci.yml:117` `docker-build` `curl /health` + 4 endpoints

---

## 17. Glosario (si vienes de web/Arduino)

| Término backend | Equiv. web | Equiv. Arduino/C | Qué es |
|---|---|---|---|
| `FastAPI` `Depends(get_db)` | `Express` `middleware(req.db)` | `setup()` que inyecta `Serial` | Framework + DI por request |
| `Pydantic` `Field` | `Zod` `z.string().max()` | `struct` validado `checksum` | Validación `req.body` + docs Swagger |
| `SQLAlchemy` `Column` | `Prisma` `model` | `struct` `Column` como campo | ORM que mapea `class` ↔ `TABLE` |
| `asyncpg` `async/await` | `pg Pool` `await` | `non-blocking` `millis()` | Driver Postgres async (no bloquea loop) |
| `TIMESTAMPTZ` `func.now()` | `Date.now()` UTC | `millis()` desde boot + `NOW()` DB | Timestamp con zona (no `Date` local) |
| `NUMERIC(10,4)` | `number` exacto | `float` pero exacto | Decimal fijo (no `FLOAT` binario) |
| `JSONB` `metadata` | `Json` Prisma `Record<string,any>` | — | JSON binario flexible |
| `Mosquitto` `aethernet/#` | `WebSocket` `socket.emit` | `nRF24L01` pero por TCP | Broker pub/sub (RF por cable) |

---

*Documentado como estudiante 6º semestre que conecta `fetch` en `React` (Kotlin `Retrofit` hace `POST /api/access-events`), `C` en `MEGA` (hash `djb2` → `pin_hash`), `Arduino` `UART 38400` → `Gateway HTTP` y `PostgreSQL` (`asyncpg` `NUMERIC` `JSONB`) con `Docker` como `platformio.ini` para backend — FOSS de extremo a extremo (`RNF-3.1`) con `CI` que valida `SELECT 1` y `POST 201` sin `Postgres` real (mock).*
