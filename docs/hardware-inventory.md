# Inventario de Hardware — AetherNet IoT

Fuente base: matriz de la sección 2 del documento académico (PDF Proyecto Integrador UTP), ampliada con los componentes de indicación visual (LED RGB, bombillo Tuya) que no estaban formalizados allí pero sí en `prd.md` y `requirements.md`.

| Sub-sistema | Hardware Asignado | Función y Protocolos |
|---|---|---|
| Gateway Central | ESP32-WROOM-32U + Antena U.FL + nRF24L01 (#1) | Servidor WebSockets/MQTT, comunicación UART con MEGA, enlace RF 2.4 GHz con el Rover. |
| Controlador de Potencia & Acceso | Arduino MEGA 2560 + Teclado 4x4 + Servomotor MG90S + Láser KY-008 + Relés | Manejo directo de matriz de relés, cerrojo de puerta con clave PIN y trampa láser de seguridad. |
| **Indicador Visual Local** | **LED RGB (ánodo/cátodo común) conectado a Arduino MEGA, pines PWM junto al teclado 4x4/servo** | **Feedback físico inmediato del estado de acceso, sin depender de red: verde al desbloquear (HU-01), rojo al detectar intrusión vía láser (HU-02). Es el indicador local; no requiere Wi-Fi ni MQTT para funcionar.** |
| Rover Tanque Autónomo | Arduino UNO (#1) + Chasis Oruga Aluminio + L298N + nRF24L01 (#2) + HC-SR04 + 3x TCRT5000 + TP4056 + StepUp 5V | Navegación teledirigida o autónoma, anti-caída por bordes (TCRT5000), evasión de obstáculos y telemetría por RF. |
| Nodo Ambiental Remoto | ESP8266MOD + Tira LED + Sensor KY-037 (Sonido) | Iluminación ambiental reactiva al sonido y monitoreo acústico del cuarto. |
| Nodo Acceso Compacto | Arduino Nano + Sensor FC-51 + Módulo Bluetooth HC-06 | Detección discreta de presencia en entrada y enlace Bluetooth directo a smartphone. |
| Módulos de Laboratorio | 2x Arduino UNO (#2 y #3) + Sensor Color GY-31 + 2x HC-06 + 2x L9110S | Prototipado rápido, pruebas de calibración de color y bancos de prueba aislados. |
| **Iluminación Inteligente de Ambiente** | **Bombillo RGB compatible Tuya / Smart Life / Smart Living** | **Control local vía `tuya-local` (LAN, sin nube Tuya). Orquestado por Node-RED: parpadeo en rojo ante intrusión (HU-02), cambios de color por eventos ambientales. Es el indicador de habitación completa, complementario al LED RGB local del MEGA.** |

## Nota de diseño: LED RGB vs. Bombillo Tuya

Son dos capas de feedback distintas y no redundantes:

- **LED RGB (Arduino MEGA):** indicador *local y determinista*. Funciona aunque caiga el Wi-Fi o el contenedor Docker, porque vive en el mismo Edge que procesa el teclado y el láser (ver restricción de Contingencia en `prd.md`, sección 6).
- **Bombillo Tuya:** indicador *ambiental y de red*, depende de LAN + Node-RED + `tuya-local`. Da visibilidad a toda la habitación, no solo al panel de acceso.

Cualquier agente de código que implemente HU-01 o HU-02 debe tocar **ambos** componentes, no solo uno.

## Pendiente de definición

- Pines exactos del LED RGB en el MEGA (documentar en el firmware, no aquí).
- Modelo/referencia específica del bombillo Tuya (para confirmar compatibilidad con `tuya-local`, ya que no todos los firmwares Tuya exponen el protocolo local).
