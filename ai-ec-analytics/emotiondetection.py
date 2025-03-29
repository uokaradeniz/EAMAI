import os

import cv2
import keras as k
import numpy as np

from config import UPLOAD_FOLDER, logging, clear_upload_folder

def preprocess_image():
    logging.info("Processing image")
    images = []
    for file in os.listdir(UPLOAD_FOLDER):
        img_path = os.path.join(UPLOAD_FOLDER, file)
        img = cv2.imread(img_path, cv2.IMREAD_GRAYSCALE)
        logging.info("Processing image: {}".format(img_path))
        if img is None:
            logging.error(f"Failed to read image: {img_path}")
            continue
        img = cv2.resize(img, (48, 48))
        img = img.astype('float32') / 255.0
        img = np.expand_dims(img, axis=0)
        img = np.expand_dims(img, axis=-1)
        images.append((img, img_path))
    return images

def predict_emotion():
    model = k.models.load_model('models/emotion_detection_model.h5')
    images = preprocess_image()
    mood_array = ['angry', 'disgust', 'fear', 'happy', 'sad', 'neutral', 'surprise']
    predictions = []

    for img, img_path in images:
        prediction = model.predict(img)
        emotion = np.argmax(prediction)
        img_name = os.path.basename(img_path)
        predictions.append({
            'image': img_name,
            'emotion': mood_array[emotion]
        })
        print(f'Image: {img_name}\nThe predicted emotion is: {mood_array[emotion]}')

    clear_upload_folder()
    return predictions