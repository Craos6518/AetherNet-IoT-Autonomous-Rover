# Email — Estadística TS4D3 (María Paula Rodas) — Versión final

**Asunto:** Propuesta y avances del componente estadístico (AetherNet) — TS4D3 Gr. 402

**Repositorio:** https://github.com/Craos6518/AetherNet-IoT-Autonomous-Rover

---

Estimada profesora María Paula:

Atendiendo a la orientación que me brindó previamente para el proyecto integrador, estructuré la propuesta formal del componente estadístico de **AetherNet IoT & Autonomous Rover** y avancé en el prototipado validado en Python (con estructura lista para inyección de lecturas reales de los sensores HC-SR04/KY-037).

**Alineación del proyecto con el syllabus TS4D3:**

* **U1 (Descriptiva):** Análisis exploratorio de lecturas ruidosas de sensores ultrasónico HC-SR04 y acústico KY-037 — caracterización por varianza/MSE y visualización crudo vs. filtrado.
* **U4-5 (Variables Discretas):** Modelado de eventos de intrusión (Bernoulli/Poisson) y reintentos de comunicación RF (Geométrica) a partir del histórico de eventos.
* **U6-7 (Continuas, art. con RNF-2.2):** Comparación de latencia de red RF (nRF24L01) vs. Wi-Fi/MQTT mediante Prueba t-Student de dos muestras (Welch), validando supuestos de normalidad (Shapiro-Wilk) y homocedasticidad (Levene) — con alternativa Mann-Whitney U si falla el supuesto.
* **U8 (Confiabilidad):** Análisis del enlace RF como sistema en serie — modelado del tiempo entre fallos bajo modelo exponencial (extensión opcional a Weibull) para contraste teórico del timeout fail-safe de 500 ms (RF-3.3 / HU-04).

**Entregables específicos propuestos:**

| Aplicación | Método / Prueba | Resultado esperado |
|---|---|---|
| Filtro de suavizado (EMA) | Media Móvil Exponencial (α = 0.2) — `S_t = α·Y_t + (1-α)·S_{t-1}` | Reducción de ruido >85% (KPI PRD), validado en Python y portado a C++ |
| Comparativa RF vs. Wi-Fi | Prueba t-Student para 2 muestras (Welch) | Reporte con p-value, IC95% de la diferencia y d de Cohen |
| Confiabilidad de enlace | Modelo exponencial (Weibull opcional) | Contraste de tasa de falla estimada vs. timeout empírico 500 ms |

**Estado actual:** Prototipo EMA validado y testeado en CI. Los módulos de inferencia (t-Student) y análisis descriptivo quedan propuestos para Sprint 4, pendientes de la captura de datos reales instrumentados (EST-09/EST-10).

**Documentación de referencia en el repositorio:**

* Mapa académico completo (unidad → aplicación en proyecto): `docs/materias/estadistica.md`
* Backlog operativo del área (EST-01..11, dependencias y criterios): `docs/materias/backlog-estadistica.md`
* Roadmap técnico y orden crítico: `docs/materias/roadmap-estadistica.md`
* Requisitos trazables: `docs/requirements.md:42` (RNF-2.1 EMA, RNF-2.2 histórico) y `docs/requirements.md:72` (HU-03) + `docs/prd.md:51` (KPI >85%)
* Planificación Scrum: `docs/sprints.md:40` (Sprint 4 — Filtrado e inferencia)
* Prototipo analítico EMA (Python): `stats/ema_filter.py:15` (`EMAFilter`), `stats/ema_filter.py:99` (`calculate_noise_reduction`) y `stats/tests/test_ema_filter.py`
* Implementación en firmware (C++): `firmware/rover-uno/src/rover.ino:246` (`readSensors` / `ultrasonicEma`) y `firmware/rover-uno/src/rover.ino:63` (`FAILSAFE_TIMEOUT_MS`)
* Evidencia reproducible: `stats/data/ema_demo.json` y `stats/experiments/alpha_sweep.py` (barrido de α) + `stats/notebooks/EMA_Estadistica.ipynb`

Quedo atento a sus comentarios o ajustes para adaptar el cronograma de evaluación.

Un saludo cordial,

**Andres Felipe Martinez Henao**
Estudiante TS4D3 Estadística — Grupo 402
Proyecto Integrador: AetherNet IoT & Autonomous Rover
