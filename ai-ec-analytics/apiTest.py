import os
from google import genai

# Initialize the Gemini API client
client = genai.Client(api_key=os.environ['GEMINI_API_KEY'])

def test_gemini_api():
    try:
        # Send a simple text prompt to the Gemini API
        response = client.models.generate_content(
            model="gemini-2.0-flash",
            contents=["Hello, can you respond to this test?"]
        )
        print("Gemini API Response:", response.text)
    except Exception as e:
        print("Error:", str(e))

if __name__ == "__main__":
    test_gemini_api()