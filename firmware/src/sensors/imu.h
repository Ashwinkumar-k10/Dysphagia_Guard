#ifndef IMU_H
#define IMU_H

#include <MPU6050.h>

void imuInit();
bool imuRead(int16_t &ax, int16_t &ay, int16_t &az);

#endif