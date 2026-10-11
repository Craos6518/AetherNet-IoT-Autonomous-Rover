# Roadmap por Materia — Estadística (TS4D3)

**Alcance de este roadmap:** todos los temas estadísticos necesarios para COMPLETAR la parte analítica del proyecto: filtrado en tiempo real (firmware), validación offline (Python) e inferencia sobre datos históricos (PostgreSQL→Pandas). Mapeo académico completo en [`estadistica.md`](estadistica.md).

**Estado al Aug 2026:** EMA prototipado y testeado ✅ · EMA en firmware Rover ✅ · **falta:** EMA para KY-037, extracción de histórico desde Postgres, t-Student de latencias, reporte con KPI.

Backlog operativo detallado: [`backlog-estadistica.md`](backlog-estadistica.md)

---

## Bloque 1 — Estadística descriptiva (base conceptual + práctica)

| Tema | Profundidad | Para qué | Específico del proyecto |
|---|---|---|---|
| Media, mediana, desviación estándar, cuartiles | Fluido (Pandas `describe`) | EST-06 | Caracterizar ruido crudo vs filtrado del HC-SR04 |
| Varianza y MSE como momentos de 2º orden | Fluido | KPI PRD >85% | La métrica `noise_reduction_pct` ya implementada ES una razón de varianzas: saber defenderla |
| Visualización: series de tiempo señal cruda vs filtrada | Operativo (matplotlib) | Reporte EST-07 | Gráfica obligatoria para demostrar el suavizado ante evaluadores |

## Bloque 2 — Probabilidad y modelado del ruido

| Tema | Profundidad | Para qué | Específico |
|---|---|---|---|
| Variable aleatoria continua; distribución Normal | Aplicado | Validación EMA offline | El simulador ya asume ruido gaussiano (σ configurable): entender qué implica ese supuesto |
| Esperanza condicional → justificar EMA como estimador recursivo | Conceptual sólido | Sustentación HU-03 | S_t = αY_t+(1−α)S_{t−1} es E[S_t|historia]: argumento teórico del filtro |
| Trade-off α: sesgo-varianza / suavizado-latencia | Cuantificar experimentalmente | Elección de α=0.2 | Barrido α ∈ {0.1,0.2,0.3,0.5} sobre la misma serie simulada → tabla reducción% vs retardo; evidencia de que 0.2 fue decisión informada |
| Bernoulli aplicado a eventos de acceso | Aplicar simple | Análisis auditoría | p̂ de intentos fallidos desde tabla AccessEvent (bonus descriptivo) |

## Bloque 3 — Inferencia: prueba t-Student (núcleo Sprint 4)

| Tema | Profundidad | Para qué requisito | Específico |
|---|---|---|---|
| Formulación de hipótesis H0/H1 (dos colas) | Formal correcto | RNF-2.2, EST-05 | H0: μ_RF = μ_WiFi vs H1: μ_RF ≠ μ_WiFi (latencias comando→acción) |
| Supuestos: normalidad (Shapiro-Wilk o Q-Q plot), independencia, varianzas (Levene/Welch) | Saber verificar Y reportar si fallan | EST-05 | Latencias no negativas y posiblemente asimétricas: si normalidad falla → alternativa Mann-Whitney U documentada |
| t de Welch (varianzas desiguales) y tamaño de muestra n≥30 por grupo | Aplicar con SciPy | EST-05 | Recolección: timestamps instrumentados en gateway/firmware (tarea conjunta con DevOps) |
| Interpretación: p-valor, intervalo de confianza 95%, tamaño de efecto (d de Cohen) | Reportar correctamente | Informe final | No basta "p<0.05": concluir en términos del proyecto (RF es X ms más rápido) |

## Bloque 4 — Herramientas de análisis

| Tema | Profundidad | Para qué | Específico |
|---|---|---|---|
| Pandas: DataFrames, filtros, resample temporal | Operativo | EST-04/06/07 | Serie temporal por sensor_id desde PostgreSQL |
| Acceso Python↔PostgreSQL: SQLAlchemy/psycopg2 async-safe | Operativo | EST-04 | La BD vive en Docker: conectar desde host al puerto expuesto 5432 con credenciales de `.env` |
| SciPy.stats: shapiro, levene, ttest_ind(equal_var=False), mannwhitneyu | Operativo | EST-05 | Un solo módulo `stats/hypothesis_tests.py` reutilizable |
| Organización de notebooks vs scripts | Decisión: scripts+tests (ya establecido) | Reproducibilidad | CI ejecuta stats-test: todo código nuevo debe mantener pytest verde |

## Bloque 5 — Integración con firmware (aplicación en vivo)

| Tema | Profundidad | Para qué | Específico |
|---|---|---|---|
| EMA embebido: límites float32, inicialización, lecturas inválidas | Ya resuelto en rover.ino → replicar patrón | EST-02/03 | Patrón: init lazy + descarte de lecturas fuera de rango + fórmula idéntica a Python |
| KY-037 (micrófono): naturaleza de la señal (AC alrededor de umbral) | Analizar antes de codificar | EST-03 | El ruido del mic NO es igual al del sonar: puede requerir valor absoluto de desviación respecto a línea base antes del EMA — decidir con captura real de datos |
| Calibración en sitio: registrar crudo vs filtrado durante demo | Protocolo de captura | EST-07 | Guardar CSV paralelo (timestamp, raw, filtered) publicado por MQTT para análisis posterior |

---

## Orden crítico

1. **EST-01 ya cumplida** — capitalizarla: barrido de α (barato, puro Python) cierra la justificación técnica.
2. Instrumentación de timestamps de latencia (con DevOps) ANTES del Sprint 4: sin datos no hay t-Student.
3. Captura real del KY-037 antes de escribir su filtro (evita refactor ciego).
