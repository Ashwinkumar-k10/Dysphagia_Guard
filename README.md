# DysphagiaGuard

DysphagiaGuard is an offline-first wearable system that detects safe/unsafe swallowing events using an ESP32 hardware device with sensors, connected locally to this Jetpack Compose Android app.

## Hardware Components (ESP32)
- **ESP32 DevKit V1** (Microcontroller & WiFi AP)
- **MPU6050** (IMU - Laryngeal vibration)
- **MAX9814** (Microphone - Acoustic swallow sound)
- **ERM Vibration Motor** (Silent mode alerts)
- **Buzzer & LEDs** (Day mode alerts)

## Wiring Diagram
- `MPU6050 SDA` -> `GPIO 21`
- `MPU6050 SCL` -> `GPIO 22`
- `MAX9814 OUT` -> `GPIO 34` (ADC)
- `Buzzer` -> `GPIO 25`
- `ERM Vibration Motor` -> `GPIO 26`
- `Green LED` -> `GPIO 27`
- `Red LED` -> `GPIO 14`

## Setup & Build Steps

### ESP32 Firmware
1. Open the `firmware/` folder in Visual Studio Code with PlatformIO installed.
2. PlatformIO will automatically install dependencies based on `platformio.ini`.
3. Connect your ESP32 to your computer via USB.
4. Build and Upload the firmware using PlatformIO.

### Android Application
1. Open the `android/` directory in Android Studio.
2. Sync Project with Gradle Files.
3. Build the APK via `Build > Build Bundle(s) / APK(s) > Build APK(s)`.
4. Sideload the generated APK onto any Android 7.0+ device (API 24+).

## Running the Demo
1. Power on the ESP32 device. It will start a WiFi Hotspot named **DysphagiaGuard-AP**.
2. Launch the Android app.
3. The app will attempt to connect to the ESP32 hotspot automatically. If it fails, follow the prompt to connect via WiFi settings.
4. Enter Patient Setup details and start monitoring.
5. Provide a physical "unsafe" signal to the device sensors to trigger an alert. The app will vibrate, update the screen, and track the event.
6. End the session to generate and share the PDF report.
