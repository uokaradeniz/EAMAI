from flask import Flask, request, jsonify
from emotionInterpreterAPI import process_images
import logging

app = Flask(__name__)
logging.basicConfig(level=logging.INFO)

@app.route('/processImages', methods=['POST'])
def get_unprocessed_images():
    if not request.is_json:
        return "Request must be JSON", 400

    image_pairs = request.json.get('images', [])
    logging.debug(f"Received image pairs: {image_pairs}")
    if not image_pairs:
        return "No image pairs provided", 400

    results = process_images(image_pairs)
    return jsonify(results), 200


if __name__ == '__main__':
    app.run()
