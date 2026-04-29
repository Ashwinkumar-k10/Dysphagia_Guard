# evaluate.py

import tensorflow as tf
import numpy as np
from preprocess import preprocess
from sklearn.metrics import confusion_matrix, classification_report, accuracy_score

def evaluate():

    # Load data
    X_train, X_test, y_train, y_test = preprocess()

    # Load trained model
    model = tf.keras.models.load_model("model.h5")

    # Predictions
    y_pred = model.predict(X_test)
    y_pred_classes = np.argmax(y_pred, axis=1)

    # Accuracy
    acc = accuracy_score(y_test, y_pred_classes)
    print("Accuracy:", acc)

    # Confusion Matrix
    print("\nConfusion Matrix:")
    print(confusion_matrix(y_test, y_pred_classes))

    # Classification Report
    print("\nClassification Report:")
    print(classification_report(
        y_test,
        y_pred_classes,
        target_names=["Safe", "Unsafe", "Cough", "No Activity"]
    ))

if __name__ == "__main__":
    evaluate()