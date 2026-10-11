# Guion — Presentación del proyecto (5 min)

**Reunión:** Videollamada individual con la profesora María Paula Rodas
**Materia:** TS4D3 Estadística — Grupo 402
**Fecha y hora:** Miércoles 26 de agosto de 2026, 18:20 (espacio de 10 min → ~5 min de presentación + ~5 min de diálogo)
**Objetivo del guion:** Explicar el proyecto AetherNet, mostrar cómo se conecta con el syllabus de Estadística, evidenciar disponibilidad para trabajo autónomo, y **solicitar el aval de la docente para adelantar la parte estadística como esquema de contingencia del proceso académico**.
**Contexto:** Sismo del 10 de agosto (mag. 7.4) — clases suspendidas indefinidamente en UTP Pereira; regreso progresivo virtual por definir.

---

## Bloque 1 — Saludo y contexto personal (0:00–0:40)

> Buenas tardes, profesora María Paula. Antes de empezar, gracias por organizarnos este espacio; imagino que estas semanas no han sido fáciles para nadie. Por mi parte le cuento rápido: mi familia y yo salimos bien del sismo, sin daños mayores. He podido ayudar como voluntario en lo que se ha podido, y cuento con condiciones estables para seguir estudiando desde casa: internet, equipo de trabajo, y todos los componentes del proyecto aquí conmigo.
>
> Justo por eso quise agendar esta llamada: quiero contarle qué estoy haciendo para no perder el hilo de la materia mientras se define el regreso, y al final pedirle algo muy concreto.

**Nota interna:** tono calmado, humano, sin dramatizar. No extenderse: el objetivo es llegar al proyecto.

---

## Bloque 2 — AetherNet en 30 segundos (0:40–1:20)

> Mi proyecto integrador se llama **AetherNet**: un sistema de domótica, control de acceso y un robot móvil autónomo que funciona completamente en red local, sin depender de nubes propietarias.
>
> ¿Y dónde entra la estadística? Justo en el corazón del sistema. Los sensores — ultrasonido, micrófono, láser, radiofrecuencia — producen datos ruidosos e inciertos. Mi trabajo en esta materia es convertir esas lecturas crudas en decisiones confiables: cuándo debe frenar el robot, si la radio es realmente más rápida que el Wi-Fi, o cada cuánto falla el enlace de comunicación.

**Nota interna:** si pregunta por el resto del proyecto (app, firmware), responder en una frase y volver a estadística.

---

## Bloque 3 — Las 3 aplicaciones núcleo ↔ syllabus (1:20–3:10)

> Son tres aplicaciones núcleo, y cada una se ancla a una unidad de su syllabus.
>
> **Primera: el filtro EMA**, media móvil exponencial. El sensor ultrasónico HC-SR04 da lecturas que "brincan" por ruido eléctrico y ecos fantasma. Con la fórmula *S<sub>t</sub> = α·Y<sub>t</sub> + (1−α)·S<sub>t−1</sub>* suavizo la señal en tiempo real. Estoy usando **α = 0.2**, validando en Python que la reducción de ruido supere el **85 %**, que es la meta del proyecto. Esto conecta con la **Unidad 1 (descriptiva)** y su aplicación práctica.
>
> **Segunda: prueba t-Student** para comparar latencias. La pregunta es: ¿el comando por radio a 2.4 GHz responde más rápido que por Wi-Fi vía MQTT? Comparo muestras reales de latencia con una **t de dos muestras** — verificando antes los supuestos de normalidad — para tomar una decisión de arquitectura con datos y no con intuición. Ahí aplico las **unidades 6 y 7**.
>
> **Tercera: confiabilidad con Weibull.** Modelar el tiempo entre fallos del enlace de radio me permite justificar un *timeout* de seguridad del fail-safe entre **300 y 500 milisegundos**: si el robot pierde señal, se detiene solo. Y los eventos de intrusión del láser los modelo como **proceso de Poisson**. Eso cubre las **unidades 4, 5 y 8**.

**Nota interna:** las fórmulas se dicen con naturalidad, no se lee. Si interrumpe con preguntas técnicas, es buena señal: hay espacio en el diálogo.

---

## Bloque 4 — Plan de avance autónomo en 3 fases (3:10–4:10)

