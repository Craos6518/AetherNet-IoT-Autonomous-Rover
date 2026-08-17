# Product Requirements Document (PRD)
**Producto:** AetherNet IoT & Autonomous Rover  
**Versión:** 1.0.0  
**Fecha:** Agosto 2026  
**Contexto:** Proyecto Integrador Universitario (UTP) - 100% FOSS  

---

## 1. Visión del Producto
AetherNet es una plataforma integral de domótica y robótica móvil orientada a la seguridad y la automatización inteligente. Su propósito es demostrar que es posible construir un ecosistema de hardware y software robusto, tolerante a fallos y de baja latencia utilizando exclusivamente herramientas de código abierto (FOSS), eliminando la dependencia de servicios en la nube propietarios (como AWS o Tuya Cloud).

## 2. Audiencia Objetivo y Usuarios
1. **Administradores / Habitantes:** Usuarios finales que interactúan con la habitación a través de la App móvil (Android) para controlar luces, revisar accesos y pilotar el Rover en caso de emergencias.
2. **Evaluadores / Docentes Técnicos:** Profesores universitarios que auditarán la arquitectura de software, la aplicación de estadística, la electrónica y la automatización.

---

## 3. Casos de Uso Principales (Core Use Cases)

*   **Monitoreo y Alertas en Tiempo Real:** El sistema supervisa variables de entorno (presencia, ruido) e interrupciones físicas (láser). Ante una anomalía, alerta instantáneamente vía App Móvil y Telegram.
*   **Control Físico y Robótico Táctico:** El usuario puede tomar control remoto del Rover Tanque mediante un joystick virtual para inspeccionar visualmente un área, o ponerlo en modo de patrullaje autónomo anti-colisión.
*   **Gestión de Acceso Local:** Control de entrada a la habitación basado en un panel físico (Teclado 4x4) gestionado de forma descentralizada por un Arduino, reportando la auditoría al servidor central.

---

## 4. Alcance (Scope)

### ✅ Dentro del Alcance (In-Scope)
*   Aplicación nativa de Android en Kotlin (MVVM, Jetpack Compose).
*   Backend local contenerizado en Docker (FastAPI, PostgreSQL, Mosquitto MQTT).
*   Automatización LowCode mediante Node-RED.
*   Control local de bombillería inteligente vía IP (`tuya-local`).
*   Firmware C++ en microcontroladores interconectados por RF (2.4 GHz), UART y Wi-Fi.
*   Implementación de algoritmos estadísticos (Media Móvil Exponencial) en el firmware.

### ❌ Fuera del Alcance (Out-of-Scope)
*   Despliegues en servidores Cloud de pago (AWS, GCP, Azure).
*   Aplicación para iOS o interfaces web complejas en React/Angular.
*   Visión artificial o procesamiento de imágenes (Computer Vision) en el Rover.

---

## 5. Métricas de Éxito (KPIs y Criterios de Aceptación)

Para garantizar la viabilidad y calidad del producto, el sistema será evaluado contra los siguientes indicadores clave de rendimiento:

| Métrica | Objetivo | Propósito |
| :--- | :--- | :--- |
| **Latencia de Red (Wi-Fi/MQTT)** | $< 50$ ms | Asegurar que el cambio de color de las luces o el control desde la App se sienta inmediato. |
| **Latencia de RF (2.4 GHz)** | $< 10$ ms | Garantizar la respuesta instantánea de los motores del Rover ante comandos del Joystick. |
| **Precisión del Filtro Estadístico** | Reducción de ruido $> 85\%$ | Suavizar las lecturas del sensor HC-SR04 utilizando la función $S_t=\alpha \cdot Y_t+(1-\alpha) \cdot S_{t-1}$ para evitar falsos positivos en la evasión autónoma. |
| **Tasa de Falsos Positivos (Láser)** | $0\%$ | El algoritmo de interrupción hardware no debe disparar alarmas si no se rompe físicamente la barrera. |
| **Cumplimiento FOSS** | $100\%$ | Ninguna línea de código, SDK o plataforma debe requerir una licencia comercial restrictiva o pago recurrente. |

---

## 6. Suposiciones y Restricciones Técnicas

*   **Restricción de Red:** Todos los dispositivos Wi-Fi (ESP32, ESP8266, App, Bombillo Tuya y Servidor Docker) deben operar estrictamente en la misma subred de área local (LAN).
*   **Restricción de Energía:** El Rover depende de baterías Lipo/18650, por lo que su tiempo de operación autónoma está limitado a la capacidad de la batería, sin estación de recarga automática en esta versión.
*   **Contingencia:** Si el router principal falla o pierde conexión a Internet, la automatización IP se caerá, pero el acceso a la puerta (Teclado/Servomotor) seguirá operando gracias al procesamiento *Edge* del Arduino MEGA.
