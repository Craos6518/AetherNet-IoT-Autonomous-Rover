# =============================================================================
# backend/app/__init__.py — Paquete FastAPI AetherNet | 6º Semestre UTP
# Autor: Andres Felipe Martinez Henao
# Experiencia: 2 años Python (FastAPI), 2 años JS/React (npm packages),
#              1 año PostgreSQL (SQLAlchemy = Prisma de Python)
# Analogía React: este __init__.py es el index.ts de la carpeta app — marca
# que app/ es un paquete Python importable (como tener package.json con "type": "module").
# Vacío porque no hay lógica de paquete, solo hace que `import app.models` funcione.
# En Node sería tener un index.js que re-exporta; aquí Python lo hace automático
# si el archivo existe (aunque vacío). Sprint 1 / DEVOPS-01 — RNF-1.1, FOSS.
# =============================================================================
# Intencionalmente vacío — solo declara paquete Python (ver PEP 420 namespace).
# Si vienes de React/JS: es como tener una carpeta con index.js vacío para que
# `import { X } from './app'` resuelva. En Python 3.3+ no es estrictamente
# necesario, pero lo dejamos explícito para compatibilidad y claridad.
