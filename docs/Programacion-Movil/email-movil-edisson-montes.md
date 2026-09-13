Asunto: Avance proyecto integrador AetherNet — TS6C3 Programación Móvil (contexto post-sismo)

Estimado profesor Edisson:

Le escribo con mucho respeto por la situación que atraviesa nuestra comunidad tras el **sismo del 10 de agosto (7:34 AM, mag. 7.4)**. Sé que la afectación ha sido desigual: compañeros que han perdido parte de sus hogares, otros que están de voluntarios en terreno, varios que han tenido que regresar a sus ciudades de origen. En mi caso, mi familia y yo no sufrimos daños graves, estoy colaborando como voluntario en lo que puedo, y tengo la posibilidad de seguir codeando desde casa.

Solo alcanzamos **una semana de clases** antes del evento. Agradezco mucho que haya confirmado en clase que **veremos Kotlin + Android Studio + Jetpack Compose** — ese es el stack que tengo montado y con el que quiero ir avanzando mientras se define el retorno a clases.

---

### ¿Qué es AetherNet? (Contexto general)

**AetherNet IoT & Autonomous Rover** es mi **Proyecto Integrador de 5º semestre**. Es un sistema real de **domótica + control de acceso + robot móvil** que se controla desde una **App nativa Android** que yo construyo.

**La App se llama "AetherControl"** y es la **interfaz humana** de todo el sistema: desde el celular el usuario ve sensores en tiempo real, abre la puerta con PIN, maneja el robot con un joystick virtual, y recibe alertas de intrusión — **todo funcionando en red local (Wi-Fi), y si se cae el Wi-Fi, cambia automático a Bluetooth clásico**.

---

### Stack de la App (tecnologías, explicadas en simple)

