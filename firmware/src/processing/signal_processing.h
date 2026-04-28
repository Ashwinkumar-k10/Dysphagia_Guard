#ifndef SIGNAL_PROCESSING_H
#define SIGNAL_PROCESSING_H

#include <Arduino.h>

struct SwallowResult {
    String classification;
    float imu_rms;
    float mic_envelope;
    float confidence;
};

SwallowResult processSignal(int16_t *ax, int16_t *ay, int16_t *az, uint16_t *mic);

#endif