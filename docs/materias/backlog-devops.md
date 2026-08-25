# Backlog por Materia — DevOps (incluye Firmware Base)

Backlog operativo detallado del Área 2. Cada tarea define **qué hacer exactamente, alcance (IN/OUT) y criterios de aceptación verificables**. Los IDs DEVOPS-01..09 provienen de `docs/backlog.md` y se mantienen; los nuevos continúan la serie (DEVOPS-10+). Prioridad MoSCoW: M=Must, S=Should, C=Could.

> Estado real verificado al Aug 2026: `main.py` inexistente, credenciales hardcodeadas en `gateway.ino`, conflicto de pines en `access_control.ino`, Gradle presente pero job Android desactivado.

---

## Sprint 1 — Infraestructura fundacional

### DEVOPS-01 — Orquestación docker-compose ✅ HECHO (verificar)
| Campo | Valor |
|---|---|
| Prioridad | M · Sprint 1 · Origen RNF-1.1 |

**Hecho:** 3 servicios (postgres:16-alpine, eclipse-mosquitto:2.0, fastapi build local), red bridge `aethernet-net`, volumen `postgres_data`, healthcheck Postgres, `depends_on` condicional.
**Verificación pendiente:** `docker-compose config` sin warnings + `docker-compose up -d` levanta los 3 contenedores healthy (hoy fallará el de FastAPI hasta cerrar DEVOPS-06).

### DEVOPS-02 — Configuración completa de Mosquitto
| Campo | Valor |
|---|---|
| Prioridad | M · Sprint 1 · Depende de DEVOPS-01 · Origen RF-2.1, RF-4.2 |

**Qué hacer exactamente:**
1. Crear `backend/mosquitto/config/mosquitto.conf` con: `listener 1883` (TCP para ESP32/Node-RED), `listener 9001` + `protocol websockets` (para App Android), `password_file /mosquitto/config/passwd`, `acl_file /mosquitto/config/acl`, `persistence true`, `log_dest file /mosquitto/log/mosquitto.log`.
2. Crear usuarios con `mosquitto_passwd -c backend/mosquitto/config/passwd gateway` (y `nodered`, `appbackend`). Contraseñas reales SOLO en `.env` local.
3. Crear `backend/mosquitto/config/acl`: `gateway` → readwrite `aethernet/#`; `nodered` → read `aethernet/seguridad/#`, `aethernet/access/#`, write `aethernet/relay/#`; `appbackend` → read `aethernet/#`, write `aethernet/app/#`.

**Alcance IN:** archivos de config versionados sin secretos; plantilla `.example` si lleva contraseñas.
**Alcance OUT:** TLS (red LAN confiable, fuera de alcance FOSS-local).
**Criterios de aceptación:**
- [ ] `mosquitto_pub -u gateway -P <pass> -t aethernet/test -m ok` funciona desde host
- [ ] Publicación anónima es RECHAZADA (`allow_anonymous false`)
- [ ] `nodered` NO puede publicar en `aethernet/rover/#` (verificar error ACL)

### DEVOPS-03 — Esquema PostgreSQL inicial ✅ PARCIAL
| Campo | Valor |
|---|---|
| Prioridad | M · Sprint 1 · Origen RNF-2.2 |

**Hecho:** `models.py` define `AccessEvent`, `SensorEvent`, `SecurityEvent` con índices; existe `backend/init.sql` montado como entrypoint.
**Falta exactamente:** verificar que `init.sql` y `models.py` están SINCRONIZADOS (mismas columnas/tipos); decidir estrategia única (init.sql vs Alembic — recomendado: mantener init.sql para Sprint 1-3 e introducir Alembic solo si hay cambios de esquema post-Sprint 2).
**Criterios:**
- [ ] `\d access_events` dentro del contenedor coincide columna a columna con `models.py`
- [ ] Decisión documentada en comentario cabecera de `init.sql`

### DEVOPS-04 — Pipeline CI con arduino-cli ✅ HECHO (mejorar)
| Campo | Valor |
|---|---|
| Prioridad | M · Sprint 1 · Origen RNF-1.2 |

**Hecho:** matriz compila gateway-esp32 (core esp32), mega-access y rover-uno (core avr); librerías instaladas por firmware.
**Falta exactamente (DEVOPS-10):**
1. Cachear directorio Arduino15: step `actions/cache@v4` con `path: ~/.arduino15` y clave por hash del workflow.
2. Habilitar job `android-build`: quitar `if: false` (Gradle ya existe en `app/`), agregar caché de Gradle (~/.gradle).
3. Branch protection en GitHub: CI verde requerido para merge a main/develop.
**Criterios:**
- [ ] Segundo run consecutivo del pipeline reduce ≥60% el tiempo del job firmware (comparar duraciones en Actions)
- [ ] PR de prueba queda bloqueado si un firmware no compila

