# Roadmap por Materia — Programación Móvil (App AetherControl)

**Alcance de este roadmap:** todos los temas necesarios para COMPLETAR la App Android "AetherControl" en Kotlin/Jetpack Compose con MVVM. Equivalencias con el programa académico (.NET/Xamarin→Kotlin/Compose) ya documentadas en [`programacion-movil.md`](programacion-movil.md).

**Estado al Aug 2026:** Gradle KTS ✅ · MVVM skeleton con `DashboardUiState` sellado ✅ · Room definido ✅ · **cliente MQTT inexistente (TODOs)** · **cero pantallas funcionales más allá del esqueleto** · job Android en CI desactivado.

Backlog operativo detallado: [`backlog-movil.md`](backlog-movil.md)

---

## Bloque 1 — Fundamentos Kotlin aplicados (dominio requerido, no tutorial)

| Tema | Profundidad | Para qué | Notas específicas |
|---|---|---|---|
| Null-safety, data classes, sealed interfaces | Fluido | Todo | Ya usado en `DashboardViewModel.kt`; mantener patrón exhaustivo de estados |
| Coroutines: scope, dispatchers, cancelación | Fluido | MQTT + UI | `viewModelScope` ya en uso; entender `Dispatchers.IO` para red/BD |
| Flow / StateFlow: cold vs hot, `stateIn`, `collectAsState` | Fluido | Telemetría en vivo | El stream MQTT se convertirá en Flow; la UI lo colecta como estado |

## Bloque 2 — Jetpack Compose (Sprint 2-3)

| Tema | Profundidad | Para qué requisito | Específico del proyecto |
|---|---|---|---|
| Composables, recomposición, `remember`/`mutableStateOf` | Operativo | RF-1.1 | Base de todas las pantallas |
| Layouts: Column/Row/Box, modifiers, LazyColumn/LazyGrid | Operativo | RF-1.1 | Dashboard de luces/bombillo = control color/brillo; lista de eventos |
| Theming Material3 (ya iniciado en Theme.kt) | Mantener | Calidad visual | Colores de estado (verde acceso/rojo intrusión) coherentes con HU-01/HU-02 |
| **Custom Canvas: joystick virtual** | Implementar desde cero | RF-1.2 | Núcleo diferencial del sprint 3: `Canvas` + detección de gesto (`pointerInput`/`detectDragGestures`) normalizando a vector X,Y ∈ [-1,1]; knob que regresa al centro al soltar |
| Animaciones simples (pulse en alerta) | Opcional | HU-02 | Solo si sobra tiempo |

## Bloque 3 — Arquitectura MVVM y datos (Sprint 2)

| Tema | Profundidad | Para qué requisito | Específico |
|---|---|---|---|
| ViewModel + UiState inmutable (patrón YA establecido) | Extender | Todos | Nuevas pantallas replican el sealed interface por feature |
| Repository pattern entre ViewModel y fuentes (Room/MQTT) | Implementar | MOV-03 | Capa `data/repository/`: única vía de datos; facilita tests MOV-10 |
| Room: DAOs, entidades, migraciones básicas | Ya definido → conectar | Persistencia local | `AppDatabase/Daos/Entities` existen; falta exponer y usar (caché de últimos eventos offline) |
| Inyección manual de dependencias (sin Hilt para acotar alcance) | Decidir y documentar | MOV-01 | Recomendado: constructor simple + Application class (ya existe `AetherControlApplication.kt`) |

## Bloque 4 — Comunicación: MQTT/WebSocket (Sprint 2-3) ← *núcleo pendiente*

| Tema | Profundidad | Para qué requisito | Específico |
|---|---|---|---|
| Cliente MQTT Android: Eclipse Paho (`org.eclipse.paho.client.mqttv3`) o HiveMQ client | Implementar completo | RF-1.1 | Decisión: Paho es el clásico con servicio persistente; documentar elección |
| Conexión sobre TCP 1883 vs WebSocket 9001 según ACL del broker | Verificar contra DEVOPS-02 | RF-1.1 | La App autentica como usuario `appbackend` (ver ACL en backlog DevOps) |
| Suscripción a telemetría y publicación de comandos | Implementar | RF-1.1/1.2 | Topics ya definidos en gateway: `aethernet/rover/command`, `aethernet/access/command`, `aethernet/app/#` |
| Throttling/debounce del joystick (~20 Hz) | Implementar | RF-1.2, MOV-06 | Sin esto, cada frame de arrastre publicaría decenas de mensajes/s y saturaría el enlace RF |
| Reconexión automática + indicador de estado de conexión | Implementar | MOV-09 | Mapear a estados `Connecting/Disconnected(reason)/Error` que YA existen en `DashboardUiState` |
| JSON serialization (kotlinx.serialization) | Implementar | Todos | Payloads idénticos a los del backend/gateway (misma fuente de verdad) |

## Bloque 5 — Hardware del dispositivo: Bluetooth SPP (Sprint 3)

| Tema | Profundidad | Para qué requisito | Específico |
|---|---|---|---|
| Bluetooth Clásico SPP ≠ BLE: `BluetoothAdapter`, `BluetoothSocket` | Implementar fallback | RF-1.3 | Conecta al HC-06 del nodo de acceso; protocolo = mismas tramas UART documentadas en DEVOPS-13 |
| Permisos runtime Android 12+: `BLUETOOTH_CONNECT`, `ACCESS_FINE_LOCATION` | Implementar | RF-1.3 | Flujo completo remember-launcher + manejo de denegación |
| Hilos: socket SPP bloqueante dentro de coroutine `Dispatchers.IO` | Aplicar | RF-1.3 | Patrón read/write loop con cancelación limpia |

## Bloque 6 — Calidad y entrega (Sprint 4)

| Tema | Profundidad | Para qué requisito |
|---|---|---|
| JUnit + coroutines-test para ViewModels (MOV-10): fake repository, Turbine opcional | Implementar | RNF calidad |
| Build release/debug en CI: habilitar job android-build (quitar `if: false`) | Configurar | Entregable APK demo |
| Manejo de errores de red UX (estados ya modelados) | Pulir | MOV-08 dashboard consolidado |

---

## Orden crítico

1. MOV-03 (MQTT client) desbloquea TODO lo visible: sin broker consumible no hay pantalla que probar.
2. Joystick custom (RF-1.2) es la pieza Compose más difícil → prototipar temprano aunque sea con broker de prueba (`mosquitto_pub` simulando telemetría).
3. Bluetooth solo después de que DEVOPS-13 fije el protocolo de tramas.
