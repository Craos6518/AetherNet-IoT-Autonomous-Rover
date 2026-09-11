# Diario de Campo — Hardware

> **Inventario canónico:** `docs/hardware-inventory.md` · **Fritzing:** `docs/fritzing/` · **Notebook:** `notebooks/Diario_Hardware.ipynb`

## Checklist fotos hardware real (12MP, luz natural, fondo blanco, regla)

- [ ] **Gateway Central** — `fotos/gateway-esp32.jpg` — ESP32-WROOM-32U + nRF24L01 + antena U.FL
- [ ] **MEGA Cercojo panel** — `fotos/mega-panel.jpg` — MEGA + Keypad 4x4 + Servo MG90S + LED RGB 44/45/46
- [ ] **Láser KY-008 + LDR** — `fotos/laser-ldr.jpg` — haz + LDR con tubo negro
- [ ] **Rover chasis TT** — `fotos/chasis-tt.jpg` — TT 6V 1:48 ×4 + L298N + HC-SR04 + TCRT×3
- [ ] **Nodo Ambiental** — `fotos/nodo-ambiental.jpg` — ESP8266MOD + KY-037 + tira LED (si existe)
- [ ] **Nodo Compacto** — `fotos/nodo-compacto.jpg` — Nano + FC-51 + HC-06 (si existe)

Si falta: en `Diario_Hardware.ipynb` deja:

```markdown
![FOTO PENDIENTE](fotos/mega-panel.jpg) — tomar foto cenital con regla, 12MP, luz natural
```

## Fritzing → foto correspondencia

| Subsistema | Fritzing | Foto real destino |
|---|---|---|
| MEGA Cerrojo | `AetherNet-P3-MEGA-Cerrojo-v1-breadboard.png` | `fotos/mega-panel.jpg` |
| Rover | `AetherNet-P4-Rover-v1-breadboard.png` | `fotos/chasis-tt.jpg` / `fotos/rover-uno.jpg` |
| Gateway RF | `AetherNet-P2-RF-Link-v1-breadboard.png` | `fotos/gateway-esp32.jpg` |
| Láser | `AetherNet-P5-Laser-v1-breadboard.png` | `fotos/laser-ldr.jpg` |
| Ambiental | `AetherNet-P6-Ambiental-v1-breadboard.png` | `fotos/nodo-ambiental.jpg` |
