import base64
import logging
import os

from google import genai
from google.genai import types

client = genai.Client(api_key=os.environ['GEMINI_API_KEY'])
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

        response = client.models.generate_content(
            model="gemini-2.0-flash",
            contents=[
                f"""One image is a photo and the other is a screenshot.
                Based on the person's emotion in the photo, always infer and comment on the 
                user experience of the application shown in the screenshot, even if the connection is unclear.
                Respond concisely (max 50 characters). Do not say 'no relationship' or similar. No introductions or extra commentary.""",
                types.Part.from_bytes(data=photo_bytes, mime_type='image/jpeg'),
                types.Part.from_bytes(data=screenshot_bytes, mime_type='image/jpeg')
            ]
        )
        results.append({
            "analysis": response.text
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

        response = client.models.generate_content(
            model="gemini-2.0-flash",
            contents=[
                f"""Analyze the following results collectively: {combined_input}. 
                Respond with only ONE of these labels and nothing else: 
                Happiness, Sadness, Anger, Fear, Surprise, Disgust, Neutral"""
            ]
        )

        processed_results.append({
            "analysis": response.text
        })
    except Exception as e:
        logging.error(f"Error processing results: {e}")
        processed_results.append({
            "error": str(e)
        })

    return processed_results
