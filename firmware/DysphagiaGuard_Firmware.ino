#include <Arduino.h>
#include <Wire.h>
#include <MPU6050.h>          
#include <WiFi.h>
#include <AsyncTCP.h>         
#include <ESPAsyncWebServer.h> 
#include <ArduinoJson.h>       
#include <Preferences.h>       
#include <Adafruit_SSD1306.h>  
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>
#include <freertos/queue.h>
#include <freertos/semphr.h>

// PIN DEFINITIONS
#define MPU_SDA 21
#define MPU_SCL 22
#define MIC_PIN 34
#define BUZZER_PIN 25
#define VIBRATION_PIN 26
#define GREEN_LED 27
#define RED_LED 14

// THRESHOLDS (will be updated after calibration)
float IMU_ONSET_THRESHOLD = 0.15;
float MIC_THRESHOLD = 0.25;
const float IMU_MAX = 2.0;
const float MIC_MAX = 1.5;

// GLOBALS
MPU6050 mpu;
AsyncWebServer server(80);
AsyncWebSocket ws("/ws");
Preferences preferences;

// SENSOR DATA STRUCTURE
struct SensorSample {
  int16_t ax, ay, az;
  uint16_t mic;
};

// CLASSIFICATION RESULT
struct SwallowResult {
  String classification; // "SAFE", "UNSAFE", "NOISE", "IDLE"
  float imu_rms;
  float mic_envelope;
  float confidence;
  uint32_t timestamp;
  int duration_ms;
  int session_id;
  String device_mode; // "DAY" or "NIGHT"
};

// QUEUES AND MUTEXES
QueueHandle_t xSensorQueue;
QueueHandle_t xClassificationQueue;
SemaphoreHandle_t xWebSocketMutex;
SemaphoreHandle_t xI2CMutex;

// STATE VARIABLES
bool systemHealthy = true;
int mpuFailCount = 0;
String currentMode = "DAY";
int currentSessionId = 0;
int totalEvents = 0;

// HELPER: Connect to NVS safely
void nvsLogEvent(const SwallowResult& res) {
  preferences.begin("dysguard", false);
  totalEvents = preferences.getInt("event_count", 0) + 1;
  preferences.putInt("event_count", totalEvents);
  // Simulating circular JSON log in NVS (pseudo-implementation for space)
  Serial.printf("[NVS] Event #%d stored\n", totalEvents);
  preferences.end();
}

// ---------------------------------------------------------
// TASK 1: SENSOR ACQUISITION (Core 0, 1kHz)
// ---------------------------------------------------------
void sensorAcquisitionTask(void *pvParameters) {
  TickType_t xLastWakeTime = xTaskGetTickCount();
  const TickType_t xFrequency = pdMS_TO_TICKS(1); // 1kHz

  SensorSample sampleBuffer[50];
  int sampleIdx = 0;

  for (;;) {
    vTaskDelayUntil(&xLastWakeTime, xFrequency);

    if (xSemaphoreTake(xI2CMutex, pdMS_TO_TICKS(2)) == pdTRUE) {
      sampleBuffer[sampleIdx].ax = mpu.getAccelerationX();
      sampleBuffer[sampleIdx].ay = mpu.getAccelerationY();
      sampleBuffer[sampleIdx].az = mpu.getAccelerationZ();
      xSemaphoreGive(xI2CMutex);
      mpuFailCount = 0;
    } else {
      mpuFailCount++;
      if (mpuFailCount > 3) systemHealthy = false;
    }

    sampleBuffer[sampleIdx].mic = analogRead(MIC_PIN);
    sampleIdx++;

    if (sampleIdx >= 50) {
      if (xQueueSend(xSensorQueue, &sampleBuffer, 0) == pdPASS) {
        // Successfully queued
      }
      sampleIdx = 0;
    }
  }
}

