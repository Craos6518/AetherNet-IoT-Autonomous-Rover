# Documento de Requisitos (Requirements Specification)
**Proyecto:** AetherNet IoT & Autonomous Rover  
**Institución:** Universidad Tecnológica de Pereira (UTP) - Programa de Ingeniería de Sistemas / Desarrollo de Software  
**Fecha:** Agosto 2026  

---

## 1. Descripción General
AetherNet IoT es un sistema distribuido de domótica modular, control de acceso y robótica móvil. La plataforma integra una aplicación nativa en Android (Kotlin), una arquitectura de microservicios backend contenerizada (Docker), automatización de eventos LowCode y algoritmos de suavizado estadístico para sensores hardware.

---

## 2. Requisitos Funcionales (RF)

### 2.1. Aplicación Móvil (Android/Kotlin)
* **RF-1.1:** La aplicación debe presentar un *Dashboard* en tiempo real desarrollado en Jetpack Compose que consuma datos de telemetría vía WebSockets/MQTT.
* **RF-1.2:** La aplicación debe incluir un mando virtual (Joystick) que envíe vectores de dirección (X,Y) al ESP32 para controlar el Rover.
* **RF-1.3:** El sistema debe proveer una conexión de contingencia vía Bluetooth (SPP) para operar nodos críticos si la red Wi-Fi falla.

### 2.2. Procesamiento Central y Gateway (ESP32 & MEGA)
* **RF-2.1:** El ESP32 debe actuar como *Gateway*, enrutando paquetes MQTT de la red local hacia el protocolo Serial (UART) del Arduino MEGA y radiofrecuencia (nRF24L01) hacia el Rover.
* **RF-2.2:** El Arduino MEGA debe procesar un teclado matricial 4x4 y controlar un servomotor MG90S para el bloqueo/desbloqueo de la puerta.
* **RF-2.3:** El sistema debe detectar la interrupción de la barrera láser (KY-008) y transmitir el evento de intrusión al Gateway.

### 2.3. Robótica Móvil (Rover Tanque Autónomo)
* **RF-3.1:** El Rover (Arduino UNO + L298N) debe recibir comandos de tracción con una latencia mínima a través del transceptor nRF24L01 (2.4 GHz).
* **RF-3.2:** El vehículo debe esquivar obstáculos frontales usando el sensor HC-SR04 y evitar caídas en bordes utilizando la matriz de sensores infrarrojos TCRT5000.
* **RF-3.3 (fail-safe RF):** Si el Rover no recibe un paquete RF válido del Gateway dentro de una ventana de timeout definida (a calibrar en firmware, valor de referencia inicial: 300-500 ms), debe detener inmediatamente ambos motores (fail-stop) hasta recibir un nuevo comando válido. El Rover no debe continuar en movimiento ni intentar maniobras autónomas adicionales mientras el enlace esté caído — ver `docs/architecture.md` §6 y riesgo relacionado en `docs/risk-register.md`.

### 2.4. Automatización y LowCode (Node-RED)
* **RF-4.1:** El motor de reglas debe capturar eventos críticos (ej. intrusión) y enviar una notificación push/mensaje a través de un Bot de Telegram.
* **RF-4.2:** El sistema debe cambiar el estado y color de un bombillo inteligente (protocolo Tuya Local) en respuesta a eventos de sensores sin depender de la nube externa.

---

## 3. Requisitos No Funcionales (RNF)

### 3.1. Arquitectura y DevOps
* **RNF-1.1:** Todos los servicios backend (FastAPI, Mosquitto MQTT, PostgreSQL) deben desplegarse como contenedores mediante `docker-compose`.
* **RNF-1.2:** El repositorio debe integrar un pipeline CI/CD en GitHub Actions que utilice `arduino-cli` para linting y compilación automática del firmware en cada `push`.

### 3.2. Rendimiento y Estadística
* **RNF-2.1:** Las lecturas analógicas del sensor ultrasónico y del micrófono (KY-037) deben ser procesadas utilizando un algoritmo estadístico de Media Móvil Exponencial (EMA) para mitigar el ruido.
* **RNF-2.2:** El sistema debe almacenar un registro histórico de accesos y eventos de sensores para análisis descriptivo y pruebas de hipótesis ($t$-Student de latencia).

### 3.3. Licenciamiento
* **RNF-3.1:** El 100% de la pila de software (lenguajes, bases de datos, herramientas de orquestación) debe utilizar licencias de código abierto (FOSS).

---

## 4. Historias de Usuario Principales (Scrum / BDD)

### HU-01: Control de Acceso por Teclado
**Como** usuario del sistema,
**Quiero** ingresar un PIN en el teclado físico,
**Para** desbloquear la puerta de la habitación de forma segura.
* **Criterios de Aceptación (BDD):**
  * *Dado* que la puerta está bloqueada,
  * *Cuando* el usuario ingresa el PIN correcto y presiona '#',
  * *Entonces* el MEGA gira el servomotor 90 grados, el ESP32 registra el acceso en la BD y el LED RGB pasa a verde.

### HU-02: Alerta de Intrusión (Trampa Láser)
**Como** administrador de la habitación,
**Quiero** ser notificado en mi celular si se cruza la puerta,
**Para** tomar acciones de seguridad inmediatas.
* **Criterios de Aceptación (BDD):**
  * *Dado* que el sistema está en modo "Armado",
  * *Cuando* la señal del láser KY-008 se interrumpe,
  * *Entonces* Node-RED envía un mensaje con prioridad alta a Telegram y el bombillo Tuya parpadea en rojo.

### HU-03: Filtrado Estadístico de Telemetría
**Como** analista de datos/desarrollador,
**Quiero** aplicar un filtro de ruido a las mediciones del Rover,
**Para** evitar decisiones erróneas del algoritmo autónomo.
* **Criterios de Aceptación (BDD):**
  * *Dado* que el HC-SR04 devuelve lecturas con dispersión,
  * *Cuando* se aplica el filtro EMA ($\alpha = 0.2$),
  * *Entonces* la señal resultante se estabiliza, descartando picos anómalos (falsos obstáculos) antes de enviar la orden a los motores.

### HU-04: Fail-safe del Rover ante pérdida de enlace RF
**Como** operador del sistema,
**Quiero** que el Rover se detenga si pierde comunicación con el Gateway,
**Para** evitar que quede en movimiento sin control ante una pérdida de señal.
* **Criterios de Aceptación (BDD):**
  * *Dado* que el Rover está en movimiento recibiendo comandos RF válidos,
  * *Cuando* transcurre la ventana de timeout sin recibir un nuevo paquete válido,
  * *Entonces* el firmware del UNO corta el PWM de ambos motores (fail-stop) y permanece detenido hasta recibir un comando nuevo.
