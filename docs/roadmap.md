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
- Configuración de broker Mosquitto MQTT (topics, ACLs, persistencia) — usado por RF-2.1 y RF-4.2.
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
- Node-RED: flujos, nodos `mqtt in`/`mqtt out`, function nodes en JavaScript, debug/inject nodes.
- Telegram Bot API: creación del bot vía BotFather, envío de mensajes vía HTTP request node.
- **`tuya-local`**: cómo obtener el `local_key` y `device_id` de un dispositivo Tuya (típicamente vía Tuya IoT Platform o herramientas como `tuya-cli`/`tinytuya`), y cómo se integra como nodo o llamada HTTP dentro de Node-RED — crítico porque es la pieza que mantiene el proyecto 100% FOSS sin depender de Tuya Cloud (ver nota de diseño en `docs/hardware-inventory.md`).
- Home Assistant (opcional según cuánto se use como capa intermedia vs. Node-RED puro).

**Se despliega en:** Sprint 4.

**Habilita:** RF-4.1, RF-4.2, HU-02 (bombillo Tuya parpadeando en rojo).

**Riesgo a validar temprano:** no todos los bombillos "compatibles con Smart Life" exponen el protocolo local de `tuya-local`; conviene confirmar el modelo específico del bombillo **antes** de llegar al Sprint 4 (esto ya quedó como pendiente en `docs/hardware-inventory.md`).

---

## 4. Administración y Planeación de Proyectos — Gestión Metodológica

**Conocimientos previos esperados**
- Fundamentos de Scrum (roles, ceremonias, backlog).

**Conocimientos a adquirir**
- Redacción de Historias de Usuario con formato INVEST + criterios de aceptación BDD (`Dado/Cuando/Entonces`) — ya aplicado en `requirements.md`, pero el equipo debe poder extender el patrón para nuevas HU.
- WBS (Work Breakdown Structure) para descomponer cada sprint en tareas verificables.
- Matriz de riesgos — particularmente relevante para riesgos de integración hardware/software (ej. el riesgo de `tuya-local` arriba mencionado).
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
- Media Móvil Exponencial (EMA): entender el rol de `α = 2/(N+1)` en el trade-off entre suavizado y latencia de respuesta — se usa con `α = 0.2` según HU-03.
- Filtro de Kalman (mencionado como alternativa/complemento a EMA en la matriz del PDF) — al menos su intuición conceptual (predicción + corrección) aunque se implemente la versión EMA.
- Pandas/SciPy para análisis descriptivo e inferencial sobre los datos históricos almacenados en PostgreSQL.
- Prueba de hipótesis $t$-Student de dos muestras (RF vs. Wi-Fi) — plantear correctamente $H_0$/$H_1$, verificar supuestos (normalidad, varianzas) antes de aplicarla.
- Conexión Python → PostgreSQL (`psycopg2`/`SQLAlchemy`) para extraer el histórico de eventos.

**Se despliega en:** Sprint 4 (aunque el diseño del algoritmo puede prototiparse desde antes, en paralelo al Sprint 1-2).

**Habilita:** RNF-2.1, RNF-2.2, HU-03; condiciona directamente el KPI "Precisión del Filtro Estadístico > 85%" de `prd.md`.

---

## Vista consolidada: conocimiento transversal (no ligado a una sola materia)

- **C++ para microcontroladores** (Arduino UNO/MEGA, ESP32/ESP8266): interrupciones, lectura analógica/digital, comunicación serial. Es la base común de DevOps (CI/CD del firmware), Estadística (dónde corre el EMA) y Automatizaciones (eventos que disparan Node-RED.
- **Protocolo MQTT**: entender pub/sub, topics y QoS es necesario para entender cómo se comunican App, Backend, Node-RED y ESP32 entre sí.
- **Redes LAN**: todo el sistema (App, ESP32, ESP8266, bombillo Tuya, servidor Docker) vive en la misma subred — sin este entendimiento, cualquier debugging de conectividad se vuelve adivinanza.

## Orden sugerido de aprendizaje (si el equipo parte de cero)

1. Docker + Git (DevOps) — sin esto no se puede levantar nada localmente.
2. C++ básico en Arduino + protocolo UART/RF — para tener algo físico funcionando pronto.
3. MQTT — es el "idioma común" que conecta casi todos los componentes.
4. Kotlin/Compose — en paralelo al punto 3, ya que la app es el punto de entrada visible para evaluadores.
5. Node-RED + `tuya-local` — una vez los eventos ya existen (de los pasos 2-3).
6. Estadística aplicada (EMA, prueba t) — al final, cuando ya hay datos reales fluyendo para analizar.
