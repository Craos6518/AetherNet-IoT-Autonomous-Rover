# Plan de Pruebas — AetherNet IoT & Autonomous Rover

Estrategia de validación por subsistema. Los criterios de aceptación BDD (`Dado/Cuando/Entonces`) de `requirements.md` son la fuente de verdad para las pruebas funcionales — este documento no los repite, define **cómo y con qué herramienta** se verifica cada uno, y agrega las pruebas no funcionales que el PRD exige (latencia, precisión del filtro, falsos positivos).

---

## 1. Niveles de prueba

| Nivel | Qué cubre | Dónde vive | Herramienta |
|---|---|---|---|
| Unitarias | Funciones aisladas (ViewModels, endpoints, funciones de firmware) | `app/`, `backend/`, `firmware/*/test/` | JUnit (Kotlin), PyTest (FastAPI) |
| Integración | Comunicación entre 2 subsistemas (ej. MEGA↔Gateway por UART) | Bancos de prueba físicos | Manual + logging serial |
| End-to-end (E2E) | Flujo completo HU-01/HU-02/HU-03/HU-04 de punta a punta | Sistema montado completo | Manual, checklist |
| No funcional | Latencia, precisión estadística, falsos positivos (KPIs del PRD) | Sistema montado completo | Scripts de medición (`stats/`) |

## 2. Pruebas por Historia de Usuario (HU)

### HU-01 — Control de Acceso por Teclado

| Caso | Tipo | Cómo se valida |
|---|---|---|
| PIN correcto + `#` → servo gira 90°, LED verde, acceso registrado en BD | E2E | Checklist manual con multímetro/observación directa del servo y LED; verificar registro vía query directa a PostgreSQL o endpoint de consulta |
| PIN incorrecto → no se acciona el servo | E2E | Igual al anterior, variando el input del teclado |
| Servo no responde si el ESP32 está caído (procesamiento Edge) | Integración | Desconectar Wi-Fi del ESP32 y repetir el caso anterior — debe seguir funcionando (ver `prd.md` §6, Contingencia) |
| Endpoint de registro de acceso (`DEVOPS-06`) | Unitaria | PyTest: request válido/ inválido, verificar status code y persistencia |
| ViewModel de pantalla de PIN (`MOV-04`) | Unitaria | JUnit: estados de éxito/error del envío de PIN |

### HU-02 — Alerta de Intrusión (Trampa Láser)

| Caso | Tipo | Cómo se valida |
|---|---|---|
| Interrupción del láser → LED local rojo (sin red) | Integración | Cortar la barrera con Wi-Fi del ESP32 desconectado — el LED debe encender igual |
| Interrupción del láser → mensaje en Telegram | E2E | Cortar la barrera con el sistema completo activo, verificar llegada del mensaje y medir tiempo transcurrido |
| ~~Interrupción del láser → bombillo Tuya~~ — cancelado | — | Cancelado 2026-09-01 (R-01). Intrusión se verifica por LED RGB rojo + Telegram |
| Tasa de falsos positivos del láser | No funcional | Dejar el sistema armado N horas sin interrupción real; contar alertas disparadas — objetivo del PRD: 0% |
| Flujo Node-RED de intrusión completo (`LOW-05`) | Integración | Simular publicación del evento MQTT manualmente (ej. con `mosquitto_pub`) y verificar que Node-RED reacciona sin depender del hardware físico |

### HU-03 — Filtrado Estadístico de Telemetría

| Caso | Tipo | Cómo se valida |
|---|---|---|
| Filtro EMA (α=0.2) estabiliza lecturas del HC-SR04 | Unitaria/offline | `EST-01`: comparar señal cruda vs. filtrada con datos simulados en Python antes de portar a firmware |
| Reducción de ruido > 85% (KPI del PRD) | No funcional | `EST-06`: análisis descriptivo (varianza antes/después) sobre datos reales ya persistidos |
| Prueba t-Student, latencia RF vs. Wi-Fi | No funcional | `EST-05`: recolectar muestras de ambos protocolos con el método de §3, verificar supuestos (normalidad, homocedasticidad) antes de aplicar la prueba, documentar H0/H1 y resultado |
| El Rover no reacciona a falsos obstáculos tras aplicar el filtro | E2E | Generar ruido controlado frente al HC-SR04 y verificar que el Rover no frena/gira innecesariamente |

