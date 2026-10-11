# Documentación Académica — Estadística (TS4D3)

**Proyecto:** AetherNet IoT & Autonomous Rover
**Asignatura UTP:** Estadística — Código TS4D3, 3 créditos, 4 h/semana
**Área del proyecto:** Área 5 — Estadística (Filtrado y Analítica) · ver `docs/backlog.md` §Área 5

> Este documento mapea el contenido académico oficial de la asignatura (PDF `docs/UTP/2344_TS4D3 Estadística.pdf`) contra lo que realmente se aplica en el proyecto, indicando el área y archivo exacto donde vive cada aplicación. Referencias cruzadas: RNF-2.1, RNF-2.2, HU-03, KPIs de `docs/prd.md`.

---

## 1. Contenido académico según el PDF

| Unidad | Tema | Subtemas |
|---|---|---|
| **U1** | Estadística Descriptiva | Definiciones básicas y organización de datos; medidas de tendencia central (posición); medidas de dispersión o variabilidad; momentos |
| **U2** | Introducción a la Probabilidad | Métodos de enumeración; definiciones básicas; espacio muestral; definición axiomática y propiedades |
| **U3** | Probabilidad Condicional e Independencia | Probabilidad condicional (eventos dependientes); Teorema de Bayes; eventos independientes |
| **U4** | Variables Aleatorias Discretas | Noción general; esperanza y varianza; función generatriz de momentos |
| **U5** | Distribuciones Discretas | Causal (Degenerada), Bernoulli, Binomial, Hipergeométrica, Poisson, Multinomial, Geométrica, Pascal |
| **U6** | Variables Aleatorias Continuas | Función de discriminación; momentos |
| **U7** | Principales Distribuciones Continuas | Uniforme, Exponencial, Proceso de Poisson, Normal |
| **U8** | Teoría de la Confiabilidad | Conceptos básicos; ley normal de falla; ley exponencial de falla; ley exponencial + Poisson; ley de Weibull; confiabilidad de sistemas |

Texto guía: Mendenhall & Sincich, *Probabilidad y Estadística para Ingeniería y Ciencias*.

---

## 2. Mapa: tema académico → aplicación en el proyecto → área

### U1 — Estadística Descriptiva ✅ Aplicado

| Lo que se ve en el contenido | Lo que se aplica en el proyecto | Dónde está aplicado |
|---|---|---|
| Medidas de dispersión (varianza) | La **reducción de ruido se mide como razón de varianzas**: MSE crudo vs. MSE filtrado (`noise_reduction_pct = (1 - MSE_f/MSE_r)·100`). El KPI del PRD (>85%) es una comparación directa de dispersión entre señal ruidosa y filtrada | `stats/ema_filter.py:99-123` (`calculate_noise_reduction`) |
| Momentos (segundo orden) | El **Error Cuadrático Medio (MSE)** es el momento central de segundo orden del error de medición | `stats/ema_filter.py:110-114`; demo en `stats/ema_filter.py:126-161` |
| Organización y presentación de datos | Histórico de eventos de sensores almacenado en PostgreSQL con columnas `value` (crudo) y `filtered_value` (filtrado) para análisis descriptivo posterior | `backend/app/models.py` → clase `SensorEvent` |
| Gráficas / análisis descriptivo | Análisis descriptivo (medias, varianzas, gráficas) de telemetría planificado como ítem EST-06 del backlog (Pandas/SciPy) | `docs/backlog.md` EST-06 · `docs/roadmap.md` §5 |

### U2-U4 — Probabilidad y Variables Aleatorias ✅ Aplicado (parcial U3)

| Lo que se ve en el contenido | Lo que se aplica en el proyecto | Dónde está aplicado |
|---|---|---|
| Esperanza condicional (U4): E[S_t \| S_{t-1}] | El filtro **EMA es literalmente una esperanza condicional**: S_t = α·Y_t + (1−α)·S_{t-1}. El valor suavizado de hoy depende del estado anterior — es un proceso estocástico recursivo | Fórmula documentada en `stats/ema_filter.py:1-7`; implementación Python `stats/ema_filter.py:25-33` |
| Modelado de incertidumbre (U2) | El ruido de medición se modela como **variable aleatoria gaussiana** para probar el filtro offline antes de tocar hardware: `random.gauss(0, σ)` sobre valores verdaderos | `stats/ema_filter.py:88-96` (`simulate_noisy_signal`, σ=8 cm para HC-SR04 simulado) |
| Eventos independientes / probabilidad condicional (U3) | **No aplicado formalmente.** Brecha identificada: no hay cálculo Bayesiano explícito. Aplicación conceptual más cercana: la decisión de "obstáculo" del Rover combina evidencia de múltiples sensores (ultrasónico filtrado + IR) | Lógica de fusión de sensores en `firmware/rover-uno/src/rover.ino:315-358` (sin formalismo bayesiano) |

### U5-U7 — Distribuciones ✅ Aplicado

