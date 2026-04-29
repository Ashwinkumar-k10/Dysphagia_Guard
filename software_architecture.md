# 📱 DysphagiaGuard — Software Architecture

---

## 1. Technology Stack

### Firmware (ESP32)

| Component | Technology |
|---|---|
| Language | C++ / Arduino Framework |
| Microcontroller | ESP32 |
| Networking | `AsyncTCP` + `ESPAsyncWebServer` |
| Storage | `Preferences.h` (Non-Volatile Storage) |

### Android Application

| Component | Technology |
|---|---|
| Language | Kotlin |
| UI Framework | Jetpack Compose + Material 3 + Glassmorphism |
| Architecture | MVVM + Kotlin Coroutines + `StateFlow` |
| Networking | `OkHttp3` (WebSocket client) |
| Local Database | Room (SQLite) |
| PDF Generation | `iTextG` |
| Build System | Gradle (Kotlin DSL) |

---

## 2. Data Flow

### Phase 1 — Hardware Acquisition & Analysis (ESP32)

The ESP32 runs a **20Hz (50ms) windowed** processing loop:

1. **Sensor Polling** — Analog microphone (pharyngeal acoustics) + MPU6050 IMU (laryngeal motion).
2. **Feature Extraction** per window:
   - **IMU RMS** — Root Mean Square of motion data
   - **Mic Envelope** — Peak acoustic amplitude
   - **ZCR (Zero-Crossing Rate)** — Turbulence proxy for wet gurgles / coughs
3. **Classification Engine** — Combines thresholds to label each window:

| Event | Duration | IMU RMS | Mic | ZCR | Meaning |
|---|---|---|---|---|---|
| `SAFE` | 100–200ms | Moderate | Low | Low | Normal coordinated swallow |
| `UNSAFE` | 100–200ms | High | High | Low | Wet gurgle / laryngeal penetration |
| `COUGH` | < 80ms | Very High | High | High | Aspiration reflex / turbulent airflow |
| `NOISE` | Too short | — | — | — | Filtered out |
| `IDLE` | — | Near-zero | Near-zero | — | Baseline, no event |

### Phase 2 — WebSocket Transmission

- ESP32 acts as a **WiFi Access Point** (`DysphagiaGuard-AP`)
- Every **100ms**, packages current state into JSON and broadcasts over `ws://192.168.4.1/ws`

```json
{
  "event": "UNSAFE",
  "imu_rms": 0.87,
  "mic_env": 412,
  "zcr": 0.12,
  "session_safe": 4,
  "session_unsafe": 3,
  "session_cough": 1
}
```

### Phase 3 — Android Reception & Reactive UI

```
OkHttp3 WebSocket
      │
      ▼
SwallowEventData (Kotlin object)
      │
      ▼
MonitorViewModel (StateFlow)
      │
      ▼
Jetpack Compose UI  ──►  WaveformCanvas redraws
                    ──►  Text color changes
                    ──►  Haptic feedback fires
                         (all < 100ms from ESP32 event)
```

---

## 3. Core Features

### A. Cough vs. Swallow Classifier
- **Clinical insight**: Silent aspiration leaves only a subtle cough as the reflex.
- **Detection**: Events < 80ms = cough (explosive diaphragm); > 100ms = swallow (coordinated muscle).
- **UI**: Coughs shown in **orange**, trigger double-pulse vibration, feed into Aspiration Risk Ratio.

### B. Context-Aware AI Clinical Assistant
- Reads live `MonitorViewModel` counts (safe / unsafe / cough).
- Logic: `unsafe >= 3` → generates strict stop-feeding warning.
- Explains clinical terms (Silent Aspiration, Laryngeal Penetration) in plain language for caregivers.

### C. Live Demo Engine (`DemoDataSource.kt`)
- Kotlin Coroutines simulate mathematically accurate sensor signals:
  - Safe swallow → smooth bell-curve IMU
  - Unsafe swallow → high-amplitude burst
  - Cough → rapid noisy double-peak
- Injected into the same pipeline as real hardware — indistinguishable in the UI.

### D. Bi-Directional Manual Triggering
- SAFE / UNSAFE / COUGH buttons on `LiveMonitorScreen` inject events on demand.
- When connected to ESP32, also sends WebSocket command back (`ws.send("COUGH")`), syncing physical LED + motor feedback.

### E. Emergency SMS Fallback
- After **3 consecutive UNSAFE** events → `Intent.ACTION_SENDTO` fires.
- Opens system SMS app with caregiver number + pre-filled distress message.
- Bypasses Android background SMS restrictions cleanly.

### F. Persistent Storage & PDF Reports
- Every event saved to **Room (SQLite)**.
- `SessionRepository` calculates totals and duration on session close.
- `DailyReportScreen` generates formatted PDF via `iTextG` — shareable with Speech-Language Pathologist (SLP).

---

## 4. App Screen Map

```
MainActivity
├── LiveMonitorScreen    ← WaveformCanvas, event counters, manual triggers
├── AiAssistantScreen    ← Context-aware clinical chat
├── AlertHistoryScreen   ← Past events from Room DB
├── DailyReportScreen    ← PDF report generation
└── DemoModeScreen       ← DemoDataSource.kt simulation
```

---

## 5. Architecture Diagram

```
╔══════════════════════════════════════════════════════╗
║              ANDROID APPLICATION (MVVM)              ║
║                                                      ║
║  View (Jetpack Compose)                              ║
║  └── LiveMonitorScreen                               ║
║  └── AiAssistantScreen          ◄──── StateFlow      ║
║  └── AlertHistoryScreen                  │           ║
║                                          │           ║
║  ViewModel (MonitorViewModel)            │           ║
║  └── _eventStream: MutableStateFlow      │           ║
║  └── totalSwallows, coughCount  ─────────┘           ║
║                  │                                   ║
║  Model / Data Layer                                  ║
║  └── WebSocketClient (OkHttp3)  ◄── ESP32 JSON       ║
║  └── Room Database              ── SwallowEventDao   ║
║  └── SessionRepository          ── PDF Export        ║
╚══════════════════════════════════════════════════════╝
```
