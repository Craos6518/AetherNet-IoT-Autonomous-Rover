# Google Stitch Prompt — AetherControl (Versión Simple para Estudiante)

## Contexto
**App:** AetherControl — Control domótico + Rover
**Stack:** Android, Kotlin, Jetpack Compose, MVVM
**Usuario:** Estudiante — diseño simple, funcional, sin animaciones complejas

---

## Pantallas Necesarias (6 pantallas básicas)

### 1. **Pantalla Principal (Dashboard)**
```
┌─────────────────────────────────────┐
│ AetherControl        🟢 Conectado   │
├─────────────────────────────────────┤
│ [🔒 Puerta: CERRADA]  [🔴 Láser: OK]│
│ [💡 Bombillo: APAGADO] [🔴 LED: ---] │
│ [🔊 Sonido: 12%]      [📡 RF: OK]   │
├─────────────────────────────────────┤
│ [🏠 Inicio] [🎮 Rover] [🔐 Acceso]  │
│ [📊 Historial] [⚙️ Ajustes]         │
└─────────────────────────────────────┘
```
- 5 tarjetas simples en grid 2x3 (última posición libre o info RF)
- Solo texto + iconos básicos
- Botón "Abrir Puerta" en tarjeta de acceso
- ~~Switch on/off para bombillo Tuya~~ — cancelado ADR-001 (solo LED RGB local)
- Barra inferior con 5 iconos + texto

---

### 2. **Control del Rover**
```
┌─────────────────────────────────────┐
│ Rover          RF: ████░░ 45ms     │
├─────────────────────────────────────┤
│                                     │
│        [  JOYSTICK  ]               │
│        (círculo simple)             │
│                                     │
│   X: 0.0   Y: 0.0                   │
├─────────────────────────────────────┤
│ [Auto] [Manual] [Parar]   Vel: 50%  │
│ [PARADA EMERGENCIA] (rojo, grande)  │
├─────────────────────────────────────┤
│ Dist: 42cm  Batería: 78%            │
│ Bordes: [●][●][○]  Motores: 1.2A    │
└─────────────────────────────────────┘
```
- Joystick básico (arrastrar y suelta = vuelve al centro)
- 3 botones de modo + slider velocidad
- Botón rojo grande "PARADA EMERGENCIA"
- 4 líneas de telemetría abajo (texto simple)

---

### 3. **Control de Acceso**
```
┌─────────────────────────────────────┐
│ Control de Acceso                   │
├─────────────────────────────────────┤
│ Puerta: 🔒 CERRADA                  │
│                                     │
│ PIN: [ _ _ _ _ ]  [DESBLOQUEAR]     │
│                                     │
│ ─────────────────────────────────   │
│ Últimos accesos:                    │
│ 10:23  ✓  PIN correcto  (App)       │
│ 09:15  ✗  PIN incorrecto (Teclado)  │
│ 08:00  ✓  PIN correcto  (Teclado)   │
└─────────────────────────────────────┘
```
- Estado puerta grande arriba
- Campo PIN 4 dígitos + botón
- Lista simple de últimos 10 accesos

---

### 4. **Historial / Gráficas**
```
┌─────────────────────────────────────┐
│ Telemetría          [24h] [Semana]  │
├─────────────────────────────────────┤
│ Sensor: [Distancia ▼]               │
│                                     │
│  Gráfica simple de líneas           │
│  (distancia vs tiempo)              │
│                                     │
│  EMA activado: α=0.2                │
│  Ruido reducido: 87% ✓              │
├─────────────────────────────────────┤
│ [Exportar CSV]                      │
└─────────────────────────────────────┘
```
- Selector de sensor + rango tiempo
- Una gráfica simple (líneas)
- Texto con resultado EMA
- Botón exportar CSV

---

### 5. **Ajustes**
```
┌─────────────────────────────────────┐
│ Ajustes                             │
├─────────────────────────────────────┤
│ RED                                 │
│ Broker MQTT: 192.168.1.50:1883      │
│ [Probar conexión]                   │
│                                     │
│ BLUETOOTH                           │
│ HC-06: [Conectar]  Estado: ❌       │
│                                     │
│ ROVER                               │
│ Timeout fail-safe: [400] ms         │
│ Velocidad máx: [80]%                │
│                                     │
│ TEMA: [Claro] [Oscuro] [Sistema]    │
│                                     │
│ [Ver logs MQTT] [Versiones FW]      │
└─────────────────────────────────────┘
```
- Lista simple de opciones
- Campos de texto editables
- Switches para tema
- Botones de acción al final

---

### 6. **Pantalla de Carga / Error**
```
┌─────────────────────────────────────┐
│                                     │
│      AetherControl                  │
│                                     │
│   Conectando al broker...           │
│   ████████░░ 80%                    │
│                                     │
│   [Reintentar]  [Ajustes red]       │
│                                     │
└─────────────────────────────────────┘
```
- Pantalla simple para carga/error
- Barra de progreso
- 2 botones abajo

---

## Especificaciones Simples

### Colores (solo 4 principales)
```
Azul principal:   #2196F3  (botones, enlaces)
Verde éxito:      #4CAF50  (puerta abierta, conectado)
Rojo error:       #F44336  (puerta cerrada, emergencia, desconectado)
Gris fondo:       #F5F5F5  (fondo pantalla)
Texto:            #212121  /  #757575 (secundario)
```

### Tipografía
- **Títulos:** 20sp, negrita
- **Normal:** 16sp, regular
- **Valores/Telemetría:** 14sp, monospace
- **Botones:** 16sp, negrita

### Espaciado
- Margen pantalla: 16dp
- Entre tarjetas: 12dp
- Padding tarjetas: 16dp
- Radio bordes: 8dp (todo igual)

### Componentes Básicos (reutilizables)

**Tarjeta Simple:**
```
┌──────────────────┐
│ 🔒  Título       │
│    Valor grande  │
│    Subtexto      │
│ [Botón]          │
└──────────────────┘
```

**Joystick Básico:**
- Círculo gris 200dp
- Círculo azul 80dp (se arrastra)
- Vuelve al centro solo al soltar
- Sin física, sin vibración

**Botón Emergencia:**
- Rojo, 48dp alto, ancho completo
- Texto blanco "PARADA EMERGENCIA"
- Sin animación, solo ripple nativo

---

## Qué NO Incluir
- ❌ Animaciones complejas (spring, morph, shared element)
- ❌ Modos oscuras/claro forzados (usa tema del sistema)
- ❌ Gráficas interactivas (zoom, pan, tooltip)
- ❌ Sparkline en tarjetas
- ❌ Chips de estado animados
- ❌ Haptic feedback personalizado
- ❌ Biometría (solo PIN simple)
- ❌ Exportar PDF / reportes
- ❌ Diagnósticos avanzados (ping, RSSI, SNR)
- ❌ Permisos runtime complejos

---

## Flujo Mínimo a Mostrar
1. Abre app → carga → dashboard con datos
2. Tab Rover → mueve joystick → ve telemetría
3. Tab Acceso → escribe PIN → abre puerta
4. Tab Historial → ve gráfica distancia
5. Tab Ajustes → cambia IP broker → prueba conexión

---

## Entregables Stitch
6 pantallas en **modo claro solamente**:
1. Carga/Error
2. Dashboard
3. Rover
4. Acceso
5. Historial
6. Ajustes

+ Hoja componentes: Tarjeta, Botón, Joystick, Campo PIN, Gráfica simple

**Estilo:** Material 3 básico, componentes nativos, sin custom drawing complejo.