| Lo que se ve en el contenido | Lo que se aplica en el proyecto | Dónde está aplicado |
|---|---|---|
| Distribución **Bernoulli** (éxito/fallo) | Cada intento de acceso por PIN es un ensayo Bernoulli: campo `success BOOLEAN` en la tabla de auditoría. La tasa de accesos fallidos es p̂ (estimador de la probabilidad de fallo) | `backend/app/models.py` → `AccessEvent.success` |
| Distribución **Normal** (U7) | Modelo del ruido del sensor ultrasónico en las simulaciones (gaussiana con σ configurable) — supuesto base para validar el EMA offline | `stats/ema_filter.py:96` (`random.gauss`) |
| Variable aleatoria continua (U6) | La distancia medida por el HC-SR04 (0–200 cm continuos) es la variable aleatoria de interés; su versión filtrada alimenta decisiones discretas (esquivar/no esquivar) | `firmware/rover-uno/src/rover.ino:246-259` (`readSensors`) |
| Distribución Exponencial / Proceso de Poisson (U7) | **Aplicación natural disponible pero no formalizada:** los tiempos entre eventos de intrusión/acceso almacenados con timestamp podrían modelarse como proceso de Poisson (conteo de eventos raros en el tiempo). Queda registrado como extensión para EST-06/EST-07 | Datos disponibles en `SensorEvent.timestamp` / `SecurityEvent.timestamp` (`backend/app/models.py`) |

### U8 — Teoría de la Confiabilidad ✅ Aplicado

| Lo que se ve en el contenido | Lo que se aplica en el proyecto | Dónde está aplicado |
|---|---|---|
| Confiabilidad de sistemas (serie) | La cadena de comando App→ESP32→RF→UNO es un **sistema en serie**: si cualquier enlace falla, el sistema falla. De ahí el diseño fail-safe | `docs/architecture.md` §6 · `docs/risk-register.md` |
| Ley exponencial de falla (tasa constante) | **Fail-safe RF con timeout:** si no llega paquete válido en 500 ms, el Rover detiene motores (fail-stop). Es diseño tolerante a fallas asumiendo que la pérdida de enlace puede ocurrir en cualquier instante (proceso sin memoria ≈ exponencial) | `firmware/rover-uno/src/rover.ino:63` (`FAILSAFE_TIMEOUT_MS 500`), lógica en `rover.ino:209-218` (`checkFailsafe`) · Requisito RF-3.3 / HU-04 |
| Detección de falla a nivel dato | **Checksum aritmético** en cada paquete RF: paquete corrupto → descartado → no reinicia el timer del fail-safe | `firmware/rover-uno/src/rover.ino:372-379` (`verifyChecksum`) |

---

## 3. El resultado estrella: EMA en firmware embebido (RNF-2.1 / HU-03)

La aplicación más importante de la materia está **duplicada en dos áreas**, lo que demuestra portabilidad del concepto:

1. **Prototipo analítico en Python** (`stats/ema_filter.py`) — área Estadística:
   - Clase `EMAFilter(α=0.2)` con validación α ∈ (0,1], gestión multi-sensor (`MultiSensorEMA`), métrica de reducción de ruido vs. KPI.
   - Tests unitarios en `stats/tests/test_ema_filter.py` (corren en CI).
2. **Implementación en C++ embebido** (`firmware/rover-uno/src/rover.ino:246-259`) — área Firmware/Rover:
   - Misma fórmula ejecutándose a 100 Hz en el Arduino UNO sobre lecturas reales del HC-SR04, con inicialización lazy (`ultrasonicInitialized`) y descarte de lecturas fuera de rango.
   - El valor filtrado (`ultrasonicEma`) es el que decide evasión autónoma (`executeAutoMode`, umbrales 30 cm/15 cm), no la lectura cruda.

**Parámetro académico clave:** α = 0.2 (HU-03). Trade-off: α pequeño ⇒ más suavizado pero más latencia de respuesta. El valor fue fijado como requisito (no negociable sin confirmación humana — ver `AGENTS.md` §4).

**KPI asociado (PRD §5):** reducción de ruido > 85%. La función `calculate_noise_reduction()` retorna `meets_kpi` verificable programáticamente.

---

## 4. Pendiente / brechas (trazable al backlog)

| Ítem backlog | Descripción | Estado |
|---|---|---|
| EST-02 | EMA para KY-037 (sonido) en firmware | Pendiente (Sprint 4) |
| EST-04 | Extracción de histórico desde PostgreSQL (`psycopg2`/SQLAlchemy) | Pendiente — requiere datos reales de DEVOPS-03 |
| EST-05 | **Prueba t-Student** de dos muestras (latencia RF vs. Wi-Fi) | Pendiente — nota: la prueba de hipótesis formal NO aparece en el PDF del programa (el PDF cubre hasta confiabilidad); se hereda del entregable del proyecto integrador. Verificar supuestos (normalidad, varianzas) antes de aplicarla |
| EST-07 | Reporte final con % reducción de ruido | Pendiente (Sprint 4) |
| U3 (Bayes) | Sin aplicación formal — oportunidad de mejora opcional | Registrado |

---

## 5. Trazabilidad

- **Requisitos:** RNF-2.1 (EMA obligatorio en sensores), RNF-2.2 (histórico para análisis descriptivo e inferencial)
- **Historias de Usuario:** HU-03 (criterios BDD del filtrado)
- **KPIs PRD:** Precisión del filtro > 85% de reducción de ruido
- **Sprints:** prototipo EST-01 adelantado a Sprint 1-2 (riesgo mitigado); resto Sprint 4 (`docs/sprints.md`)
