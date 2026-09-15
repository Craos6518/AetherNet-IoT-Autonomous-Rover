# Guion de Presentación — AetherNet DevOps (Sprint 3)

**Asignatura:** TS6D3 DevOps — Universidad Tecnológica de Pereira (UTP)  
**Proyecto Integrador:** AetherNet IoT & Autonomous Rover  
**Docente:** Profesor Julián Sánchez  
**Expositor / Desarrollador:** Andrés Felipe Martínez Henao (Proyecto 100% unipersonal)  
**Fecha actual:** 12 de septiembre de 2026 (Sprint 3 — Día 4)  
**Fecha de inicio del proyecto:** 12 de agosto de 2026 (Tras los eventos telúricos del 10 al 12 de agosto)  
**Duración objetivo:** 8 a 9 minutos (~50 segundos por diapositiva)  

---

## 🎙️ Pautas y Directrices de Comunicación

1. **Tono general:** Profesional, directo, técnico y seguro. 
2. **Gramática en singular:** Todo el proyecto ha sido diseñado, programado, desplegado y documentado de forma individual por mí (**Andrés Felipe Martínez Henao**). Usaré estrictamente la primera persona del singular ("yo diseñé", "desarrollé", "implementé").
3. **Contexto temporal:** No estamos en semana 1. Hoy nos encontramos en el **Día 4 del Sprint 3** (12 de septiembre de 2026). El proyecto comenzó formalmente su ejecución el **12 de agosto de 2026**, justo después de evaluar el escenario de contingencia generado por el sismo del 10 al 12 de agosto.
4. **Independencia de laboratorios:** No requiero acceso a laboratorios universitarios este semestre para el hardware, ya que dispongo de todo el equipamiento necesario (ESP32, Arduino MEGA, Arduino UNO, sensores y módulos de radiofrecuencia) en casa desde hace bastante tiempo.
5. **Conceptos clave explicados:** Durante la exposición se definen con claridad **FOSS** y **MQTT**.

---

## ⏱️ Escaleta de Tiempos y Estructura

| Diapositiva | Título / Tema | Tiempo sugerido | Minuto acumulado |
|:---|:---|:---:|:---:|
| **Slide 1** | Portada: Sprint 3 (Día 4) y origen individual | 0:45 | 0:45 |
| **Slide 2** | Contexto temporal (10-12 Ago) y hardware en casa | 1:15 | 2:00 |
| **Slide 3** | Definiciones clave: FOSS y MQTT aplicados en AetherNet | 1:15 | 3:15 |
| **Slide 4** | Arquitectura en 3 capas y Edge Computing | 1:00 | 4:15 |
| **Slide 5** | Soberanía FOSS y enfoque Local-First | 1:00 | 5:15 |
| **Slide 6** | DevOps aplicado desde el primer día | 1:00 | 6:15 |
| **Slide 7** | Stack técnico: Docker Compose y MQTT en LAN (Taller 1) | 1:00 | 7:15 |
| **Slide 8** | CI/CD con GitHub Actions y `arduino-cli` (Taller 2) | 1:00 | 8:15 |
| **Slide 9** | Estado actual en Sprint 3 y preguntas finales | 0:45 | 9:00 |

---

## 🎙️ Guion Detallado Diapositiva por Diapositiva

---

### Slide 1: Portada — AetherNet DevOps (Sprint 3)
*(Tiempo: 0:45 min | Minuto 0:00 - 0:45)*

* **Frase gatillo:** *"Buenas tardes profesor Julián y compañeros. Hoy presento el estado actual de AetherNet en el Día 4 de nuestro Sprint 3."*
* **Qué decir:**
  > "Mi nombre es Andrés Felipe Martínez Henao. Soy el único desarrollador y responsable de este proyecto integrador de 5° semestre en Ingeniería de Sistemas y Computación de la UTP.
  > 
  > Hoy no estamos en semana 1; nos encontramos a **12 de septiembre de 2026**, ejecutando firmemente el Sprint 3. El proyecto inició formalmente su desarrollo el **12 de agosto**, inmediatamente después de procesar el escenario de contingencia planteado por el sismo que vivimos en la región entre el 10 y el 12 de agosto.
  > 
  > A lo largo de esta presentación mostraré cómo he aplicado principios de DevOps, una arquitectura cien por ciento **FOSS** y comunicación **MQTT** para construir un sistema robusto, operable de forma autónoma desde mi casa, sin depender de laboratorios ni de nubes comerciales."
* **Señalamiento visual:** Señalar el badge superior de *Sprint 3 (Día 4)* y el bloque lateral con la fecha de inicio.
* **Transición:** *"Para comprender el trasfondo de esta iniciativa, veamos cómo estructuré el inicio del proyecto y la independencia de hardware."*
* 🚫 **Qué NO decir:** No hablar en plural ("nosotros", "creamos", "en nuestro equipo"). El desarrollo es estrictamente individual.