| Tecnología | Qué es | Para qué la uso en AetherControl |
|------------|--------|----------------------------------|
| **Kotlin** | Lenguaje oficial Android (moderno, seguro, conciso) | Todo el código de la app |
| **Jetpack Compose** | **UI declarativa** (nuevo estándar Android): describes *cómo se ve* la pantalla según el *estado*, y el sistema la redibuja solo | Pantallas reactivas: dashboard, joystick, PIN, alertas |
| **MVVM (Model-View-ViewModel)** | Arquitectura: **View** (Compose) ↔ **ViewModel** (lógica + estado) ↔ **Model** (datos/repositorio) | Separación limpia, testeable, ciclo de vida correcto |
| **Coroutines + Flow** | Programación asíncrona nativa Kotlin (hilos, streams reactivos) | MQTT/WebSocket en background, UI nunca se traba |
| **Hilt / Koin** | Inyección de dependencias (automatiza crear objetos) | ViewModels, Repositorios, Clientes MQTT/Bluetooth |
| **MQTT (Eclipse Paho / HiveMQ)** | Protocolo ligero pub/sub para IoT (tópicos: `telemetria/rover`, `alertas/intrusion`, `control/rover/cmd`) | Tiempo real con backend y gateway |
| **Bluetooth SPP (RFCOMM)** | Bluetooth **clásico** (no BLE) — puerto serie virtual | **Contingencia**: si Wi-Fi falla, app habla directo con Arduino Nano (HC-06) |
| **Permisos Android 12+** | `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, `ACCESS_FINE_LOCATION` | Requeridos en runtime para Bluetooth y escaneo |

> **Nota:** Usted definió **Kotlin + Compose** en clase. El syllabus original menciona Xamarin/.NET/C#, pero **los objetivos de aprendizaje son los mismos**: MVVM, UI reactiva, consumo de servicios, hardware/permisos, emuladores. Compose es el estándar actual de Android (2024-2025) y es 100% FOSS (requisito del proyecto integrador).

---

### Alineación: Unidad del Syllabus → Qué construyo en la App

| Unidad Syllabus (enfoque Xamarin) | Equivalente en Kotlin/Compose (lo que hago) | Qué avanzo en casa (emulador + celular propio) |
|-----------------------------------|---------------------------------------------|------------------------------------------------|
| **U1: Plataforma .NET** (Framework, IDE, C#, colecciones) | **Kotlin + Android Studio + Gradle**: Null-safety, Coroutines, Flow, Colecciones, Build system | ✅ Proyecto base MVVM + Hilt + dependencias listo |
| **U2: Front Móvil** (XAML, Pages, StackLayout/Grid, Views) | **Compose**: `State`/`StateFlow`, Material3 (`Column`, `Row`, `Box`, `LazyColumn`), Recomposición automática | 🟡 Pantallas: Dashboard (telemetría), Luces/bombillo Tuya, PIN cerrojo, Alertas |
| **U3: Back Móvil** (Xamarin.Forms, MVVM, Emuladores) | **MVVM nativo**: `ViewModel` + `StateFlow` + `Repository` + DI (Hilt), Emulador + Device físico | 🟡 `Repository` unificado (MQTT + Bluetooth), `ViewModel` por pantalla, tests unitarios |
| **U4: Datos y Entorno** (REST/JSON, APIs nativas, Hardware, Permisos, Multiplataforma) | **MQTT/WebSocket + Bluetooth SPP (RF-1.3) + Permisos runtime Android 12+** | 🟡 Cliente MQTT (reconexión, QoS), Módulo Bluetooth SPP, Pantalla permisos |

---

### Funcionalidades núcleo de "AetherControl" (con criterios BDD medibles)

| Feature | Qué hace el usuario | Tecnología clave | Criterio de Aceptación (BDD) |
|---------|---------------------|------------------|------------------------------|
| **Dashboard Telemetría** | Ve distancia robot, estado puerta, sensores, alertas — **en vivo** | Compose + `StateFlow` observando `Repository` (MQTT) | `Dado` robot conectado `Cuando` hay lectura `Entonces` UI se actualiza <200ms |
| **Joystick Virtual** | Arrastra dedo → robot se mueve en esa dirección/velocidad | `PointerInput` + `drag` gestures → vectores X/Y [-1,1] → throttle 50ms → MQTT `control/rover/cmd` | `Dado` joystick activo `Cuando` arrastra `Entonces` comando enviado cada 50ms máx |
| **PIN Cerrojo** | Teclado numérico en pantalla → envía "abrir" → puerta abre (servo) + LED verde | Compose `LazyColumn` numérico → MQTT `access/door/unlock` | `Dado` puerta cerrada `Cuando` PIN correcto + # `Entonces` servo 90° + LED verde (HU-01) |
| **Alerta Intrusión** | Notificación push + vibración + sonido + banner rojo si láser se corta | FCM local / `NotificationManager` + prioridad `HIGH` | `Dado` modo Armado `Cuando` láser interrumpido `Entonces` notificación <2s (HU-02) |
| **Fallback Bluetooth** | Si Wi-Fi cae → botón "Modo Bluetooth" → conecta a Arduino Nano (HC-06) → control básico | `BluetoothAdapter` + `BluetoothSocket` (RFCOMM/SPP) | `Dado` Wi-Fi caído `Cuando` activa BT `Entonces` comandos llegan a Nano (RF-1.3) |

---

### Mi estrategia: **App + Hardware real en casa (MQTT + Bluetooth), E2E completo cuando vuelva al lab**

**Tengo el hardware completo en casa:** ESP32 (gateway MQTT), Arduino Nano + HC-06 (Bluetooth SPP), Arduino MEGA (cerrojo), Rover UNO, sensores. Puedo **probar la App contra MQTT real (broker Mosquitto en Docker) y Bluetooth SPP real (Nano HC-06) ahora mismo**, no solo en emulador.

| Ahora (casa — App + Hardware real) | Cuando volvamos al lab (integración completa) |
|------------------------------------|-----------------------------------------------|
| UI completa en Compose (Dashboard, Joystick, PIN, Alertas) | Pruebas E2E sistema completo, medir KPIs finales |
| `ViewModel` + `Repository` + `StateFlow` testeados (JUnit) | Validación latencia real Wi-Fi <50ms, RF <10ms |
| **MQTT real contra Mosquitto (Docker) + Bluetooth SPP real contra Nano HC-06** | Fallback Wi-Fi→BT automático en escenario real |
| **Joystick → MQTT → ESP32 → Radio nRF24L01 → Rover UNO (prueba banco)** | Integración con Node-RED, Telegram, Tuya-local |
| Tests unitarios ViewModel (`MOV-10`) | Demo física completa, defensa |

---

### Evidencia técnica (repositorio — todo Kotlin/Compose)
- `app/` — Proyecto Android completo (Gradle, Kotlin, Compose, MVVM, Hilt)
- `app/src/main/java/.../ui/dashboard/` — Dashboard reactivo (telemetría MQTT → StateFlow → Compose)
- `app/src/main/java/.../ui/joystick/` — Joystick `PointerInput` + throttling 50ms
- `app/src/main/java/.../bluetooth/` — Módulo Bluetooth SPP (`BluetoothSocket` RFCOMM)
- `app/src/main/java/.../mqtt/` — Cliente MQTT (Paho/HiveMQ) + reconexión exponencial + topics tipados
- `docs/requirements.md` RF-1.1 a RF-1.3, HU-01 a HU-04 (criterios BDD)
- `docs/backlog.md` Área 1 — MOV-01 a MOV-10 (MoSCoW, sprints, dependencias)
- `docs/roadmap.md` §1 — Conocimientos previos/adquiridos, despliegue por sprint

---

Entiendo que el semestre se reacomodará y que la prioridad institucional es el bienestar de la comunidad. Este correo solo busca **explicar con claridad qué estoy construyendo, con qué herramientas, y cómo cada parte cubre los objetivos de su materia**, para que cuando se definan los nuevos tiempos podamos alinear la evaluación.

Quedo atento a sus indicaciones sobre cronograma, entregables (APK firmado + código + demo físico) y modalidad de evaluación.

Con respeto y solidaridad,

**[Su Nombre]**
Estudiante TS6C3 Programación Móvil - Grupo 401
Proyecto Integrador: AetherNet IoT & Autonomous Rover