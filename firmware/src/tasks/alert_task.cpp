#include "../config.h"
#include <Arduino.h>

void handleAlert(String classification) {

    if (classification == "UNSAFE") {
        digitalWrite(RED_LED, HIGH);
    } else if (classification == "SAFE") {
        digitalWrite(GREEN_LED, HIGH);
    }
}