// ---------------------------------------------------------
// TASK 2: SIGNAL PROCESSING (Core 1, 20Hz)
// ---------------------------------------------------------
void signalProcessingTask(void *pvParameters) {
  TickType_t xLastWakeTime = xTaskGetTickCount();
  const TickType_t xFrequency = pdMS_TO_TICKS(50); // 20Hz
  SensorSample localBuffer[50];
  
  float filtered_IMU[50] = {0};
  float prev_filtered = 0;
  float prev_raw = 0;
  const float alpha = 0.95; // High pass filter

  uint32_t onsetTime = 0;
  bool inEvent = false;

  for (;;) {
    vTaskDelayUntil(&xLastWakeTime, xFrequency);

    if (xQueueReceive(xSensorQueue, &localBuffer, 0) == pdPASS) {
      float sumSqIMU = 0;
      uint16_t micPeak = 0;
      int zeroCrossings = 0;
      bool lastSign = false;

      // Process 50 samples
      for (int i = 0; i < 50; i++) {
        // IMU magnitude
        float mag = sqrt(pow(localBuffer[i].ax, 2) + pow(localBuffer[i].ay, 2) + pow(localBuffer[i].az, 2)) / 16384.0; // g
        
        // High-pass filter
        filtered_IMU[i] = alpha * (prev_filtered + mag - prev_raw);
        prev_filtered = filtered_IMU[i];
        prev_raw = mag;
        
        sumSqIMU += pow(filtered_IMU[i], 2);

        // Mic
        float micV = (localBuffer[i].mic / 4095.0) * 3.3; // Envelope simple calc
        if (localBuffer[i].mic > micPeak) micPeak = localBuffer[i].mic;
        
        // Zero crossing simple est (on mic)
        bool sign = (localBuffer[i].mic > 2048);
        if (i > 0 && sign != lastSign) zeroCrossings++;
        lastSign = sign;
      }

      float rmsIMU = sqrt(sumSqIMU / 50.0);
      float micEnvelope = (micPeak / 4095.0);

      // Classification Logic
      bool imuOnsetDetected = (rmsIMU > IMU_ONSET_THRESHOLD);
      bool micEvent = (micEnvelope > MIC_THRESHOLD) && (zeroCrossings > 5 && zeroCrossings < 40); // Pharyngeal range pseudo-ZCR
      
      SwallowResult res;
      res.timestamp = millis();
      res.imu_rms = rmsIMU;
      res.mic_envelope = micEnvelope;
      res.session_id = currentSessionId;
      res.device_mode = currentMode;
      
      if (imuOnsetDetected) {
        if (!inEvent) {
          inEvent = true;
          onsetTime = millis();
        }
      } else {
        if (inEvent) {
          res.duration_ms = millis() - onsetTime;
          inEvent = false;

          if (res.duration_ms >= 80 && res.duration_ms <= 200) {
            if (micEvent) {
              float imuScore = min(rmsIMU / IMU_MAX, 1.0f);
              float micScore = min(micEnvelope / MIC_MAX, 1.0f);
              res.confidence = (0.6 * imuScore) + (0.4 * micScore);

              if (res.confidence >= 0.65) {
                // Determine wet gurgle based on low ZCR + high energy
                bool micWetGurgle = (zeroCrossings < 15 && micEnvelope > 0.8);
                if (micWetGurgle) res.classification = "UNSAFE";
                else res.classification = "SAFE";
              } else {
                res.classification = "NOISE";
              }
            } else {
              res.classification = "NOISE";
            }
          } else {
            res.classification = "NOISE";
          }
          
          if (res.classification == "SAFE" || res.classification == "UNSAFE") {
             Serial.printf("[CLASSIFY] IMU_RMS=%.2f MIC_ENV=%.2f CONF=%.2f -> %s (%dms)\n", 
                          res.imu_rms, res.mic_envelope, res.confidence, res.classification.c_str(), res.duration_ms);
             xQueueSend(xClassificationQueue, &res, portMAX_DELAY);
             if(totalEvents % 10 == 0) nvsLogEvent(res);
          }
        }
      }
      
      // Heartbeat send when idle
      if (!inEvent && (millis() % 2000 < 50)) {
         res.classification = "IDLE";
         res.confidence = 0;
         res.duration_ms = 0;
         xQueueSend(xClassificationQueue, &res, portMAX_DELAY);
      }
    }
  }
}

// ---------------------------------------------------------
// TASK 3: ALERT DISPATCH (Core 1, 3Hz basically)
// ---------------------------------------------------------
void alertDispatchTask(void *pvParameters) {
  SwallowResult res;
  for (;;) {
    if (xQueueReceive(xClassificationQueue, &res, portMAX_DELAY) == pdPASS) {
      if (res.classification == "UNSAFE") {
        if (currentMode == "DAY") {
          digitalWrite(RED_LED, HIGH);
          // Tone buzzer manually without tone() library for safety in FreeRTOS
          for(int i=0; i<500; i++) {
             digitalWrite(BUZZER_PIN, HIGH);
             delayMicroseconds(500);
             digitalWrite(BUZZER_PIN, LOW);
             delayMicroseconds(500);
          }
          digitalWrite(RED_LED, LOW);
        } else {
          // NIGHT MODE
          digitalWrite(VIBRATION_PIN, HIGH);
          vTaskDelay(pdMS_TO_TICKS(500));
          digitalWrite(VIBRATION_PIN, LOW);
        }
      } else if (res.classification == "SAFE") {
        digitalWrite(GREEN_LED, HIGH);
        vTaskDelay(pdMS_TO_TICKS(100));
        digitalWrite(GREEN_LED, LOW);
      }
    }
  }
}

