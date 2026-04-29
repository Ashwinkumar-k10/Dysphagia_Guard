#include "imu.h"
#include "../config.h"
#include <Wire.h>

MPU6050 mpu;

void imuInit() {
    Wire.begin(MPU_SDA, MPU_SCL);
    mpu.initialize();
}

bool imuRead(int16_t &ax, int16_t &ay, int16_t &az) {
    if (!mpu.testConnection()) return false;

    ax = mpu.getAccelerationX();
    ay = mpu.getAccelerationY();
    az = mpu.getAccelerationZ();
    return true;
}