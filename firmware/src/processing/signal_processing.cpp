#include "signal_processing.h"
#include "../utils/filters.h"
#include <math.h>

SwallowResult processSignal(int16_t *ax, int16_t *ay, int16_t *az, uint16_t *mic) {

    SwallowResult res;

    float sumSq = 0;
    uint16_t peak = 0;

    for (int i = 0; i < 50; i++) {

        float mag = sqrt(ax[i]*ax[i] + ay[i]*ay[i] + az[i]*az[i]) / 16384.0;
        float filtered = highPassFilter(mag);

        sumSq += filtered * filtered;

        if (mic[i] > peak) peak = mic[i];
    }

    float rms = sqrt(sumSq / 50.0);
    float envelope = peak / 4095.0;

    res.imu_rms = rms;
    res.mic_envelope = envelope;

    if (rms > 0.2 && envelope > 0.3) {
        res.classification = "SAFE";
        res.confidence = 0.8;
    } else {
        res.classification = "NOISE";
        res.confidence = 0.3;
    }

    return res;
}