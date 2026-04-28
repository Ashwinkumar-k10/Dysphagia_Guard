# DysphagiaGuard

DysphagiaGuard is an offline-first wearable system that detects safe/unsafe swallowing events using an ESP32 hardware device with sensors, connected locally to a comprehensive Flutter mobile application with a built-in AI Assistant (Aegis Medical).

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

### Flutter Application
1. Ensure you have the Flutter SDK installed.
2. Open the `dysphagiaguard_app/` directory.
3. Run `flutter pub get` to install dependencies.
4. Run the app on an emulator or connected device via `flutter run`.

## Running the Demo
1. Power on the ESP32 device. It will start a WiFi Hotspot named **DysphagiaGuard-AP**.
2. Launch the Flutter app.
3. The app will attempt to connect to the ESP32 hotspot automatically.
4. Enter Patient Setup details and start monitoring.
5. Provide a physical "unsafe" signal to the device sensors to trigger an alert.
6. Use the new **Aegis Medical AI Assistant** to ask questions or trigger an emergency SOS.
7. End the session to generate and share the PDF report.
