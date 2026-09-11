# Roadmap por Materia — Firmware (C++/arduino-cli)
Alcance: todos los temas necesarios para completar firmware MEGA/Gateway/Rover con arduino-cli 1.5.1. Ver `firmware.md` y `notebooks/Firmware_Notebook.ipynb`.

| Tema | Profundidad | Para qué RF | Búsqueda Google sugerida |
|---|---|---|---|
| Arduino millis() no bloqueante | Fluido | RF-2.2/2.3 | arduino millis non blocking delay without delay tutorial |
| nRF24L01 SPI RF24 library | Operativo | RF-2.1/3.1 | nRF24L01 RF24 library arduino datasheet 2.4GHz |
| HC-SR04 + EMA α=0.2 | Operativo | HU-03 | HC-SR04 ultrasonic datasheet timing EMA exponential moving average |
| TCRT5000 analogRead threshold | Operativo | RF-3.2 | TCRT5000 datasheet analogRead threshold infrared line tracking |
| L298N vs TB6612FNG | Conceptual | Rover TT 6V | L298N voltage drop 2V vs TB6612FNG datasheet |
| Servo MG90S PWM | Operativo | RF-2.2 | MG90S servo datasheet PWM 0-90 degrees Arduino |
| UART 38400 divisor 5V→3.3V | Operativo | RF-2.1 | UART baudrate 38400 voltage divider 5V 3.3V ESP32 |
| KY-008 + LDR divisor 10k | Operativo | RF-2.3 | KY008 laser datasheet LDR voltage divider Arduino |

## Datasheets
- nRF24L01+ datasheet Nordic Semiconductor pdf
- HC-SR04 datasheet ultrasonic distance sensor timing diagram
- TCRT5000 Vishay datasheet reflective optical sensor
- L298N datasheet STMicroelectronics dual H-bridge
- ESP32-WROOM-32U datasheet Espressif
- MG90S datasheet tower pro servo
