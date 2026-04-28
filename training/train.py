# train.py

import tensorflow as tf
from tensorflow.keras import regularizers
from tensorflow.keras.callbacks import EarlyStopping
from preprocess import preprocess
from config import *

def build_model():

    model = tf.keras.Sequential([
        tf.keras.layers.Dense(
            LAYER1_UNITS,
            activation='relu',
            input_shape=(NUM_FEATURES,),
            kernel_regularizer=regularizers.l2(0.001)
        ),
        tf.keras.layers.Dropout(DROPOUT_RATE),
        tf.keras.layers.Dense(
            LAYER2_UNITS,
            activation='relu',
            kernel_regularizer=regularizers.l2(0.001)
        ),
        tf.keras.layers.Dense(NUM_CLASSES, activation='softmax')
    ])

    model.compile(
        optimizer='adam',
        loss='sparse_categorical_crossentropy',
        metrics=['accuracy']
    )

    return model


def train():

    X_train, X_test, y_train, y_test = preprocess()

    model = build_model()

    early_stop = EarlyStopping(
        monitor='val_loss',
        patience=PATIENCE,
        restore_best_weights=True
    )

    model.fit(
        X_train, y_train,
        epochs=EPOCHS,
        batch_size=BATCH_SIZE,
        validation_data=(X_test, y_test),
        callbacks=[early_stop]
    )

    loss, acc = model.evaluate(X_test, y_test)
    print("Final Accuracy:", acc)

    model.save("model.h5")
    print("Model saved!")

if __name__ == "__main__":
    train()