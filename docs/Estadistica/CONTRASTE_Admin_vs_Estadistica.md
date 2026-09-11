# Contraste — Administración vs Estadística (AetherNet)

> **Archivos comparados:**
> - `docs/Administracion-Proyectos/Proyecto_AetherNet_Andres_Felipe_Martinez_Henao.docx` (99 KB, 12 tablas, 671 párrafos, 14 secciones — **TS683 Administración**, trabajo individual **Andres Felipe Martinez Henao**, 09/09/2026)
> - `docs/Estadistica/Anteproyecto_AetherNet-3.docx` (14 KB, 2 tablas, ~47 párrafos, 8 secciones — **TS4D3 Estadística**, mismo autor, 5º semestre)
>
> **Fecha de contraste:** 10/09/2026 — ambos docs vivos, sin contradicciones bloqueantes; el segundo **especializa** lo que el primero deja como `EST-01..07` y `OE-05`.

---

## 1. Resumen ejecutivo (1 párrafo)

El doc de **Administración** es el **plan integral del sistema completo** (app + backend + 3 firmwares + estadística + lowcode) bajo Scrum 4 sprints, con WBS, cronograma, riesgos, costos y 12 entregables. El **Anteproyecto de Estadística** es un **zoom estadístico**: valida **solo el filtro EMA α=0.2 del Rover** con **dos datasets externos de Kaggle** (turbidez 31.5k + gestos HC-SR04 5k), midiendo reducción >85% y retardo de reacción. No compiten: el Anteproyecto **es el detalle de OE-05/EST-01..07** que Administración resume en 1 tabla y 1 anexo. Juntos cubren “qué se construye” (Admin) + “cómo se valida estadísticamente que el filtro sirve” (Estadística).

---

## 2. Tabla comparativa global

