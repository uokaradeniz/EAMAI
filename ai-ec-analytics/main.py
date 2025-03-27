import os

from flask import Flask, request

from config import UPLOAD_FOLDER, logging, clear_upload_folder

from emotiondetection import predict_emotion

app = Flask(__name__)
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
logging.basicConfig(level=logging.INFO)

@app.route('/processImages', methods=['POST'])
def get_unprocessed_images():
    clear_upload_folder()
    if 'images' not in request.files:
        return "No images part in the request", 400

    files = request.files.getlist('images')
    if not os.path.exists(UPLOAD_FOLDER):
        os.makedirs(UPLOAD_FOLDER)

    for file in files:
        if file.filename == '':
            return "One or more files have no filename", 400
        file_path = os.path.join(UPLOAD_FOLDER, file.filename)
        file.save(file_path)
        logging.info(f"Saved image: {file_path}")

    return predict_emotion(), 200
if __name__ == '__main__':
    app.run(debug=True)