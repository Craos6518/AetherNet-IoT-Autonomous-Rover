Asunto: Avance proyecto integrador AetherNet — TS4D3 Estadística (contexto post-sismo)

Estimada profesora María Paula:

Le escribo con mucho respeto por lo que está viviendo nuestra comunidad tras el **sismo del 10 de agosto (7:34 AM, mag. 7.4)**. Conozco compañeros que han perdido parte de sus hogares, otros que están de voluntarios en zonas afectadas, y varios que han tenido que viajar a sus regiones de origen. En mi caso, mi familia y yo no tuvimos daños mayores, estoy ayudando como voluntario en lo que puedo, y cuento con las condiciones para seguir estudiando desde casa.

Solo tuvimos **una semana de clases** antes del sismo. Mientras se reorganiza el semestre, mi intención es **ir prototipando la parte estadística del proyecto integrador** para no perder el hilo y llegar con avances concretos cuando retomemos.

---

### ¿Qué es AetherNet? (Contexto general)

**AetherNet IoT & Autonomous Rover** es mi **Proyecto Integrador de 5º semestre**. Es un sistema real de **domótica + control de acceso + robot móvil autónomo** que funciona **100% en red local (sin Internet)**, todo con herramientas de código abierto.

**El lado estadístico del proyecto:** Los sensores físicos (ultrasonido, micrófono, láser, radiofrecuencia) generan datos **ruidosos e inciertos**. Mi trabajo en estadística es **convertir esas lecturas crudas en decisiones confiables** para el sistema.

---

### ¿Por qué estadística en un proyecto de hardware/software? (El "para qué")

| Problema Real del Hardware | Solución Estadística | Unidad del Syllabus |
|----------------------------|---------------------|---------------------|
| **Sensor ultrasónico HC-SR04** da lecturas que "brincan" (ruido eléctrico, ecos) | **Filtro EMA (Media Móvil Exponencial)**: suaviza la señal en tiempo real → el robot no "ve" obstáculos fantasmas | U1 (Descriptiva) + Aplicación práctica |
| **¿Es más rápido el comando por Radio (2.4GHz) que por Wi-Fi (MQTT)?** | **Prueba t-Student de dos muestras**: comparar latencias reales con datos → decisión de arquitectura | U6-7 (Normal, t-Student) |
| **¿Cada cuánto falla el enlace de radio del robot?** | **Distribución Weibull / Exponencial de fallas**: modelar tiempo entre fallos → definir *timeout* de seguridad (fail-safe) | U8 (Confiabilidad) |
| **Eventos de intrusión (láser) — ¿son aleatorios o hay patrón?** | **Proceso de Poisson / Distribución discreta**: modelar llegada de eventos → dimensionar alertas | U4-5 (Discretas: Poisson, Geométrica) |

> **Nota importante:** La asignatura **no tiene enfoque en programación**. Todo el análisis estadístico lo prototipo en **Python (Jupyter Notebooks)** con librerías estándar (Pandas, SciPy, NumPy) — lo mismo que se usa en clase. La implementación final en el microcontrolador (C++) es solo la *aplicación* de la fórmula validada.

---

### Stack estadístico (herramientas, no jerga)

| Herramienta | Para qué se usa en el proyecto |
|-------------|--------------------------------|
| **Python + Jupyter** | Prototipar fórmulas, graficar, probar con datos simulados |
| **Pandas** | Manipular tablas de datos (lecturas de sensores, timestamps) |
| **SciPy / statsmodels** | Prueba t-Student, ajuste Weibull, pruebas de normalidad (Shapiro-Wilk) |
| **NumPy** | Cálculos vectorizados (EMA, desviación estándar, percentiles) |
| **Matplotlib / Seaborn** | Gráficas para informe final (boxplots, series temporales, PDF/CDF Weibull) |

---

### Alineación concreta: Unidad del Syllabus → Qué hago en el proyecto

