# preprocess.py

import numpy as np
import pandas as pd
from sklearn.preprocessing import StandardScaler
from sklearn.model_selection import train_test_split
from config import *

def preprocess():

    df = pd.read_csv(DATA_PATH)

    X = df.drop("Label", axis=1)
    y = df["Label"]

    # Add noise
    noise = np.random.normal(0, GAUSSIAN_NOISE_STD, X.shape)
    X = X + noise
    X += np.random.uniform(-UNIFORM_NOISE_RANGE, UNIFORM_NOISE_RANGE, X.shape)

    # Normalize
    scaler = StandardScaler()
    X_scaled = scaler.fit_transform(X)

    # Save scaler
    np.save(SCALER_MEAN_PATH, scaler.mean_)
    np.save(SCALER_STD_PATH, scaler.scale_)

    # Split
    X_train, X_test, y_train, y_test = train_test_split(
        X_scaled, y,
        test_size=TEST_SIZE,
        stratify=y,
        random_state=RANDOM_STATE
    )

    return X_train, X_test, y_train, y_test