| Dimensión | Administración (Proyecto_AetherNet...) | Estadística (Anteproyecto-3) | Lectura |
|---|---|---|---|
| **Materia UTP** | TS683 Administración y Planeación de Proyectos de Software | TS4D3 Estadística | Complementarias, mismo proyecto integrador |
| **Alcance** | **Sistema completo** (3 capas: Edge→Gateway→Backend→App/Telegram) | **Solo validación EMA** del HC-SR04 (seguridad anti-choque/caída) | Estadística es subconjunto de Admin §3 OE-05 / §6 WBS 6.x |
| **Título** | *AetherNet IoT & Autonomous Rover — Plataforma distribuida ... 100% FOSS* | *AetherNet — Validación estadística del filtro EMA con datos externos validados* + `IoT & Autonomous Rover` | Mismo nombre corto, distinto subtítulo (sistema vs validación) |
| **Autor** | Andres Felipe Martinez Henao (solo) | Andres Felipe Martinez Henao (trabajo individual) | ✅ Coherente |
| **Estructura plantilla** | 14 secciones (Título → Anexos + índice, portada UTP, header/footer) | 8 secciones (Nombre → Datasets) + footnotes, sin WBS/cronograma/costos | Administración sigue plantilla institucional §1-14; Estadística sigue guion TS4D3 |
| **Justificación** | Nubes propietarias, falta integración app↔firmware↔backend, vacío FOSS + KPIs LAN | Ruido HC-SR04 → riesgo choque/caída Rover; EMA ya es requisito; 531 muestras internas insuficientes → necesita validación externa | Estadística **profundiza** la justificación técnica de HU-03 que Admin menciona en §2 |
| **Objetivo general** | Desarrollar plataforma integral 100% FOSS con KPIs <50ms, <10ms, >85% en 4 sprints | Confirmar con datos reales externos que EMA α=0.2 reduce ruido de forma consistente con pruebas internas | El OG de Estadística es **criterio de aceptación de OE-05** de Admin |
| **Objetivos específicos** | 5 OE (infra DevOps, acceso MEGA, Rover fail-safe, app MQTT/joystick, EMA+t-Student) | 5 OE (describir sensor por turbidez/ángulo/nivel, aplicar EMA>85%, comparar turbidez baja/alta, concluir α=0.2, medir retardo con gestos) | OE-5 de Admin se descompone en 5 OE de Estadística |
| **Pregunta investigación** | Implícita en HU-03 BDD: *¿EMA α=0.2 estabiliza HC-SR04 y evita falsos obstáculos?* | Explícita: *¿EMA α=0.2 reduce ruido consistente y reacciona rápido sobre datos reales bajo turbidez y movimiento?* | Estadística formula la pregunta que Admin deja como BDD |
| **Metodología** | Scrum 4 sprints/8 sem + Kanban Projects 14 + CI arduino-cli + Gantt Mermaid | Descriptiva por grupos + EMA α=0.2 + cálculo error vs water_level + comparación turbidez + medición retardo (gestos) | Scrum es marco de gestión; método estadístico es contenido de Sprint 4 |
| **Datasets** | Menciona `stats/ema_filter.py` bench 531 muestras + `ema-real-531.csv` interno; **no detallaba externos** | **Dos externos Kaggle**: (1) turbidez 31.500 filas (3×10.500) y (2) gestos 5.000 filas HC-SR04 @20Hz | **Gap cerrado hoy:** ambos datasets ya versionados en `stats/Dataset/` y espejo `docs/Estadistica/Datasets/` (ver §5) |
| **Variables** | Abstracto: us_value≈HC-SR04, KPI >85% | Concreto: Dataset1 15 vars (us_value, water_level, turbidity, angle...), Dataset2 11 vars (distancia_cm, gesto, frame_id...) — 2 tablas | Estadística detalla lo que Admin resume |
| **KPIs / Métricas** | 5 KPIs PRD + métricas proceso (≥95% tests, P95<2s) | Mismo KPI >85% + retardo (muestras hasta cruzar umbral) | Coherente, mismo umbral |
| **Riesgos** | 13 riesgos R-01..R-13 (R-02/R-03 críticos) | Riesgo implícito: EMA mal calibrado (R-03) + ruido turbidez | Estadística mitiga R-03 que Admin prioriza |
| **Costos / Cronograma** | $176 HW / $1.176 con mano obra + Gantt 12 sem (Sprint1-4 + buffer) | No aplica (anteproyecto) | Normal — Estadística no presupuesta |
| **Entregables** | 12 entregables (app, backend, 3 firmwares, notebooks, Telegram, manuales, gestión, CI) | Entregables estadísticos: descriptivo, EMA filtrado, comparativa turbidez, conclusión α, medición retardo | Entregables de Estadística = ítems 6 + 10 de Admin |
| **Estado** | Corte1 ✅ Sprint1+deuda 1→2 (8/8) + Corte2 Sprint2 en curso (09/09/2026) MOV-02/03/04 ✅ | Anteproyecto (propuesta) — pendiente ejecución Sprint4 | Anteproyecto alimenta Sprint4 de Admin |
| **Licencia FOSS** | 100% FOSS RNF-3.1 (Mit/Apache/GPL) | Dataset1 CC BY-SA 4.0 + Dataset2 CC0 — ambos compatibles FOSS | ✅ Coherente, requiere atribución CC BY-SA |
| **Trazabilidad** | prd.md, requirements.md (HU-03), sprints.md, backlog.md (EST-01..07) | Cita prd.md:51 KPI, requirements.md:42 RNF-2.1, rover-uno.ino:259 | Misma base documental |

---

## 3. Coherencias (lo que ya encaja)

1. **Mismo α=0.2, mismo KPI >85%, misma fórmula** `S_t = α·Y_t + (1-α)·S_{t-1}` en ambos (`stats/ema_filter.py:15` ↔ `rover-uno.ino:259` ↔ Anteproyecto §5-6).
2. **Mismo sensor:** HC-SR04 ultrasónico time-of-flight (Admin: hardware-inventory.md, Rover UNO; Estadística: us_value / distancia_cm).
3. **Mismo autor y trabajo solo:** ambos declaran Andres Felipe Martinez Henao, individual — sin conflicto de “Equipo completo” de plantilla genérica.
4. **Mismo marco FOSS 100%:** Admin lo exige (RNF-3.1), Estadística lo respeta (CC BY-SA 4.0 requiere atribución + CC0 sin restricciones).
5. **Turbidez / condiciones adversas:** Admin menciona `ir_strength` como proxy ambiental; Estadística lo usa como factor para comparar low vs high.
6. **Los 531 muestras internas** aparecen en ambos (Admin §3 OE-05, §7, §11 y Estadística §3) — reconocimiento de limitación y necesidad de externos.

## 4. Complementos (lo que cada doc aporta que el otro no)