---

### Slide 2: Contexto 10-12 de Agosto y Hardware en Casa
*(Tiempo: 1:15 min | Minuto 0:45 - 2:00)*

* **Frase gatillo:** *"Tomé la decisión individual de arrancar este proyecto el 12 de agosto, asegurando desde el primer momento que mi infraestructura fuera 100% autosuficiente."*
* **Qué decir:**
  > "Entre el 10 y el 12 de agosto, tras el sismo de 7.4 Mw y la incertidumbre sobre la presencialidad en la UTP, tomé una decisión de ingeniería: no podía permitir que el desarrollo de mi proyecto integrador quedara bloqueado por factores externos o restricciones de acceso al campus.
  > 
  > Por ello, el 12 de agosto inicié la planeación y ejecución bajo un enfoque **Local-First**. 
  > Además, un punto fundamental: **no requiero acceso a los laboratorios de la universidad este semestre**. Dispongo de todo el equipamiento de hardware necesario —placas ESP32, Arduino MEGA, Arduino UNO, sensores ultrasónicos, módulos de radiofrecuencia y chasis— en mi casa desde hace bastante tiempo, lo que me ha permitido programar, flashear y depurar sistemas embebidos de manera continua y sin fricciones."
* **Señalamiento visual:** Apuntar a la línea de tiempo del 10 al 12 de agosto y al bloque lateral de *Cero dependencia de laboratorios*.
* **Transición:** *"Para entender los cimientos técnicos de AetherNet, es fundamental definir dos conceptos que sustentan toda la arquitectura: FOSS y MQTT."*
* 🚫 **Qué NO decir:** No mencionar que el equipo de trabajo tuvo reuniones de brainstorming; todo el planteamiento y ejecución recae sobre mi trabajo personal.

---

### Slide 3: Definiciones Clave — FOSS y MQTT en AetherNet
*(Tiempo: 1:15 min | Minuto 2:00 - 3:15)*

* **Frase gatillo:** *"FOSS y MQTT son los dos pilares tecnológicos que garantizan la soberanía y la velocidad de comunicación de todo el sistema."*
* **Qué decir:**
  > "Quiero definir con precisión dos términos esenciales en mi arquitectura:
  > 
  > Primero, **FOSS**, que significa *Free and Open Source Software* (Software Libre y de Código Abierto). Esto implica que no utilizo ninguna librería comercial privativa, SDKs cerrados ni servicios de pago como AWS IoT o Tuya Cloud. Todo el código, desde el backend hasta los firmwares en C++, es 100% abierto, auditable y libre de regalías.
  > 
  > Segundo, **MQTT**, que significa *Message Queuing Telemetry Transport*. Es un protocolo de mensajería extremadamente ligero basado en el patrón Publicación/Suscripción (*Pub/Sub*), optimizado específicamente para dispositivos IoT con recursos acotados y redes propensas a intermitencias.
  > 
  > En AetherNet, el broker Mosquitto gestiona los tópicos bajo el prefijo `aethernet/#`, permitiendo que el gateway ESP32, el backend en FastAPI y la app móvil intercambien telemetría y comandos en menos de 50 milisegundos dentro de la LAN."
* **Señalamiento visual:** Apuntar a los bloques explicativos de FOSS superior y las tarjetas MQTT / Local-First inferiores.
* **Transición:** *"Esta filosofía se refleja directamente en la organización del sistema en tres capas distribuidas."*
* 🚫 **Qué NO decir:** No omitir las definiciones de FOSS y MQTT; el docente solicitó explícitamente su explicación.

---

### Slide 4: Arquitectura en 3 Capas y Edge Computing
*(Tiempo: 1:00 min | Minuto 3:15 - 4:15)*

* **Frase gatillo:** *"La arquitectura está diseñada para que la inteligencia resida en el borde (Edge Computing)."*
* **Qué decir:**
  > "El sistema opera sobre tres capas claramente separadas:
  > 
  > * **Capa 1 - Edge Computing (Microcontroladores en casa):** Aquí operan el Arduino MEGA (gestionando el cerrojo con teclado 4x4, servomotor, LED RGB local y láser KY-008) y el Arduino UNO (gestionando el rover tanque con motores L298N, ultrasonido HC-SR04 y sensores de línea TCRT5000). Las decisiones de seguridad críticas se toman aquí de forma instantánea, sin requerir red.
  > * **Capa 2 - Gateway de Coordinación:** Un ESP32 que actúa como puente traduciendo comunicación Serial UART y radiofrecuencia 2.4 GHz hacia Wi-Fi y MQTT.
  > * **Capa 3 - Servicios y Aplicación (Docker en LAN):** FastAPI, PostgreSQL y Mosquitto corriendo en contenedores, junto a la aplicación Android en Jetpack Compose."
