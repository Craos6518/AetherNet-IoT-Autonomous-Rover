# Roadmap — AetherNet IoT & Autonomous Rover

Organizado por materia (5º semestre UTP). Cada bloque indica: conocimientos previos esperados, lo que hay que aprender/reforzar sobre la marcha, en qué sprint se despliega, y qué Historias de Usuario / Requisitos habilita. Referencias cruzadas a `docs/requirements.md` y `docs/sprints.md`.

> Este archivo responde a "qué necesito saber para desplegar esto", no repite el qué (eso ya está en `prd.md`/`requirements.md`).

---

## 1. Programación Para Móviles — App "AetherControl"

**Conocimientos previos esperados**
- Kotlin básico (sintaxis, null-safety, coroutines a nivel conceptual).
- Fundamentos de arquitectura MVVM (separación View / ViewModel / Model).

**Conocimientos a adquirir**
- Jetpack Compose (composables, `State`/`StateFlow`, recomposición) — necesario para RF-1.1 (Dashboard) y RF-1.2 (Joystick).
- `Coroutines` + `Flow` para consumir streams de telemetría sin bloquear el hilo principal.
- Cliente MQTT en Android (librería tipo Eclipse Paho o HiveMQ) y/o WebSockets con `Retrofit`/`OkHttp`.
- Bluetooth API clásica (SPP) para el fallback de RF-1.3 — distinto del BLE, ojo con esa confusión común.
- Manejo de permisos en runtime (Android 12+): `BLUETOOTH_CONNECT`, `ACCESS_FINE_LOCATION` para escaneo BT.

**Se despliega en:** Sprint 2 (pantallas base de luces/bombillo) → Sprint 3 (Joystick + telemetría).

**Habilita:** RF-1.1, RF-1.2, RF-1.3, HU-01 (interfaz de desbloqueo).

---

## 2. DevOps — Infraestructura Containerizada & CI/CD

**Conocimientos previos esperados**
- Docker básico (`Dockerfile`, `docker-compose.yml`).
- Git/GitHub (branches, PRs).

**Conocimientos a adquirir**
- Orquestación con `docker-compose` de múltiples servicios interdependientes (FastAPI + PostgreSQL + Mosquitto) — RNF-1.1.
- Configuración de broker Mosquitto MQTT (topics, ACLs, persistencia) — usado por RF-2.1 y RF-4.1.
- GitHub Actions: sintaxis de workflows, runners, cacheo de dependencias.
- `arduino-cli`: compilación headless de sketches C++, gestión de boards/cores por línea de comandos — necesario para RNF-1.2.
- Testing: PyTest para FastAPI, Jest si la app usa alguna capa JS (o solo JUnit/Kotlin test si es 100% nativo).
- Nociones de redes LAN/subnetting — todo el sistema depende de estar en la misma subred (ver restricción en `prd.md`, sección 6).

**Se despliega en:** Sprint 1 (fundacional — todo lo demás depende de esto).

**Habilita:** RNF-1.1, RNF-1.2; es prerrequisito técnico de prácticamente todas las demás materias.

---

## 3. Automatizaciones y LowCode — Motor de Reglas & Notificaciones

**Conocimientos previos esperados**
- Conceptos de eventos/webhooks.
- JSON básico (los flujos de Node-RED se exportan/importan como JSON).

