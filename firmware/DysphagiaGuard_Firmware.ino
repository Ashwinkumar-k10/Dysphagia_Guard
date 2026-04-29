/*
 * DysphagiaGuard — ESP32 Firmware (SENSOR-REAL)
 * Single-core, timer-driven broadcast, full JSON payload.
 * No FreeRTOS tasks touching WebSocket — fixes "values not updating" bug.
 * Real sensors: MPU6050 (I2C, SDA=21, SCL=22) + Analog Mic (ADC pin 34)
 */

#include <Arduino.h>
#include <WiFi.h>
#include <AsyncTCP.h>
#include <ESPAsyncWebServer.h>
#include <Preferences.h>
#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#include <MPU6050_light.h>        // Install: "MPU6050_light" by rfetick in Arduino Library Manager

#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 64
#define OLED_RESET    -1
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, OLED_RESET);

// ─── PINS ────────────────────────────────────────────────────────────────────
#define MIC_PIN       34
#define BUZZER_PIN    25
#define VIBRATION_PIN 26
#define GREEN_LED     27
#define RED_LED       14

// ─── Sensor objects ──────────────────────────────────────────────────────────
MPU6050 mpu(Wire);

// ─── Sensor reading config ───────────────────────────────────────────────────
#define MIC_SAMPLES        64      // Number of ADC samples per RMS window
#define MIC_SAMPLE_DELAY   200     // Microseconds between samples (~5kHz effective)
#define ADC_VREF           3.3f
#define ADC_RESOLUTION     4095.0f

// ─── WiFi ────────────────────────────────────────────────────────────────────
const char* AP_SSID = "DysphagiaGuard-AP";

// ─── Server ──────────────────────────────────────────────────────────────────
AsyncWebServer server(80);
AsyncWebSocket ws("/ws");
Preferences    prefs;

// ─── Device state ────────────────────────────────────────────────────────────
int    sessionId   = 1;
int    totalSafe   = 0;
int    totalUnsafe = 0;
int    totalEvents = 0;
String deviceMode  = "DAY";
char   curClass[16]= "IDLE";

// ─── Timing ──────────────────────────────────────────────────────────────────
unsigned long lastBroadcast = 0;
unsigned long lastCleanup   = 0;

// ─── Pending command from WS callback → acted on in loop() ──────────────────
volatile bool pendingCmd = false;
char pendingPayload[32]  = {0};

// ─────────────────────────────────────────────────────────────────────────────
// Read real IMU RMS — magnitude of acceleration vector minus gravity baseline
// Returns value in g units (0.0 = perfectly still)
// ─────────────────────────────────────────────────────────────────────────────
float readIMU_RMS() {
  mpu.update();
  float ax = mpu.getAccX();
  float ay = mpu.getAccY();
  float az = mpu.getAccZ();

  // Subtract gravity component (z-axis when flat) to get motion-only signal
  // Total acceleration magnitude minus 1g baseline
  float mag = sqrtf(ax*ax + ay*ay + az*az) - 1.0f;
  if (mag < 0.0f) mag = -mag;  // absolute value
  return mag;
}

// ─────────────────────────────────────────────────────────────────────────────
// Read real Mic envelope — RMS of ADC samples, normalized to 0.0–1.0
// Uses AC-coupled approach: subtract mean to remove DC bias
// ─────────────────────────────────────────────────────────────────────────────
float readMicEnvelope() {
  long   sum    = 0;
  int    buf[MIC_SAMPLES];

  // Collect samples
  for (int i = 0; i < MIC_SAMPLES; i++) {
    buf[i] = analogRead(MIC_PIN);
    sum += buf[i];
    delayMicroseconds(MIC_SAMPLE_DELAY);
  }

  // DC offset (mean)
  float mean = (float)sum / MIC_SAMPLES;

  // RMS of AC component
  float sumSq = 0.0f;
  for (int i = 0; i < MIC_SAMPLES; i++) {
    float ac = (float)buf[i] - mean;
    sumSq += ac * ac;
  }
  float rms = sqrtf(sumSq / MIC_SAMPLES);

  // Normalize: max AC swing on 12-bit ADC ≈ 2047 (half of 4095)
  float normalized = rms / 2047.0f;
  if (normalized > 1.0f) normalized = 1.0f;
  return normalized;
}

