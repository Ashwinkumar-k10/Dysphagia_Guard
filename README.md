<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:0d0221,30:1a0533,60:0d47a1,100:00e5ff&height=250&section=header&text=🫁%20DysphagiaGuard&fontSize=65&fontColor=ffffff&fontAlignY=42&desc=Real-Time%20AI-Assisted%20Swallowing%20Disorder%20Detection%20%26%20Aspiration%20Prevention&descAlignY=62&descSize=16&animation=fadeIn" width="100%"/>

<br/>

![ESP32](https://img.shields.io/badge/Platform-ESP32%20%2B%20Android-00E5FF?style=for-the-badge&logo=espressif&logoColor=black&labelColor=0d47a1)
![Kotlin](https://img.shields.io/badge/Language-Kotlin%20%7C%20C%2B%2B%20%7C%20Python-CE93D8?style=for-the-badge&logo=kotlin&logoColor=white&labelColor=4a148c)
![Status](https://img.shields.io/badge/Status-MVP%20Complete%20✅-00E676?style=for-the-badge&labelColor=1b5e20)
![Built](https://img.shields.io/badge/Built%20In-14%20Hours%20⚡-FF6D00?style=for-the-badge&labelColor=bf360c)
![Hz](https://img.shields.io/badge/Classification-10%20Hz%20Real--Time-FF4081?style=for-the-badge&labelColor=880e4f)
![BOM](https://img.shields.io/badge/Hardware%20Cost-Under%20₹1200-FFD740?style=for-the-badge&labelColor=ff6f00)

<br/><br/>

### *A neck-worn IoT wearable + Android clinical dashboard that continuously classifies swallowing events — protecting patients from silent aspiration, 24 hours a day.*

<br/>

> 🏥 *8 million+ dysphagia patients in India alone. Every unsafe swallow risks aspiration pneumonia — a leading cause of preventable death in stroke, Parkinson's, and ALS patients.*

<br/>

[![Demo](https://img.shields.io/badge/🚀%20Live%20Demo-00B0FF?style=for-the-badge&logoColor=white)](#-demo-mode)
[![Architecture](https://img.shields.io/badge/📐%20Architecture-7C4DFF?style=for-the-badge)](#-architecture)
[![Business](https://img.shields.io/badge/💼%20Business%20Model-FF6D00?style=for-the-badge)](#-business-model-summary)
[![Roadmap](https://img.shields.io/badge/🛣%20Roadmap-00E676?style=for-the-badge)](#-roadmap)
[![Setup](https://img.shields.io/badge/📋%20Setup%20Guide-FF4081?style=for-the-badge)](#-setup--installation)

</div>

---

## 🔴 The Problem

Dysphagia (swallowing disorder) is a life-threatening complication of neurological conditions including **stroke, Parkinson's Disease, ALS, and traumatic brain injury**. The core danger is **silent aspiration** — food or liquid silently entering the airway without triggering a visible swallow, leading directly to aspiration pneumonia.

<div align="center">

| 📊 Metric | 💡 Value |
|:---|:---|
| 🇮🇳 Patients with dysphagia in India | **8 million+** |
| 🧠 Stroke patients who develop dysphagia | **~50%** |
| 🏥 ICU patients with dysphagia risk | **~91%** (ventilated) |
| 💀 Aspiration pneumonia mortality rate | **20–30%** |
| 💸 Cost of gold standard (VFSS) | **₹8,000–25,000/session** |
| ⏱ VFSS frequency possible | **Episodic only** (not continuous) |

</div>

> 💥 **DysphagiaGuard replaces episodic clinical assessment with continuous, real-time, at-the-throat monitoring — at a hardware cost of under ₹1,200.**

---

## ✅ Solution Overview

DysphagiaGuard is a **dual-sensor neck wearable** (ESP32 + MPU6050 IMU + analog microphone) paired with a **clinical-grade Android dashboard** that:

| | Feature |
|:---:|:---|
| 🟢 | Classifies every swallow event as **SAFE / UNSAFE / COUGH** in real time at **10 Hz** |
| 🔔 | Alerts caregivers with **haptic + visual + SMS fallback** on unsafe events |
| 🗂 | Maintains a **persistent session history** and generates **PDF clinical reports** for SLPs |
| 🤖 | Provides an **AI-powered clinical assistant** that gives contextual guidance in plain language |
| 📡 | Works **entirely on-device** — no cloud, no internet required, no data privacy risk |

```
╔══════════════════════════════════════════════════════════════════════════╗
║                    DYSPHAGUIA GUARD — SYSTEM FLOW                       ║
║                                                                          ║
║  [Mic Sensor]  ──► Peak Envelope ──►┐                                    ║
║  [MPU6050 IMU] ──► IMU RMS       ──►├──► Threshold Classifier ──► JSON   ║
║                    ZCR            ──►┘         (50ms window)             ║
║                                                    │ WebSocket           ║
║                                                    ▼ ws://192.168.4.1/ws ║
║  ┌─────────────────────────────────────────────────────────────────┐     ║
║  │                     ANDROID APPLICATION                         │     ║
║  │  OkHttp3  ──► MonitorViewModel ──► Jetpack Compose UI           │     ║
║  │  (WS Client)  (StateFlow/Coroutines)   Waveform + Alerts        │     ║
║  │                       │                                         │     ║
║  │                  Room (SQLite)  ──► PDF Report (iTextG)         │     ║
║  └─────────────────────────────────────────────────────────────────┘     ║
╚══════════════════════════════════════════════════════════════════════════╝
```

---

## 🏗 Architecture

### ⚙️ Hardware Layer

<div align="center">

| 🔩 Component | 🛒 Part | 🎯 Role |
|:---|:---|:---|
| Microcontroller | ESP32 (240MHz dual-core) | Main processing + WiFi AP |
| Motion Sensor | MPU6050 (I2C) | Laryngeal acceleration (IMU RMS) |
| Acoustic Sensor | Analog Electret Mic | Pharyngeal sound (peak envelope + ZCR) |
| Power | Li-Po 3.7V 500mAh | ~6 hours continuous operation |
| Enclosure | 3D printed neck mount | Patient-wearable form factor |
| 💰 **Total BOM Cost** | | **~₹800–1,200** |

</div>

### 🔧 Firmware (ESP32)

```
Language     : C++ / Arduino Framework
Networking   : AsyncTCP + ESPAsyncWebServer (non-blocking WebSocket)
Storage      : Preferences.h (NVS — session persistence across resets)
Loop Rate    : 20Hz (50ms windowed signal analysis)
Broadcast    : 100ms JSON packets via WebSocket
```

### 📱 Android Application

```
Language     : Kotlin
UI           : Jetpack Compose + Material 3 + Glassmorphism
Architecture : MVVM + StateFlow + Kotlin Coroutines
Networking   : OkHttp3 (persistent WebSocket client)
Database     : Room (SQLite) — patient profiles + event history
PDF          : iTextG
Build        : Gradle (Kotlin DSL)
```

---

## 🔬 Classification Engine

The core signal processing pipeline runs a **20Hz windowed analysis** loop extracting three features per 50ms window:

<div align="center">

| 🧪 Feature | 📡 Sensor | 🏥 Clinical Meaning |
|:---|:---|:---|
| **IMU RMS** | MPU6050 | Laryngeal movement magnitude |
| **Mic Envelope** | Electret Mic | Pharyngeal sound burst amplitude |
| **ZCR** (Zero-Crossing Rate) | Electret Mic | Signal turbulence — detects wet gurgles |

</div>

These three features are evaluated against threshold logic to produce one of five labels:

<div align="center">

| Label | IMU RMS | Mic Envelope | Duration | ZCR | Clinical Meaning |
|:---:|:---|:---|:---|:---|:---|
| ✅ `SAFE` | Moderate | Low–Moderate | 100–200ms | Low | Normal coordinated swallow |
| ⚠️ `UNSAFE` | High | High | 100–200ms | Low | Wet gurgle / laryngeal penetration |
| 🟠 `COUGH` | Very High | High | <80ms | **High** | Turbulent airflow / aspiration reflex |
| ⬜ `NOISE` | — | — | Too short | — | Filtered (low confidence window) |
| ⬛ `IDLE` | Near-zero | Near-zero | — | — | Baseline, no event |

</div>

### 🔑 The Cough–Swallow Distinction (Clinical Key Feature)

> Silent aspiration often triggers a **subtle cough** as the only physiological reflex.

- ⏱ **Duration**: Swallows require coordinated laryngeal muscle activity lasting >100ms. Coughs are explosive diaphragm contractions lasting <80ms.
- 📈 **ZCR**: Coughs produce turbulent high-frequency airflow — significantly higher zero-crossing rate.
- 🚨 Coughs trigger a **double-pulse haptic alert**, are tracked in the **Aspiration Risk Ratio**, and generate a **separate caregiver alert**.

---

## 📱 Core App Features

### 🔴 Live Monitor Screen
- Real-time waveform canvas rendering sensor data
- Color-coded event overlays (🟢 green / 🔴 red / 🟠 orange)
- Session counter (Safe / Unsafe / Cough totals)
- Aspiration Risk Ratio metric

### 🤖 AI Clinical Assistant
- Reads live `MonitorViewModel` state
- Generates contextual safety guidance for caregivers
- Issues strict stop-feeding warnings when `unsafe_count ≥ 3`
- Explains clinical terms in plain language for non-medical users

### 🚨 Emergency Alerting

| Alert Type | Trigger | Action |
|:---|:---|:---|
| 📳 Haptic | Every event | Single / double / continuous pulse |
| 💬 SMS Fallback | 3 consecutive UNSAFE | Pre-filled caregiver distress message |
| 🔴 Visual | Every UNSAFE | Screen flash + banner notification |

### 🗄 History & Reports
- All events persisted to Room (SQLite) with timestamp + session ID
- Daily and session-level summaries
- **PDF Export** (iTextG) — shareable clinical report for Speech-Language Pathologists (SLPs)

### 🎭 Demo Mode
- Kotlin Coroutines software simulation generating realistic sensor signals
- Safe / Unsafe / Cough patterns injected into the same data pipeline as real hardware
- Bi-directional: Manual trigger buttons also sync LED/motor feedback on ESP32 via WebSocket

---

## 🔌 Setup & Installation

### Prerequisites

```
Android Studio Hedgehog or later
ESP-IDF / Arduino IDE with ESP32 board support
Android device running API 26+ (Android 8.0)
```

### 1️⃣ Flash the Firmware

```bash
# Clone the repository
git clone https://github.com/Ashwinkumar-k10/Dysphagia_Guard.git
cd Dysphagia_Guard/firmware

# Board      : ESP32 Dev Module
# Libraries  : AsyncTCP, ESPAsyncWebServer, MPU6050, ArduinoJson
# Upload to ESP32 via Arduino IDE
```

### 2️⃣ Build the Android App

```bash
cd Dysphagia_Guard/dysphagiaguard_app
# Open in Android Studio → Sync Gradle → Build → Run on device or emulator
```

### 3️⃣ Connect Hardware to App

```
1. Power on the ESP32 wearable
2. Android WiFi Settings → Connect to "DysphagiaGuard-AP"
3. Launch the app → tap "Connect" on the Monitor screen
4. WebSocket auto-connects to ws://192.168.4.1/ws
```

### 4️⃣ Demo Mode (No Hardware Required)

```
Launch app → tap "Demo Mode" on the splash screen
The software simulation engine generates realistic event sequences automatically
```

---

## 📁 Repository Structure

```
Dysphagia_Guard/
├── firmware/                   # ⚙️  ESP32 C++ firmware
│   ├── main.ino                #     Core acquisition + classification loop
│   ├── websocket_server.cpp    #     AsyncWebSocket server
│   └── sensor_fusion.cpp       #     IMU + mic feature extraction
│
├── dysphagiaguard_app/         # 📱  Android application (Kotlin)
│   └── app/src/main/
│       ├── data/               #     Room DB, repositories, models
│       ├── ui/                 #     Jetpack Compose screens
│       ├── viewmodel/          #     MVVM ViewModels + StateFlow
│       └── network/            #     OkHttp3 WebSocket client
│
├── ML_Pipeline/                # 🧠  Python ML training pipeline
├── training/                   # 📊  Dataset + model training scripts
├── dysphagiaguard_demo/        # 🎭  Standalone demo app (Flutter/Dart)
├── android/                    # 🤖  Alternate Android build
├── HARDWARE_ARCHITECTURE.md    # 🔩  Detailed circuit diagrams
├── software_architecture.md    # 🏗   Full system design doc
├── BUSINESS_MODEL.md           # 💼  GTM strategy + financials
├── PROGRESS.md                 # 📝  Build log (14-hour sprint)
└── README.md
```

---

## 💼 Business Model Summary

> 📈 DysphagiaGuard targets a **₹2,400 Cr+ addressable market** in India across three segments:

<div align="center">

| 🏢 Tier | 🎯 Segment | 💳 Model | 💰 Price |
|:---|:---|:---|:---|
| 🏥 **Enterprise** | Hospitals & ICUs | Device lease + SaaS dashboard | ₹4,999/mo per ward |
| 👵 **B2B** | Elder care / nursing homes | Per-patient monthly subscription | ₹999/mo per patient |
| 🏠 **Consumer** | Home caregivers | Device purchase + app subscription | ₹7,999 + ₹299/mo |

</div>

```
              🔄  DATA FLYWHEEL
────────────────────────────────────────────────────
  More patients wearing DysphagiaGuard
              ↓
  Larger proprietary swallowing event dataset
              ↓
  Better ML model accuracy → Better clinical outcomes
              ↓
  More hospital endorsements → More referrals
              ↓
  Data moat grows — competition can't replicate  🔒
────────────────────────────────────────────────────
```

> 📄 Full business model, financial projections, and GTM strategy in [`BUSINESS_MODEL.md`](./BUSINESS_MODEL.md)

---

## 🛣 Roadmap

### ✅ Phase 1 — MVP (Complete)
- [x] ESP32 firmware with dual-sensor fusion
- [x] Android app with MVVM architecture
- [x] Real-time WebSocket pipeline
- [x] Room DB + PDF report generation
- [x] AI clinical assistant
- [x] Demo mode + bi-directional control

### 🔬 Phase 2 — Clinical Validation
- [ ] IRB-approved pilot with 20 patients at a Bangalore hospital
- [ ] Sensitivity / specificity benchmarking vs. videofluoroscopy
- [ ] Adaptive threshold calibration per patient
- [ ] CDSCO Class B Medical Device registration

### 🧠 Phase 3 — ML Upgrade
- [ ] Replace rule-based classifier with on-device TFLite model
- [ ] Self-learning personalized baselines from session history
- [ ] Early aspiration prediction (pre-event trend detection)
- [ ] Cloud sync for multi-device caregiver access

### 🌏 Phase 4 — Scale (2026)
- [ ] Multi-patient ward dashboard (hospital B2B)
- [ ] EHR / EMR API integration (HL7 FHIR)
- [ ] CE Mark + FDA 510(k) pathway initiation
- [ ] Elder care home rollout partnerships

---

## 🏁 Results

<div align="center">

| 📊 Metric | 💡 Value |
|:---|:---|
| ⚡ Classification rate | **10 Hz** (real-time) |
| 🖥 UI latency | **< 100ms** end-to-end |
| 🏷 Detection classes | **3** (Safe, Unsafe, Cough) |
| 💰 Hardware cost | **~₹1,000** |
| ⏱ Build time | **14 hours** (hackathon sprint) |
| 📝 Lines of code | **~3,200** (Kotlin + C++ + Python) |

</div>

> *"The system accurately captures multi-sensor data and classifies swallowing events with real-time visualization and caregiver alerts — demonstrating clinical-grade signal processing on commodity hardware."*

---

## 🏥 Regulatory Pathway

DysphagiaGuard is designed as a **Class B Medical Device** under Indian CDSCO MDR 2017 (equivalent to FDA Class II).

```
  ✅ Current          📋 Phase 2             🔬 Phase 3
Research Prototype ──► CDSCO Pre-Submission ──► Clinical Study (ISO 14155)
                                                        │
                                        ┌───────────────┘
                                        ▼
                           🇮🇳 CDSCO Class B ──► 🇪🇺 CE Mark ──► 🇺🇸 FDA 510(k)
```

---

## 🤝 Contributing

We welcome contributions from clinical engineers, SLPs, and embedded developers. Please open an issue to discuss before submitting a PR.

---

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:00e5ff,40:0d47a1,70:4a148c,100:0d0221&height=140&section=footer&text=DysphagiaGuard%20—%20Protecting%20every%20swallow%2C%20in%20real%20time.&fontSize=18&fontColor=ffffff&fontAlignY=60&animation=fadeIn" width="100%"/>

<div align="center">

*Built for EthAum Venture Partners Healthcare MVP Competition · May 2025*
*Chennai Institute of Technology, Bangalore*

</div>
