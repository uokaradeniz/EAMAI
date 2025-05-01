from flask import Flask, request, jsonify
from emotionInterpreterAPI import process_images, process_results
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


@app.route('/processResults', methods=['POST'])
def get_process_results():
    if not request.is_json:
        return "Request must be JSON", 400

    session_id = request.json.get('sessionId')
    sessionResults = request.json.get('results')

    if not session_id or not sessionResults:
        return "Missing sessionId or sessionResults", 400

    if not isinstance(sessionResults, list):
        return "Results must be a list", 400

    logging.info(f"Processing sessionResults for session_id: {session_id}")
    processed_results = process_results(sessionResults)


    return jsonify({
        "session_id": session_id,
        "processed_results": processed_results
    }), 200

if __name__ == '__main__':
    app.run()
