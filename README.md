<div align="center">

<img src="https://img.shields.io/badge/Platform-ESP32%20%2B%20Android-00BFFF?style=for-the-badge&logo=espressif&logoColor=white"/>
<img src="https://img.shields.io/badge/Language-Kotlin%20%7C%20C%2B%2B%20%7C%20Python-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white"/>
<img src="https://img.shields.io/badge/Status-MVP%20Complete-22C55E?style=for-the-badge"/>
<img src="https://img.shields.io/badge/Built%20In-14%20Hours-FF6B35?style=for-the-badge"/>
<img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge"/>

<br/><br/>

# 🫁 DysphagiaGuard

### *Real-Time AI-Assisted Swallowing Disorder Detection & Aspiration Prevention*

**A neck-worn IoT wearable + Android clinical dashboard that continuously classifies swallowing events — protecting patients from silent aspiration, 24 hours a day.**

<br/>

> 🏥 *8 million+ dysphagia patients in India alone. Every unsafe swallow risks aspiration pneumonia — a leading cause of preventable death in stroke, Parkinson's, and ALS patients.*

<br/>

[🚀 Live Demo](#demo) · [📐 Architecture](#architecture) · [💼 Business Model](#business-model-summary) · [🛣 Roadmap](#roadmap) · [📋 Setup Guide](#setup)

</div>

---

## 📌 The Problem

Dysphagia (swallowing disorder) is a life-threatening complication of neurological conditions including **stroke, Parkinson's Disease, ALS, and traumatic brain injury**. The core danger is **silent aspiration** — food or liquid silently entering the airway without triggering a visible swallow, leading directly to aspiration pneumonia.

| Metric | Value |
|---|---|
| Patients with dysphagia in India | **8 million+** |
| Stroke patients who develop dysphagia | **~50%** |
| ICU patients with dysphagia risk | **~91%** (ventilated) |
| Aspiration pneumonia mortality rate | **20–30%** |
| Cost of current gold standard (VFSS) | **₹8,000–25,000/session** |
| VFSS frequency possible | **Episodic only** (not continuous) |

**DysphagiaGuard replaces episodic clinical assessment with continuous, real-time, at-the-throat monitoring — at a hardware cost of under ₹1,200.**

---

## ✅ Solution Overview

DysphagiaGuard is a **dual-sensor neck wearable** (ESP32 + MPU6050 IMU + analog microphone) paired with a **clinical-grade Android dashboard** that:

- Classifies every swallow event as **SAFE / UNSAFE / COUGH** in real time at 10 Hz
- Alerts caregivers with **haptic + visual + SMS fallback** on unsafe events
- Maintains a **persistent session history** and generates **PDF clinical reports** for SLPs
- Provides an **AI-powered clinical assistant** that gives contextual guidance in plain language
- Works **entirely on-device** — no cloud, no internet required, no data privacy risk

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     DYSPHAGUIA GUARD — SYSTEM FLOW                     │
│                                                                         │
│  [Mic Sensor]  ──► Peak Envelope ──►┐                                   │
│  [MPU6050 IMU] ──► IMU RMS       ──►├──► Threshold Classifier ──► JSON  │
│                    ZCR            ──►┘       (50ms window)              │
│                                                 │ WebSocket             │
│                                                 ▼ ws://192.168.4.1/ws   │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                     ANDROID APPLICATION                          │   │
│  │  OkHttp3  ──► MonitorViewModel ──► Jetpack Compose UI            │   │
│  │  (WS Client)   (StateFlow/Coroutines)   Waveform + Alerts        │   │
│  │                       │                                          │   │
│  │                  Room (SQLite)  ──► PDF Report (iTextG)          │   │
│  └──────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 🏗 Architecture

### Hardware Layer

| Component | Part | Role |
|---|---|---|
| Microcontroller | ESP32 (240MHz dual-core) | Main processing + WiFi AP |
| Motion Sensor | MPU6050 (I2C) | Laryngeal acceleration (IMU RMS) |
| Acoustic Sensor | Analog Electret Mic | Pharyngeal sound (peak envelope + ZCR) |
| Power | Li-Po 3.7V 500mAh | ~6 hours continuous operation |
| Enclosure | 3D printed neck mount | Patient-wearable form factor |
| **Total BOM Cost** | | **~₹800–1,200** |

### Firmware (ESP32)

```
Language    : C++ / Arduino Framework
Networking  : AsyncTCP + ESPAsyncWebServer (non-blocking WebSocket)
Storage     : Preferences.h (NVS — session persistence across resets)
Loop Rate   : 20Hz (50ms windowed signal analysis)
Broadcast   : 100ms JSON packets via WebSocket
```

### Android Application

```
Language    : Kotlin
UI          : Jetpack Compose + Material 3 + Glassmorphism
Architecture: MVVM + StateFlow + Kotlin Coroutines
Networking  : OkHttp3 (persistent WebSocket client)
Database    : Room (SQLite) — patient profiles + event history
PDF         : iTextG
Build       : Gradle (Kotlin DSL)
```

---

## 🔬 Classification Engine

The core signal processing pipeline runs a **20Hz windowed analysis** loop extracting three features per 50ms window:

| Feature | Sensor | Clinical Meaning |
|---|---|---|
| **IMU RMS** | MPU6050 | Laryngeal movement magnitude |
| **Mic Envelope** | Electret Mic | Pharyngeal sound burst amplitude |
| **ZCR** (Zero-Crossing Rate) | Electret Mic | Signal turbulence — detects wet gurgles |

These three features are evaluated against threshold logic to produce one of five labels:

| Label | IMU RMS | Mic Envelope | Duration | ZCR | Clinical Meaning |
|---|---|---|---|---|---|
| `SAFE` ✅ | Moderate | Low–Moderate | 100–200ms | Low | Normal coordinated swallow |
| `UNSAFE` ⚠️ | High | High | 100–200ms | Low | Wet gurgle / laryngeal penetration |
| `COUGH` 🟠 | Very High | High | <80ms | **High** | Turbulent airflow / aspiration reflex |
| `NOISE` | — | — | Too short | — | Filtered (low confidence window) |
| `IDLE` | Near-zero | Near-zero | — | — | Baseline, no event |

### The Cough–Swallow Distinction (Clinical Key Feature)

Silent aspiration often triggers a **subtle cough** as the only physiological reflex. DysphagiaGuard distinguishes coughs from swallows using:

- **Duration**: Swallows require coordinated laryngeal muscle activity lasting >100ms. Coughs are explosive diaphragm contractions lasting <80ms.
- **ZCR**: Coughs produce turbulent high-frequency airflow — significantly higher zero-crossing rate.
- Coughs trigger a **double-pulse haptic alert**, are tracked in the **Aspiration Risk Ratio**, and generate a **separate caregiver alert**.

---

## 📱 Core App Features

### 🔴 Live Monitor Screen
- Real-time waveform canvas rendering sensor data
- Color-coded event overlays (green / red / orange)
- Session counter (Safe / Unsafe / Cough totals)
- Aspiration Risk Ratio metric

### 🤖 AI Clinical Assistant
- Reads live `MonitorViewModel` state
- Generates contextual safety guidance for caregivers
- Issues strict stop-feeding warnings when `unsafe_count ≥ 3`
- Explains clinical terms in plain language for non-medical users

### 🚨 Emergency Alerting
- **Haptic**: Single pulse (safe), double pulse (cough), continuous (unsafe)
- **SMS Fallback**: After 3 consecutive UNSAFE events, opens system SMS with pre-filled caregiver alert (bypasses Android background SMS restrictions)
- **Visual**: Screen flash + banner notification

### 🗄 History & Reports
- All events persisted to Room (SQLite) with timestamp + session ID
- Daily and session-level summaries
- **PDF Export** (iTextG) — shareable clinical report for Speech-Language Pathologists (SLPs)

### 🎭 Demo Mode
- Kotlin Coroutines software simulation generating realistic sensor signals
- Safe / Unsafe / Cough patterns injected into the same data pipeline as real hardware
- Bi-directional: Manual trigger buttons also sync LED/motor feedback on ESP32 via WebSocket

---

## 🔌 Setup & Installation <a name="setup"></a>

### Prerequisites

```
Android Studio Hedgehog or later
ESP-IDF / Arduino IDE with ESP32 board support
Android device running API 26+ (Android 8.0)
```

### 1. Flash the Firmware

```bash
# Clone the repository
git clone https://github.com/Ashwinkumar-k10/Dysphagia_Guard.git
cd Dysphagia_Guard/firmware

# Open in Arduino IDE
# Board: ESP32 Dev Module
# Required Libraries: AsyncTCP, ESPAsyncWebServer, MPU6050, ArduinoJson

# Upload to ESP32
```

### 2. Build the Android App

```bash
cd Dysphagia_Guard/dysphagiaguard_app

# Open in Android Studio
# Sync Gradle → Build → Run on device or emulator
```

### 3. Connect Hardware to App

```
1. Power on the ESP32 wearable
2. On your Android device → WiFi Settings → Connect to "DysphagiaGuard-AP"
3. Launch the app → tap "Connect" on the Monitor screen
4. WebSocket auto-connects to ws://192.168.4.1/ws
```

### 4. Demo Mode (No Hardware Required)

```
Launch app → tap "Demo Mode" on the splash screen
The software simulation engine generates realistic event sequences automatically
```

---

## 📁 Repository Structure

```
Dysphagia_Guard/
├── firmware/                   # ESP32 C++ firmware
│   ├── main.ino                # Core acquisition + classification loop
│   ├── websocket_server.cpp    # AsyncWebSocket server
│   └── sensor_fusion.cpp       # IMU + mic feature extraction
│
├── dysphagiaguard_app/         # Android application (Kotlin)
│   └── app/src/main/
│       ├── data/               # Room DB, repositories, models
│       ├── ui/                 # Jetpack Compose screens
│       ├── viewmodel/          # MVVM ViewModels + StateFlow
│       └── network/            # OkHttp3 WebSocket client
│
├── ML_Pipeline/                # Python ML training pipeline
│   └── stitch_dysphagiaguard_mobile_monitor/
│
├── training/                   # Dataset + model training scripts
├── dysphagiaguard_demo/        # Standalone demo app (Flutter/Dart)
├── android/                    # Alternate Android build
│
├── HARDWARE_ARCHITECTURE.md    # Detailed circuit diagrams
├── software_architecture.md    # Full system design doc
├── PROGRESS.md                 # Build log (14-hour sprint)
└── README.md
```

---

## 💼 Business Model Summary <a name="business-model-summary"></a>

DysphagiaGuard targets a **₹2,400 Cr+ addressable market** in India across three segments:

| Tier | Segment | Model | Price |
|---|---|---|---|
| **Enterprise** | Hospitals & ICUs | Device lease + SaaS dashboard | ₹4,999/mo per ward |
| **B2B** | Elder care / nursing homes | Per-patient monthly subscription | ₹999/mo per patient |
| **Consumer** | Home caregivers | Device purchase + app subscription | ₹7,999 device + ₹299/mo |

> Full business model, financial projections, and GTM strategy in [`BUSINESS_MODEL.md`](./BUSINESS_MODEL.md)

---

## 🛣 Roadmap <a name="roadmap"></a>

### Phase 1 — MVP (✅ Complete)
- [x] ESP32 firmware with dual-sensor fusion
- [x] Android app with MVVM architecture
- [x] Real-time WebSocket pipeline
- [x] Room DB + PDF report generation
- [x] AI clinical assistant
- [x] Demo mode + bi-directional control

### Phase 2 — Q3 2025 (Clinical Validation)
- [ ] IRB-approved pilot with 20 patients at a Bangalore hospital
- [ ] Sensitivity / specificity benchmarking vs. videofluoroscopy
- [ ] Adaptive threshold calibration per patient
- [ ] CDSCO Class B Medical Device registration

### Phase 3 — Q4 2025 (ML Upgrade)
- [ ] Replace rule-based classifier with on-device TFLite model
- [ ] Self-learning personalized baselines from session history
- [ ] Early aspiration prediction (pre-event trend detection)
- [ ] Cloud sync for multi-device caregiver access

### Phase 4 — 2026 (Scale)
- [ ] Multi-patient ward dashboard (hospital B2B)
- [ ] EHR / EMR API integration (HL7 FHIR)
- [ ] CE Mark + FDA 510(k) pathway initiation
- [ ] Elder care home rollout partnerships

---

## 🏁 Results

| Metric | Value |
|---|---|
| Classification rate | **10 Hz** (real-time) |
| UI latency | **< 100ms** end-to-end |
| Detection classes | **3** (Safe, Unsafe, Cough) |
| Hardware cost | **~₹1,000** |
| Build time | **14 hours** (hackathon sprint) |
| Lines of code | **~3,200** (Kotlin + C++ + Python) |

> *"The system accurately captures multi-sensor data and classifies swallowing events with real-time visualization and caregiver alerts — demonstrating clinical-grade signal processing on commodity hardware."*

---

## 🔬 Regulatory Pathway

DysphagiaGuard is designed as a **Class B Medical Device** under Indian CDSCO MDR 2017 (equivalent to FDA Class II). The planned pathway:

1. **Current**: Research prototype / hackathon MVP (no regulatory requirement)
2. **Phase 2**: CDSCO pre-submission consultation
3. **Phase 3**: Clinical performance study (ISO 14155)
4. **Scale**: CDSCO Class B registration → CE Mark (EU) → FDA 510(k) (US)

---

## 👥 Team

| Name | Role | Institution |
|---|---|---|
| Ashwinkumar K | Hardware + Firmware + Android | Chennai Institute of Technology, Bangalore |
| Dhamarai | ML Pipeline + Signal Processing | Chennai Institute of Technology, Bangalore |

---

## 📄 License

This project is licensed under the **MIT License** — see [`LICENSE`](./LICENSE) for details.

---

## 🤝 Contributing

We welcome contributions from clinical engineers, SLPs, and embedded developers. Please open an issue to discuss before submitting a PR.

---

<div align="center">

**DysphagiaGuard — Protecting every swallow, in real time.**

*Built for EthAum Venture Partners Healthcare MVP Competition · May 2025*

[![GitHub stars](https://img.shields.io/github/stars/Ashwinkumar-k10/Dysphagia_Guard?style=social)](https://github.com/Ashwinkumar-k10/Dysphagia_Guard)

</div>
