# dataset_generation.py

import numpy as np
import pandas as pd
from config import DATA_PATH

def generate_sample(label):

    acc = np.random.normal(0.5, 0.3, 3)
    gyro = np.random.normal(0.5, 0.3, 3)
    sound = np.random.normal(0.4, 0.2)

    if label == 0:
        acc += np.random.uniform(0.4, 1.0, 3)
        gyro += np.random.uniform(0.3, 0.9, 3)
        sound += np.random.uniform(0.3, 0.6)

    elif label == 1:
        acc += np.random.uniform(0.7, 1.4, 3)
        gyro += np.random.uniform(0.6, 1.2, 3)
        sound += np.random.uniform(0.5, 0.8)

    elif label == 2:
        acc += np.random.uniform(1.2, 2.2, 3)
        gyro += np.random.uniform(1.2, 2.5, 3)
        sound += np.random.uniform(0.8, 1.3)

    else:
        acc += np.random.uniform(0.0, 0.4, 3)
        gyro += np.random.uniform(0.0, 0.4, 3)
        sound += np.random.uniform(0.0, 0.2)

    return list(acc) + list(gyro) + [sound, label]


def generate_dataset():
    np.random.seed(42)
    samples = []

    for _ in range(300): samples.append(generate_sample(0))
    for _ in range(300): samples.append(generate_sample(1))
    for _ in range(200): samples.append(generate_sample(2))
    for _ in range(200): samples.append(generate_sample(3))

    df = pd.DataFrame(samples, columns=[
        "AccX","AccY","AccZ",
        "GyroX","GyroY","GyroZ",
        "Sound_Level","Label"
    ])

    df.to_csv(DATA_PATH, index=False)
    print("Dataset generated!")

if __name__ == "__main__":
    generate_dataset()