// ─────────────────────────────────────────────────────────────────────────────
// Compute confidence from sensor values based on current classification
// Higher sensor activity + matching pattern → higher confidence
// ─────────────────────────────────────────────────────────────────────────────
float computeConfidence(const char* cls, float imu, float mic) {
  float conf = 0.0f;
  if (strcmp(cls, "SAFE") == 0) {
    // Safe swallow: moderate imu + moderate mic, both present
    float imuScore = (imu > 0.03f && imu < 0.60f) ? 0.8f : 0.4f;
    float micScore = (mic > 0.02f && mic < 0.60f) ? 0.8f : 0.4f;
    conf = (imuScore + micScore) / 2.0f;
  } else if (strcmp(cls, "UNSAFE") == 0) {
    // Unsafe: high imu OR high mic
    float imuScore = (imu > 0.10f) ? (imu > 0.50f ? 0.95f : 0.75f) : 0.45f;
    float micScore = (mic > 0.08f) ? (mic > 0.70f ? 0.95f : 0.75f) : 0.45f;
    conf = (imuScore * 0.5f) + (micScore * 0.5f);
  } else if (strcmp(cls, "COUGH") == 0) {
    // Cough: very high mic, moderate-high imu
    float micScore = (mic > 0.60f) ? 0.90f : (mic > 0.30f ? 0.70f : 0.40f);
    float imuScore = (imu > 0.40f) ? 0.85f : (imu > 0.15f ? 0.65f : 0.35f);
    conf = (micScore * 0.6f) + (imuScore * 0.4f);
  } else if (strcmp(cls, "NOISE") == 0) {
    // Noise: low imu but some mic, or very low everything
    conf = (imu < 0.10f && mic < 0.20f) ? 0.70f : 0.35f;
  } else {
    conf = 0.0f;
  }
  // Clamp
  if (conf > 1.0f) conf = 1.0f;
  if (conf < 0.0f) conf = 0.0f;
  return conf;
}

// ─────────────────────────────────────────────────────────────────────────────
// Build full JSON from REAL sensors and broadcast — called ONLY from loop()
// ─────────────────────────────────────────────────────────────────────────────
void broadcastState(bool isSpike) {
  float imu, mic, conf;
  int   dur = 0;
  const char* c = curClass;

  if (strcmp(c, "IDLE") == 0 || strcmp(c, "NOISE") == 0) {
    // For IDLE/NOISE, still read sensors but expect low values
    imu  = readIMU_RMS();
    mic  = readMicEnvelope();
    conf = (strcmp(c, "NOISE") == 0) ? computeConfidence("NOISE", imu, mic) : 0.0f;
    dur  = 0;
  } else {
    // For active classifications, read real sensors
    imu  = readIMU_RMS();
    mic  = readMicEnvelope();
    conf = computeConfidence(c, imu, mic);

    // Duration: fixed typical values per class (ms)
    // Real duration would require event start/end timestamps — set on processCmd trigger
    if      (strcmp(c,"SAFE")==0)   dur = 150;
    else if (strcmp(c,"UNSAFE")==0) dur = 160;
    else if (strcmp(c,"COUGH")==0)  dur = 60;
    else                             dur = 40;
  }

  char buf[384];
  snprintf(buf, sizeof(buf),
    "{\"classification\":\"%s\","
    "\"imu_rms\":%.4f,"
    "\"mic_envelope\":%.4f,"
    "\"confidence\":%.4f,"
    "\"duration_ms\":%d,"
    "\"timestamp\":%lu,"
    "\"session_id\":%d,"
    "\"device_mode\":\"%s\","
    "\"total_safe\":%d,"
    "\"total_unsafe\":%d,"
    "\"system_healthy\":true}",
    c, imu, mic, conf, dur,
    (unsigned long)millis(),
    sessionId, deviceMode.c_str(),
    totalSafe, totalUnsafe
  );

  ws.textAll(buf);
}

