# Inventario de Hardware — AetherNet IoT

Fuente base: matriz de la sección 2 del documento académico (PDF Proyecto Integrador UTP), ampliada con el componente de indicación visual LED RGB (bombillo Tuya cancelado 2026-09-01).

| Sub-sistema | Hardware Asignado | Función y Protocolos |
|---|---|---|
| Gateway Central | ESP32-WROOM-32U + Antena U.FL + nRF24L01 (#1) | Servidor WebSockets/MQTT, comunicación UART con MEGA, enlace RF 2.4 GHz con el Rover. |
| Controlador de Potencia & Acceso | Arduino MEGA 2560 + Teclado 4x4 + Servomotor MG90S + Láser KY-008 | Cerrojo de puerta con clave PIN y trampa láser de seguridad. |
| **Indicador Visual Local** | **LED RGB (ánodo/cátodo común) conectado a Arduino MEGA, pines PWM junto al teclado 4x4/servo** | **Feedback físico inmediato del estado de acceso, sin depender de red: verde al desbloquear (HU-01), rojo al detectar intrusión vía láser (HU-02). Es el indicador local; no requiere Wi-Fi ni MQTT para funcionar.** |
| Rover Tanque Autónomo | Arduino UNO (#1) + Chasis Oruga Aluminio + L298N + nRF24L01 (#2) + HC-SR04 + 3x TCRT5000 + TP4056 + StepUp 5V | Navegación teledirigida o autónoma, anti-caída por bordes (TCRT5000), evasión de obstáculos y telemetría por RF. |
| Nodo Ambiental Remoto | ESP8266MOD + Tira LED + Sensor KY-037 (Sonido) | Iluminación ambiental reactiva al sonido y monitoreo acústico del cuarto. |
| Nodo Acceso Compacto | Arduino Nano + Sensor FC-51 + Módulo Bluetooth HC-06 | Detección discreta de presencia en entrada y enlace Bluetooth directo a smartphone. |
| Módulos de Laboratorio | 2x Arduino UNO (#2 y #3) + Sensor Color GY-31 + 2x HC-06 + 2x L9110S | Prototipado rápido, pruebas de calibración de color y bancos de prueba aislados. |
| ~~Iluminación Inteligente de Ambiente~~ | ~~Bombillo RGB Tuya~~ — **CANCELADO 2026-09-01** | Cancelado por políticas de integración (API) propietaria + `local_key` inaccesible (R-01, ADR-001, viola RNF-3.1). Se mantiene solo **LED RGB local** (ver `docs/adr/adr-001-cancelacion-tuya.md`). |

## Nota de diseño: LED RGB local (único indicador visual vigente)

Tras la cancelación del bombillo Tuya (2026-09-01, R-01, ADR-001 — políticas API propietaria), el **LED RGB del MEGA** es el único indicador visual de HU-01/HU-02:

- **LED RGB (Arduino MEGA):** indicador *local y determinista*. Funciona aunque caiga Wi-Fi/Docker, porque vive en el Edge que procesa teclado y láser (`prd.md` §6 Contingencia). Verde = acceso concedido (HU-01), Rojo = intrusión (HU-02).

Ya no hay capa ambiental de habitación completa.

## Pendiente de definición

- Pines exactos del LED RGB en el MEGA (documentar en el firmware, no aquí).
- ~~Modelo bombillo Tuya~~ — cancelado 2026-09-01.
