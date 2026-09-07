/*
 * =============================================================================
 * Banco B — Test EMA Aislado Solo UNO | 6º Semestre UTP | HU-03 / RNF-2.1
 * Autor: Est. Tec. Desarrollo Software + Ing. Sistemas (2 años Arduino/C,
 *        2 años Python/JS/React) — Prototipo filtro stats/ema_filter.py:15
 * Filtro: S_t = α·Y_t + (1-α)·S_{t-1} con α=0.2 (mismo que rover-uno.ino:66)
 * Wiring: HC-SR04 VCC→5V GND→GND TRIG→D2 ECHO→D3 (rover-uno.ino:42)
 * Salida: Serial 115200 "raw,ema" para Tools → Serial Plotter (gráfica tiempo real)
 * Uso: mueve cartón 10→100 cm frente al HC-SR04, ve raw (ruidosa) vs ema (suave)
 * =============================================================================
 * POR QUÉ BANCO AISLADO:
 *   Para validar HU-03 sin montar todo el Rover (L298N, nRF, TCRT).
 *   Solo necesitas UNO + HC-SR04 + USB. Si el filtro no suaviza aquí,
 *   no suavizará en el Rover. Es como test unitario en Python/JS — aísla el módulo.
 *   En 2 años de Arduino aprendí que debuguear todo integrado es infierno;
 *   probar EMA solo te ahorra horas.
 *
 * PYTHON ANALOGY (2 años Python):
 *   Este sketch es el equivalente a stats/ema_filter.py pero en C embebido.
 *   En Python harías: ema = alpha*raw + (1-alpha)*ema_prev en pandas.
 *   Aquí igual pero en loop() a 20Hz con NewPing en vez de leer CSV.
 * =============================================================================
 */
#include <Arduino.h> // Core Arduino — Serial, millis, delay
#include <NewPing.h> // Lib HC-SR04 no bloqueante — evita pulseIn() bloqueante (FOSS)

#define ULTRASONIC_TRIG 2          // TRIG D2 — OUT dispara pulso 10us (ver rover-uno.ino:42)
#define ULTRASONIC_ECHO 3          // ECHO D3 — IN recibe eco (duración → distancia)
#define MAX_DISTANCE_CM 200        // Máx 200cm — NewPing devuelve 0 si >200 o sin eco
#define EMA_ALPHA 0.2f             // α=0.2 HU-03 / RNF-2.1 — mismo que stats/ema_filter.py:17 y rover-uno.ino:66
// α=0.2 → 20% raw + 80% historial. Suaviza picos pero responde en ~5 muestras (250ms a 20Hz)
// KPI prd.md:51 reducción ruido >85% con α=0.2

// Objeto sonar — maneja TRIG/ECHO y timeout internamente (no bloquea loop mucho)
NewPing sonar(ULTRASONIC_TRIG, ULTRASONIC_ECHO, MAX_DISTANCE_CM);

float ema = 0;                     // Valor filtrado — float para decimales (como float en Python)
bool inited = false;               // false hasta primer lectura válida — evita ema=0 inicial erróneo
unsigned long lastPrint = 0;       // Timer para throttling Serial a 20Hz (no saturar plotter)

void setup() {
  Serial.begin(115200); // 115200 rápido para CSV sin lag
  while (!Serial) delay(10); // Espera monitor (máx 10ms)
  Serial.println("raw,ema"); // Header CSV para Plotter — lo detecta como 2 series (como header en pandas DataFrame)
  Serial.println("=== Test EMA UNO — solo HC-SR04 α=0.2 ===");
  Serial.println("Mueve objeto 10-100 cm frente al sensor. raw=ruidoso, ema=filtrado.");
  // Instrucción para evaluador — sin esto no sabe qué hacer con el gráfico
}

void loop() {
  // --- Lectura HC-SR04 ---
  // NewPing ping_cm() ya filtra timeout y convierte tiempo a cm (tiempo * 343m/s /2 /100)
  // 0 = sin eco (fuera de rango o objeto absorbente). No bloquea más de ~30ms (200cm max)
  unsigned int raw = sonar.ping_cm();
  // Mantener último valor si no hay eco — evita caídas a 0 que EMA interpretaría como obstáculo
  // Es como forward-fill en pandas: df.fillna(method='ffill')
  static unsigned int lastRaw = 50; // static = persiste entre loops (como variable fuera de función en JS)
  if (raw == 0 || raw > MAX_DISTANCE_CM) raw = lastRaw; // Sin eco → usa previo (no 0)
  else lastRaw = raw; // Eco válido → actualiza previo

  // --- EMA ---
  if (!inited) {
    ema = raw; // Primera lectura — inicializa EMA con raw (no hay historial, como ema[0]=raw en Python)
    inited = true;
  } else {
    // Fórmula EMA: S_t = α·Y_t + (1-α)·S_{t-1}
    // En Python: ema = EMA_ALPHA * raw + (1 - EMA_ALPHA) * ema (idéntico)
    // Cada loop actualiza ema un 20% hacia raw — picos se atenúan 80%
    ema = EMA_ALPHA * raw + (1.0f - EMA_ALPHA) * ema;
  }

  // --- Salida throttled 20 Hz (50ms) — suficiente para Plotter fluido, evita saturar Serial ---
  // Sin throttling, loop a 100Hz mandaría 100 líneas/s y el Plotter se laguea (como spam console.log)
  // 20Hz = cada 50ms — como requestAnimationFrame en JS pero a 20fps
  if (millis() - lastPrint >= 50) {
    lastPrint = millis();
    Serial.print(raw);   // Valor crudo — ruidoso, con picos
    Serial.print(",");   // Separador CSV
    Serial.println(ema, 1); // Valor filtrado — 1 decimal (como round(ema,1) en Python)
    // En Plotter verás 2 líneas: raw con dientes de sierra, ema suave que sigue con retardo
  }

  delay(10); // 10ms base — loop real ~50ms por throttling, pero delay deja respirar CPU
}
