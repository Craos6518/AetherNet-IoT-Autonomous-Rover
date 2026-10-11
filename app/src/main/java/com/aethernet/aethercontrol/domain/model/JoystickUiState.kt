package com.aethernet.aethercontrol.domain.model

// =============================================================================
// JoystickUiState.kt — Estado UI Joystick Virtual | 6º Semestre UTP | MOV-05 RF-1.2
// Autor: Andres Felipe Martinez Henao
// Experiencia: 2 años JS/React (useState joystick), 2 años electrónica (L298N, nRF24L01),
//              1 año C (RoverCommand struct)
// Analogía React: este data class es como `type JoystickState = { x: number, y: number, leftPwm: number, rightPwm: number, mode: number }`
// en TS — estado normalizado -1..1 y su traducción a PWM -255..255 para L298N.
// Analogía C: espejo de `struct RoverCommand { int16_t left_pwm, right_pwm; uint8_t mode; }`
// en firmware/rover-uno/rover-uno.ino:106 y gateway-esp32.ino:133 — aquí con floats normalizados para UI.
// FOSS: data class pura Kotlin (RNF-3.1).
// Origen: MOV-05 RF-1.2 — Joystick virtual Compose Canvas captura vectores X,Y normalizados -1..1 → tank-steering.
// Flujo: JoystickScreen drag → JoystickViewModel.onPositionChanged(x,y) → JoystickMapper.vectorToPwm →
//        MqttManager.publishRoverCommand → aethernet/rover/command:69 → Gateway handleRoverCommand:241 → radio.write → rover-uno.ino:219.
// =============================================================================

/**
 * MOV-05 RF-1.2 — Estado UI joystick virtual.
 * x,y ∈ [-1, 1] normalizados (0,0 centro). leftPwm/rightPwm ∈ [-255, 255] para L298N.
 * mode: 0=stop (centro), 1=manual (joystick activo), 2=auto (no usado por joystick, reservado RF-3.2).
 * isSending true mientras publish en vuelo; error nullable para badge MQTT.
 */
data class JoystickUiState(
    val x: Float = 0f, // -1 izquierda .. 1 derecha — como axis X en gamepad (JoystickViewModel normaliza con radius)
    val y: Float = 0f, // -1 atrás .. 1 adelante — Y positivo es adelante (invertido respecto a Canvas Y-down)
    val leftPwm: Int = 0, // -255..255 — PWM izquierdo calculado (JoystickMapper.vectorToPwm)
    val rightPwm: Int = 0, // -255..255 — PWM derecho
    val mode: Int = 0, // 0 stop si x=y=0, 1 manual si |x|>0 o |y|>0 — espejo gateway-esp32.ino:136
    val isSending: Boolean = false, // true si publishRoverCommand en vuelo (opcional spinner)
    val error: String? = null, // mensaje si MQTT no conectado o publish fallido — como Result.Error msg
    val isActive: Boolean = false // true mientras dedo sobre joystick (dragging) — para anillo activo en UI
)