**Conocimientos a adquirir**
- **Telegram Bot API directo (vigente Sprint 2-4):** creación del bot vía BotFather, envío HTTP `POST https://api.telegram.org/bot<token>/sendMessage` con `chat_id` + `parse_mode Markdown` — sin Node-RED (ver `automation/flows/intrusion_alert.json` como referencia JSON exportable).
- **Node-RED — DEUDA TÉCNICA 2026-09-09:** flujos, nodos `mqtt in`/`mqtt out`, function nodes JS, debug/inject — **no se deploya esta iteración** por indicación asesor (ver `docs/sprints.md:42`, `docs/backlog.md:56` LOW-02 Won't; `architecture.md` §3/§5). El flujo `automation/flows/intrusion_alert.json` (`mqtt-intrusion` → `function-parse-intrusion` → `telegram-alert` → `http-telegram`) queda como referencia versionada para retomar en Sprint 4 si cambia la directriz.
- ~~`tuya-local`~~ — **CANCELADO 2026-09-01** (ADR-001, R-01 políticas API propietaria — viola RNF-3.1). Ya no se requiere `local_key`; notificación solo LED RGB local + Telegram directo.
- Home Assistant (opcional según cuánto se use como capa intermedia vs. Node-RED puro — también en deuda si Node-RED no se retoma).

**Se despliega en:** Sprint 4 (Telegram directo), Node-RED **deuda** (no Sprint 2-3).

**Habilita:** RF-4.1, HU-02 (Telegram + LED RGB local). RF-4.2 cancelado.

**Notas:** 2026-09-01 riesgo `tuya-local` cerrado (ADR-001, R-01) + 2026-09-09 LOW-02 Node-RED deuda — HU-02 ahora Telegram HTTP directo, no vía broker Node-RED.

---

## 4. Administración y Planeación de Proyectos — Gestión Metodológica

**Conocimientos previos esperados**
- Fundamentos de Scrum (roles, ceremonias, backlog).

**Conocimientos a adquirir**
- Redacción de Historias de Usuario con formato INVEST + criterios de aceptación BDD (`Dado/Cuando/Entonces`) — ya aplicado en `requirements.md`, pero el equipo debe poder extender el patrón para nuevas HU.
- WBS (Work Breakdown Structure) para descomponer cada sprint en tareas verificables.
- Matriz de riesgos — particularmente relevante para riesgos de integración hardware/software.
- Planning Poker para estimación relativa entre tareas de firmware, app y backend (que tienen complejidades muy distintas entre sí).
- Diagrama de Gantt para visualizar dependencias entre sprints (ver `docs/sprints.md`, sección "Depende de").

**Se despliega en:** transversal — arranca antes del Sprint 1 y se actualiza en cada sprint.

**Habilita:** la trazabilidad HU ↔ RF ↔ Sprint que usan `AGENTS.md` y `docs/sprints.md` para que un agente de código no trabaje a ciegas.

---

## 5. Estadística — Filtrado en Tiempo Real & Analítica

**Conocimientos previos esperados**
- Estadística descriptiva básica (media, varianza, desviación estándar).
- Python básico (o R) para manipulación de datos.

**Conocimientos a adquirir**
- Media Móvil Exponencial (EMA): entender el rol de `α = 2/(N+1)` en el trade-off entre suavizado y latencia de respuesta — se usa con `α = 0.2` según HU-03. Prototipado en `stats/ema_filter.py:15` y validado en banco `firmware/test-ema-uno` + `notebooks/EMA_Estadistica.ipynb:2` + validación externa 36.5k `stats/water_turbidity_analysis.py:1` (`water_level_turbidity` 31.5k CC BY-SA 4.0 + `gesture` 5k CC0, ver `stats/Dataset/README.md` / `docs/Estadistica/Datasets/README.md`).
- Filtro de Kalman (mencionado como alternativa/complemento a EMA en la matriz del PDF) — al menos su intuición conceptual (predicción + corrección) aunque se implemente la versión EMA.
- Pandas/SciPy para análisis descriptivo e inferencial sobre los datos históricos almacenados en PostgreSQL + validación externa (`stats/materias/estadistica.md`, `notebooks/EMA_Estadistica.ipynb:6` barrido α Monte Carlo 100×, `stats/water_turbidity_analysis.py` Welch `t-Student` + ANOVA sobre `us_value` vs `water_level` por turbidez/angle).
- Prueba de hipótesis $t$-Student de dos muestras (RF vs. Wi-Fi) — plantear correctamente $H_0$/$H_1$, verificar supuestos (normalidad, varianzas) antes de aplicarla (`stats/notebooks/README.md` espejo de `notebooks/`).
- Conexión Python → PostgreSQL (`psycopg2`/`SQLAlchemy`) para extraer el histórico de eventos (`stats/visualize_ema.py`, `stats/serial_plot_ema.py`).

**Notebooks centralizados:** `notebooks/EMA_Estadistica.ipynb` (canónico, ver `notebooks/README.md` y `docs/notebooks/README.md`; espejo `stats/notebooks/` no editar) + datasets externos 36.5k (`stats/Dataset/` canónico, espejo `docs/Estadistica/Datasets/`, reporte `stats/data/water_turbidity_report.json` + PNGs `water_us_vs_true.png`/`water_ir_by_angle.png`).

**Se despliega en:** Sprint 4 (aunque el diseño del algoritmo puede prototiparse desde antes, en paralelo al Sprint 1-2 — **ya adelantado:** `stats/ema_filter.py` Sprint 1-2 + `water_turbidity_analysis.py` 36.5k validación externa §7c).

**Habilita:** RNF-2.1, RNF-2.2, HU-03; condiciona directamente el KPI "Precisión del Filtro Estadístico > 85%" de `prd.md`.

---

## Vista consolidada: conocimiento transversal (no ligado a una sola materia)

- **C++ para microcontroladores** (Arduino UNO/MEGA, ESP32/ESP8266): interrupciones, lectura analógica/digital, comunicación serial. Es la base común de DevOps (CI/CD del firmware), Estadística (dónde corre el EMA) y Automatizaciones (eventos que disparan Telegram directo; Node-RED en deuda).
- **Protocolo MQTT**: entender pub/sub, topics y QoS es necesario para entender cómo se comunican App, Backend, Gateway (ESP32↔MEGA UART) y Rover (nRF24L01) entre sí — topics canónicos `aethernet/#` (`acl.conf:1`, `architecture.md` §7).
- **Redes LAN**: todo el sistema (App, ESP32/ESP8266, servidor Docker) vive en la misma subred LAN.

## Orden sugerido de aprendizaje (si el equipo parte de cero)

1. Docker + Git (DevOps) — sin esto no se puede levantar nada localmente.
2. C++ básico en Arduino + protocolo UART/RF — para tener algo físico funcionando pronto.
3. MQTT — es el "idioma común" que conecta casi todos los componentes.
4. Kotlin/Compose — en paralelo al punto 3, ya que la app es el punto de entrada visible para evaluadores.
5. Telegram Bot directo (sin Node-RED esta iteración — deuda 2026-09-09) — una vez los eventos ya existen (de los pasos 2-3).
6. Estadística aplicada (EMA, descriptivo, t-Student/Welch) — al final, cuando ya hay datos reales fluyendo para analizar (+ validación externa 36.5k ya disponible).