void sendStateTo(AsyncWebSocketClient* client) {
  float imu = readIMU_RMS();
  float mic = readMicEnvelope();
  char buf[384];
  snprintf(buf, sizeof(buf),
    "{\"classification\":\"%s\","
    "\"imu_rms\":%.4f,\"mic_envelope\":%.4f,\"confidence\":0.0000,"
    "\"duration_ms\":0,\"timestamp\":%lu,"
    "\"session_id\":%d,\"device_mode\":\"%s\","
    "\"total_safe\":%d,\"total_unsafe\":%d,\"system_healthy\":true}",
    curClass, imu, mic, (unsigned long)millis(),
    sessionId, deviceMode.c_str(), totalSafe, totalUnsafe
  );
  client->text(buf);
}

// ─────────────────────────────────────────────────────────────────────────────
// Non-blocking hardware alert
// ─────────────────────────────────────────────────────────────────────────────
void updateOLED(const char* cls) {
  display.clearDisplay();
  
  if (strcmp(cls, "UNSAFE") == 0) {
    display.fillRect(0, 0, 128, 64, SSD1306_WHITE);
    display.setTextColor(SSD1306_BLACK); 
    display.setTextSize(2);
    display.setCursor(15, 25);
    display.print("! UNSAFE !");
  } 
  else if (strcmp(cls, "SAFE") == 0) {
    display.setTextColor(SSD1306_WHITE);
    display.setTextSize(3);
    display.setCursor(30, 20);
    display.print("SAFE");
  } 
  else if (strcmp(cls, "COUGH") == 0) {
    display.setTextColor(SSD1306_WHITE);
    display.setTextSize(3);
    display.setCursor(20, 20);
    display.print("COUGH");
  }
  else {
    display.setTextColor(SSD1306_WHITE);
    display.setTextSize(1);
    display.setCursor(0, 0);
    display.print("Mode: ");
    display.print(deviceMode);
    display.setTextSize(2);
    display.setCursor(5, 30);
    display.print("MONITORING");
  }
  display.display();
}

void startAlert(const char* cls) {
  updateOLED(cls);
  
  if (strcmp(cls,"UNSAFE")==0) {
    if (deviceMode=="DAY") {
      digitalWrite(RED_LED, HIGH);
      for (int i=0;i<500;i++) {
        digitalWrite(BUZZER_PIN,HIGH); delayMicroseconds(500);
        digitalWrite(BUZZER_PIN,LOW);  delayMicroseconds(500);
      }
      digitalWrite(RED_LED, LOW);
    } else {
      digitalWrite(VIBRATION_PIN, HIGH);
      delay(500);
      digitalWrite(VIBRATION_PIN, LOW);
    }
    delay(1000);
  } else if (strcmp(cls,"SAFE")==0) {
    digitalWrite(GREEN_LED, HIGH);
    delay(100);
    digitalWrite(GREEN_LED, LOW);
    delay(500);
  } else if (strcmp(cls,"COUGH")==0) {
    if (deviceMode=="DAY") {
      digitalWrite(BUZZER_PIN, HIGH); delay(50); digitalWrite(BUZZER_PIN, LOW); delay(50);
      digitalWrite(BUZZER_PIN, HIGH); delay(50); digitalWrite(BUZZER_PIN, LOW);
    } else {
      digitalWrite(VIBRATION_PIN, HIGH); delay(50); digitalWrite(VIBRATION_PIN, LOW); delay(50);
      digitalWrite(VIBRATION_PIN, HIGH); delay(50); digitalWrite(VIBRATION_PIN, LOW);
    }
    delay(800);
  }
  
  updateOLED("IDLE");
}

