# import cv2
# import mediapipe as mp
# from  import GazeEstimator
# import numpy as np
#
# mp_face_mesh = mp.solutions.face_mesh
# face_mesh = mp_face_mesh.FaceMesh(static_image_mode=True, max_num_faces=1, refine_landmarks=True)
#
# gaze_estimator = GazeEstimator()
#
# def detect_and_estimate_gaze(photo, screenshot):
    # photo_rgb = cv2.cvtColor(photo, cv2.COLOR_BGR2RGB)
    # screenshot_rgb = cv2.cvtColor(screenshot, cv2.COLOR_BGR2RGB)
    #
    # results = face_mesh.process(photo_rgb)
    # if not results.multi_face_landmarks:
    #     return {"error": "No face detected in the photo"}
    #
    # landmarks = results.multi_face_landmarks[0]
    # left_eye = np.array([(landmarks.landmark[i].x, landmarks.landmark[i].y) for i in range(33, 133)])
    # right_eye = np.array([(landmarks.landmark[i].x, landmarks.landmark[i].y) for i in range(362, 463)])
    #
    # gaze_result = gaze_estimator.estimate(left_eye, right_eye)
    #
    # screenshot_height, screenshot_width, _ = screenshot.shape
    # gaze_x = int(gaze_result['x'] * screenshot_width)
    # gaze_y = int(gaze_result['y'] * screenshot_height)
    #
    # return {"gaze_point_on_screenshot": (gaze_x, gaze_y)}