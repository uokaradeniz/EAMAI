import base64
import logging
import os
import time

import google.generativeai as genai
from google.generativeai import types

genai.configure(api_key=os.environ['GEMINI_API_KEY'])
logging.basicConfig(level=logging.INFO)


def process_images(json_data):
    results = []
    try:
        if not isinstance(json_data, list):
            raise ValueError("Expected a list as input")

        if len(json_data) != 2:
            raise ValueError("Expected exactly 2 images in the input list")

        photo_data = json_data[0].get('photo_data', '')
        screenshot_data = json_data[1].get('screenshot_data', '')

        if not screenshot_data or not photo_data:
            raise ValueError("Missing 'photo_data' or 'screenshot_data' in the images")

        photo_bytes = base64.b64decode(photo_data)
        screenshot_bytes = base64.b64decode(screenshot_data)

        for attempt in range(3):
            try:
                response = genai.generate_content(
                    model="gemini-2.0-flash",
                    contents=[
                        "One image is a photo and the other is a screenshot. Comment on the context, connection between the persons emotion and the user experience of the application on the screenshot. Respond concisely. No introductions. Your response must be max of 100 characters",
                        types.Part.from_bytes(data=photo_bytes, mime_type='image/jpeg'),
                        types.Part.from_bytes(data=screenshot_bytes, mime_type='image/jpeg')
                    ]
                )
                results.append({
                    "analysis": response.text
                })
                break
            except Exception as e:
                if attempt < 2:
                    logging.warning(f"Attempt {attempt + 1} failed: {e}. Retrying...")
                    time.sleep(0.25)
                else:
                    logging.error(f"Error processing images: {e}")
                    results.append({
                        "error": str(e)
                    })

    except Exception as e:
        logging.error(f"Error processing images: {e}")
        results.append({
            "error": str(e)
        })

    return results


def process_results(sessionResults):
    processed_results = []
    try:
        if not isinstance(sessionResults, list):
            raise ValueError("Expected a list of results as input")

        combined_input = "\n".join(sessionResults)

        for attempt in range(3):
            try:
                response = genai.generate_content(
                    model="gemini-2.0-flash",
                    contents=[
                        f"Analyze the following results collectively: {combined_input}. Respond concisely. No introductions. Answer with only ONE of these labels: Happiness, Sadness, Anger, Fear, Surprise, Disgust, Neutral"
                    ]
                )
                processed_results.append({
                    "analysis": response.text
                })
                break
            except Exception as e:
                if attempt < 2:
                    logging.warning(f"Attempt {attempt + 1} failed: {e}. Retrying...")
                    time.sleep(0.25)
                else:
                    logging.error(f"Error processing results: {e}")
                    processed_results.append({
                        "error": str(e)
                    })

    except Exception as e:
        logging.error(f"Error processing results: {e}")
        processed_results.append({
            "error": str(e)
        })

    return processed_results