> Mi estrategia tiene tres fases. Lo importante: **valido primero la estadística en Python, con datos reales que capturo yo mismo**, porque tengo todo el hardware aquí en casa: HC-SR04, micrófono KY-037, módulo láser, radios nRF24L01, y las tres placas — ESP32, Arduino MEGA y UNO. No dependo de simulaciones ni del laboratorio.
>
> - **Fase 1 (ahora):** prototipar en Jupyter Notebooks con Pandas, SciPy y NumPy. De hecho, ya tengo un primer prototipo del filtro EMA con sus pruebas automatizadas.
> - **Fase 2:** portar la fórmula ya validada al firmware en C++, sobre el microcontrolador.
> - **Fase 3 (cuando volvamos al laboratorio):** medir latencias end-to-end del sistema completo y cerrar el informe final.
>
> Todo queda trazado en mi backlog del proyecto, tareas EST-01 a EST-07, alineadas a los requisitos RNF-2.1, RNF-2.2 y a la historia de usuario HU-03.

**Nota interna:** enfatizar "datos reales" y "no dependo del laboratorio" — es el argumento clave de viabilidad.

---

## Bloque 5 — Petición formal de contingencia (4:10–5:00)

> Y esto me lleva exactamente a lo que quiero pedirle hoy, profesora.
>
> ¿Contaría con su **aval para adelantar esta parte estadística del proyecto como esquema de contingencia de mi proceso académico**, mientras se define el regreso progresivo?
>
> Entiendo perfectamente que la prioridad ahora es el bienestar de todos, y que cualquier directriz de la universidad manda sobre cualquier plan mío. Me comprometo a adaptarme a lo que usted indique: cronograma, entregables, forma de evaluación — notebooks, informes parciales por unidad, lo que usted considere pertinente. Solo busco llegar al regreso con avances concretos, medibles y verificables.
>
> En síntesis: **¿qué condiciones pondría usted para aprobar este esquema?**

**Nota interna:** pausa después de la pregunta final. Escuchar y tomar notas. Si duda, ofrecer enviar por escrito el plan por fases con entregables por unidad del syllabus.

---

## Recordatorios rápidos (antes de colgar)

- [ ] Agradecer su tiempo y preocupación por el grupo.
- [ ] Confirmar canal de entrega de evidencias (correo, repositorio, notebook compartido).
- [ ] Preguntar fecha tentativa de siguiente chequeo, si ella lo desea.
- [ ] No prometer fechas que dependan de decisiones de la universidad.

## Posibles preguntas de la docente y respuestas sugeridas

| Pregunta probable | Respuesta sugerida |
|---|---|
| "¿Cómo capturas datos sin clases?" | Tengo todos los sensores y microcontroladores en casa; las mediciones son reales, no simuladas. Solo la fase de integración completa requiere el laboratorio. |
| "La asignatura no tiene enfoque en programación, ¿no?" | Correcta. Uso Python/Jupyter únicamente como herramienta de análisis — la misma que manejamos en clase (Pandas, SciPy). El C++ es solo la aplicación final de la fórmula que yo valide estadísticamente; el análisis y la interpretación siguen siendo de estadística. |
| "¿Cómo puedo verificar tu avance?" | Notebooks compartidos por unidad del syllabus, informes parciales con gráficas y conclusiones, y un backlog trazable (EST-01 a EST-07). Puedo enviar un cronograma escrito hoy mismo. |
| "¿Y si el calendario académico cambia?" | El plan es modular: cada fase cierra un entregable independiente, así se puede ajustar sin perder lo avanzado. |
| "¿Qué necesitas de mí?" | Su aval al esquema de contingencia, y que usted defina las evidencias y fechas de chequeo que considere adecuadas. |

---

### Fuentes internas

- `docs/emails-docentes/email-estadistica-maria-paula-rodas.md` — base del contenido estadístico y mapeo al syllabus
- `docs/prd.md` §5 — KPIs (EMA >85 %, RF <10 ms, Wi-Fi <50 ms, fail-safe 300–500 ms)
- `docs/requirements.md` — RNF-2.1, RNF-2.2, HU-03
- `docs/backlog.md` Área 5 — EST-01 a EST-07
- `stats/ema_filter.py` (+ tests) — evidencia existente del prototipo EMA
