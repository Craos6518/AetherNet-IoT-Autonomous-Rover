# Auditoría de Secretos — Sprint 1 → Sprint 2 (PM-08)

**Fecha:** 2026-08-29  
**Rama auditada:** `feature/firmware-mega-cerrojo` (HEAD `c06bc94`) + historial completo `--all`  
**Alcance:** PM-08 del backlog (`docs/backlog.md` — deuda Sprint 1→Sprint 2). Busca `WIFI_SSID/PASSWORD`, `BACKEND_HOST`, `MQTT_BROKER`, `POSTGRES_PASSWORD`, `TUYA_*`, `TELEGRAM_*` expuestos en commits trackeados.  
**Método:** `git log --all -p -S "WIFI_PASSWORD|FELIPE|2516f751|BACKEND_HOST"` + `grep -R --exclude-dir=.git -n "changeme|FELIPE|2516|password|secret|local_key|device_id|api_key"` + revisión de `.gitignore:48-53` y `env.example:1-49`.

---

## 1. Hallazgos confirmados (severidad alta)

### H-01 — WiFi doméstico hardcodeado en firmware trackeado
- **Archivo:** `firmware/gateway-esp32/gateway-esp32.ino:24-25`
  ```cpp
  #define WIFI_SSID "FELIPE."
  #define WIFI_PASSWORD "2516f751"
  ```
- **Commits que lo introducen y lo mantienen en historial:**
  - `ed557b7 fix(gateway): WiFi FELIPE.-5GHz + timeout 10s` — cambia `AetherNet-LAN/changeme` → `FELIPE.-5GHz/2516f751` (2026-08-26)
  - `ddebd48 fix(gateway): WiFi 2.4GHz FELIPE.` — corrige a `FELIPE./2516f751` (2026-08-26)
  - `c06bc94` HEAD aún contiene ambos valores en claro.
- **Impacto:** password de router doméstico real queda en historial git público si el repo es público; cualquier clon del historial lo conserva aunque se borre del HEAD. Severidad **Alta** (R-12 a crear en `docs/risk-register.md`).
- **LAN IP también hardcodeada:** `gateway-esp32.ino:26,35` `MQTT_BROKER 192.168.1.14` y `BACKEND_HOST 192.168.1.14` (commits `4ac71f1`, `ed557b7`). No es secreto pero acopla despliegue a IP del host `wlp1s0`.

### H-02 — Ausencia de `.env` real es correcta, pero falta `backend/.env.example`
- `.gitignore:37-42` ignora `.env`, `backend/.env`, `automation/.env` correctamente — no hay `.env` trackeado (verificado `find . -name ".env*" -type f` vacío).
- **Deuda:** `backend/.env.example` no existe (solo `env.example` raíz `env.example:1-49`). `backend/app/config.py:7` default `postgresql+asyncpg://aethernet:changeme@...` y `docker-compose.yml:10` `POSTGRES_PASSWORD:-changeme` quedan documentados solo en raíz, rompe `DEVOPS-08` (`docs/backlog.md:44`) "Documentar variables de entorno / .env.example".

---

## 2. No hallazgos (descartados)

- **MQTT/Telegram/Tuya:** `env.example:28-37` solo placeholders `your_bot_token`, `your_device_id`, `your_local_key` — no hay `TUYA_BULB_LOCAL_KEY` ni `TELEGRAM_BOT_TOKEN` reales en `git log -p` ni en `grep -R` fuera de `env.example`.
- **Postgres `changeme`:** `backend/app/config.py:7` y `docker-compose.yml:10` usan `changeme` como default dev local; es intencional y sobreescribible por `POSTGRES_PASSWORD` env. No se considera leak si se documenta en `.env.example` y se fuerza override en prod (a mitigar en DEVOPS-11).
- **Otros archivos con `password`:** solo docs ejemplos (`.agents/skills/.../api-design-template.md:98`) y no credenciales reales.

---

## 3. Recomendaciones inmediatas (para DEVOPS-11)

1. **Extraer a `secrets.h`:** crear `firmware/gateway-esp32/secrets.h.example` + `secrets.h` (gitignored) y mover `WIFI_SSID/PASSWORD/MQTT_BROKER/BACKEND_HOST/PORT` de `gateway-esp32.ino:24-37` a `#include "secrets.h"` con `#ifndef WIFI_SSID #error`. Misma para `firmware/mega-access/src/config.h:48 VALID_PIN` si aplica.
2. **Alternativa PlatformIO:** `firmware/gateway-esp32/platformio.ini:19 build_flags = -DWIFI_SSID=\"${sysenv.WIFI_SSID}\" ...` si se prefiere env sobre header.
3. **Crear `backend/.env.example`** espejo de `env.example:8-48` pero scoped a backend.
4. **Rotación:** si `FELIPE./2516f751` es red doméstica productiva, rotar password en router antes o junto con ACT-02. Si es red de prueba aislada, basta con purgar del HEAD y documentar que el historial previo queda contaminado (decidir si `git filter-repo --replace-text` + `push --force-with-lease` a las 35 ramas de `docs/branching-strategy.md:27` compensa el coste).

---

## 4. Evidencia reproducible

```bash
git log --all -p -S "2516f751" -- firmware/gateway-esp32/gateway-esp32.ino
grep -R --exclude-dir=.git -n "FELIPE\|2516" .  # solo gateway-esp32.ino:23-25
cat env.example          # placeholders ok
cat .gitignore | grep -A2 "Environment files"  # .env ignorado correctamente
```

**Próximo paso:** marcar ACT-01 como completada en `docs/deuda-sprint1-sprint2.md` y continuar con ACT-02 (DEVOPS-11) según dependencias.
