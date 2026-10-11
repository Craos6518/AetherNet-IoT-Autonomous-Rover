# Inventario de Hardware — AetherNet IoT

Fuente base: matriz de la sección 2 del documento académico (PDF Proyecto Integrador UTP), ampliada con el componente de indicación visual LED RGB (bombillo Tuya cancelado 2026-09-01).

| Sub-sistema | Hardware Asignado | Función y Protocolos |
|---|---|---|
| Gateway Central | ESP32-WROOM-32U + Antena U.FL + nRF24L01 (#1) | Servidor WebSockets/MQTT, comunicación UART con MEGA, enlace RF 2.4 GHz con el Rover. |
| Controlador de Potencia & Acceso | Arduino MEGA 2560 + Teclado 4x4 + Servomotor MG90S + Láser KY-008 (láser deshabilitado en `feature/firmware-mega-cerrojo`) | Cerrojo de puerta con clave PIN y trampa láser de seguridad. |
| **Indicador Visual Local** | **LED RGB ánodo común conectado a Arduino MEGA, pines PWM `44(R)/45(G)/46(B)` junto al teclado 4x4/servo — ver § Detalle de pines** | **Feedback físico inmediato del estado de acceso, sin depender de red: verde sincronizado a ventana 5 s de desbloqueo (HU-01), rojo remoto vía Gateway en fallo PIN / intrusión láser (HU-02). No requiere Wi-Fi ni MQTT para el verde local. Lógica invertida `LED_COMMON_ANODE` (`255-valor`).** |
| Rover Tanque Autónomo | Arduino UNO (#1) + Chasis Oruga Aluminio + L298N + nRF24L01 (#2) + HC-SR04 + 3x TCRT5000 + TP4056 + StepUp 5V | Navegación teledirigida o autónoma, anti-caída por bordes (TCRT5000), evasión de obstáculos y telemetría por RF. |
| Nodo Ambiental Remoto | ESP8266MOD + Tira LED + Sensor KY-037 (Sonido) | Iluminación ambiental reactiva al sonido y monitoreo acústico del cuarto. |
| Nodo Acceso Compacto | Arduino Nano + Sensor FC-51 + Módulo Bluetooth HC-06 | Detección discreta de presencia en entrada y enlace Bluetooth directo a smartphone. |
| Módulos de Laboratorio | 2x Arduino UNO (#2 y #3) + Sensor Color GY-31 + 2x HC-06 + 2x L9110S | Prototipado rápido, pruebas de calibración de color y bancos de prueba aislados. |
| ~~Iluminación Inteligente de Ambiente~~ | ~~Bombillo RGB Tuya~~ — **CANCELADO 2026-09-01** | Cancelado por políticas de integración (API) propietaria + `local_key` inaccesible (R-01, ADR-001, viola RNF-3.1). Se mantiene solo **LED RGB local** (ver `docs/adr/adr-001-cancelacion-tuya.md`). |

## Nota de diseño: LED RGB local (único indicador visual vigente)

Tras la cancelación del bombillo Tuya (2026-09-01, R-01, ADR-001 — políticas API propietaria), el **LED RGB del MEGA** es el único indicador visual de HU-01/HU-02:

- **LED RGB (Arduino MEGA):** indicador *local y determinista*. Funciona aunque caiga Wi-Fi/Docker, porque vive en el Edge que procesa teclado y láser (`prd.md` §6 Contingencia). Verde = acceso concedido (HU-01), Rojo = intrusión (HU-02).

Ya no hay capa ambiental de habitación completa.

## Detalle de pines — MEGA Cerrojo (RF-2.2 / HU-01)

Fuente: `firmware/mega-access/src/config.h` (branch `feature/firmware-mega-cerrojo`, actualizado 2026-08-26):

- **Keypad 4x4:** Rows `22,24,26,28` | Cols `30,32,34,36` — `Keypad@3.1.1`.
- **Servo MG90S:** Pin `9` PWM — `0°` bloqueada / `90°` desbloqueada (`SERVO_LOCKED/SERVO_UNLOCKED`).
- **LED RGB local (ánodo común):** `44(R),45(G),46(B)` PWM con `LED_COMMON_ANODE` invertido — HU-01 verde sólido mientras `doorUnlocked` (ventana `DOOR_AUTO_LOCK_MS=5000`), OFF al re-bloquear; rojo 1s en PIN erróneo, azul 50ms por dígito (no bloqueante).
- **Matriz de relés:** **eliminada** — no hay hardware en el inventario actual; todo rastro de `RELAY_PINS / relayStates / CMD:RELAY` removido del firmware en esta rama (ver decisión 2026-08-26).
- **Láser KY-008:** `TX 8 / RX 7` reservados pero **deshabilitados en esta rama**; se implementan en `feature/firmware-mega-laser` (HU-02, RF-2.3).
- **UART a Gateway ESP32:** `Serial2` `RX16/TX17` `115200` bd.

## Pendiente de definición

- Pines exactos del LED RGB en el MEGA (documentar en el firmware, no aquí).
- ~~Modelo bombillo Tuya~~ — cancelado 2026-09-01.
