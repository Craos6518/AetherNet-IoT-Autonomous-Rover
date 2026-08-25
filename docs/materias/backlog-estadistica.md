# Backlog por Materia — Estadística

Backlog operativo detallado del Área 5. IDs EST-01..07 provienen de `docs/backlog.md`; nuevos continúan la serie (EST-08+).

> Estado real: `stats/ema_filter.py` + tests ✅ · EMA en `rover.ino` ✅ · resto pendiente. Dependencia dura: EST-04/05 requieren el puente MQTT→PostgreSQL (DEVOPS-12) y datos reales fluyendo.

---

## Sprint 1-2 — Validación offline (sin hardware)

### EST-01 — Prototipo EMA en Python ✅ HECHO
**Hecho:** `EMAFilter(α=0.2)` con validación, `MultiSensorEMA`, métrica KPI (`calculate_noise_reduction` → `meets_kpi`), simulador gaussiano, demo reproducible.

### EST-08 — Barrido de α con justificación cuantitativa *(nuevo)*
| S · Sprint 1-2 · Depende de EST-01 · Origen HU-03 |
**Qué hacer exactamente:**
1. Crear `stats/experiments/alpha_sweep.py`: para α ∈ {0.05,0.1,0.2,0.3,0.5,0.8}, correr 100 simulaciones Monte Carlo de señal constante+picos anómalos (σ=8) y tabular: reducción de ruido %, retardo de respuesta (muestras hasta detectar un escalón real de 50→20 cm), falsos obstáculos detectados.
2. Exportar tabla a `stats/data/alpha_sweep.json` y gráfica comparativa PNG.
3. Concluir por escrito si α=0.2 se sostiene (se espera: buen balance; si no, proponer cambio formal — umbral de seguridad requiere confirmación humana según AGENTS.md §4).

**Criterios:**
- [ ] Tabla + gráfica versionadas y citables en el informe
- [ ] Conclusión escrita de ≤10 líneas sobre la elección de α

## Sprint 3 — Preparación de datos (paralela a DevOps)

### EST-09 — Instrumentación de latencias para t-Student *(nuevo, coordinada)*
| M · Sprint 3 · Depende de DEVOPS-13 (tramas) · Origen RNF-2.2 |
**Qué hacer exactamente:** acordar formato de timestamp: cada comando del Rover lleva `t_app` (epoch ms al publicar) y cada telemetría devuelve `t_gw_rx`; gateway imprime deltas por serial; script captura 50 muestras por protocolo (RF vía nRF24 vs Wi-Fi/MQTT directo a actuador relé) en CSV `stats/data/latency_rf.csv` y `latency_wifi.csv`.
**Alcance IN:** solo medición y almacenamiento crudo.
**Criterios:**
- [ ] ≥50 pares de latencias válidas por protocolo registradas
- [ ] Protocolo de captura documentado (quién envía qué, cuántas veces)

### EST-10 — Conector PostgreSQL→Pandas *(nuevo)*
| M · Sprint 3 · Depende de DEVOPS-12 (datos reales en BD) · Origen EST-04 |
**Qué hacer exactamente:**
1. Crear `stats/db_extract.py`: función `load_sensor_events(sensor_type=None, since=None) -> pd.DataFrame` usando SQLAlchemy con URL desde variable de entorno `DATABASE_URL` (misma del backend).
2. CLI mínimo: `python -m stats.db_extract --sensor ultrasonic --since 24h > eventos.csv`.

**Criterios:**
- [ ] Extrae filas reales insertadas por el puente MQTT (verificación cruzada con psql)
- [ ] Credenciales SOLO por entorno, nunca hardcodeadas

## Sprint 4 — Inferencia y cierre

### EST-02 — EMA KY-037 en firmware
| M · Sprint 4 · Depende de EST-11 · Origen RNF-2.1 |
**Qué hacer exactamente:** tras validar el diseño offline (EST-11), replicar patrón de `rover.ino:246-259` en el sketch que procese el micrófono; mismo α salvo que EST-11 justifique otro; publicar valor filtrado en MQTT junto al crudo.
**Criterios:**
- [ ] Filtro compilado en CI (job firmware)
- [ ] Comparación crudo-vs-filtrado visible en MQTT durante operación

### EST-11 — Análisis previo del KY-037 *(nuevo)*
| M · Sprint 3-4 · Depende de sensor montado |
**Qué hacer exactamente:** capturar 60 s de señal cruda en silencio y con palmada; graficar histograma; decidir transformación previa (valor absoluto de desviación respecto a baseline) ANTES del EMA; documentar decisión en comentario cabecera del sketch y en informe.
**Criterios:**
- [ ] Gráficas antes/después guardadas como evidencia

### EST-05 — Prueba t-Student RF vs Wi-Fi
| M · Sprint 4 · Depende de EST-09, EST-10 · Origen RNF-2.2 |
**Qué hacer exactamente:**
1. Crear `stats/hypothesis_tests.py`: carga los 2 CSV; descriptivos por grupo (n, media, sd); Shapiro-Wilk por grupo; Levene; t de Welch (`scipy.stats.ttest_ind(equal_var=False)`); IC95% de la diferencia; d de Cohen.
2. Si Shapiro falla (p<0.05): ejecutar Mann-Whitney U y REPORTAR AMBOS con justificación.
3. Salida: reporte markdown auto-generado `stats/data/hypothesis_report.md` con conclusión en lenguaje del proyecto ("el enlace RF es X ms más rápido (IC95% [a,b])").

**Criterios:**
- [ ] Reporte incluye supuestos verificados, no solo el p-valor
- [ ] Test unitario mínimo del módulo con datos sintéticos conocidos

### EST-06 — Análisis descriptivo de sensores
| S · Sprint 4 · Depende de EST-10 |
**Qué hacer exactamente:** `stats/descriptive.py`: por cada sensor_type en BD → describe() (media, sd, min, cuartiles), serie temporal crudo vs `filtered_value`, detección simple de outliers (>3σ). Genera `stats/data/sensor_summary.md` con las gráficas PNG embebidas por ruta relativa.

### EST-07 — Reporte final estadístico
| M · Sprint 4 · Depende de EST-02, EST-05, EST-06, EST-08 |
**Qué hacer exactamente:** consolidar en `docs/materias/informe-estadistica.md`: metodología EMA (fórmula, α elegido + evidencia del barrido), resultados de reducción de ruido vs KPI >85%, resultado t-Student completo, conclusiones y limitaciones (supuestos, tamaño muestral). Este documento ES el entregable académico de la materia.
**Criterios:**
- [ ] KPI PRD evaluado explícitamente (cumple/no cumple con número)
- [ ] Todas las cifras trazables a scripts versionados (comando de reproducción incluido)

---

## Resumen dependencias

```
EST-01 ✅ → EST-08 (α) ──────────────┐
DEVOPS-12 (bridge BD) → EST-10 ─────┼→ EST-06 → EST-07 (informe final)
DEVOPS-13 (tramas)    → EST-09      │
KY-037 físico        → EST-11 → EST-02 ┘
```
