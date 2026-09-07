# =============================================================================
# backend/app/routers/__init__.py — Paquete routers FastAPI | 6º Semestre UTP
# Autor: Est. Tecnología en Desarrollo Software + Ing. Sistemas (UTP)
# Experiencia: 2 años Python (FastAPI APIRouter), 2 años JS/React (Express routers),
#              1 año PostgreSQL
# Analogía React/Express: este archivo es el `routers/index.ts` que agrupa
# `events.ts` — declara que app/routers/ es paquete importable para que
# `from app.routers.events import router` funcione (ver main.py:25).
# Sprint 1 / DEVOPS-06 — RNF-1.1, RF-2.1.
# Analogía C/Arduino: como tener `src/` con headers — organiza módulos por dominio.
# =============================================================================
"""Routers package for AetherNet FastAPI — Sprint 1. Agrupa endpoints /api/* (events, futuros)."""
# Vacío salvo docstring — el router real está en events.py (como tener routes/events.js).
# Si agregas nuevo router (ej. auth.py), lo importas en main.py: `from app.routers.auth import router`.
