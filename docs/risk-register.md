# Matriz de Riesgos — AetherNet IoT & Autonomous Rover

Resuelve `PM-03` del backlog. Consolida los riesgos ya identificados de forma dispersa en `hardware-inventory.md`, `roadmap.md`, `sprints.md` y `backlog.md`, con probabilidad, impacto y plan de mitigación explícitos.

**Escala:**
- **Probabilidad:** Baja / Media / Alta
- **Impacto:** Bajo / Medio / Alto / Crítico (crítico = bloquea una HU o KPI completo del PRD)
- **Severidad** = Probabilidad × Impacto (usada para ordenar la tabla)

---

## Riesgos críticos (revisar en cada retro de sprint)

| ID | Riesgo | Probabilidad | Impacto | Severidad | Mitigación | Dueño | Sprint de validación |
|---|---|---|---|---|---|---|---|
| R-01 | El bombillo Mercury LB401 (lote 20MER93) no expone protocolo `tuya-local` en su firmware actual — BLOQUEADO 2026-08-31, 2da opción en evaluación | Alta | Crítico (bloquea HU-02, RF-4.2 completos) | **Alta** | `tinytuya wizard` bloqueado por acceso difícil al `local_key` (confirmado 2026-08-31). Plan B oficial: **skill Alexa / Google Home como alternativa a `tuya-local`** (si se confirma incompatibilidad, retirar LOW-01 de FOSS-LAN y reemplazar por integración vía skill en la nube — documentar excepción a RNF-3.1 — o cambiar bombillo a ref. compatible Tasmota/ESPHome). Mantener `tuya-local` como preferido si se logra acceso; si no, pivotar a skill en Sprint 4 (LOW-04). | Área LowCode (LOW-01) | Sprint 1→2 (pivot 2026-08-31) |
| R-02 | Retraso en Sprint 1 (Docker + CI/CD + enlace RF ESP32↔UNO) | Media | Crítico (bloquea Móviles, LowCode y Estadística en cascada) | **Alta** | Priorizar DEVOPS-01 a DEVOPS-05 sobre cualquier otra tarea; no iniciar Sprint 2 sin Sprint 1 cerrado | Área DevOps | Continuo (Sprint 1) |
| R-03 | Prototipo del filtro EMA (EST-01) se deja para el cierre y el α=0.2 resulta mal calibrado para el HC-SR04 real | Media | Alto (compromete KPI "Precisión del Filtro >85%") | Media-Alta | Adelantar EST-01 con datos simulados a Sprint 1-2, en paralelo a DevOps | Área Estadística | Sprint 1-2 |

## Riesgos técnicos (hardware / firmware)

| ID | Riesgo | Probabilidad | Impacto | Severidad | Mitigación | Dueño |
|---|---|---|---|---|---|---|
| R-04 | Interferencia o alcance insuficiente del enlace RF nRF24L01 (2.4 GHz) entre Gateway y Rover | Media | Alto (RF-3.1, latencia <10ms del PRD) | Media | Probar el enlace temprano (DEVOPS-05, Sprint 1) en el entorno real, no solo en banco de pruebas | Firmware/DevOps |
| R-05 | Falsos positivos/negativos del láser KY-008 por desalineación física | Baja | Alto (KPI "Tasa de Falsos Positivos = 0%") | Media | Fijación mecánica robusta del emisor/receptor; prueba de estabilidad antes de considerar HU-02 cerrada | Firmware (MEGA) |
| R-06 | Autonomía de batería del Rover insuficiente durante pruebas/sustentación | Media | Medio (limita demostración, no bloquea desarrollo) | Media | Medir consumo real temprano en Sprint 3; tener baterías de repuesto cargadas el día de evaluación | Área Rover |
| R-07 | El bombillo Tuya requiere estar vinculado a una cuenta de Tuya IoT Platform para obtener el `local_key` inicial, lo que roza la restricción 100% FOSS/sin nube (RNF-3.1) | Alta | Bajo (es un paso único de configuración, no una dependencia en tiempo de ejecución) | Media | Documentar explícitamente que la cuenta Tuya se usa solo una vez para extraer credenciales, y que en producción el bombillo opera 100% LAN sin la nube | Área LowCode |

