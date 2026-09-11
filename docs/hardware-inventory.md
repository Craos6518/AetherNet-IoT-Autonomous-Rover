# Inventario de Hardware — AetherNet IoT

Fuente base: matriz de la sección 2 del documento académico (PDF Proyecto Integrador UTP), ampliada con el componente de indicación visual LED RGB (bombillo Tuya cancelado 2026-09-01).

| Sub-sistema | Hardware Asignado | Función y Protocolos |
|---|---|---|
| Gateway Central | ESP32-WROOM-32U + Antena U.FL + nRF24L01 (#1) | Servidor WebSockets/MQTT, comunicación UART con MEGA, enlace RF 2.4 GHz con el Rover. |
| Controlador de Potencia & Acceso | Arduino MEGA 2560 + Teclado 4x4 + Servomotor MG90S + Láser KY-008 (láser deshabilitado en `feature/firmware-mega-cerrojo`) | Cerrojo de puerta con clave PIN y trampa láser de seguridad. |
| **Indicador Visual Local** | **LED RGB ánodo común conectado a Arduino MEGA, pines PWM `44(R)/45(G)/46(B)` junto al teclado 4x4/servo — ver § Detalle de pines** | **Feedback físico inmediato del estado de acceso, sin depender de red: verde sincronizado a ventana 5 s de desbloqueo (HU-01), rojo remoto vía Gateway en fallo PIN / intrusión láser (HU-02). No requiere Wi-Fi ni MQTT para el verde local. Lógica invertida `LED_COMMON_ANODE` (`255-valor`).** |
| Rover Tanque Autónomo | Arduino UNO (#1) + Chasis Oruga **TT 6V 1:48 ×4** (antes 9-12V 170-350 RPM 3.5 KG·cm) + L298N + nRF24L01 (#2) + HC-SR04 + 3x TCRT5000 (A0 trasero/A1 izq/A2 der) + TP4056 + StepUp 5V | Navegación teledirigida o autónoma, anti-caída por bordes (TCRT5000), evasión de obstáculos y telemetría por RF. **Cambio 2026-09-11 por espacio:** chasis reducido → TT 6V 1:48 (potencia ↓ 77% torque, ver nota calce § Rover). **TCRT 2026-09-11:** A0 trasero, A1 izq, A2 der (corrige doc previo A0 izq). |
| Nodo Ambiental Remoto | ESP8266MOD + Tira LED + Sensor KY-037 (Sonido) | Iluminación ambiental reactiva al sonido y monitoreo acústico del cuarto. |
| Nodo Acceso Compacto | Arduino Nano + Sensor FC-51 + Módulo Bluetooth HC-06 | Detección discreta de presencia en entrada y enlace Bluetooth directo a smartphone. |
| Módulos de Laboratorio | 2x Arduino UNO (#2 y #3) + Sensor Color GY-31 + 2x HC-06 + 2x L9110S | Prototipado rápido, pruebas de calibración de color y bancos de prueba aislados. |
| ~~Iluminación Inteligente de Ambiente~~ | ~~Bombillo RGB Tuya~~ — **CANCELADO 2026-09-01** | Cancelado por políticas de integración (API) propietaria + `local_key` inaccesible (R-01, ADR-001, viola RNF-3.1). Se mantiene solo **LED RGB local** (ver `docs/adr/adr-001-cancelacion-tuya.md`). |

## Nota de diseño: LED RGB local (único indicador visual vigente)

Tras la cancelación del bombillo Tuya (2026-09-01, R-01, ADR-001 — políticas API propietaria), el **LED RGB del MEGA** es el único indicador visual de HU-01/HU-02:

- **LED RGB (Arduino MEGA):** indicador *local y determinista*. Funciona aunque caiga Wi-Fi/Docker, porque vive en el Edge que procesa teclado y láser (`prd.md` §6 Contingencia). Verde = acceso concedido (HU-01), Rojo = intrusión (HU-02).

Ya no hay capa ambiental de habitación completa.

## Detalle de pines — MEGA Cerrojo (RF-2.2 / HU-01)

Fuente: `firmware/mega-access/src/config.h` (branch `feature/firmware-mega-laser-v2` = `cerrojo` + `laser`, actualizado 2026-08-26 cerrojo + RF-2.3 HU-02):

- **Keypad 4x4:** Rows `22,24,26,28` | Cols `30,32,34,36` — `Keypad@3.1.1`.
- **Servo MG90S:** Pin `9` PWM — `0°` bloqueada / `90°` desbloqueada (`SERVO_LOCKED/SERVO_UNLOCKED`).
- **LED RGB local (ánodo común):** `44(R),45(G),46(B)` PWM con `LED_COMMON_ANODE` invertido — HU-01 verde sólido mientras `doorUnlocked` (ventana `DOOR_AUTO_LOCK_MS=5000`), OFF al re-bloquear; rojo 1s en PIN erróneo, azul 50ms por dígito (no bloqueante).
- **Matriz de relés:** **eliminada** — no hay hardware en el inventario actual; todo rastro de `RELAY_PINS / relayStates / CMD:RELAY` removido del firmware en esta rama (ver decisión 2026-08-26).
- **Láser KY-008 + LDR discreta:** `KY-008 S→8` (VCC→5V GND→GND) + `LDR discreta 5V→●→7 + 10kΩ→GND` (nodo ●→7 `INPUT` sin `PULLUP`, `HIGH=haz ~3.3V` `LOW=corte ~0.1V`) **activo en `feature/firmware-mega-laser-v2`** (`laser.cpp:15` `INPUT`, RF-2.3 HU-02 `SECURITY:` vía UART `38400`); deshabilitado en `feature/firmware-mega-cerrojo`. LDR con tubo negro anti-luz ambiente.
- **UART a Gateway ESP32:** `Serial2` `RX16/TX17` `38400` bd (estable con divisor 5V→3.3V; ver `config.h:77`).

## Calce Rover — TT 6V 1:48 vs 9-12V original (2026-09-11)

**Motivo:** espacio chasis aluminio original no cabe en baúl/escritorio lab → chasis TT compacto con **4× Motorreductor TT 6V 1:48** (reductora amarilla).

| Parámetro | Inicial 9-12V | Actual TT 6V 1:48 | Impacto |
|---|---|---|---|
| Tensión nominal | 9-12V | 6V (StepUp 5V → L298N drop 1.8V → 3.2V efectivo, usar 2S 7.4V recomendado) | L298N pierde 30% — alimentar con 7.4V 2S o cambiar a TB6612FNG |
| Consumo sin carga | 100 mA | 120-180 mA (medido) / stall 700-800 mA ×4 = 3.2A peak | TP4056 + StepUp 5V 2A **insuficiente** para 4 motores en stall → añadir pack 18650 2S 2Ah + BEC 5V 3A |
| Velocidad | 170-350 RPM (directa) | ~120 RPM en rueda (1:48) ~0.35 m/s @ 6V | -50% vel. máx — ajustar `BASE_SPEED 120→150` si chasis pesa <1kg |
| Torque | 3.5 KG·cm | 0.8 KG·cm @6V (stall 1:48) | **-77%** — no sube rampa >15°, evitar alfombra gruesa, reduce carga útil <300g |
| Tracción | 2 motores (oruga) | 4 motores (2 por lado en paralelo L298N) — ENA Izq (2 motores) ENB Der (2 motores) | L298N 2A máx por canal → 2×TT en paralelo 1.6A stall OK pero al límite térmico → disipador + `MIN_PWM_FOR_MOVEMENT 60→70` recalibrar |

**Acciones firmware:** `rover-uno.ino:88 MIN_PWM_FOR_MOVEMENT 60→70` (calibrar con chasis cargado), `BASE_SPEED 120→150` si `T4 RF RX` muestra avance lento. **Docs:** actualizar `prd.md` KPI maniobrabilidad y `notebooks/Firmware_Notebook.ipynb` §4.

## Fotos por subsistema (diario de campo)

| Subsistema | Fritzing | Foto real | Estado |
|---|---|---|---|
| MEGA Cerrojo | `docs/fritzing/AetherNet-P3-MEGA-Cerrojo-v1-breadboard.png` | `docs/Firmware/fotos/mega-panel.jpg` | ![FOTO PENDIENTE] si no existe — ver `docs/Firmware/README.md` |
| Rover | `docs/fritzing/AetherNet-P4-Rover-v1-breadboard.png` | `docs/Hardware/fotos/rover-uno.jpg` / `chasis-tt.jpg` | ![FOTO PENDIENTE] — ver `docs/Hardware/README.md` |
| Gateway | `docs/fritzing/AetherNet-P2-RF-Link-v1-breadboard.png` | `docs/Firmware/fotos/gateway-esp32.jpg` | ![FOTO PENDIENTE] |
| Láser | `docs/fritzing/AetherNet-P5-Laser-v1-breadboard.png` | `docs/Firmware/fotos/laser-ldr.jpg` | ![FOTO PENDIENTE] |

> Instrucción: 12MP, luz natural, fondo blanco, incluir regla. Checklist en cada `docs/<Materia>/README.md`.

## Pendiente de definición

- Pines exactos del LED RGB en el MEGA (documentar en el firmware, no aquí).
- ~~Modelo bombillo Tuya~~ — cancelado 2026-09-01.