### DEVOPS-05 — Prueba de enlace SPI/RF ESP32↔UNO
| Campo | Valor |
|---|---|
| Prioridad | M · Sprint 1 · Origen RF-3.1 |

**Qué hacer exactamente:** con ambos firmwares flasheados, validar ida y vuelta: Gateway publica comando manual (PWM L/R) → UNO lo ejecuta y responde telemetría vía ACK payload → Gateway la republica a MQTT `aethernet/rover/telemetry`. Registrar latencia ida-vuelta promedio de 50 muestras con `millis()` impreso por serial.
**Alcance OUT:** autonomía del Rover (Sprint 3), integración App.
**Criterios:**
- [ ] Telemetría llega al topic MQTT correcta (checksum válido)
- [ ] Tabla de latencias registrada en `docs/test-plan.md` o bitácora del sprint (insumo EST-05)

## Sprint 1 (crítico, nuevo)

### DEVOPS-11 — Sanitizar credenciales del gateway ESP32
| Campo | Valor |
|---|---|
| Prioridad | M · Sprint 1 · Sin dependencias · Origen seguridad/RNF-3.1 |

**Problema actual:** `firmware/gateway-esp32/src/gateway.ino:16-22` tiene `WIFI_SSID "AetherNet-LAN"`, `WIFI_PASSWORD "changeme"`, `MQTT_BROKER "192.168.1.100"` hardcodeados.
**Qué hacer exactamente:**
1. Crear `firmware/gateway-esp32/src/secrets.h.example` con placeholders (`#define WIFI_SSID "TU_SSID"` etc.).
2. Crear `secrets.h` real (gitignored — agregar a `.gitignore`) y hacer `#include "secrets.h"` desde gateway.ino.
3. Verificar que `ci.yml` sigue compilando (agregar step que genere `secrets.h` desde el example antes de compilar en CI).

**Criterios:**
- [ ] `git grep -i changeme firmware/` no retorna nada
- [ ] CI verde tras el cambio

## Sprint 2 — Backend API y puentes

### DEVOPS-06 — Endpoints FastAPI mínimos ⚠️ BLOQUEANTE
| Campo | Valor |
|---|---|
| Prioridad | M · Sprint 2 (adelantar YA) · Depende de DEVOPS-01, DEVOPS-03 · Origen RF-2.1 |

**Problema actual:** NO existe `backend/app/main.py` → el contenedor FastAPI crashea y el job `docker-build` del CI falla en `curl /health`.
**Qué hacer exactamente:**
1. Crear `backend/app/main.py`: app FastAPI con lifespan, CORS abierto en LAN, inclusión de routers.
2. Crear `backend/app/routers/health.py`: `GET /health` → `{"status":"ok","db":"up|down","mqtt":"connected|error"}` (chequeo real contra Postgres y broker, no hardcoded).
3. Crear `backend/app/routers/events.py`: `POST /events/access` (valida payload, inserta AccessEvent), `GET /events/access?limit=` , `GET /events/sensors?sensor_type=&limit=`.
4. Crear `backend/app/schemas.py` con Pydantic v2 (AccessEventIn/Out, SensorEventOut).
5. Verificar que el Dockerfile instala todo y arranca `uvicorn app.main:app --host 0.0.0.0 --port 8000`.

**Alcance IN:** solo estos 4 endpoints; autenticación simple opcional (header token en .env).
**Alcance OUT:** WebSockets propios (la App usa MQTT directo), paginación compleja, OpenAPI custom.
**Criterios:**
- [ ] `curl -f http://localhost:8000/health` responde 200 desde el job CI `docker-build`
- [ ] POST/GET roundtrip inserta y lee en Postgres real
- [ ] `/docs` (Swagger) accesible y muestra los 4 endpoints

### DEVOPS-12 — Puente MQTT→PostgreSQL en el backend
| Campo | Valor |
|---|---|
| Prioridad | M · Sprint 2 · Depende de DEVOPS-02, DEVOPS-06 · Origen RF-2.1, RNF-2.2 |

**Qué hacer exactamente:**
1. Agregar dependencia `aiomqtt` (o paho-mqtt en thread) a `backend/requirements.txt`.
2. Crear `backend/app/mqtt_bridge.py`: tarea async del lifespan que se suscribe a `aethernet/access/event`, `aethernet/seguridad/intrusion`, `aethernet/rover/telemetry`.
3. Parsear JSON de cada mensaje y persistir en `AccessEvent` / `SecurityEvent` / `SensorEvent` respectivamente (reconexión automática con backoff exponencial 1s→30s).

**Alcance IN:** persistencia cruda de eventos; logging de mensajes malformados sin crashear.
**Alcance OUT:** procesamiento estadístico online (eso es área Estadística/LowCode).
**Criterios:**
- [ ] Publicando un evento de prueba con `mosquitto_pub`, aparece la fila correspondiente en la tabla (verificar con psql)
- [ ] Matando el broker 10 s y restituyéndolo, el puente se reconecta solo

