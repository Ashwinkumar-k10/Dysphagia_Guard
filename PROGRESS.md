# 🛠️ DysphagiaGuard — Hackathon Build Log

> **14-Hour Sprint** | 28th April 2026, 7:00 PM → 29th April 2026, 7:00 AM  
> Built end-to-end at a hackathon — firmware, ML pipeline, and Android app.

---

## ⏱️ Commit Timeline

| # | Timestamp | Commit | What Was Built |
|---|---|---|---|
| 1 | 28 Apr 2026 — 7:00 PM | 🚀 *Hackathon Begins* | Ideation, sensor selection, system design |
| 2 | 29 Apr 2026 — 12:39 AM | `feae76a` Initial firmware structure | ESP32 project setup, sensor pin mapping, WebSocket server skeleton, WiFi AP initialization |
| 3 | 29 Apr 2026 — 12:56 AM | `d611710` Added full firmware code | Complete signal acquisition loop (Mic ADC + MPU6050), feature extraction (IMU RMS, Mic Envelope, ZCR), 5-class rule-based classifier, JSON broadcast at 100ms intervals |
| 4 | 29 Apr 2026 — 03:14 AM | `c853c24` Added ML training pipeline | Sensor data logging scripts, feature engineering, model training pipeline for swallow event classification |
| 5 | 29 Apr 2026 — 06:18 AM | `37e67d0` Initial commit: README and APK | First working APK build, project documentation added |
| 6 | 29 Apr 2026 — 06:21 AM | `4110bfb` Add README | Final README pushed, repo made presentation-ready |
| — | 29 Apr 2026 — 7:00 AM | 🏁 *Hackathon Ends* | Submission complete |

---

## 📈 Build Phases

```
28 Apr  7:00 PM ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  29 Apr 7:00 AM
        │                                                              │
        ▼                                                              ▼
  [Ideation]──►[Firmware Init]──►[Full Firmware]──►[ML Pipeline]──►[App + Docs]
   7:00 PM       12:39 AM          12:56 AM           3:14 AM       6:18–6:21 AM
```

---

## 🧩 What Each Phase Delivered

### 🔧 Phase 1 — Firmware Foundation (12:39 AM)
- ESP32 project scaffolded with Arduino framework
- WiFi Access Point (`DysphagiaGuard-AP`) initialized
- WebSocket server route `/ws` registered
- MPU6050 (I2C) and Microphone (ADC) pin configuration

### ⚡ Phase 2 — Full Firmware (12:56 AM)
- 20Hz (50ms) windowed signal processing loop
- Feature extraction: IMU RMS, Mic Peak Envelope, Zero-Crossing Rate
- 5-class event classifier: `SAFE`, `UNSAFE`, `COUGH`, `NOISE`, `IDLE`
- JSON serialization and 10Hz WebSocket broadcast
- NVS (Non-Volatile Storage) for session persistence across reboots

### 🧠 Phase 3 — ML Training Pipeline (3:14 AM)
- Sensor data collection and logging utilities
- Feature engineering for swallow event dataset
- Model training pipeline for swallow classification
- Integration hooks for future on-device inference

### 📱 Phase 4 — Android App + Docs (6:18–6:21 AM)
- Full MVVM Android app with Jetpack Compose UI
- OkHttp3 WebSocket client connecting to ESP32
- Room database for event persistence
- AI Clinical Assistant, Demo Mode, Emergency SMS
- PDF report generation with iTextG
- README and APK pushed to GitHub

---

## ⚡ Stats

| Metric | Value |
|---|---|
| Total Duration | 14 Hours |
| Total Commits | 5 code commits |
| Firmware Lines of Code | ~400+ |
| Android Screens | 5 (Monitor, AI, History, Report, Demo) |
| Event Classes Detected | 5 (SAFE, UNSAFE, COUGH, NOISE, IDLE) |
| WebSocket Broadcast Rate | 10 Hz |
| End-to-End Latency | < 100ms |

---

*Built with 💙 under pressure — DysphagiaGuard Hackathon Team*
