import base64
import logging
import os

from google import genai
from google.genai import types

client = genai.Client(api_key=os.environ['GEMINI_API_KEY'])
logging.basicConfig(level=logging.DEBUG)

def process_images(json_data):
    results = []
    try:
        # Ensure json_data is a list
        if not isinstance(json_data, list):
            raise ValueError("Expected a list as input")

        if len(json_data) != 2:
            raise ValueError("Expected exactly 2 images in the input list")

        # Extract and decode image data
        photo_data = json_data[0].get('photo_data', '')
        screenshot_data = json_data[1].get('screenshot_data', '')

        if not screenshot_data or not photo_data:
            raise ValueError("Missing 'photo_data' or 'screenshot_data' in the images")

        img1_bytes = base64.b64decode(photo_data)
        img2_bytes = base64.b64decode(screenshot_data)

        # Send to Gemini API
        response = client.models.generate_content(
            model="gemini-2.0-flash",
            contents=[
                "One image is a photo and the other is a screenshot. Comment on the context, connection between the persons emotion and the user experience of the application on the screenshot. Respond concisely. No introductions. Your response must be max of 100 characters",
                types.Part.from_bytes(data=img1_bytes, mime_type='image/jpeg'),
                types.Part.from_bytes(data=img2_bytes, mime_type='image/jpeg')
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