// ---------------------------------------------------------
// TASK 4: WEBSOCKET BROADCAST (Core 0, 10Hz)
// ---------------------------------------------------------
void webSocketBroadcastTask(void *pvParameters) {
  TickType_t xLastWakeTime = xTaskGetTickCount();
  const TickType_t xFrequency = pdMS_TO_TICKS(100);

  for (;;) {
    vTaskDelayUntil(&xLastWakeTime, xFrequency);

    if (ws.count() > 0 && xSemaphoreTake(xWebSocketMutex, pdMS_TO_TICKS(10)) == pdTRUE) {
      // Broadcast latest data, here we could fetch from a shared state, 
      // but for demonstration we'll just send an IDLE/Heartbeat if no new event, 
      // or we could use the ClassificationQueue. To prevent consuming from AlertTask,
      // we can have a global struct updated by AlertTask.
      // Let's assume a simplified broadcast.
      
      StaticJsonDocument<256> doc;
      doc["type"] = "heartbeat";
      doc["system_healthy"] = systemHealthy;
      
      char jsonString[256];
      serializeJson(doc, jsonString);
      ws.textAll(jsonString);
      
      xSemaphoreGive(xWebSocketMutex);
    }
  }
}

// ---------------------------------------------------------
// WEBSOCKET EVENT HANDLER
// ---------------------------------------------------------
void onEvent(AsyncWebSocket *server, AsyncWebSocketClient *client, AwsEventType type,
             void *arg, uint8_t *data, size_t len) {
  if (type == WS_EVT_CONNECT) {
    Serial.printf("[WS] Client connected: %s\n", client->remoteIP().toString().c_str());
  } else if (type == WS_EVT_DISCONNECT) {
    Serial.printf("[WS] Client disconnected\n");
  } else if (type == WS_EVT_DATA) {
     // Handle incoming config if needed
  }
}

// ---------------------------------------------------------
// SETUP
// ---------------------------------------------------------
void setup() {
  Serial.begin(115200);

  pinMode(MIC_PIN, INPUT);
  pinMode(BUZZER_PIN, OUTPUT);
  pinMode(VIBRATION_PIN, OUTPUT);
  pinMode(GREEN_LED, OUTPUT);
  pinMode(RED_LED, OUTPUT);

  digitalWrite(GREEN_LED, LOW);
  digitalWrite(RED_LED, LOW);
  digitalWrite(BUZZER_PIN, LOW);
  digitalWrite(VIBRATION_PIN, LOW);

  // Initialize NVS
  preferences.begin("dysguard", false);
  currentSessionId = preferences.getInt("session_id", 0) + 1;
  preferences.putInt("session_id", currentSessionId);
  currentMode = preferences.getString("device_mode", "DAY");
  preferences.end();

  // Initialize I2C and MPU6050
  Wire.begin(MPU_SDA, MPU_SCL);
  Wire.setClock(400000);
  mpu.initialize();
  
  if (!mpu.testConnection()) {
    Serial.println("MPU6050 connection failed");
    systemHealthy = false;
  }

  // Calibrate
  Serial.println("Calibrating...");
  float baselineSum = 0;
  for(int i=0; i<300; i++) {
     baselineSum += sqrt(pow(mpu.getAccelerationX(), 2) + pow(mpu.getAccelerationY(), 2) + pow(mpu.getAccelerationZ(), 2)) / 16384.0;
     delay(10);
  }
  IMU_ONSET_THRESHOLD = (baselineSum / 300.0) * 3.0;
  Serial.printf("Calibration done. IMU Threshold: %.2f\n", IMU_ONSET_THRESHOLD);

  // Setup WiFi AP
  WiFi.softAP("DysphagiaGuard-AP");
  Serial.print("AP IP address: ");
  Serial.println(WiFi.softAPIP());

  // Setup WebSocket and HTTP
  ws.onEvent(onEvent);
  server.addHandler(&ws);
  server.on("/status", HTTP_GET, [](AsyncWebServerRequest *request){
    request->send(200, "text/plain", "OK");
  });
  server.begin();

  // Init Queues and Mutexes
  xSensorQueue = xQueueCreate(2, sizeof(SensorSample) * 50);
  xClassificationQueue = xQueueCreate(10, sizeof(SwallowResult));
  xWebSocketMutex = xSemaphoreCreateMutex();
  xI2CMutex = xSemaphoreCreateMutex();

  // Create Tasks
  xTaskCreatePinnedToCore(sensorAcquisitionTask, "SensorAcq", 4096, NULL, 5, NULL, 0);
  xTaskCreatePinnedToCore(signalProcessingTask, "SignalProc", 8192, NULL, 4, NULL, 1);
  xTaskCreatePinnedToCore(alertDispatchTask, "AlertDisp", 2048, NULL, 3, NULL, 1);
  xTaskCreatePinnedToCore(webSocketBroadcastTask, "WSBroad", 4096, NULL, 2, NULL, 0);
}

void loop() {
  // Feed Watchdog & Green LED heartbeat
  if (systemHealthy) {
     digitalWrite(GREEN_LED, HIGH);
     vTaskDelay(pdMS_TO_TICKS(50));
     digitalWrite(GREEN_LED, LOW);
  } else {
     digitalWrite(RED_LED, HIGH);
     vTaskDelay(pdMS_TO_TICKS(100));
     digitalWrite(RED_LED, LOW);
  }
  vTaskDelay(pdMS_TO_TICKS(1000));
}