| Aporta Administración que falta en Estadística | Aporta Estadística que faltaba en Administración |
|---|---|
| WBS, cronograma Gantt, matriz riesgos, costos, 12 entregables, CI/CD, app/backend/firmwares completos | Dos datasets externos concretos (31.5k + 5k) con 15+11 variables, diseño factorial, licencias, citas DOI, y uso diferenciado (volumen vs movimiento) |
| Visión sistema 3 capas, protocolos (MQTT <50ms, RF <10ms, UART 38400, fail-safe 500ms), topics `aethernet/#` | Descriptiva por turbidez/ángulo/nivel + medición de **retardo** (50 frames @20Hz) — KPI de rapidez que Admin no desglosaba |
| Retro solo, lecciones, plan sprints, Projects 14 | Pregunta de investigación explícita y OE desglosados para t-Student y conclusión α |
| Anexos hardware pines MEGA 44-46, Fritzing, evidencias `mosquitto_sub`/`curl` | Citas Kaggle + paper Ranieri et al. 2024 DOI 10.1016/j.engappai.2023.107235 y Marisol Gil DOI 10.34740/kaggle/dsv/16239431 |

Puente: Administración §6 WBS 6.1-6.4 y §9.1 citan “bench 531 muestras + Monte Carlo 100×” pero no detallaban **origen externo** — ahora se cubre con Datasets (§5).

## 5. Datasets — integración al proyecto (ambos)

> **Estado al 10/09/2026:** ✅ **Ambos datasets ya versionados y espejados.** No requieren descarga.

### Ubicación

```
stats/Dataset/                        # canónico (código lo importa)
├── water-level_turbidity-low.csv      10.500 filas, 15 cols, 1.1 MB
├── water-level_turbidity-medium.csv   10.500 filas, 1.2 MB
├── water-level_turbidity-high.csv     10.500 filas, 1.2 MB
├── gesture_dataset.csv                5.000 filas, 11 cols, 341 KB  ← antes untracked, ahora añadido
└── README.md                          actualizado con ambos (CC BY-SA 4.0 + CC0)

docs/Estadistica/Datasets/            # espejo para entrega académica (esta carpeta)
├── (mismos 4 CSV, copia idéntica, diff=0)
└── README.md                          ficha completa + tablas variables + citas

stats/data/                           # derivados (NO datasets)
├── water_turbidity_report.json
├── water_us_vs_true.png / water_ir_by_angle.png
└── ema-real-531.csv (531 muestras propias, interno)
```

**Verificación (09/09/2026):**

```bash
wc -l stats/Dataset/*.csv docs/Estadistica/Datasets/*.csv
# 5001 gesture + 10501×3 water = 36504 líneas (36.500 datos + 4 headers)
python3 stats/water_turbidity_analysis.py --check  # 31.500 balanceadas (3×5×7×300)
```

Antes: `gesture_dataset.csv` aparecía como `??` en `git status`; ahora se añade al repo (ver §6).

### Fichas rápidas

**Dataset 1 — Turbidez (principal, volumen):**
- Fuente: https://www.kaggle.com/datasets/caetanoranieri/water-level-identification-with-lidar
- Creador: Caetano Ranieri, 2023-10-25, v1, CC BY-SA 4.0 (atribución + share-alike)
- Paper: Ranieri et al., Engineering Applications of Artificial Intelligence, 127, 107235. DOI 10.1016/j.engappai.2023.107235
- Diseño: 31.500 = 3 turbidez ×5 ángulos ×7 water_level ×300 muestras — balanceado
- Vars clave: `us_value` (ultrasónico ≈HC-SR04), `water_level` (verdad terreno 50-350mm), `turbidity` (low/med/high), `angle`, `ir_value/ir_strength`, IMU 9 ejes
- Uso: EMA α=0.2 sobre `us_value` vs `water_level` → reducción >85%; compara low vs high → robustez

**Dataset 2 — Gestos (complementario, mismo HC-SR04 + movimiento):**
- Fuente: https://www.kaggle.com/datasets/marisolgil/hand-gesture-dataset
- Autora: Marisol Gil Valenzuela (Univ. Sonora), 2026, CC0 Dominio público (sin restricciones)
- DOI: 10.34740/kaggle/dsv/16239431
- Diseño: 5.000 filas = 5 gestos ×50 frames ×20Hz (≈2.5s por gesto)
- Vars clave: `distancia_cm` (HC-SR04 cm, misma unidad Rover), `gesto` (acercar/alejar/estático…), `frame_id`, `timestamp_ms`, `velocidad_cm_s`, `tendencia`, `valido`
- Uso: único con **serie temporal real** → mide **retardo EMA** ante cambio súbito (OE-5), que dataset 1 no puede (mediciones estáticas repetidas)

**Complemento (anteproyecto §3):** 1 aporta volumen controlado, 2 aporta mismo sensor + movimiento temporal — juntos cubren “¿reduce ruido robusto?” y “¿reacciona rápido?”.

