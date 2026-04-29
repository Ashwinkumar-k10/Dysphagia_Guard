# convert.py

import tensorflow as tf
from config import MODEL_TFLITE_PATH, MODEL_HEADER_PATH

def convert():

    model = tf.keras.models.load_model("model.h5")

    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]

    tflite_model = converter.convert()

    # Save TFLite
    with open(MODEL_TFLITE_PATH, "wb") as f:
        f.write(tflite_model)

    print("TFLite model saved!")

    # Convert to header
    with open(MODEL_TFLITE_PATH, "rb") as f:
        model_bytes = f.read()

    with open(MODEL_HEADER_PATH, "w") as f:
        f.write("const unsigned char model[] = {")
        f.write(",".join(str(b) for b in model_bytes))
        f.write("};\n")
        f.write("unsigned int model_len = sizeof(model);")

    print("model.h created!")

if __name__ == "__main__":
    convert()