### DEVOPS-07 — Tests PyTest del backend
| Campo | Valor |
|---|---|
| Prioridad | S · Sprint 2 · Depende de DEVOPS-06 · Origen calidad |

**Qué hacer exactamente:** `backend/tests/` con `conftest.py` (fixture de BD: SQLite async en memoria o Testcontainers si disponible), tests de: health devuelve 200 y db up; POST evento inválido devuelve 422; POST/GET roundtrip. Job `backend-test` del CI ya ejecuta `pytest` — solo deben existir los tests.

**Criterios:**
- [ ] `pytest -v` verde en local y CI, ≥5 casos cubriendo happy path + validación

### DEVOPS-08 — Variables de entorno documentadas ✅ PARCIAL
**Falta exactamente:** revisar que TODA variable usada en Compose/backend/gateway está en `env.example` con valor placeholder; agregar las nuevas de Mosquitto (usuarios/contraseñas) y del bridge MQTT.

**Criterios:**
- [ ] Copiar `env.example`→`.env` permite levantar TODO el stack sin editar más nada

## Sprint 3-4 — Firmware de integración

### DEVOPS-13 — Protocolo UART Gateway↔MEGA documentado e implementado simétrico
| Campo | Valor |
|---|---|
| Prioridad | M · Sprint 2-3 · Depende de DEVOPS-05 · Origen RF-2.1 |

**Problema actual:** ambos sketches abren Serial2@115200 pero el formato de trama no está especificado en ningún documento.
**Qué hacer exactamente:**
1. Escribir `docs/uart-protocol.md`: tabla de tramas (comando relé, estado acceso, evento intrusión, heartbeat), encoding elegido (recomendado: JSON una-línea+\n para debuggabilidad, o binario con header 0xAA + len + checksum si se prioriza CPU), y manejo de errores.
2. Implementar encoder/decoder espejo en `gateway.ino` y `access_control.ino` según esa spec.
**Criterios:**
- [ ] Comando de relé enviado por MQTT llega al relé físico pasando por ambas capas
- [ ] Evento de teclado llega a `aethernet/access/event` con <100 ms medidos

### DEVOPS-14 — Corregir conflicto de pines LED RGB vs relés en MEGA
| Campo | Valor |
|---|---|
| Prioridad | M · Sprint 2 · Origen HU-01/HU-02 |

**Problema actual:** `access_control.ino` define LED RGB en 44/45/46 Y relés en pins 40-47 (el propio código advierte el choque).
**Qué hacer exactamente:** reasignar relés a pins libres del MEGA (ej. 22-29 ya usados por keypad → usar 38,39,40,41,42,43,47,48 verificando contra pinout completo del sketch), actualizar comentarios de pinout cabecera, recompilar en CI.
**Criterios:**
- [ ] Ningún pin duplicado (grep de #define y arrays de pins)
- [ ] LED RGB y relés operan simultáneamente sin interferencia en banco de pruebas

### DEVOPS-15 — PIN de acceso en EEPROM
| Campo | Valor |
|---|---|
| Prioridad | S · Sprint 2-3 · Origen HU-01 |

**Problema actual:** `VALID_PIN = "1234"` hardcodeado con TODO.
**Qué hacer exactamente:** leer/escribir PIN en EEPROM (dirección fija + magic byte de validez); comando UART/MQTT `aethernet/access/command {"action":"set_pin","pin":"XXXX"}` protegido por PIN admin; PIN hasheado (no plano) usando FNV/djb2 embebido si EEPROM lo permite.
**Criterios:**
- [ ] Desconectar energía no borra el PIN configurado
- [ ] PIN por defecto documentado y cambio funcional end-to-end

### DEVOPS-09 — Script de arranque único
| Campo | Valor |
|---|---|
| Prioridad | C · Sprint 4 |

**Qué hacer exactamente:** `scripts/up.sh` (copia `.env.example`→`.env` si no existe, `docker-compose up -d --build`, espera healthchecks, imprime URLs/servicios) y `scripts/down.sh`. **Criterios:** máquina limpia = proyecto arriba en un comando.

---

## Resumen de dependencias críticas

```
DEVOPS-11 (secrets) ─┐
DEVOPS-02 (broker) ──┼─→ DEVOPS-12 (bridge) ─→ EST-04/EST-05 (datos reales)
DEVOPS-06 (API) ─────┘        │
DEVOPS-13 (UART) ────────────┼─→ MOV-02..MOV-08 (App consume)
DEVOPS-14/15 (MEGA) ─────────┘
```

**Regla del área:** nada se mergea sin CI verde; cualquier atraso aquí es atraso de TODAS las demás materias.