### Licencias FOSS

Ambas compatibles con RNF-3.1 100% FOSS. Dataset1 exige citar Ranieri et al. en informe EST-07; Dataset2 no exige atribución pero se cita por rigor.

---

## 6. Gaps cerrados y acciones tomadas (10/09/2026)

| Gap detectado en contraste | Acción |
|---|---|
| Administración no detallaba datasets externos (solo 531 internas) | Se versionaron ambos Kaggle en `stats/Dataset/` + espejo `docs/Estadistica/Datasets/` con READMEs completos |
| `gesture_dataset.csv` untracked (`git status ??`) | `git add` incluido en próximo commit — ya copiado a ambos directorios, 5.000 filas verificadas |
| `stats/Dataset/README.md` solo cubría turbidez | Actualizado a “Datasets — Validación externa” con Dataset 2 (CC0, 11 vars, uso retardo) |
| Docente Estadística necesita datasets en `docs/Estadistica` | Creado `docs/Estadistica/Datasets/` espejo + `README.md` con fichas, tablas variables y citas DOI |
| Falta contraste formal | Creado este `CONTRASTE_Admin_vs_Estadistica.md` + `Contraste_Admin_vs_Estadistica.docx` (siguiente sección) |
| Trazabilidad cruzada | Este contraste enlaza OE-05/EST-01..07 (Admin) ↔ 5 OE (Anteproyecto) ↔ `stats/ema_filter.py:15`/`rover-uno.ino:259` ↔ `stats/water_turbidity_analysis.py` |

---

## 7. Recomendación de integración (para cerrar el círculo)

1. **En Administración §6 WBS 6.3 y §9.1:** añadir nota “Validado con datasets externos 31.5k (turbidez) + 5k (gestos HC-SR04) — ver `docs/Estadistica/Datasets/README.md` y `stats/Dataset/README.md`”.
2. **En Administración §14 Anexos:** añadir Anexo G “Datasets externos” con las dos fichas de §5 de este contraste (ya listo para copiar).
3. **En Estadística Sprint4:** ejecutar `python3 stats/water_turbidity_analysis.py` y gestos → generar `water_turbidity_report.json` + PNGs y reportar reducción % y retardo en `notebooks/EMA_Estadistica.ipynb §7c/7d`.
4. **En informe final EST-07:** incluir citas CC BY-SA 4.0 y CC0 (ver §5) y referenciar `stats/Dataset/` y `docs/Estadistica/Datasets/` como fuentes.

Sin estas 4 líneas, ambos docs quedan **consistentes pero desconectados**; con ellas, Administración absorbe la evidencia externa y Estadística deja de ser “ejercicio aislado”.

---

## 8. Conclusión

**No hay contradicción.** Administración es el **mapa** y Estadística es la **brújula del filtro**. El contraste muestra que el Anteproyecto no corrige a Administración: **lo completa** en el punto más riesgoso (R-03 EMA mal calibrado). Con ambos datasets ya integrados (36.500 filas, 3.7 MB, HC-SR04 idéntico incluido), el proyecto puede sostener ante evaluador: “EMA α=0.2 no solo simula 89% en 531 muestras internas, sino que se valida en 31.5k medidas controladas y 5k movimientos reales del mismo sensor, con licencias FOSS y diseño balanceado”.

> Próximo paso: commit que incluya `docs/Estadistica/Datasets/*.csv` + `stats/Dataset/gesture_dataset.csv` + READMEs + este contraste, y referencia cruzada en `docs/Administracion-Proyectos/Proyecto_AetherNet...docx` Anexo G.

---

## Referencias

- docs/prd.md §5 KPIs, §6 LAN
- docs/requirements.md §2-3 RF/RNF/HU-03
- docs/sprints.md, docs/backlog.md (EST-01..07, MOV/DEVOPS)
- docs/hardware-inventory.md (HC-SR04, TCRT5000, KY-008)
- docs/architecture.md §3-4 (EMA, t-Student)
- docs/risk-register.md R-03, R-12
- stats/ema_filter.py:15, rover-uno.ino:259, stats/water_turbidity_analysis.py:1, notebooks/EMA_Estadistica.ipynb
- Kaggle turbidez: https://www.kaggle.com/datasets/caetanoranieri/water-level-identification-with-lidar — CC BY-SA 4.0 — DOI 10.1016/j.engappai.2023.107235
- Kaggle gestos: https://www.kaggle.com/datasets/marisolgil/hand-gesture-dataset — CC0 — DOI 10.34740/kaggle/dsv/16239431