* **Señalamiento visual:** Recorrer las tres capas de arriba hacia abajo, enfatizando el procesamiento en el borde.
* **Transición:** *"Profundicemos en por qué elegí este modelo soberano y descentralizado."*
* 🚫 **Qué NO decir:** No insinuar que el backend procesa la apertura de la puerta en tiempo real crítico; el MEGA decide localmente en la Capa 1.

---

### Slide 5: Soberanía FOSS y Enfoque Local-First
*(Tiempo: 1:00 min | Minuto 4:15 - 5:15)*

* **Frase gatillo:** *"Mantener el control absoluto del software y del protocolo de red me otorga independencia total ante fallos externos."*
* **Qué decir:**
  > "Al trabajar de forma unipersonal bajo un enfoque FOSS y Local-First, obtengo tres ventajas competitivas invaluables:
  > 
  > 1. **Soberanía tecnológica:** No dependemos de la disponibilidad de servidores en la nube de terceros. Si el enlace a internet del hogar o del sitio de despliegue cae, la red local sigue operando sin interrupciones.
  > 2. **Eficiencia con MQTT:** La sobrecarga de red de MQTT es mínima en comparación con HTTP tradicional, lo que permite cientos de mensajes de telemetría por segundo en un router doméstico estándar.
  > 3. **Rigor de desarrollo individual:** Al no delegar partes en otros integrantes, tengo trazabilidad completa de cada línea de código y cada conexión de hardware."
* **Señalamiento visual:** Apuntar a las tres tarjetas de soberanía, MQTT y desarrollo individual.
* **Transición:** *"Para gestionar este volumen de desarrollo sin perder calidad, implementé un flujo DevOps desde el primer día."*
* 🚫 **Qué NO decir:** No decir que se usaron herramientas propietarias de pago; todo el stack cumple con RNF-3.1.

---

### Slide 6: DevOps Aplicado desde el Primer Día
*(Tiempo: 1:00 min | Minuto 5:15 - 6:15)*
* **Frase gatillo:** *"Desarrollar solo no significa descuidar las buenas prácticas; al contrario, DevOps es mi marco de control de calidad."*
* **Qué decir:**
  > "Incluso trabajando de forma unipersonal, estructuré el proyecto bajo tres pilares de ingeniería DevOps:
  > 
  > * **Reproducibilidad:** Mediante un único archivo `docker-compose.yml`, levanto todo el entorno de servicios en segundos.
  > * **Trazabilidad:** Cada requisito funcional y tarea del backlog MoSCoW está relacionado directamente con commits limpios y ramas organizadas en Git.
  > * **Verificación Continua:** No confío únicamente en mis pruebas manuales; cada cambio de código activa pipelines automáticos en la nube."
* **Señalamiento visual:** Apuntar a los tres pilares (1. Reproducibilidad, 2. Trazabilidad, 3. Verificación).
* **Transición:** *"Veamos concretamente cómo opera el Taller 1 con Docker Compose."*
* 🚫 **Qué NO decir:** No omitir que las prácticas de DevOps aplican y benefician plenamente al desarrollo individual.

---

### Slide 7: Stack Técnico — Docker Compose y MQTT en la LAN
*(Tiempo: 1:00 min | Minuto 6:15 - 7:15)*

* **Frase gatillo:** *"En cumplimiento del Taller 1 de DevOps, toda la infraestructura backend está completamente orquestada en contenedores."*
* **Qué decir:**
  > "En la pantalla izquierda pueden ver el archivo `docker-compose.yml` que tengo operando:
  > 
  > * **PostgreSQL 16:** Con un volumen persistente para almacenar eventos de acceso y telemetría de forma segura.
  > * **Eclipse Mosquitto 2.0:** El broker MQTT expuesto en los puertos 1883 y 9001 para la red local.
  > * **FastAPI (Python 3.12):** La API central que utiliza una condición de salud (`depends_on: service_healthy`) para asegurar que la base de datos esté lista antes de aceptar peticiones.
  > 
  > Con el comando `docker compose up -d` despliego todo el ecosistema en mi equipo local, y gestiono las credenciales de forma segura mediante un archivo `.env` excluido en `.gitignore`, versionando únicamente la plantilla `.env.example`."
* **Señalamiento visual:** Señalar el bloque de código de Docker Compose y las tarjetas de servicios de la derecha.
* **Transición:** *"Y el segundo pilar práctico es nuestro pipeline de Integración Continua para software y firmware."*
* 🚫 **Qué NO decir:** No decir que el compose se despliega en un servidor remoto de pago; corre localmente en la LAN.

---

### Slide 8: CI/CD con GitHub Actions y `arduino-cli`
*(Tiempo: 1:00 min | Minuto 7:15 - 8:15)*

