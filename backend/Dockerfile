# =============================================================================
# Dockerfile — Backend FastAPI + PostgreSQL | 6º Semestre UTP | RNF-1.1, DEVOPS-01
# Autor: Est. Tecnología en Desarrollo Software + Ing. Sistemas (UTP)
# Experiencia: 2 años Python (Docker), 2 años JS/React (Dockerfile Node),
#              1 año PostgreSQL (libpq-dev), 2 años electrónica (CI como flasheo)
# Analogía React: este archivo es el `Dockerfile` de un `create-react-app` pero
# para FastAPI — `FROM node:18` aquí es `FROM python:3.12-slim` (imagen base).
# Analogía C: es el `platformio.ini` pero para backend — define entorno reproducible.
# FOSS: python:3.12-slim (Debian slim, MIT) + pip + apt — RNF-3.1, sin ECR propietario.
# Usado en: docker-compose.yml (servicio backend) + CI .github/workflows/ci.yml
# =============================================================================
# Imagen base — python:3.12-slim es Debian slim con Python 3.12 — liviana (~45MB)
# slim no trae gcc/libpq, los instalamos abajo solo para compilar psycopg2
# En React sería `FROM node:18-alpine` — slim/alpine es minimal (no full Debian)
FROM python:3.12-slim

# Directorio trabajo dentro del contenedor — como `WORKDIR /app` en Node Dockerfile
# Todo COPY/RUN es relativo a /app (como `cd /app` en bash)
WORKDIR /app

# Dependencias sistema para compilar psycopg2-binary y asyncpg (que usan C)
# gcc — compilador C (como gcc en PlatformIO para firmware)
# libpq-dev — headers de PostgreSQL para psycopg2 (como -dev libs en apt)
# --no-install-recommends evita traer docs/man extras — imagen más pequeña (como npm --production)
# rm -rf /var/lib/apt/lists/* limpia cache apt — no deja basura (como npm cache clean)
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    libpq-dev \
    && rm -rf /var/lib/apt/lists/*

# Copia requirements primero para aprovechar cache Docker — si no cambia requirements.txt,
# Docker reusa capa pip install (como cache de `npm ci` en CI)
# En React harías COPY package.json + RUN npm ci antes de COPY src
COPY requirements.txt .
# Instala deps Python — --no-cache-dir no guarda wheel cache (imagen más pequeña, como npm ci --omit=dev)
RUN pip install --no-cache-dir -r requirements.txt

# Copia código app — como COPY ./src ./src en Node Dockerfile
# Solo ./app, no todo backend/ (no copia .env, tests, .pycache) — .dockerignore lo filtra
COPY ./app ./app

# Puerto FastAPI — 8000 es el que usa uvicorn y gateway-esp32 BACKEND_PORT 8000 (config.py:13)
# EXPOSE es documentación — no abre puerto, pero docker-compose.yml mapea 8000:8000 (como ports en compose)
EXPOSE 8000

# Comando arranque — uvicorn es el servidor ASGI (como `node server.js` o `npm start`)
# app.main:app — módulo app.main, variable app (FastAPI) — como `node app/main.js` con export app
# --host 0.0.0.0 escucha en todas las interfaces (para que Docker mapee), --port 8000
# En React sería CMD ["npm", "start"] — aquí es uvicorn directo (no gunicorn, FOSS simple)
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
