# DevOps Contingencia 10 de Agosto — Presentación y Guion Semana 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Construir una presentación DevOps de 9-10 slides + guion natural de 8-9 min que capture la esencia completa de AetherNet (visión, casos de uso, arquitectura, FOSS, contingencia) sin afirmar trabajo no hecho en semana 1, sin mencionar bombillo Tuya ni Telegram/BotFather sin aprobación LowCode, y anclada al terremoto 7.4 del 10 de agosto en Dosquebradas/Pereira/UTP como origen del proyecto por supuestos propios (cancelación/virtualidad/aplazamiento).

**Architecture:** Narrativa en 3 actos: 1) Contexto humano (sismo → supuestos → nacimiento de la idea después, no antes), 2) Esencia del sistema (qué es AetherNet en 60s: domótica + rover + app + backend local, 100% FOSS, 3 capas en misma LAN, KPIs/RNFs como promesas no como hechos), 3) DevOps como habilitador de contingencia desde día 1 (infra reproducible, CI mínimo, local-first). Visual: tema Navy/Teal/Coral sobre `pptxgenjs` LAYOUT_WIDE (10"×5.625" safe, fallback LAYOUT_WIDE 13.3" si se necesita), 1 motivo repetido (cards con header color + icono en círculo), sin barras decorativas. Generación: un único script Node `tmp/gen_*.js` que produce `docs/DevOps_AetherNet_Contingencia_10Agosto.pptx` + validación `scripts/office/validate.py`.

**Tech Stack:** pptxgenjs (preinstalado, no `npm install` salvo fallo), Node 24, Python validate, `docs/prd.md`, `docs/requirements.md`, `docs/hardware-inventory.md`, `docs/sprints.md`, `docs/roadmap.md`, `docs/architecture.md` como fuentes de verdad, Calibri/Cambria safe fonts.

**Spec:** docs/prd.md §1-6 (visión, usuarios, casos de uso, scope, KPIs, restricciones), docs/requirements.md §2-4 (RF-1.x a RF-3.x, RNF-1.1/1.2/2.1/2.2/3.1, HU-01 a HU-04), docs/hardware-inventory.md (MEGA 2560 + Keypad/Servo/LED 44/45/46/ KY-008 + Rover UNO + ESP32 gateway), docs/sprints.md Sprint1 original + estado actual honesto semana 1, docs/roadmap.md §2 DevOps, restricción usuario: semana 1, proyecto no existía antes del sismo, supuestos propios, NO Tuya, NO Telegram.

## Global Constraints

- Semana 1 honesta: no afirmar endpoints, latencias medidas, o firmware compilado como hecho; todo es propuesta/plan con verbos "proponemos", "validaremos con docente".
- Origen: proyecto nace DESPUÉS del sismo 10 ago 7.4 Dosquebradas/Pereira/UTP por pensamiento propio con supuestos (cancelación semestre / virtualidad / aplazamiento); no existía antes; aclarar "supuestos propios, no oficiales".
- NO mencionar bombillo Tuya / tuya-local / local_key / ADR-001 de forma protagonista; si aparece, solo como "fuera de alcance" en una palabra sin detalle.
- NO mencionar Telegram Bot / BotFather / api.telegram.org / Node-RED como solución; LowCode aún no aprobado → decir "automatización por definir con docente".
- Mantener esencia completa: mostrar visión FOSS 100%, 3 casos de uso (monitoreo/alertas, rover táctico, acceso local), 3 capas, RFs, RNF-1.1 docker-compose, RNF-1.2 arduino-cli CI, restricciones LAN y Edge failover.
- Audiencia: compañeros + docente, tono docente no lector, 8-9 min total, ppt en `docs/DevOps_AetherNet_Contingencia_10Agosto.pptx`.
- Visual: pptxgenjs gotchas: pres.layout antes de slides, colores sin `#` y sin 8 dígitos, transparency separado, objetos options no compartidos, shadow offset ≥0, charSpacing no letterSpacing, bullet true, una instancia pptxgen, rectRadius solo ROUNDED_RECTANGLE, margin 0, validate.py al final.
- Idioma: español, FOSS, sem_LAN.

---

### Task 1: Auditoría de esencia y mapa de narrativa

**Files:**
- Read: `docs/prd.md:1-61`
- Read: `docs/requirements.md:1-87`
- Read: `docs/hardware-inventory.md:1-63`
- Read: `docs/sprints.md:1-20` (Sprint 1 original)
- Read: `docs/roadmap.md:27-45` (DevOps)
- Read: `docs/architecture.md:7-15` (3 capas)
- Create: `docs/superpowers/plans/2026-09-12-devops-contingencia-presentacion.md` (este archivo)

**Interfaces:**
- Consumes: nada previo
- Produces: `esencia_map` = { vision, usuarios, 3 casos de uso, scope in/out, KPIs latencia/EMA/FOSS, RF-1.1/1.2/2.1/2.2/2.3/3.1/3.2/3.3, RNF-1.1/1.2/3.1, HU-01/02/03/04, hardware 3 subsistemas, restricciones LAN+Edge, narrativa 3 actos } que Task 2 usará para ordenar slides

- [ ] **Step 1: Extraer visión y usuarios de prd.md:9-14**

```markdown
Visión: plataforma integral domótica + robótica móvil para seguridad/automatización, demostrar ecosistema robusto/tolerante a fallos/baja latencia 100% FOSS sin nube propietaria.
Usuarios: habitantes (app) + evaluadores/docentes técnicos.
```

- [ ] **Step 2: Listar 3 casos de uso core prd.md:18-22**

```markdown
1) Monitoreo/alertas tiempo real (presencia, ruido, láser → alerta)
2) Control físico/robótico táctico (rover joystick vs autónomo anti-colisión)
3) Gestión acceso local (teclado 4x4 → MEGA → auditoría)
```

- [ ] **Step 3: Anotar alcance honesto semana 1**

```markdown
In: app Kotlin/Compose (por hacer), backend Docker local (propuesto), firmware C++ RF/UART/Wi-Fi (por hacer), EMA (por hacer)
Out: nube pago, iOS, visión artificial
Semana1: nada terminado, solo propuesta; decirlo explícito
```

- [ ] **Step 4: Copiar KPIs y RNFs como promesas**

```markdown
KPIs: Wi-Fi/MQTT <50ms, RF <10ms, EMA >85%, láser 0% falsos, FOSS 100% — presentar como objetivos, no mediciones
RNF-1.1 docker-compose, RNF-1.2 arduino-cli por push, RNF-3.1 FOSS
```

- [ ] **Step 5: Mapear hardware mínimo para esencia sin Tuya**

```markdown
MEGA+keypad+servo+LED 44/45/46+KY-008, ESP32 gateway+ nRF24L01, UNO rover+L298N+HC-SR04+3x TCRT5000
LED RGB es único feedback visual local (no bombillo)
```

- [ ] **Step 6: Commit auditoría**

```bash
git add docs/superpowers/plans/2026-09-12-devops-contingencia-presentacion.md
git commit -m "docs: plan esencia AetherNet semana1 para presentacion DevOps"
```

### Task 2: Diseño de narrativa 3 actos y escaleta de 9 slides

**Files:**
- Modify: `docs/superpowers/plans/2026-09-12-devops-contingencia-presentacion.md`

**Interfaces:**
- Consumes: esencia_map de Task 1
- Produces: `escaleta` = 9 títulos + objetivo + tiempo + qué no decir por slide, que Task 4 y 5 consumirán

- [ ] **Step 1: Definir Acto 1 — Contexto humano (slides 1-2, 2:00)**

```markdown
S1 Portada (0:40): titular + "semana 1 / propuesta / supuestos propios"
S2 10 de agosto (1:20): hecho 7.4 + 4 supuestos propios (cancelación/virtualidad/aplazamiento/entrega sin campus) + "proyecto nace después"
Restricción: no culpar a UTP, aclarar "no oficial"
```

- [ ] **Step 2: Definir Acto 2 — Esencia del sistema (slides 3-5, 3:30)**

```markdown
S3 ¿Qué es AetherNet en 60s? (1:10): 3 casos de uso con iconos, scope in/out honesto
S4 Arquitectura 3 capas (1:10): Edge (MEGA/UNO), Coordinación (ESP32), Interfaz (App) + Backend Docker en misma LAN + restricción Edge sigue sin internet
S5 Por qué FOSS y local-first (1:10): RNF-3.1 + prd §6 contingencia (MEGA sigue aunque caiga router)
```

- [ ] **Step 3: Definir Acto 3 — DevOps como contingencia desde día 1 (slides 6-8, 2:30)**

```markdown
S6 Por qué DevOps en semana 1 (0:50): reproducible/trazable/local-first si nos mandan a virtualidad
S7 Stack propuesto (0:50): docker-compose 3 servicios FOSS, .env.example, /health como promesa
S8 CI mínimo desde día 1 (0:50): 4 jobs propuestos (backend lint/test, firmware arduino-cli, docker build, calidad) — "mínimo viable, iteramos con docente"
```

- [ ] **Step 4: Cierre honesto (slide 9, 0:40)**

```markdown
S9 Semana1 dónde estamos + preguntas: tenemos propuesta/docs, nos falta todo lo demás, pedimos feedback
Restricción: no prometer Telegram/Tuya, decir "automatización por definir"
```

- [ ] **Step 5: Commit escaleta**

```bash
git add docs/superpowers/plans/2026-09-12-devops-contingencia-presentacion.md
git commit -m "docs: escaleta 9 slides 3 actos con tiempos"
```

### Task 3: Sistema visual y reglas pptxgenjs

**Files:**
- Modify: `docs/superpowers/plans/2026-09-12-devops-contingencia-presentacion.md`

**Interfaces:**
- Consumes: escaleta de Task 2
- Produces: `visual_spec` con paleta, tipografía, motivo, márgenes y gotchas checklist que Task 4 usará

- [x] **Step 1: Fijar paleta contingencia pero esperanzadora**

```markdown
navy 1B2130 dominante 65%, offwhite F2F3F5 fondos, teal 2EC4B6 acento DevOps, coral E94E3C solo para sismo, gold F9C846 para FOSS/KPIs, slate 8A9BA8 secundario. Nunca 4 colores al mismo peso.
```
→ Implementado en `visual_spec.paleta` (ver JSON abajo). Dominancia navy 65% + offwhite, teal 20%, coral ≤10% solo S2, gold ≤10% solo S5.

- [x] **Step 2: Tipografía safe**

```markdown
Títulos: Cambria 28-36 bold (serif, contraste), Cuerpo: Calibri 8-12, Código: Courier New 6.5-7. Dejar 10% slack en títulos si se usa Cambria.
```
→ Implementado en `visual_spec.tipografia`.

- [x] **Step 3: Motivo único**

```markdown
Cards con header en color + icono en círculo blanco + sombra suave. Repetir en S3,S4,S6,S7,S8. Prohibido: barras decorativas, líneas bajo títulos, stripes.
```
→ Implementado en `visual_spec.motivo`.

- [x] **Step 4: Checklist gotchas**

```markdown
pres.layout antes de addSlide, colores sin #, transparency aparte, no compartir options, shadow offset≥0, charSpacing, bullet true, una instancia pptxgen, ROUNDED_RECTANGLE, margin 0, validate.py siempre
```
→ Implementado en `visual_spec.gotchas_checklist` (12 items + margen/LO).

- [x] **Step 5: Commit visual spec — NO commit (handoff JSON)**

```bash
# git add docs/superpowers/plans/2026-09-12-devops-contingencia-presentacion.md
# git commit -m "docs: spec visual Navy/Teal/Coral + reglas pptxgenjs"
# → Entregado como visual_spec JSON para Task 4 (sin commit por instrucción Task 3 Step 5)
```

**visual_spec JSON (Task 3 → Task 4):**

```json
{
  "meta": {
    "task": "Task 3 Sistema visual y reglas pptxgenjs",
    "consumes": ["esencia_map Task1 (vision, 3 casos uso, 3 capas, RF/RNF/HU, hardware MEGA+ESP32+UNO, KPIs como promesa)", "escaleta Task2 (9 slides 3 actos con tiempos)"],
    "produces": "visual_spec para Task4 gen_devops_v3.js",
    "contexto": "semana1 honesta, sin Tuya/Telegram, origen sismo 10 ago 7.4 Dosquebradas/Pereira/UTP despues supuestos propios",
    "layout": "LAYOUT_WIDE 10x5.625 safe (pptxgenjs LAYOUT_16x9). Solo usar LAYOUT_WIDE 13.33x7.5 si overflow medido tras QA",
    "idioma": "es",
    "archivo_salida": "docs/DevOps_AetherNet_Contingencia_10Agosto.pptx"
  }
}
```
(Ver JSON completo entregado en respuesta — bloque siguiente)

### Task 4: Generar deck V3 con pptxgenjs

**Files:**
- Create: `/tmp/gen_devops_v3.js`
- Create: `docs/DevOps_AetherNet_Contingencia_10Agosto.pptx` (output, overwrite)
- Modify: `/tmp/gen_devops_v3.js` si validate falla

**Interfaces:**
- Consumes: escaleta (Task2) + visual_spec (Task3) + esencia_map (Task1)
- Produces: pptx binario validado que Task 6 verificará

- [ ] **Step 1: Escribir script base con header y paleta**

```javascript
const pptxgen = require("pptxgenjs");
let pres = new pptxgen();
pres.layout = "LAYOUT_WIDE"; // 10x5.625 safe; usar 13.33 solo si se necesita
const C = { navy:"1B2130", navy2:"243044", white:"FFFFFF", offwhite:"F2F3F5", light:"E8ECF1", teal:"2EC4B6", tealDark:"1A9E94", coral:"E94E3C", gold:"F9C846", goldSoft:"FFD166", sage:"A7C4BC", slate:"8A9BA8", charcoal:"36454F", softBlue:"DDE7F0" };
function shadow(){ return { type:"outer", color:"000000", blur:6, offset:3, angle:45, opacity:0.18 } }
function titleBar(slide, text, sub){ slide.addShape(pres.shapes.ROUNDED_RECTANGLE,{x:0.5,y:0.35,w:6,h:0.08,fill:{color:C.teal},line:{color:C.teal},rectRadius:0.04}); slide.addText(text,{x:0.5,y:0.55,w:12,h:0.65,fontFace:"Calibri",fontSize:30,color:C.navy,bold:true}); if(sub) slide.addText(sub,{x:0.5,y:1.15,w:12,h:0.3,fontFace:"Calibri",fontSize:11,color:C.slate,italic:true}); }
```

- [ ] **Step 2: Implementar slides 1-3 (Actos 1 y 2 inicio) con contenido exacto**

```javascript
// S1 Portada: badge "SEMANA 1 • PROPUESTA • SUPUESTOS PROPIOS", título AetherNet, subtítulo "DevOps como contingencia", pregunta, derecha 10 AGO 7.4 + "proyecto nace después"
// S2 10 agosto: timeline 4 filas (07:14 hecho, horas después hecho, supuestos 1 y 2), derecha "No había proyecto antes del sismo" + preguntas ¿cancelan? ¿virtualidad?
// S3 Qué es en 60s: 3 cards con iconos ◈/⬡/◎ y textos core use cases + scope honesto
```

- [ ] **Step 3: Implementar slides 4-6 (arquitectura + FOSS + por qué DevOps semana1)**

```javascript
// S4 Arquitectura 3 capas: Edge (MEGA+LED+KY-008), Gateway ESP32 (UART↔RF), Coordinación (Docker en LAN), App (misma Wi-Fi) + barra LAN
// S5 FOSS/local-first: prd §6 contingencia, RNF-3.1 100%, 3 KPIs como objetivos no medidos
// S6 Por qué DevOps semana1: 3 cards Reproducible/Trazable/Local-first
```

- [ ] **Step 4: Implementar slides 7-9 (stack propuesto + CI mínimo + cierre honesto)**

```javascript
// S7 Stack propuesto: code block docker-compose.yml propuesto + 3 servicios con puertos + .env.example
// S8 CI mínimo: 4 jobs propuestos (Backend ruff/mypy/pytest, Firmware arduino-cli, Docker build, Calidad) + nota "iteramos con docente"
// S9 Cierre: izquierda "No venimos a mostrar un final" + derecha preguntas ¿stack LAN? ¿CI mínimo? ¿LowCode? ¿entregables virtualidad?
```

- [ ] **Step 5: Escribir y validar**

```bash
node /tmp/gen_devops_v3.js
python3 .agents/skills/pptx/scripts/office/validate.py docs/DevOps_AetherNet_Contingencia_10Agosto.pptx
# Expected: All validations PASSED!
# Si falla por shape/color/shadow: arreglar en gen script, no en xml, y re-ejecutar
```

- [ ] **Step 6: Commit deck**

```bash
git add docs/DevOps_AetherNet_Contingencia_10Agosto.pptx
git commit -m "feat: deck DevOps contingencia semana1 con esencia completa sin Tuya/Telegram"
```

### Task 5: Guion natural 8-9 min (no lectura)

**Files:**
- Create: `docs/DevOps_Guion_Contingencia_10Agosto.md` (o entregar inline si se prefiere)

**Interfaces:**
- Consumes: escaleta + esencia_map + pptx final
- Produces: guion por slide con tiempo, frase gatillo, transición y qué no decir

- [ ] **Step 1: Escribir principio de guion**

```markdown
Regla: cada slide = 1 historia corta + 1 frase gatillo + 1 pausa. Nunca leer bullets. Mirar a 2 personas distintas por slide.
```

- [ ] **Step 2: Redactar guion S1-S3 (2:00)**

```markdown
S1 (0:40): "No teníamos proyecto antes del 10 de agosto..." + pausa 2s
S2 (1:20): contar sismo real, luego supuestos con disclaimer "no oficial"
S3 (1:10): "AetherNet en 60s son 3 ideas: vigilar, mover, abrir — todo en local"
```

- [ ] **Step 3: Redactar guion S4-S6 (3:30)**

```markdown
S4: explicar 3 capas señalando, no leyendo
S5: FOSS como soberanía: "sin nube que expire cuando más la necesitas"
S6: "DevOps no es deploy bonito, es no perder el semestre si nos separan"
```

- [ ] **Step 4: Redactar guion S7-S9 (2:30)**

```markdown
S7: mostrar docker-compose como promesa reproducible
S8: CI mínimo como "red de seguridad académica"
S9: cerrar con honestidad + 4 preguntas al docente/compañeros, no con "eso es todo"
```

- [ ] **Step 5: Añadir anti-lectura y manejo de preguntas Tuya/Telegram**

```markdown
Si preguntan por Tuya/Telegram: "Lo sacamos a propósito hasta tener aprobación LowCode, preferimos propuesta honesta que promesa no validada"
```

- [ ] **Step 6: Commit guion**

```bash
git add docs/DevOps_Guion_Contingencia_10Agosto.md
git commit -m "docs: guion natural 8-9 min con esencia y semana1 honesta"
```

### Task 6: Verificación antes de entrega

**Files:**
- Read: `docs/DevOps_AetherNet_Contingencia_10Agosto.pptx` via validate + visual QA
- Read: `docs/DevOps_Guion_Contingencia_10Agosto.md`

**Interfaces:**
- Consumes: pptx + guion
- Produces: checklist de entrega

- [ ] **Step 1: Validación técnica**

```bash
python3 .agents/skills/pptx/scripts/office/validate.py docs/DevOps_AetherNet_Contingencia_10Agosto.pptx
# Expected: All validations PASSED!
```

- [ ] **Step 2: Verificación esencia**

```markdown
Checklist: ¿visión FOSS? ¿3 casos de uso? ¿3 capas + LAN? ¿KPIs como objetivos? ¿RNF-1.1/1.2? ¿hardware MEGA+ESP32+UNO+LED? ¿semana1 honesta? ¿origen después del sismo + supuestos? ¿cero Tuya/Telegram?
```

- [ ] **Step 3: Verificación guion**

```markdown
¿8-9 min cronometrado? ¿frase gatillo por slide? ¿transiciones act1→2→3? ¿manejo de pregunta Tuya/Telegram?
```

- [ ] **Step 4: Visual QA**

```bash
python3 .agents/skills/pptx/scripts/office/soffice.py --headless --convert-to pdf docs/DevOps_AetherNet_Contingencia_10Agosto.pptx
pdftoppm -jpeg -r 150 docs/DevOps_AetherNet_Contingencia_10Agosto.pdf slide
# Revisar overflow, contraste, márgenes 0.5", gaps 0.3"
```

---

## Self-Review

**1. Spec coverage:** prd visión/casos/scope/KPIs/restricciones → S3/S4/S5. requirements RF-1.1/1.2/2.1/2.2/2.3/3.1/3.2/3.3 y RNF-1.1/1.2/3.1 y HU-01..04 → S3/S4/S7. hardware-inventory MEGA/ESP32/UNO/LED → S4. sprints Sprint1 + semana1 honesta → S8/S9. roadmap DevOps → S6/S7. Contingencia sismo/supuestos → S1/S2. NO Tuya/Telegram → excluido en Tasks 4-5.

**2. Placeholder scan:** ningún "TBD/TODO/Similar to" — todo con código/texto exacto.

**3. Type consistency:** C paleta, pres.shapes.ROUNDED_RECTANGLE, shadow(), titleBar() consistentes en Tasks 4-5.

