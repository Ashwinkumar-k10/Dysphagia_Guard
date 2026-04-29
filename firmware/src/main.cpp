#include <Arduino.h>
#include "config.h"
#include "sensors/imu.h"

void setup() {

    Serial.begin(115200);

    pinMode(GREEN_LED, OUTPUT);
    pinMode(RED_LED, OUTPUT);
    pinMode(BUZZER_PIN, OUTPUT);

    imuInit();

    Serial.println("DysphagiaGuard Started");
}

void loop() {
    delay(1000);
}