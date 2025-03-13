import cv2
import numpy as np
import matplotlib.pyplot as plt
import logging
from flask import Flask, request
import os

import keras as k
import tensorflow as tf
import requests

def process_image():
    logging.info("Processing image")

    for file in os.listdir(UPLOAD_FOLDER):
        logging.info(f"Processing {file}")
        # Resmi oku
        image = cv2.imread(UPLOAD_FOLDER+"/"+file)
        logging.info("Image read successfully")

        # Resmi griye çevir
        gray_image = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        logging.info("Converted image to grayscale")

        # Resmi göster
        plt.imshow(gray_image, cmap='gray')
        plt.show()
        logging.info("Displayed grayscale image")

        # Resmi yeniden boyutlandır
        resized_image = cv2.resize(gray_image, (28, 28))

        # Resmi göster
        plt.imshow(resized_image, cmap='gray')
        plt.show()

        # Resmi yeniden boyutlandır
        # resized_image = resized_image.reshape(1, 28, 28, 1)
        #
        # Resmi normalize et
        # resized_image = resized_image / 255.0

        # Modeli yükle
        # model = k.models.load_model("model.h5")
        #
        # Tahmin yap
        # prediction = model.predict(resized_image)
        #
        # Tahmini göster
        # print(f"Tahmin: {np.argmax(prediction)}")
        #
        # return np.argmax(prediction)
app = Flask(__name__)

UPLOAD_FOLDER = "images"
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

logging.basicConfig(level=logging.INFO)

@app.before_request
def log_request_info():
    logging.info(f"Request: {request.method} {request.url}")
    logging.info(f"Headers: {request.headers}")

@app.route('/processImage', methods=['POST'])
def get_unprocessed_image():
    file = request.files['file']

    if file.filename == '':
        return "Dosya adı geçersiz!", 400

    # Dosyayı kaydet
    file_path = os.path.join(UPLOAD_FOLDER, file.filename)
    file.save(file_path)

    return f"Dosya başarıyla kaydedildi: {file_path}", 200

@app.route('/triggerProcessImage', methods=['GET'])
def trigger_process_image():
    process_image()
    return "Image processing triggered", 200

if __name__ == '__main__':
    app.run(debug=True)