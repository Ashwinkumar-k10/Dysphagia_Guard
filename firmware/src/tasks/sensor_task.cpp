#include "../sensors/imu.h"
#include "../sensors/mic.h"
#include <Arduino.h>

void sensorTask(void *pv) {

    int16_t ax, ay, az;

    for (;;) {

        if (imuRead(ax, ay, az)) {
            uint16_t mic = micRead();
            Serial.println(mic);
        }

        vTaskDelay(1);
    }
}