// ─────────────────────────────────────────────────────────────────────────────
// Process command — called from loop() only
// ─────────────────────────────────────────────────────────────────────────────
void processCmd(const char* msg) {
  if (strcmp(msg,"MODE:DAY")==0) {
    deviceMode="DAY";
    prefs.begin("dysguard",false); prefs.putString("device_mode","DAY"); prefs.end();
    broadcastState(false);

  } else if (strcmp(msg,"MODE:NIGHT")==0) {
    deviceMode="NIGHT";
    prefs.begin("dysguard",false); prefs.putString("device_mode","NIGHT"); prefs.end();
    broadcastState(false);

  } else if (strcmp(msg,"RESET")==0) {
    totalSafe=0; totalUnsafe=0; totalEvents=0; sessionId++;
    strncpy(curClass,"IDLE",15);
    prefs.begin("dysguard",false);
    prefs.putInt("session_id",sessionId);
    prefs.putInt("event_count",0);
    prefs.end();
    Serial.printf("[RESET] Session #%d\n",sessionId);
    broadcastState(false);

  } else if (strcmp(msg,"SAFE")==0 || strcmp(msg,"UNSAFE")==0 ||
             strcmp(msg,"COUGH")==0 || strcmp(msg,"NOISE")==0 || strcmp(msg,"IDLE")==0) {
    strncpy(curClass, msg, 15);
    if (strcmp(curClass,"IDLE")!=0) {
      // Broadcast 5 readings rapidly to show sensor spike
      for (int i=0;i<5;i++) { broadcastState(true); delay(20); }
      if (strcmp(curClass,"SAFE")==0)   totalSafe++;
      if (strcmp(curClass,"UNSAFE")==0) totalUnsafe++;
      totalEvents++;
      prefs.begin("dysguard",false); prefs.putInt("event_count",totalEvents); prefs.end();
      Serial.printf("[EVENT] %s safe=%d unsafe=%d imu=%.3f mic=%.3f\n",
                    curClass, totalSafe, totalUnsafe,
                    readIMU_RMS(), readMicEnvelope());
      broadcastState(false);
      startAlert(curClass);
    }
    broadcastState(false);
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// WS event — ONLY sets pendingPayload flag, never calls ws.textAll()
// ─────────────────────────────────────────────────────────────────────────────
void onWsEvent(AsyncWebSocket* srv, AsyncWebSocketClient* client,
               AwsEventType type, void* arg, uint8_t* data, size_t len) {
  if (type == WS_EVT_CONNECT) {
    Serial.printf("[WS] #%u connected %s\n", client->id(), client->remoteIP().toString().c_str());
    sendStateTo(client);
  } else if (type == WS_EVT_DISCONNECT) {
    Serial.printf("[WS] #%u disconnected\n", client->id());
  } else if (type == WS_EVT_DATA) {
    AwsFrameInfo* info = (AwsFrameInfo*)arg;
    if (!info->final || info->index!=0 || info->len!=len || info->opcode!=WS_TEXT) return;
    if (len < 31 && !pendingCmd) {
      memcpy(pendingPayload, data, len);
      pendingPayload[len] = '\0';
      pendingCmd = true;
    }
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// HTML — served at http://192.168.4.1  (unchanged from original)
// ─────────────────────────────────────────────────────────────────────────────
const char INDEX_HTML[] PROGMEM =
"<!DOCTYPE html><html lang=\"en\"><head>"
"<meta charset=\"UTF-8\">"
"<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no\">"
"<title>DysphagiaGuard</title><style>"
"@import url('https://fonts.googleapis.com/css2?family=DM+Mono:wght@400;500&family=Syne:wght@700;800&display=swap');"
"*{box-sizing:border-box;margin:0;padding:0;-webkit-tap-highlight-color:transparent;}"
":root{--bg:#0a0e14;--card:#111822;--border:#1e2d3d;--safe:#00e5a0;--unsafe:#ff3b5c;--cough:#ff6d00;--noise:#f5a623;--idle:#4a6fa5;--text:#e8f0fe;--sub:#607899;--safe-dim:#00e5a018;--unsafe-dim:#ff3b5c18;--cough-dim:#ff6d0018;--noise-dim:#f5a62318;}"
"html,body{height:100%;background:var(--bg);color:var(--text);font-family:'DM Mono',monospace;}"
"body{display:flex;flex-direction:column;padding:0 0 env(safe-area-inset-bottom);}"
"header{padding:20px 20px 0;display:flex;align-items:center;gap:12px;}"
".logo{width:36px;height:36px;border-radius:10px;background:linear-gradient(135deg,var(--safe),#0094ff);display:flex;align-items:center;justify-content:center;font-size:20px;}"
".brand{font-family:'Syne',sans-serif;font-size:18px;font-weight:800;letter-spacing:-0.5px;}"
".brand span{color:var(--safe);}"
".dot{width:8px;height:8px;border-radius:50%;background:var(--idle);margin-left:auto;transition:background .3s;}"
".dot.on{background:var(--safe);box-shadow:0 0 8px var(--safe);}"
".statusbar{margin:14px 20px 0;background:var(--card);border:1px solid var(--border);border-radius:14px;padding:11px 16px;display:flex;align-items:center;gap:10px;font-size:12px;color:var(--sub);}"
".mode-badge{background:var(--border);border-radius:6px;padding:3px 8px;font-size:11px;color:var(--text);}"
".cls-display{margin:16px 20px 0;border-radius:16px;padding:20px;border:1px solid var(--border);background:var(--card);text-align:center;transition:all .25s;}"
".cls-display.SAFE{border-color:var(--safe);background:var(--safe-dim);}"
".cls-display.UNSAFE{border-color:var(--unsafe);background:var(--unsafe-dim);}"
".cls-display.NOISE{border-color:var(--noise);background:var(--noise-dim);}"
".cls-label{font-family:'Syne',sans-serif;font-size:26px;font-weight:800;letter-spacing:2px;transition:color .25s;}"
".cls-sub{font-size:11px;color:var(--sub);margin-top:4px;}"
".metrics{display:grid;grid-template-columns:1fr 1fr 1fr;gap:10px;margin:12px 20px 0;}"
".metric{background:var(--card);border:1px solid var(--border);border-radius:12px;padding:11px 8px;text-align:center;}"
".metric-val{font-family:'Syne',sans-serif;font-size:19px;font-weight:700;}"
".metric-lbl{font-size:9px;color:var(--sub);margin-top:3px;letter-spacing:.5px;}"
".section-title{margin:18px 20px 8px;font-size:10px;letter-spacing:2px;color:var(--sub);text-transform:uppercase;}"
".btn-grid{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin:0 20px;}"
".btn{border:none;border-radius:14px;padding:18px 10px;font-family:'Syne',sans-serif;font-weight:700;font-size:15px;letter-spacing:1px;cursor:pointer;transition:transform .1s;}"
".btn:active{transform:scale(0.96);}"
".btn.active-btn{outline:3px solid white;outline-offset:2px;}"
".btn-safe{background:linear-gradient(135deg,#00e5a0,#00b87c);color:#003322;box-shadow:0 4px 18px #00e5a028;}"
".btn-unsafe{background:linear-gradient(135deg,#ff3b5c,#c41c3a);color:#fff;box-shadow:0 4px 18px #ff3b5c28;}"
".btn-noise{background:linear-gradient(135deg,#f5a623,#c47f0a);color:#2a1a00;box-shadow:0 4px 18px #f5a62328;}"
".btn-idle{background:linear-gradient(135deg,#1e2d3d,#2a3d52);color:var(--sub);border:1px solid var(--border);}"
".btn .icon{display:block;font-size:24px;margin-bottom:5px;}"
".btn .sub{font-size:10px;font-family:'DM Mono',monospace;font-weight:400;opacity:.7;margin-top:3px;display:block;}"
".mode-row{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin:10px 20px 0;}"
".btn-mode{background:var(--card);border:1px solid var(--border);border-radius:14px;padding:13px 10px;font-family:'Syne',sans-serif;font-weight:700;font-size:13px;cursor:pointer;color:var(--sub);transition:all .2s;text-align:center;}"
".btn-mode.active{border-color:var(--safe);color:var(--safe);background:var(--safe-dim);}"
".btn-mode:active{transform:scale(0.97);}"
".btn-reset{width:calc(100% - 40px);margin:10px 20px 0;background:var(--card);border:1px solid var(--border);border-radius:14px;padding:13px;font-family:'DM Mono',monospace;font-size:13px;color:var(--sub);cursor:pointer;transition:all .2s;letter-spacing:1px;}"
".btn-reset:active{color:var(--text);border-color:var(--text);}"
".counters{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin:10px 20px 0;}"
".counter{background:var(--card);border:1px solid var(--border);border-radius:12px;padding:13px;display:flex;justify-content:space-between;align-items:center;}"
".counter-n{font-family:'Syne',sans-serif;font-size:24px;font-weight:800;}"
".counter-n.safe{color:var(--safe);}"
".counter-n.unsafe{color:var(--unsafe);}"
".counter-lbl{font-size:10px;color:var(--sub);text-align:right;letter-spacing:.5px;line-height:1.4;}"
".log-area{margin:10px 20px 28px;background:var(--card);border:1px solid var(--border);border-radius:14px;padding:12px;max-height:130px;overflow-y:auto;font-size:11px;color:var(--sub);line-height:1.6;}"
".entry{display:flex;gap:8px;}"
".tag.SAFE{color:var(--safe);}.tag.UNSAFE{color:var(--unsafe);}.tag.COUGH{color:var(--cough);}.tag.NOISE{color:var(--noise);}.tag.IDLE{color:var(--idle);}.tag.SYS{color:#607899;}"
"</style></head><body>"
"<header><div class=\"logo\">&#x1FAC1;</div><div class=\"brand\">Dyspha<span>Guard</span></div><div class=\"dot\" id=\"dot\"></div></header>"
"<div class=\"statusbar\"><span id=\"ws-status\">Connecting...</span><span class=\"mode-badge\" id=\"mode-badge\">DAY MODE</span><span style=\"margin-left:auto;font-size:11px\" id=\"session-lbl\">Session #&mdash;</span></div>"
"<div class=\"cls-display\" id=\"cls-display\"><div class=\"cls-label\" id=\"cls-label\" style=\"color:var(--idle)\">MONITORING</div><div class=\"cls-sub\">Awaiting swallow event</div></div>"
"<div class=\"metrics\">"
"<div class=\"metric\"><div class=\"metric-val\" id=\"m-imu\">&mdash;</div><div class=\"metric-lbl\">IMU RMS</div></div>"
"<div class=\"metric\"><div class=\"metric-val\" id=\"m-mic\">&mdash;</div><div class=\"metric-lbl\">MIC ENV</div></div>"
"<div class=\"metric\"><div class=\"metric-val\" id=\"m-conf\">&mdash;</div><div class=\"metric-lbl\">CONFIDENCE</div></div>"
"</div>"
"<div class=\"section-title\">Swallow Classification</div>"
"<div class=\"btn-grid\">"
"<button class=\"btn btn-safe\" id=\"b-safe\" onclick=\"trig('SAFE')\"><span class=\"icon\">&#x2705;</span>SAFE<span class=\"sub\">Normal swallow</span></button>"
"<button class=\"btn btn-unsafe\" id=\"b-unsafe\" onclick=\"trig('UNSAFE')\"><span class=\"icon\">&#x26A0;&#xFE0F;</span>UNSAFE<span class=\"sub\">Aspiration risk</span></button>"
"<button class=\"btn\" id=\"b-cough\" style=\"border-color:var(--cough);color:var(--cough);\" onclick=\"trig('COUGH')\"><span class=\"icon\">&#x1F4A8;</span>COUGH<span class=\"sub\">&lt;80ms burst</span></button>"
"<button class=\"btn btn-noise\" id=\"b-noise\" onclick=\"trig('NOISE')\"><span class=\"icon\">&#x3030;&#xFE0F;</span>NOISE<span class=\"sub\">Artifact / reject</span></button>"
"</div>"
"<div class=\"section-title\">Alert Mode</div>"
"<div class=\"mode-row\">"
"<button class=\"btn-mode active\" id=\"btn-day\" onclick=\"setMode('DAY')\">&#x2600;&#xFE0F; Day Mode<br><small style=\"font-size:10px;font-family:'DM Mono'\">Buzzer + LED</small></button>"
"<button class=\"btn-mode\" id=\"btn-night\" onclick=\"setMode('NIGHT')\">&#x1F319; Night Mode<br><small style=\"font-size:10px;font-family:'DM Mono'\">Vibration only</small></button>"
"</div>"
"<button class=\"btn-reset\" onclick=\"send('RESET')\">&#x21BA;&nbsp;Reset Session</button>"
"<div class=\"section-title\">Session Counters</div>"
"<div class=\"counters\">"
"<div class=\"counter\"><div class=\"counter-n safe\" id=\"cnt-safe\">0</div><div class=\"counter-lbl\">SAFE<br>SWALLOWS</div></div>"
"<div class=\"counter\"><div class=\"counter-n unsafe\" id=\"cnt-unsafe\">0</div><div class=\"counter-lbl\">UNSAFE<br>EVENTS</div></div>"
"<div class=\"counter\"><div class=\"counter-n\" style=\"color:var(--cough)\" id=\"cnt-cough\">0</div><div class=\"counter-lbl\">COUGH<br>EVENTS</div></div>"
"</div>"
"<div class=\"section-title\">Event Log</div>"
"<div class=\"log-area\" id=\"log\"></div>"
"<script>"
"var sock=null,pingT=null,retryDelay=500;"
"var CC={SAFE:'var(--safe)',UNSAFE:'var(--unsafe)',NOISE:'var(--noise)',IDLE:'var(--idle)'};"
"function g(id){return document.getElementById(id);}"
"function ui(d){"
"  var c=d.classification||'IDLE';"
"  g('cls-display').className='cls-display '+(c==='IDLE'?'':c);"
"  g('cls-label').textContent=c==='IDLE'?'MONITORING':c;"
"  g('cls-label').style.color=CC[c]||CC.IDLE;"
"  g('m-imu').textContent=(+d.imu_rms||0).toFixed(3);"
"  g('m-mic').textContent=(+d.mic_envelope||0).toFixed(3);"
"  g('m-conf').textContent=((+d.confidence||0)*100).toFixed(0)+'%';"
"  g('cnt-safe').textContent=d.total_safe||0;"
"  g('cnt-unsafe').textContent=d.total_unsafe||0;"
"  if(g('cnt-cough')&&d.total_cough!==undefined)g('cnt-cough').textContent=d.total_cough||0;"
"  g('session-lbl').textContent='Session #'+(d.session_id||'-');"
"  var m=d.device_mode||'DAY';"
"  g('mode-badge').textContent=m+' MODE';"
"  g('btn-day').classList.toggle('active',m==='DAY');"
"  g('btn-night').classList.toggle('active',m==='NIGHT');"
"}"
"function connect(){"
"  if(sock){try{sock.close();}catch(e){}}"
"  sock=new WebSocket('ws://'+location.hostname+'/ws');"
"  sock.onopen=function(){"
"    retryDelay=500;"
"    g('dot').classList.add('on');"
"    g('ws-status').textContent='Live \u00B7 '+location.hostname;"
"    addLog('SYS','Connected');"
"    clearInterval(pingT);"
"    pingT=setInterval(function(){if(sock.readyState===1)sock.send('PING');},5000);"
"  };"
"  sock.onmessage=function(e){"
"    if(e.data==='PONG')return;"
"    try{var d=JSON.parse(e.data);if(d.classification)ui(d);}catch(x){console.error(x);}"
"  };"
"  sock.onclose=function(){"
"    g('dot').classList.remove('on');"
"    g('ws-status').textContent='Reconnecting...';"
"    clearInterval(pingT);"
"    setTimeout(connect,retryDelay);"
"    retryDelay=Math.min(retryDelay*2,4000);"
"  };"
"  sock.onerror=function(e){sock.close();};"
"}"
"function send(m){if(sock&&sock.readyState===1)sock.send(m);}"
"function trig(c){"
"  send(c);"
"  ['safe','unsafe','cough','noise','idle'].forEach(function(x){var b=g('b-'+x);if(b)b.classList.remove('active-btn');});"
"  var btn=g('b-'+c.toLowerCase());if(btn)btn.classList.add('active-btn');"
"  addLog(c,'Triggered: '+c);"
"}"
"function setMode(m){send('MODE:'+m);addLog('SYS','Mode \u2192 '+m);}"
"function addLog(tag,msg){"
"  var el=g('log'),d=new Date();"
"  var ts=('0'+d.getHours()).slice(-2)+':'+('0'+d.getMinutes()).slice(-2)+':'+('0'+d.getSeconds()).slice(-2);"
"  var r=document.createElement('div');r.className='entry';"
"  r.innerHTML='<span class=\"tag '+tag+'\">['+tag+']</span><span>'+ts+' '+msg+'</span>';"
"  el.appendChild(r);el.scrollTop=el.scrollHeight;"
"  if(el.children.length>60)el.removeChild(el.firstChild);"
"}"
"connect();"
"</script></body></html>";

// ─────────────────────────────────────────────────────────────────────────────
void setup() {
  Serial.begin(115200);
  delay(300);

  pinMode(MIC_PIN,       INPUT);
  pinMode(BUZZER_PIN,    OUTPUT);
  pinMode(VIBRATION_PIN, OUTPUT);
  pinMode(GREEN_LED,     OUTPUT);
  pinMode(RED_LED,       OUTPUT);
  digitalWrite(BUZZER_PIN,    LOW);
  digitalWrite(VIBRATION_PIN, LOW);
  digitalWrite(GREEN_LED,     LOW);
  digitalWrite(RED_LED,       LOW);

  // Configure ADC for mic pin
  analogReadResolution(12);          // 12-bit ADC → 0-4095
  analogSetAttenuation(ADC_11db);    // Full 0–3.3V range

  prefs.begin("dysguard", false);
  sessionId   = prefs.getInt("session_id", 0) + 1;
  prefs.putInt("session_id", sessionId);
  deviceMode  = prefs.getString("device_mode", "DAY");
  totalEvents = prefs.getInt("event_count", 0);
  prefs.end();
  Serial.printf("[NVS] Session #%d  Mode:%s  Events:%d\n",
                sessionId, deviceMode.c_str(), totalEvents);

  // Init I2C and MPU6050
  Wire.begin(21, 22);  // SDA=21, SCL=22

  byte mpu_status = mpu.begin();
  if (mpu_status != 0) {
    Serial.printf("[MPU6050] Init failed, code %d\n", mpu_status);
  } else {
    Serial.println("[MPU6050] OK — calibrating (keep device still)...");
    delay(1000);
    mpu.calcOffsets(true, true);  // gyro + accelerometer auto-calibration
    Serial.println("[MPU6050] Calibration done");
  }

  for (int i=0;i<3;i++){
    digitalWrite(GREEN_LED,HIGH); delay(100);
    digitalWrite(GREEN_LED,LOW);  delay(100);
  }

  // Init OLED
  if(!display.begin(SSD1306_SWITCHCAPVCC, 0x3C)) {
    Serial.println("SSD1306 allocation failed");
  } else {
    display.clearDisplay();
    display.setTextColor(SSD1306_WHITE);
    display.setTextSize(2);
    display.setCursor(0, 15);
    display.println("Dysphagia");
    display.println("  Guard");
    display.display();
    delay(1500);
    updateOLED("IDLE");
  }

  WiFi.softAPConfig(IPAddress(192,168,4,1),IPAddress(192,168,4,1),IPAddress(255,255,255,0));
  WiFi.softAP(AP_SSID);
  Serial.printf("[WiFi] SSID:%s  IP:192.168.4.1\n", AP_SSID);

  ws.onEvent(onWsEvent);
  server.addHandler(&ws);
  server.on("/", HTTP_GET, [](AsyncWebServerRequest* r){
    r->send_P(200,"text/html",INDEX_HTML);
  });
  server.on("/status", HTTP_GET, [](AsyncWebServerRequest* r){
    r->send(200,"text/plain","OK");
  });
  server.begin();
  Serial.println("[HTTP] Ready — http://192.168.4.1");
}

// ─────────────────────────────────────────────────────────────────────────────
// loop — single-threaded, no concurrency issues
// ─────────────────────────────────────────────────────────────────────────────
void loop() {
  unsigned long now = millis();

  // 1. Process pending WS command
  if (pendingCmd) {
    char cmd[32];
    strncpy(cmd, pendingPayload, 31);
    pendingCmd = false;
    if (strcmp(cmd,"PING") != 0) processCmd(cmd);
  }

  // 2. 10 Hz heartbeat broadcast (real sensor data)
  if (now - lastBroadcast >= 100) {
    lastBroadcast = now;
    broadcastState(false);
  }

  // 3. Cleanup stale WS clients every 5 s
  if (now - lastCleanup >= 5000) {
    lastCleanup = now;
    ws.cleanupClients();
  }

  // 4. Yield to AsyncTCP — do NOT remove
  delay(1);
}
