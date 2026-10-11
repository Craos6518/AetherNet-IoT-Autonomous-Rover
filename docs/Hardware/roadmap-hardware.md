# Roadmap por Materia — Hardware (Electrónica)

**Alcance:** electrónica aplicada del inventario `docs/hardware-inventory.md` — pines MEGA, fritzing `docs/fritzing/AetherNet-P*.png`, calce TT 6V 1:48 y datasheets. Ver `docs/Hardware/hardware.md` y `docs/Hardware/notebook/Diario_Hardware.ipynb`.

Backlog operativo: ver `docs/hardware-inventory.md` + `docs/Hardware/hardware.md` · Diario: [`../../docs/Hardware/notebook/Diario_Hardware.ipynb`](../../docs/Hardware/notebook/Diario_Hardware.ipynb) · Fotos: `docs/Hardware/fotos/` + `docs/Firmware/fotos/`

---

| Tema | Profundidad | Para qué RF | Búsqueda Google sugerida |
|---|---|---|---|
| MEGA 2560 pines 44/45/46 LED RGB, 22/24/26/28 ROW, 30/32/34/36 COL, 9 servo, 8 TX láser, 7 LDR, 16/17 UART | Operativo | RF-2.2/2.3 HU-01/02 | Arduino MEGA 2560 pinout PWM 44 45 46 LED RGB datasheet |
| KY-008 láser + LDR GL5528 divisor 10k tubo negro anti-luz | Operativo | RF-2.3 HU-02 | KY-008 laser datasheet LDR GL5528 voltage divider Arduino |
| TP4056 charging module + MT3608 StepUp 5V 2A (TT 6V 1:48) | Operativo | RF-3.1 Rover | TP4056 charging module datasheet MT3608 StepUp 5V |
| L298N drop 1.8V vs TB6612FNG + calce TT 6V 1:48 impacto -77% torque | Conceptual | RF-3.1/3.2 | L298N voltage drop vs TB6612FNG datasheet TT motor 1:48 |
| HC-SR04 TRIG2 ECHO3 + TCRT5000 analogRead threshold 500 | Operativo | RF-3.2 HU-03 | HC-SR04 datasheet TCRT5000 Vishay reflective sensor |
| Fritzing AetherNet-P2/P3/P4/P5 + inventario hardware-inventory.md | Operativo | Todos | Fritzing breadboard Arduino MEGA ESP32 wiring diagram |

## Datasheets

- nRF24L01+ datasheet Nordic Semiconductor pdf
- HC-SR04 datasheet ultrasonic distance sensor timing diagram
- TCRT5000 Vishay datasheet reflective optical sensor
- L298N datasheet STMicroelectronics dual H-bridge
- ESP32-WROOM-32U datasheet Espressif
- MG90S datasheet Tower Pro servo
- KY-008 laser datasheet
- LDR GL5528 datasheet
- TP4056 charging module datasheet
- MT3608 StepUp datasheet

---

## Búsquedas Google por bloque (copiar tal cual)

| Tema | Búsqueda sugerida |
|---|---|
| MEGA pinout LED | Arduino MEGA 2560 pinout PWM 44 45 46 LED RGB |
| KY-008 + LDR | KY-008 laser datasheet LDR GL5528 voltage divider Arduino |
| TP4056 MT3608 | TP4056 charging module MT3608 StepUp datasheet |
| L298N vs TB6612 | L298N voltage drop vs TB6612FNG datasheet |
| HC-SR04 TCRT5000 | HC-SR04 ultrasonic TCRT5000 Vishay datasheet |
| Fritzing inventario | Fritzing breadboard Arduino wiring hardware inventory |

---

## Orden crítico

1. **MEGA pines + LED RGB primero:** sin mapa de pines no hay cerrojo/láser validable.
2. **KY-008/LDR divisor + tubo negro:** calibrar umbral en sitio antes de HU-02.
3. **TP4056/MT3608 + calce TT 1:48:** verificar alimentación 2S 7.4V vs L298N drop antes de rover en pista.