* **Frase gatillo:** *"Para el Taller 2 de DevOps, implementé un pipeline de GitHub Actions que compila tanto el backend en Python como el código en C++ de los microcontroladores."*
* **Qué decir:**
  > "El pipeline ejecuta cuatro trabajos paralelos en cada `push` o `pull request`:
  > 1. **Backend Test:** Ejecuta linters (`ruff`), tipado estático (`mypy`) y pruebas unitarias (`pytest`).
  > 2. **Firmware CI:** Utiliza `arduino-cli` en un entorno headless para compilar los sketches de C++ para ESP32, Arduino MEGA y Arduino UNO.
  > 3. **Docker Verify:** Construye las imágenes y verifica el endpoint `/health`.
  > 4. **Calidad y Seguridad:** Auditoría de dependencias y escaneo de vulnerabilidades.
  > 
  > **La innovación clave:** Compilar código embebido en CI me permite detectar errores de compilación o incompatibilidades de librerías en C++ antes de grabar el microcontrolador en casa."
* **Señalamiento visual:** Apuntar a los 4 bloques de jobs y al recuadro central sobre `arduino-cli`.
* **Transición:** *"Para concluir, veamos el balance exacto de lo que he construido hasta hoy en este Sprint 3."*
* 🚫 **Qué NO decir:** No decir que el pipeline flashea la placa por USB de manera remota; aclarar que realiza compilación cruzada y validación sintáctica.

---

### Slide 9: Estado Actual en Sprint 3 y Cierre
*(Tiempo: 0:45 min | Minuto 8:15 - 9:00)*

* **Frase gatillo:** *"Haciendo un balance al cuarto día de nuestro Sprint 3, el proyecto muestra avances reales y verificables."*
* **Qué decir:**
  > "A la fecha (12 de septiembre de 2026), he logrado consolidar:
  > * Arquitectura FOSS y Local-First operando con Mosquitto MQTT y Docker Compose.
  > * Firmware funcional en Arduino MEGA (cerrojo + láser) y Arduino UNO (rover con evasión ultrasónica y sensores de línea TCRT5000).
  > * Aplicación Android en Jetpack Compose con joystick virtual MQTT plenamente operativo.
  > 
  > Para el Sprint 4 que se avecina, completaré el filtrado EMA en Python y las notificaciones de seguridad.
  > 
  > Profesor Julián, esto es el reflejo de un trabajo unipersonal riguroso, guiado por la ingeniería y el compromiso. Quedo abierto a sus preguntas y comentarios. Muchas gracias."
* **Señalamiento visual:** Apuntar al cuadro de logros del Sprint 3 y abrir la sesión de preguntas.
* **Cierre:** Postura firme, sonrisa y atención al docente.

---

## 🛡️ Matriz de Respuestas a Preguntas Probables (Q&A)

| Pregunta del Docente o Compañeros | Respuesta Sugerida (Singular, Técnica y Precisa) |
|:---|:---|
| **"¿Por qué elegiste MQTT en lugar de HTTP tradicional para los microcontroladores?"** | *"Porque HTTP es un protocolo basado en solicitud/respuesta con cabeceras pesadas, inadecuado para microcontroladores con recursos limitados. MQTT usa un encabezado de solo 2 bytes, opera por publicación/suscripción asíncrona y mantiene bajo consumo de ancho de banda en la LAN."* |
| **"¿Qué significa exactamente que el proyecto sea FOSS en tu arquitectura?"** | *"Significa que no dependo de ninguna plataforma propietaria. Todo el stack, desde el broker Mosquitto y FastAPI hasta los sketches en C++ y la app en Kotlin, está construido con herramientas de código abierto, garantizando transparencia, auditabilidad y cero costos de licenciamiento."* |
| **"Al ser un desarrollo individual, ¿cómo garantizas que no se te pasen errores en el código?"** | *"Utilizando estrictamente las prácticas DevOps presentadas: tipado estático con `mypy`, linters con `ruff`, pruebas unitarias con `pytest`, y compilación automática de firmware con `arduino-cli` en GitHub Actions antes de cada integración."* |
| **"¿Cómo solucionaste la dependencia de laboratorios de la UTP este semestre?"** | *"No necesité laboratorios porque poseo todo el hardware (ESP32, MEGA, UNO, sensores) en mi casa desde antes. Toda la fase de prototipado, pruebas seriales UART y radiofrecuencia nRF24L01 la ejecuto directamente en mi banco de trabajo personal."* |
| **"¿Cómo valida el pipeline de CI la compilación de C++?"** | *"El workflow descarga el binario oficial de `arduino-cli`, configura los núcleos de ESP32 y AVR, instala las librerías necesarias especificadas en el repositorio y compila cada sketch (`.ino`) verificando que no existan errores de sintaxis o de enlace."* |
