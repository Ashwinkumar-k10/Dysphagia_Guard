#include "filters.h"

static float prev_filtered = 0;
static float prev_raw = 0;
static const float alpha = 0.95;

float highPassFilter(float input) {
    float output = alpha * (prev_filtered + input - prev_raw);
    prev_filtered = output;
    prev_raw = input;
    return output;
}