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
| R-01 | ~~Bombillo Mercury LB401 `tuya-local`~~ — **CANCELADO 2026-09-01**: políticas de integración (API) + `local_key` inaccesible | — | — | **Cerrado** | Decisión ADR-001: API Tuya exige cuenta propietaria en Tuya IoT Platform y expone `local_key` bajo términos no FOSS; viola RNF-3.1 (100% FOSS, sin nube propietaria) y añade dependencia de nube externa. Firmware LB401 no expone protocolo local auditable. Se descarta RF-4.2 → Won't; HU-02 se cumple con LED RGB local + Telegram. Ver `docs/adr/adr-001-cancelacion-tuya.md` y `docs/hardware-inventory.md` | Área LowCode | 2026-09-01 |
| R-02 | Retraso en Sprint 1 (Docker + CI/CD + enlace RF ESP32↔UNO) | Media | Crítico (bloquea Móviles, LowCode y Estadística en cascada) | **Alta** | Priorizar DEVOPS-01 a DEVOPS-05 sobre cualquier otra tarea; no iniciar Sprint 2 sin Sprint 1 cerrado | Área DevOps | Continuo (Sprint 1) |
| R-03 | Prototipo del filtro EMA (EST-01) se deja para el cierre y el α=0.2 resulta mal calibrado para el HC-SR04 real | Media | Alto (compromete KPI "Precisión del Filtro >85%") | Media-Alta | Adelantar EST-01 con datos simulados a Sprint 1-2, en paralelo a DevOps | Área Estadística | Sprint 1-2 |

## Riesgos técnicos (hardware / firmware)

| ID | Riesgo | Probabilidad | Impacto | Severidad | Mitigación | Dueño |
|---|---|---|---|---|---|---|
| R-04 | Interferencia o alcance insuficiente del enlace RF nRF24L01 (2.4 GHz) entre Gateway y Rover | Media | Alto (RF-3.1, latencia <10ms del PRD) | Media | Probar el enlace temprano (DEVOPS-05, Sprint 1) en el entorno real, no solo en banco de pruebas | Firmware/DevOps |
| R-05 | Falsos positivos/negativos del láser KY-008 por desalineación física | Baja | Alto (KPI "Tasa de Falsos Positivos = 0%") | Media | Fijación mecánica robusta del emisor/receptor; prueba de estabilidad antes de considerar HU-02 cerrada | Firmware (MEGA) |
| R-06 | Autonomía de batería del Rover insuficiente durante pruebas/sustentación | Media | Medio (limita demostración, no bloquea desarrollo) | Media | Medir consumo real temprano en Sprint 3; tener baterías de repuesto cargadas el día de evaluación | Área Rover |
| R-07 | ~~Bombillo Tuya / cuenta Tuya IoT Platform~~ — **CANCELADO 2026-09-01** (mismo motivo R-01) | — | — | **Cerrado** | Sin objeto tras cancelación | Área LowCode |

## Riesgos de red / infraestructura

| ID | Riesgo | Probabilidad | Impacto | Severidad | Mitigación | Dueño |
|---|---|---|---|---|---|---|
| R-08 | Dispositivos fuera de la subred LAN esperada (`prd.md` §6) | Baja | Alto (rompe MQTT y RF) | Media | Fijar IPs por DHCP reservation antes de Sprint 2 | DevOps |
| R-09 | Caída del router / pérdida de Internet | Media | Bajo (mitigado: acceso Edge MEGA sigue operando) | Baja | Ya mitigado (`prd.md` §6); solo verificar que Telegram falle de forma controlada, no silenciosa (tuya-local ya cancelado) | DevOps |

## Riesgos de gestión / proyecto

| ID | Riesgo | Probabilidad | Impacto | Severidad | Mitigación | Dueño |
|---|---|---|---|---|---|---|
| R-10 | `docs/sprints.md` ("Estado actual") no se actualiza al cierre de cada sprint | Media | Medio (colaboradores y agentes de IA trabajan con info desactualizada) | Media | PM-05 como checklist obligatorio de cierre de sprint | Gestión de proyecto |
| R-11 | Complejidad muy dispar entre tareas de firmware, app y backend genera estimaciones poco realistas | Media | Medio | Media | Planning Poker (PM-07) si un sprint se atrasa >20% | Gestión de proyecto |

---

## Resumen para sustentación / evaluación

~~R-01 cancelado.~~ Los **dos riesgos restantes de severidad Alta** (R-02, R-03) son los que más comprometen los KPIs del PRD y deben mencionarse explícitamente si un evaluador pregunta por gestión de riesgos: dos de ellos ya tienen mitigación en curso desde Sprint 1 según el backlog actualizado.

## Cómo se relaciona con el resto de la documentación

- Esta matriz formaliza riesgos que ya estaban dispersos como notas en `hardware-inventory.md` (nota de diseño LED/Tuya), `roadmap.md` (riesgo `tuya-local`) y `backlog.md` (sección "Resumen de riesgos críticos").
- Debe revisarse en cada retro de sprint (`PM-06`) y actualizarse si cambia la probabilidad/impacto de algún ítem, o si aparece un riesgo nuevo.
