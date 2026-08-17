# Planeación de Sprints — AetherNet IoT

Fuente base: sección 4 del documento académico (PDF Proyecto Integrador UTP). Cada tarea se referencia contra `docs/requirements.md` para que un agente de código sepa qué Historia de Usuario o Requisito Funcional está habilitando, y no adelante trabajo de un sprint futuro sin que exista la base del sprint anterior.

> **Nota:** este archivo describe la planeación *original*. Si el equipo se desvía de fechas o alcance, actualízalo aquí — un agente que lea un sprint desactualizado puede proponer trabajo que ya no aplica o saltarse dependencias reales.

---

## Sprint 1 (Semanas 1-2): Infraestructura & Firmware Base

- Configuración del entorno Docker (FastAPI, PostgreSQL, Mosquitto MQTT) en Linux Mint. → RNF-1.1
- Creación del pipeline CI/CD en GitHub Actions con `arduino-cli` para compilar C++. → RNF-1.2
- Pruebas de comunicación SPI (nRF24L01) entre ESP32 y Arduino UNO. → RF-2.1 (base), RF-3.1

**Habilita:** toda la infraestructura sobre la que corren los sprints siguientes. Ningún sprint posterior debería avanzar sin que esto esté cerrado.

---

## Sprint 2 (Semanas 3-4): Domótica Fija & Control de Acceso

- Implementación del cerrojo (Teclado 4x4 + Servo MG90S) en Arduino MEGA. → RF-2.2, HU-01
- Integración de la matriz de relés con protocolo UART hacia el ESP32. → RF-2.1
- **LED RGB local en el MEGA** (feedback verde/rojo de acceso). → HU-01, HU-02 (ver `docs/hardware-inventory.md`)
- Primeras pantallas en Jetpack Compose (Kotlin) para conmutación de relés. → RF-1.1

**Depende de:** Sprint 1 (Docker + UART funcionando).

---

## Sprint 3 (Semanas 5-6): Rover Tanque Autónomo & Telemetría

- Montaje mecánico del chasis oruga con motorreductores 9-12V y L298N.
- Algoritmo anti-caída (3x TCRT5000) y evasión de obstáculos (HC-SR04). → RF-3.2
- Joystick virtual en Jetpack Compose enviando comandos de baja latencia. → RF-1.2

**Depende de:** Sprint 1 (enlace RF ESP32 ↔ UNO probado).

---

## Sprint 4 (Semanas 7-8): LowCode, Filtrado Estadístico y Cierre

- Filtro de Kalman / EMA en Python para procesar datos de sensores (HC-SR04, KY-037). → RNF-2.1, HU-03
- Flujos en Node-RED con bot de Telegram para notificaciones de seguridad. → RF-4.1, HU-02
- **Integración del bombillo Tuya vía `tuya-local`** (parpadeo rojo en intrusión). → RF-4.2, HU-02
- Pruebas de integración End-to-End, documentación y pruebas unitarias.

**Depende de:** Sprint 2 (evento de intrusión ya disparándose desde el MEGA) y Sprint 3 (telemetría del Rover ya fluyendo).

---

## Estado actual

_(Actualizar manualmente o enlazar al tablero del equipo — por ejemplo GitHub Projects — cuando exista.)_

- Sprint activo: `<pendiente de definir>`
- Última actualización de este archivo: Agosto 2026
