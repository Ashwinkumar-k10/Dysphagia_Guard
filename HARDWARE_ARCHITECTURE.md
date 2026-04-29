# 🔧 DysphagiaGuard — Hardware Architecture

---

## 1. Hardware Overview

DysphagiaGuard is a neck-worn wearable device that captures **pharyngeal acoustics** and **laryngeal motion** simultaneously using two sensors driven by an ESP32 microcontroller.

---

## 2. Bill of Materials (BOM)

| Component | Purpose | Approx. Cost |
|---|---|---|
| ESP32 Dev Board | Main microcontroller, WiFi AP, WebSocket server | ₹350 |
| MPU6050 IMU | Laryngeal motion / acceleration capture | ₹80 |
| Electret Microphone (MAX9814 / analog) | Pharyngeal sound capture | ₹30 |
| Li-Po Battery 3.7V 500mAh | Portable power supply | ₹150 |
| TP4056 Charge Module | Li-Po charging circuit | ₹30 |
| RGB LED | Visual event feedback (SAFE=green, UNSAFE=red, COUGH=orange) | ₹10 |
| Vibration Motor | Haptic alert on device | ₹20 |
| PCB / Breadboard + Wires | Interconnect | ₹80 |
| 3D Printed / Foam Enclosure | Neck-worn form factor | ₹50 |
| **Total** | | **~₹800** |

---

## 3. Sensor Placement Rationale

```
         ┌─────────────────────┐
         │      NECK (Front)   │
         │                     │
         │   [Thyroid          │
         │    Cartilage]       │
         │        ▲            │
         │        │            │
         │   MPU6050 (IMU)     │  ← Placed here to capture
         │   + Microphone      │    laryngeal elevation during swallow
         └─────────────────────┘
```

### Why the Thyroid Cartilage?

| Sensor | Placement | Reason |
|---|---|---|
| **MPU6050 IMU** | Over thyroid cartilage | The larynx elevates 2–3 cm during a normal swallow. IMU captures this vertical acceleration signature directly. |
| **Microphone** | Adjacent to thyroid cartilage | Pharyngeal sounds (bolus movement, wet gurgles) are loudest at this surface location. |

---

## 4. ESP32 Pin Mapping

| Pin | Connected To | Interface |
|---|---|---|
| GPIO 34 (ADC) | Microphone OUT | Analog (ADC1) |
| GPIO 21 (SDA) | MPU6050 SDA | I2C |
| GPIO 22 (SCL) | MPU6050 SCL | I2C |
| GPIO 2 | RGB LED (Blue) | Digital OUT |
| GPIO 4 | RGB LED (Green) | Digital OUT |
| GPIO 5 | RGB LED (Red) | Digital OUT |
| GPIO 18 | Vibration Motor (via transistor) | Digital OUT |
| 3.3V | MPU6050 VCC, Mic VCC | Power |
| GND | Common Ground | Ground |

> ⚠️ **Note**: GPIO 34 is input-only on ESP32. ADC1 channels (32–39) are used since ADC2 conflicts with WiFi.

---

## 5. Power Architecture

```
Li-Po 3.7V
    │
    ▼
TP4056 (Charge + Protection)
    │
    ▼
ESP32 VIN (3.3V LDO onboard)
    ├──► MPU6050 (3.3V, ~3.9mA)
    ├──► Microphone (3.3V, ~0.5mA)
    ├──► RGB LED (3.3V, ~20mA peak)
    └──► Vibration Motor (via NPN transistor, 3.7V direct)
```

**Estimated runtime**: ~4–6 hours on 500mAh Li-Po at full operation.

---

## 6. Signal Processing Pipeline

```
Microphone (Analog)          MPU6050 (I2C, 100Hz)
      │                              │
      ▼                              ▼
  ADC Read                    accel X, Y, Z
  (GPIO 34)                         │
      │                             ▼
      ▼                        IMU RMS Calculation
  Peak Envelope               √((x²+y²+z²)/n)
  (max of window)                   │
      │                             │
      ▼                             ▼
  ZCR Calculation          ◄────────┘
  (sign changes / window)
      │
      ▼
  ┌───────────────────────────┐
  │   Rule-Based Classifier   │
  │  (IMU RMS + Mic + ZCR     │
  │   + Duration thresholds)  │
  └───────────┬───────────────┘
              │
              ▼
     SAFE / UNSAFE / COUGH / NOISE / IDLE
              │
              ▼
     LED color + Motor pulse + JSON broadcast
```

---

## 7. WiFi & Communication

| Parameter | Value |
|---|---|
| Mode | WiFi Access Point (AP) |
| SSID | `DysphagiaGuard-AP` |
| IP Address | `192.168.4.1` |
| WebSocket Route | `ws://192.168.4.1/ws` |
| Broadcast Rate | 10 Hz (every 100ms) |
| Protocol | JSON over WebSocket |

---

## 8. Challenges & Solutions

| Challenge | Solution |
|---|---|
| Noisy microphone signal | Applied smoothing / moving average filter in firmware |
| ADC2 conflict with WiFi | Switched to ADC1 channels (GPIO 34–39) |
| I2C + ADC simultaneous sampling | Used ESP32 dual-core; WiFi/WebSocket on Core 0, sensor loop on Core 1 |
| Voltage drops under motor load | Added 100µF decoupling capacitor near motor transistor |
| Sensor placement accuracy | Iterated placement based on laryngeal motion dynamics during testing |
| Common ground noise | Kept analog (mic) and digital (IMU, LED) grounds separated at star point |

---

## 9. Physical Form Factor

- **Wearable band** around the neck, sensor module positioned at the front over the thyroid cartilage.
- Lightweight enclosure (3D printed / foam-backed) ensures consistent skin contact without discomfort.
- Single USB-C port (via TP4056) for charging.
- RGB LED visible from the front for caregiver line-of-sight feedback.
