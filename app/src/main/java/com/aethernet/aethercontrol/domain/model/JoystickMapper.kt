package com.aethernet.aethercontrol.domain.model

// =============================================================================
// JoystickMapper.kt — Conversión vector → PWM tank-steering | 6º Semestre UTP | MOV-05 RF-1.2, MOV-06
// Autor: Andres Felipe Martinez Henao
// Experiencia: 2 años electrónica/Arduino (L298N, rover-uno.ino:70 MIN_PWM 60, EMA α=0.2),
//              2 años JS/React (joystick math), 1 año C (RoverCommand)
// Analogía React: este object es como `const JoystickMapper = { vectorToPwm(x,y) { return {left:..., right:...} } }`
// en JS — pure functions sin estado, testeables con JUnit (ver JoystickMapperTest si existe).
// Analogía C: espejo de `setMotorSpeeds(leftPwm, rightPwm)` en rover-uno.ino:316 pero inverso (aquí UI → PWM,
// allá PWM → IN/ENA). Deadband 60 espejo MIN_PWM_FOR_MOVEMENT 60 en rover-uno.ino:91.
// Analogía Python: como `def vector_to_pwm(x,y): return (left, right)` en stats/ — misma fórmula.
// FOSS: pure Kotlin (RNF-3.1).
// Origen: MOV-05 RF-1.2 (vectores X,Y -1..1 → left/right -255..255 tank) + MOV-06 deadband 60 + throttle 50ms.
// Fórmula tank-steering (arcade mixing): left = (y + x)*255, right = (y - x)*255, clamp -255..255.
// Y positivo = adelante (Canvas Y-down invertido en JoystickScreen antes de llamar aquí).
// =============================================================================

object JoystickMapper {

    const val MAX_PWM = 255 // PWM máx 8-bit — como MAX_PWM en rover-uno.ino:89 y gateway-struct
    const val MIN_PWM = 60 // deadband — MIN_PWM_FOR_MOVEMENT 60 en rover-uno.ino:91 (evita jitter, motor no vence fricción oruga por debajo de 60)
    const val THROTTLE_MS = 50L // ventana throttling MOV-06 — 50ms = 20Hz, <50ms MQTT prd.md:49 + RF <10ms:50, espejo gateway 100Hz loop 10ms

    /**
     * Convierte vector normalizado -1..1 a PWM tank-steering -255..255.
     * Fórmula arcade: left = (y + x)*255, right = (y - x)*255, luego clamp y deadband opcional.
     * Si vienes de React: es como `left = Math.round((y+x)*255)` en JS con clamp.
     * Si vienes de C: es la inversa de `executeManualCommand` en rover-uno.ino:361 pero con deadband espejo.
     *
     * @param x -1..1 (izq→der), y -1..1 (atrás→adelante, ya invertido si viene de Canvas)
     * @param applyDeadband true aplica deadband MIN_PWM 60 (MOV-06 espejo rover). false deja crudo para preview.
     * @return Pair(leftPwm, rightPwm) ∈ [-255,255]
     */
    fun vectorToPwm(x: Float, y: Float, applyDeadband: Boolean = true): Pair<Int, Int> {
        // Clamp entrada por seguridad — si Canvas entrega >1 por error de cálculo, lo corrige (como Math.min(Math.max(v,-1),1) en JS)
        val cx = x.coerceIn(-1f, 1f)
        val cy = y.coerceIn(-1f, 1f)

        // Arcade mixing — left = (y+x), right = (y-x). Ej: adelante (0,1) → left 255 right 255; giro der (1,0) → left 255 right -255 (tank turn)
        // Como `left = y + x` en gamepad tank-steering (ver https://www.impulseadventure.com/elec/robot-differential-steering.html)
        var left = ((cy + cx) * MAX_PWM).toInt()
        var right = ((cy - cx) * MAX_PWM).toInt()

        // Clamp -255..255 — si x=1,y=1 → left 510 → clamp 255 (como constrain() en arduino)
        left = left.coerceIn(-MAX_PWM, MAX_PWM)
        right = right.coerceIn(-MAX_PWM, MAX_PWM)

        if (applyDeadband) {
            // Deadband MOV-06 — espejo rover-uno.ino:364 `if (abs(left) < MIN_PWM_FOR_MOVEMENT) left = 0`
            // Evita jitter cerca de 0 y zumbido motor que no vence fricción oruga (medido: 40 no se mueve, 60 arranca)
            if (kotlin.math.abs(left) < MIN_PWM) left = 0
            if (kotlin.math.abs(right) < MIN_PWM) right = 0
        }
        return left to right
    }

    /**
     * Determina mode según vector: 0 stop si ambos PWM 0, 1 manual si algún PWM ≠0.
     * Gateway espera mode 0/1/2 (rover-uno.ino:109). Joystick nunca envía 2 auto (reservado autónomo RF-3.2).
     */
    fun vectorToMode(x: Float, y: Float, applyDeadband: Boolean = true): Int {
        val (l, r) = vectorToPwm(x, y, applyDeadband)
        return if (l == 0 && r == 0) 0 else 1
    }

    /**
     * Normaliza raw drag a -1..1 con círculo límite radius.
     * Si dedo sale del círculo, lo clamp al borde (como joystick físico con límite mecánico).
     * y se invierte (Canvas Y+ abajo → joystick Y+ adelante) si invertY true.
     */
    fun normalizeInCircle(rawX: Float, rawY: Float, radius: Float, invertY: Boolean = true): Pair<Float, Float> {
        if (radius <= 0f) return 0f to 0f
        var nx = rawX / radius // rawX es delta desde centro en px, radius es radio base — normaliza -1..1 (como `x/r` en JS)
        var ny = rawY / radius
        if (invertY) ny = -ny // Canvas Y-down → joystick Y-up (adelante es negativo en Canvas, positivo en lógica)
        // Si fuera del círculo (dist >1), clamp al borde — como `if (dist>1) { nx/=dist; ny/=dist }` en JS (normaliza vector)
        val dist = kotlin.math.sqrt(nx * nx + ny * ny)
        if (dist > 1f) {
            nx /= dist
            ny /= dist
        }
        return nx.coerceIn(-1f, 1f) to ny.coerceIn(-1f, 1f)
    }
}
