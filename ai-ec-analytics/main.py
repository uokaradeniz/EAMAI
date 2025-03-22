import logging
from flask import Flask, request
import os

import emotiondetection

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
    emotiondetection.predict_emotion()
    return "Image processing triggered", 200

if __name__ == '__main__':
    app.run(debug=True)