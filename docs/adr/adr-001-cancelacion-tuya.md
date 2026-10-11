# ADR-001: Cancelación de la integración del bombillo Tuya (`tuya-local`) por políticas de integración (API)

**Estado:** Aceptado — 2026-09-01  
**Decisores:** Equipo AetherNet (LowCode + DevOps)  
**Afecta a:** RF-4.2, HU-02, LOW-01, LOW-04, `automation/flows/intrusion_alert.json`, `docs/hardware-inventory.md`, `docs/architecture.md`  
**Supersede:** Pivot LOW-01 2026-08-31 (skill Alexa/Google evaluada y descartada)

## Contexto

El alcance original (PRD §4 In-Scope, RF-4.2, HU-02) contemplaba un bombillo RGB compatible Tuya/Smart Life controlado **100% LAN** vía `tuya-local` (sin Tuya Cloud) para parpadeo rojo ante intrusión. La viabilidad dependía de extraer `device_id` + `local_key` + `IP` del dispositivo ya emparejado con Smart Life (vía `tinytuya wizard` / `tuya-cli` o Tuya IoT Platform).

En validación Sprint 1 (LOW-01) se evidenciaron dos bloqueos convergentes:

1.  **Barrera técnica:** lote Mercury LB401 `20MER93` no expuso `local_key` estable/persistente en la red de prueba (`tinytuya wizard` falló repetidamente, firmware sin protocolo local 3.3/3.4 auditable).
2.  **Barrera de políticas de integración (API) — motivo determinante:** el flujo oficial para obtener `local_key` exige **cuenta propietaria en Tuya IoT Platform**, aceptar términos de API propietaria, y mantener credenciales cloud para re-extraer claves tras re-emparejes. Esto viola **RNF-3.1 (100% FOSS, sin nubes propietarias)** y introduce dependencia de proveedor (vendor lock-in) incompatible con el principio fundacional del PRD (§1, §5 Cumplimiento FOSS 100%). Incluso la alternativa `tuya-local` sigue requiriendo ese paso propietario inicial, por lo que no cumple el criterio FOSS estricto.

Node-RED ya tenía implementado el flujo `tuya-bulb-alert → tuya-local-send` (`automation/flows/intrusion_alert.json:75`), pero nunca pasó a validación E2E (LOW-04/LOW-05).

## Decisión

**Cancelar definitivamente** bombillo Tuya y toda integración `tuya-local` / Tuya Cloud API. Efectos:

-   **RF-4.2 → Won't (esta iteración).** Se mantiene documentado como cancelado en `docs/requirements.md:32`, no se elimina del spec para trazabilidad.
-   **HU-02 simplificada:** `Dado armado / Cuando láser KY-008 se interrumpe / Entonces Telegram (RF-4.1) + LED RGB local en rojo (MEGA)` — sin bombillo (`docs/requirements.md:69`, `docs/architecture.md:90`).
-   **LOW-01, LOW-04 → W (Won't).** LOW-05 renombrado a “Cadena completa sin bombillo” (`docs/backlog.md:55`, `docs/materias/backlog-lowcode.md:22`).
-   **Hardware:** tabla `docs/hardware-inventory.md:14` pasa a fila tachada con referencia a este ADR; único indicador visual vigente es LED RGB del MEGA (`hardware-inventory.md:18`).
-   **Automatización:** `automation/flows/intrusion_alert.json` simplificado a `mqtt-intrusion → function-parse → telegram-alert` + `debug` (se eliminan nodos `tuya-bulb-alert`, `tuya-local-send`, `debug-tuya`). `automation/package.json` elimina `node-red-contrib-tuya-local`.
-   **Riesgos:** R-01 y R-07 pasan a **Cerrado** con motivo “políticas API + local_key” (`docs/risk-register.md:16`), no “incompatibilidad técnica” sola.
-   No se reemplaza por skill Alexa/Google Home (evaluado 2026-08-31 y descartado por igualmente violar RNF-3.1 y añadir nube propietaria).

## Alternativas consideradas

| Alternativa | Por qué se descartó |
|---|---|
| Reintentar extracción `local_key` con otra cuenta Tuya IoT Platform | Mantiene dependencia propietaria; viola RNF-3.1 aunque funcione técnicamente |
| Reflasheo Tasmota/ESPHome | Hardware LB401 no confirmado como ESP reinscribible; riesgo de brick y costo de reemplazo sin garantía de soporte |
| Cambiar a bombillo compatible Tasmota de fábrica | Fuera de presupuesto/tiempo Sprint 2-4; introduce nuevo riesgo de procurement (R-01 bis) |
| Skill Alexa / Google Home (pivot 2026-08-31) | También nube propietaria, viola RNF-3.1, requiere certificación y cuenta vinculada; descartado explícitamente en este ADR |

## Consecuencias

-   **Positivas:** se preserva RNF-3.1 100% FOSS; se elimina variable `TUYA_*` de `env.example`; se reduce superficie de fallo (sin LAN extra al bombillo); HU-02 sigue cumplible con dos capas independientes (LED local determinista + Telegram).
-   **Negativas:** se pierde indicador ambiental de habitación completa (bombillo); se asume que LED RGB local es suficiente para evaluación. Si en iteración futura el proyecto acepta excepción FOSS, reintroducir requeriría nuevo ADR que superseda este.
-   **Deuda documental:** referencias históricas con `CANCELADO 2026-09-01` se conservan intencionalmente como rastro. Búsqueda `grep -i tuya` debe devolver solo líneas marcadas `CANCELADO` o `Tuya Cloud` genérico (lista FOSS) — ver §Verificación.

## Referencias

-   `docs/prd.md:32` — alcance marcado Won't
-   `docs/requirements.md:32` (RF-4.2) + `69` (BDD HU-02)
-   `docs/hardware-inventory.md:14,18`
-   `docs/architecture.md:90,103`
-   `docs/backlog.md:51,55,58` + `docs/sprints.md:23,43,56`
-   `docs/risk-register.md:16` R-01 (políticas API)
-   `docs/materias/backlog-lowcode.md:5,22` + `automation/flows/intrusion_alert.json`

## Verificación de cierre

-   `grep -r -i tuya --include="*.md,*.json" . | grep -v ".git|.venv" | grep -v "CANCELADO|cancelado|Tuya Cloud"` → 0 líneas activas (solo trazas canceladas permitidas)
-   `python -m json.tool automation/flows/intrusion_alert.json` → válido, 7 nodos
-   `grep bombillo` → solo líneas con `CANCELADO` o históricas de emails docentes (no reescritos)