| Unidad Syllabus | Aplicación en AetherNet | Qué avanzo ahora (casa, solo Python/datos simulados) |
|-----------------|------------------------|------------------------------------------------------|
| **U1: Descriptiva** (media, varianza, gráficas) | Análisis exploratorio de 10,000+ lecturas HC-SR04 / KY-037 | 🟡 Notebook: estadísticas básicas, histogramas, boxplots, outliers |
| **U2-3: Probabilidad** (espacio muestral, Bayes, independencia) | Modelo base: ¿qué tan probable es un falso positivo del láser? | 🟡 Definir espacio muestral, eventos, probabilidad condicional |
| **U4-5: Variables Discretas** (Bernoulli, Binomial, Poisson, Geométrica) | Intrusión = Bernoulli (sí/no); Llegada intrusos = Poisson; Reintentos RF = Geométrica | 🟡 Simular procesos, estimar parámetros (λ, p) |
| **U6-7: Variables Continuas** (Normal, Exponencial, t-Student) | **Latencias RF vs Wi-Fi → Normal → t-Student**; Tiempo entre fallas → Exponencial | 🟡 **Framework prueba hipótesis listo** (H₀: μ_RF = μ_Wi-Fi) |
| **U8: Confiabilidad** (Weibull, Ley falla, MTTF, Sistemas) | **Vida útil enlace nRF24L01** → Ajuste Weibull → MTTF → Timeout fail-safe 300-500ms | ⏳ Ajuste Weibull con datos simulados (pendiente datos reales) |

---

### Las 3 aplicaciones núcleo (lo que entregaré como evidencia estadística)

| # | Qué es | Fórmula / Método | KPI del Proyecto (PRD) | Entregable |
|---|--------|------------------|------------------------|------------|
| **1** | **Filtro EMA** (suavizado tiempo real en microcontrolador) | `S_t = α·Y_t + (1-α)·S_{t-1}` con **α = 0.2** | "Reducción ruido >85%" | Notebook validación + código C++ portable |
| **2** | **Prueba t-Student** (comparar latencia RF vs Wi-Fi) | `t = (x̄₁ - x̄₂) / √(s₁²/n₁ + s₂²/n₂)` — verificar normalidad (Shapiro) y varianzas (Levene) antes | "Latencia RF <10ms, Wi-Fi <50ms" | Informe con p-value, intervalo confianza, tamaño de efecto |
| **3** | **Análisis Weibull** (confiabilidad enlace radio) | `F(t) = 1 - exp(-(t/η)^β)` — estimar β (forma), η (escala) → MTTF = η·Γ(1+1/β) | "Fail-safe 300-500ms justificado" | Gráfico probabilidad, reporte MTTF, recomendación timeout |

---

### Mi estrategia: **Valido la estadística en Python con datos REALES (casa), luego la llevo a C++**

**Tengo todos los sensores y microcontroladores en casa** (HC-SR04, KY-037, KY-008, nRF24L01, ESP32, MEGA, UNO) + laptop. Puedo **capturar datos reales de sensores ahora mismo**, no solo simular.

| Fase | Dónde | Qué hago |
|------|-------|----------|
| **1. Prototipo + Captura real** (ahora) | Casa, Python/Jupyter + **hardware real** | **Leer HC-SR04/KY-037/KY-008 reales** → validar α=0.2 con ruido verdadero, probar t-Student con latencias reales RF vs Wi-Fi, ajustar Weibull con fallos reales de enlace |
| **2. Port a firmware** (casa) | Arduino IDE / `arduino-cli` | Implementar EMA en C++ (entero/fixed-point), flashear MEGA/UNO, validar en microcontrolador |
| **3. Validación final** (lab cuando vuelva) | Sistema integrado completo | Medir latencias E2E, t-Student con datos de producción, informe final |

---

### Evidencia disponible (repositorio)
- `stats/ema_filter.py` — Prototipo EMA con datos simulados + validación % reducción ruido
- `stats/ttest_latency.py` — Framework completo prueba hipótesis (verifica supuestos + ejecuta test)
- `stats/reliability_analysis.py` — Ajuste Weibull (MLE), gráficas supervivencia, MTTF
- `docs/requirements.md` RNF-2.1, RNF-2.2, HU-03 (criterios BDD medibles)
- `docs/backlog.md` Área 5 — Tareas EST-01 a EST-07 (MoSCoW, trazabilidad)

---

Entiendo perfectamente que la prioridad ahora es la seguridad y bienestar de todos. Este correo solo busca **explicar con claridad qué parte estadística tiene el proyecto, cómo se conecta con cada unidad de su syllabus, y cómo pienso avanzar responsablemente con el tiempo que tengo**. Quedo a la espera de sus indicaciones sobre cómo adaptar el cronograma y evaluación.

Con todo mi respeto y solidaridad,

**[Su Nombre]**
Estudiante TS4D3 Estadística - Grupo 402
Proyecto Integrador: AetherNet IoT & Autonomous Rover