### HU-04 — Fail-safe del Rover ante pérdida de enlace RF

| Caso | Tipo | Cómo se valida |
|---|---|---|
| Rover en movimiento, se apaga el transmisor del Gateway → Rover se detiene dentro de la ventana de timeout (300-500 ms) | E2E | Cronómetro/video: medir tiempo desde el corte de la señal hasta que ambos motores quedan en 0 PWM |
| Rover detenido por fail-stop no reanuda movimiento por sí solo | E2E | Verificar que permanece detenido hasta recibir un nuevo paquete RF válido, no reintenta el último comando |
| Rover recibe un paquete válido después del fail-stop → retoma control normal | Integración | Reconectar el transmisor y confirmar que responde al siguiente comando sin necesidad de reiniciar el firmware |
| Umbral de timeout (300-500 ms) no genera falsos fail-stops en operación normal | No funcional | Operar el Rover con enlace RF estable durante una sesión de prueba y confirmar 0 detenciones espurias |

## 3. Pruebas no funcionales — KPIs del PRD

| KPI (PRD §5) | Objetivo | Cómo se mide | Cuándo |
|---|---|---|---|
| Latencia MQTT/Wi-Fi | < 50 ms | Ping-pong por MQTT: el script en `stats/` publica un mensaje con ID único y timestamp de envío; un suscriptor responde en el mismo topic (o uno de respuesta); se mide el Round-Trip Time (RTT) en el mismo reloj (el del script) y se reporta latencia ≈ RTT/2 | Sprint 4 (junto a EST-05) |
| Latencia RF (Gateway↔Rover) | < 10 ms | Mismo principio: el Gateway guarda el timestamp local al enviar un comando RF y mide el RTT hasta recibir el ACK/telemetría de vuelta del Rover; latencia ≈ RTT/2. No requiere reloj sincronizado entre dispositivos porque todo se mide desde el reloj del Gateway | Sprint 3 |
| Precisión del filtro estadístico | Reducción de ruido > 85% | Comparación de varianza cruda vs. filtrada (`stats/`) | Sprint 4 |
| Falsos positivos del láser | 0% | Conteo durante ventana de prueba prolongada (ver HU-02 arriba) | Sprint 2 en adelante |
| Cumplimiento FOSS | 100% | Auditoría manual de dependencias (`pip freeze`, `package.json` si aplica, librerías Arduino) contra licencias — ninguna debe requerir pago/licencia comercial | Antes de cierre (Sprint 4) |

> **Nota de método:** se usa RTT/2 en lugar de comparar timestamps absolutos de dos dispositivos porque el Gateway, el Rover y el backend no tienen reloj sincronizado (no hay NTP entre microcontroladores). Medir desde un único reloj (el que envía y recibe) evita ese problema sin necesidad de infraestructura adicional.

## 4. Pruebas de contingencia (resiliencia)

| Escenario | Resultado esperado | Referencia |
|---|---|---|
| Se cae el contenedor Docker del backend | MEGA sigue controlando acceso físico; app muestra estado "desconectado" sin crashear | `prd.md` §6 |
| Se cae el router / Internet | Notificaciones Telegram fallan; acceso físico (MEGA + LED RGB) sigue operando | `prd.md` §6, R-09 |
| Falla el enlace RF Gateway↔Rover | Rover ejecuta fail-stop (ver HU-04 arriba) | `requirements.md` RF-3.3, `architecture.md` §6 |

## 5. Qué queda fuera de este plan

- Pruebas de carga/estrés (no aplica: es un sistema de una sola habitación, no un servicio con múltiples usuarios concurrentes).
- Pentesting formal de seguridad (fuera de alcance del PRD; ver nota en `roadmap.md` sobre riesgos, no hay RF/RNF que lo exija explícitamente — señalar al equipo si se considera necesario agregarlo).

## 6. Pendiente de definición

- Valor final calibrado del timeout de fail-stop (rango de referencia: 300-500 ms) — debe ajustarse empíricamente en Sprint 3 según la tasa real de pérdida de paquetes RF observada.