## Riesgos de red / infraestructura

| ID | Riesgo | Probabilidad | Impacto | Severidad | Mitigación | Dueño |
|---|---|---|---|---|---|---|
| R-08 | Dispositivos fuera de la subred LAN esperada (restricción de `prd.md` §6) | Baja | Alto (rompe MQTT, tuya-local y RF simultáneamente) | Media | Fijar IPs por DHCP reservation en el router antes de Sprint 2 | DevOps |
| R-09 | Caída del router principal / pérdida de Internet | Media | Bajo (mitigado por diseño: acceso Edge en el MEGA sigue operando) | Baja | Ya mitigado por arquitectura (ver `prd.md` §6, Contingencia); solo verificar que Telegram/tuya-local fallen de forma controlada, no silenciosa | DevOps |
| R-12 | Credenciales WiFi hardcodeadas en historial git (`FELIPE./2516f751` en `gateway-esp32.ino:24-25`, commits `ed557b7/ddebd48`) — PM-08 H-01 — **RESUELTO 2026-08-31** | Baja (histórico) | Alto (histórico) | **Resuelto** | ACT-02 DEVOPS-11: `firmware/gateway-esp32/secrets.h` (gitignoreado) + `secrets.h.example`; fallback `__has_include` con defaults `AetherNet-LAN/changeme` para CI. **2026-08-31:** producción rotada (password prod cambiado); AP lab `FELIPE./2516f751` recreado idéntico para compatibilidad local — prod ya no expuesto. No requiere `filter-repo` — ver `docs/auditoria-secretos-sprint1.md:10` §5 y `docs/deuda-sprint1-sprint2.md:36` | DevOps | Resuelto 2026-08-31 |
| R-13 | IP LAN hardcodeada (`192.168.1.14/100` en `gateway-esp32.ino:26,35`) acopla firmware a host `wlp1s0` y rompe CI/docker | Media | Medio (falla `POST /api/access-events` fuera de LAN de prueba) | Media | Mover `MQTT_BROKER/BACKEND_HOST` a `secrets.h` y `env.example:48`/`backend/.env.example:9`; usar DHCP reservation o mDNS (`aethernet.local`) antes de Sprint 2 | DevOps | Sprint 2 |

## Riesgos de gestión / proyecto

| ID | Riesgo | Probabilidad | Impacto | Severidad | Mitigación | Dueño |
|---|---|---|---|---|---|---|
| R-10 | `docs/sprints.md` ("Estado actual") no se actualiza al cierre de cada sprint | Media | Medio (colaboradores y agentes de IA trabajan con info desactualizada) | Media | PM-05 como checklist obligatorio de cierre de sprint | Gestión de proyecto |
| R-11 | Complejidad muy dispar entre tareas de firmware, app y backend genera estimaciones poco realistas | Media | Medio | Media | Planning Poker (PM-07) si un sprint se atrasa >20% | Gestión de proyecto |

---

## Resumen para sustentación / evaluación

Los **tres riesgos de severidad Alta vigentes** (R-01 bloqueado, R-02, R-03) son los que más comprometen los KPIs del PRD: R-12 era Alta y quedó **Resuelto 2026-08-31** (producción rotada + AP lab `FELIPE./2516f751` recreado, `gateway-esp32.ino:20-51` sin hardcode + `secrets.h`); antes Media tras mitigación DEVOPS-11 (ver `docs/auditoria-secretos-sprint1.md:15` §5). R-01 sigue Alta pero con pivot a skill Alexa/Google Home como alternativa oficial a `tuya-local`.

## Cómo se relaciona con el resto de la documentación

- Esta matriz formaliza riesgos que ya estaban dispersos como notas en `hardware-inventory.md` (nota de diseño LED/Tuya), `roadmap.md` (riesgo `tuya-local`) y `backlog.md` (sección "Resumen de riesgos críticos").
- Debe revisarse en cada retro de sprint (`PM-06`) y actualizarse si cambia la probabilidad/impacto de algún ítem, o si aparece un riesgo nuevo.
