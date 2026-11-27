# FitPulse — Guion del video (máx. 3:00)

En Android Studio usa **Ctrl+G** para saltar a una línea y **Ctrl+Shift+N** para abrir un archivo por nombre.
Rutas cortas usadas abajo:

- `M/` = `mobile/src/main/java/com/example/fitpulse/mobile/`
- `W/` = `wear/src/main/java/com/example/fitpulse/`

---

## Parte A — Código (Kenny, 0:00–1:55)

### 0:00–0:15 · Intro
Qué es FitPulse y las 2 funciones: **Start/Stop Workout** con MessageClient (reloj → teléfono → reloj) y
**Daily Step Goal** con DataClient (teléfono → reloj). El teléfono es la fuente de verdad; el reloj es el control.

### 0:15–0:35 · Constantes — `M/common/WearableConstants.kt`
| Línea | Qué mostrar / decir |
|---|---|
| 17, 20, 23 | Paths de MessageClient: `/fitpulse/workout/start`, `/stop`, `/ack` |
| 25–30 | Comandos (`START`/`STOP`) y estados del ACK (`STARTED`/`STOPPED`/`ERROR`) |
| 40, 43, 46 | Path y llaves del DataClient: `PATH_DAILY_GOAL`, `KEY_DAILY_GOAL`, `KEY_GOAL_UPDATED_AT` |

Decir: el archivo es idéntico en `W/common/WearableConstants.kt`; si un path difiere en un carácter, el mensaje no llega.

### 0:35–0:55 · Modelos de datos — `M/common/DataModels.kt`
| Línea | Qué mostrar / decir |
|---|---|
| 15 | `WorkoutCommand(type, timestamp, elapsedSeconds)`: lo que manda el reloj |
| 20 | `toJsonBytes()`: convierte el objeto a JSON y luego a `ByteArray` (el payload) |
| 31 | `fromJsonBytes()`: el receptor reconstruye el objeto desde los bytes |
| 43 | `WorkoutAck(status, message, sessionId, durationSeconds, calories, dailyGoal)`: respuesta del teléfono |
| 83 | `WorkoutSession`: solo vive en el teléfono (historial) |

Decir: el mismo archivo está en `W/common/DataModels.kt` con solo el `package` cambiado.

### 0:55–1:20 · Teléfono, Feature 1 — `M/MainActivity.kt`
| Línea | Qué mostrar / decir |
|---|---|
| 77 | La Activity implementa `OnMessageReceivedListener` y `OnDataChangedListener` |
| 140–141 / 145–146 | Registra listeners en `onResume`, los quita en `onPause` |
| 164–177 | `onMessageReceived`: el `when (messageEvent.path)` separa START de STOP (sin colisión) |
| 188 | `handleStartCommand`: el teléfono guarda **su propia** hora de inicio |
| 215 | `handleStopCommand`: calcula duración y calorías, guarda la sesión |
| 254–258 | `sendAck`: responde por `PATH_WORKOUT_ACK` con `lifecycleScope.launch` + `await()` |

### 1:20–1:40 · Reloj, Feature 1 — `W/presentation/MainActivity.kt`
| Línea | Qué mostrar / decir |
|---|---|
| 174 | `sendCommand(type)` |
| 188 | `connectedNodes.await()`: busca el teléfono emparejado |
| 202–204 | Elige el path según el comando |
| 209 | `sendMessage(node.id, path, command.toJsonBytes()).await()` |
| 228–229 | `onMessageReceived`: solo acepta `PATH_WORKOUT_ACK` |
| 239 | `when (ack.status)`: el reloj pasa a ACTIVE **solo** cuando el teléfono confirma |

### 1:40–1:55 · Feature 2 (DataClient)
| Archivo : línea | Qué mostrar / decir |
|---|---|
| `M/MainActivity.kt:301` | `publishDailyGoal()`: guarda la meta y la publica |
| `M/MainActivity.kt:315–318` | `DataMap` con `KEY_DAILY_GOAL` y `KEY_GOAL_UPDATED_AT` |
| `M/MainActivity.kt:320–323` | `PutDataRequest.create(PATH_DAILY_GOAL)` + `setUrgent()` + `putDataItem().await()` |
| `W/presentation/MainActivity.kt:153–166` | `onDataChanged`: recorre el `DataEventBuffer`, filtra por path, lee el `DataMap`, `release()` |
| `W/presentation/MainActivity.kt:131–142` | `readCurrentGoal()`: al abrir la app lee la meta ya sincronizada |

---

## Parte B — Demo en vivo (Kevin, 1:55–3:00)

Emuladores emparejados, ambas apps abiertas lado a lado.

1. **1:55–2:05** Mostrar ambos en IDLE / READY con meta 8,000.
2. **2:05–2:20** En el reloj tocar **START WORKOUT** → el teléfono cambia a **ACTIVE** y ambos cuentan igual.
3. **2:20–2:35** Esperar ~10 s, tocar **STOP WORKOUT** → el teléfono agrega la sesión al historial y el reloj
   muestra "Saved mm:ss · N kcal".
4. **2:35–2:50** En el teléfono mover el slider (p. ej. 10,000) → **Sync goal to watch** → el reloj cambia solo.
5. **2:50–3:00** Cierre: nombres de ambos y link del repo.
