import cv2
import numpy as np
import matplotlib.pyplot as plt

img = cv2.imread('indir.jpeg', cv2.IMREAD_COLOR)
import keras as k
import tensorflow as tf
import requests

var = k.__version__


def process_image():
    plt.imshow(img)
    plt.waitforbuttonpress()
    plt.close('all')


def get_image():
    url = "https://192.168.0.78:8080/api/imageAnalytics"
    resp = requests.get(url)
    if resp.status_code == 200:
        image_array = np.asarray(bytearray(resp.content), dtype=np.uint8)
        img = cv2.imdecode(image_array, cv2.IMREAD_COLOR)
        if img is not None:
            cv2.imshow('Fetched Image', img)
            cv2.waitKey(0)
            cv2.destroyAllWindows()
        else:
            print("Failed to decode image")
    else:
        print(f"Failed to fetch image, status code: {resp.status_code}")
