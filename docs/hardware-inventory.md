# Inventario de Hardware — AetherNet IoT

Fuente base: matriz de la sección 2 del documento académico (PDF Proyecto Integrador UTP), ampliada con los componentes de indicación visual (LED RGB, bombillo Tuya) que no estaban formalizados allí pero sí en `prd.md` y `requirements.md`.

| Sub-sistema | Hardware Asignado | Función y Protocolos |
|---|---|---|
| Gateway Central | ESP32-WROOM-32U + Antena U.FL + nRF24L01 (#1) | Servidor WebSockets/MQTT, comunicación UART con MEGA, enlace RF 2.4 GHz con el Rover. |
| Controlador de Potencia & Acceso | Arduino MEGA 2560 + Teclado 4x4 + Servomotor MG90S + Láser KY-008 (láser deshabilitado en `feature/firmware-mega-cerrojo`) | Cerrojo de puerta con clave PIN y trampa láser de seguridad. |
| **Indicador Visual Local** | **LED RGB ánodo común conectado a Arduino MEGA, pines PWM `44(R)/45(G)/46(B)` junto al teclado 4x4/servo — ver § Detalle de pines** | **Feedback físico inmediato del estado de acceso, sin depender de red: verde sincronizado a ventana 5 s de desbloqueo (HU-01), rojo remoto vía Gateway en fallo PIN / intrusión láser (HU-02). No requiere Wi-Fi ni MQTT para el verde local. Lógica invertida `LED_COMMON_ANODE` (`255-valor`).** |
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

## Detalle de pines — MEGA Cerrojo (RF-2.2 / HU-01)

Fuente: `firmware/mega-access/src/config.h` (branch `feature/firmware-mega-cerrojo`, actualizado 2026-08-26):

- **Keypad 4x4:** Rows `22,24,26,28` | Cols `30,32,34,36` — `Keypad@3.1.1`.
- **Servo MG90S:** Pin `9` PWM — `0°` bloqueada / `90°` desbloqueada (`SERVO_LOCKED/SERVO_UNLOCKED`).
- **LED RGB local (ánodo común):** `44(R),45(G),46(B)` PWM con `LED_COMMON_ANODE` invertido — HU-01 verde sólido mientras `doorUnlocked` (ventana `DOOR_AUTO_LOCK_MS=5000`), OFF al re-bloquear; rojo 1s en PIN erróneo, azul 50ms por dígito (no bloqueante).
- **Matriz de relés:** **eliminada** — no hay hardware en el inventario actual; todo rastro de `RELAY_PINS / relayStates / CMD:RELAY` removido del firmware en esta rama (ver decisión 2026-08-26).
- **Láser KY-008:** `TX 8 / RX 7` reservados pero **deshabilitados en esta rama**; se implementan en `feature/firmware-mega-laser` (HU-02, RF-2.3).
- **UART a Gateway ESP32:** `Serial2` `RX16/TX17` `115200` bd.

## Pendiente de definición

- Modelo/referencia específica del bombillo Tuya (para confirmar compatibilidad con `tuya-local`, ya que no todos los firmwares Tuya exponen el protocolo local).
