package com.example.mobile_app.ui.foreground;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleService;

import com.example.mobile_app.MainActivity;
import com.example.mobile_app.R;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.example.mobile_app.ui.api.BackendApiConfig.companyId;
import static com.example.mobile_app.ui.api.BackendApiConfig.currentUrl;

public class ForegroundCameraService extends LifecycleService {
    private static final String TAG = "ForegroundCameraService";
    private static final String CHANNEL_ID = "CameraServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private Handler handler;
    private UUID sessionId;
    private final Map<String, byte[]> imageMapList = new HashMap<>();
    private int maxCaptureCount = 3;
    private int delayMillis = 5000;
    private boolean isCapturing = false;
    private ExecutorService executor;


    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private int screenDensity;
    private int screenWidth;
    private int screenHeight;
    private int resultCode;
    private Intent resultData;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        sessionId = UUID.randomUUID();
        executor = Executors.newSingleThreadExecutor();

        createNotificationChannel();
        createUploadNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification("Starting camera service..."));

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);
        screenDensity = metrics.densityDpi;
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;

        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (intent != null) {
            if (intent.hasExtra("resultCode") && intent.hasExtra("resultData")) {
                resultCode = intent.getIntExtra("resultCode", 0);
                resultData = intent.getParcelableExtra("resultData");

                if (resultCode != 0 && resultData != null) {
                    initMediaProjection(resultCode, resultData);
                }
            }

            if (!isCapturing && mediaProjection != null) {
                isCapturing = true;
                initCamera();
            }

            if (intent.hasExtra("maxCaptureCount")) {
                maxCaptureCount = intent.getIntExtra("maxCaptureCount", 3);
            }

            if (intent.hasExtra("delayMillis")) {
                delayMillis = intent.getIntExtra("delayMillis", 3000);
            }
        }

        return START_STICKY;
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Camera Service Channel",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Used for camera image capture operations");

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }

    private void initMediaProjection(int resultCode, Intent data) {
        MediaProjectionManager projectionManager = (MediaProjectionManager)
                getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }

        mediaProjection = projectionManager.getMediaProjection(resultCode, data);

        mediaProjection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                Log.i(TAG, "MediaProjection stopped");
                if (virtualDisplay != null) {
                    virtualDisplay.release();
                    virtualDisplay = null;
                }
            }
        }, new Handler());

        createVirtualDisplay();
    }

    private void createVirtualDisplay() {
        if (mediaProjection == null) {
            Log.e(TAG, "MediaProjection is null");
            return;
        }

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        int density = metrics.densityDpi;

        imageReader = ImageReader.newInstance(screenWidth, screenHeight,
                PixelFormat.RGBA_8888, 2);

        virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenCapture",
                screenWidth, screenHeight, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null);
    }

    private static final int UPLOAD_NOTIFICATION_ID = 2;
    private static final String UPLOAD_CHANNEL_ID = "UploadNotificationChannel";

    private void createUploadNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                UPLOAD_CHANNEL_ID,
                "Upload Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Notifications for image upload status");

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }

    private void showUploadSuccessNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, UPLOAD_CHANNEL_ID)
                .setContentTitle("Upload Complete")
                .setContentText("Images were successfully uploaded to server")
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build();

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(UPLOAD_NOTIFICATION_ID, notification);
    }

    private Notification createNotification(String contentText) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Camera Service")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateNotification(String contentText) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, createNotification(contentText));
    }

    private void initCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCase();

                updateNotification("Capturing images...");
                captureAndSendImages(maxCaptureCount, delayMillis);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error initializing camera: " + e.getMessage());
                updateNotification("Camera initialization failed");
                stopSelf();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCase() {
        if (cameraProvider == null) {
            return;
        }

        cameraProvider.unbindAll();

        imageCapture = new ImageCapture.Builder()
                .setTargetResolution(new Size(1280, 720))
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

        try {
            cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture);
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    private void captureAndSendImages(int count, int delayMillis) {
        imageMapList.clear();
        for (int i = 0; i < count; i++) {
            handler.postDelayed(this::captureImage, (long) i * delayMillis);
        }
    }

    private void captureImage() {
        if (imageCapture == null) {
            Log.e(TAG, "Cannot capture image, ImageCapture is null");
            return;
        }

        String twinId = UUID.randomUUID().toString();

        imageCapture.takePicture(ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        try (image) {
                            String timestamp = LocalDateTime.now().format(
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));

                            byte[] photoData = imageToByteArray(image);
                            photoData = compressImage(photoData, 60);

                            imageMapList.put(twinId + "_photo_" + timestamp + ".jpg", photoData);

                            captureScreenshot(twinId);

                            updateNotification("Captured images: " + imageMapList.size() / 2 + "\n" + "Total images: " + maxCaptureCount);

                        } catch (IOException e) {
                            Log.e(TAG, "Error processing captured image: " + e.getMessage());
                        }
                    }
                });
    }

    private void captureScreenshot(String twinId) {
        if (mediaProjection == null || virtualDisplay == null || imageReader == null) {
            Log.e(TAG, "MediaProjection setup incomplete, cannot capture screenshot");
            return;
        }

        try {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> {
                try (Image image = imageReader.acquireLatestImage()) {
                    if (image != null) {
                        byte[] screenshotData = null;
                        try {
                            screenshotData = imageToByteArray(image);
                            screenshotData = compressImage(screenshotData, 60);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        String timestamp = LocalDateTime.now().format(
                                DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));

                        imageMapList.put(twinId + "_screenshot_" + timestamp + ".jpg", screenshotData);

                        Log.d(TAG, "Captured screenshot with twinId: " + twinId);

                        if (imageMapList.size() == maxCaptureCount * 2) {
                            updateNotification("Processing and sending images...");
                            sendImagesToServer(imageMapList);
                        }
                    }
                }
            }, 100);

        } catch (Exception e) {
            Log.e(TAG, "Error capturing screenshot: " + e.getMessage());
        }
    }

    private byte[] imageToByteArray(ImageProxy image) throws IOException {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);
        bitmap.recycle();

        return outputStream.toByteArray();
    }

    private byte[] imageToByteArray(Image image) throws IOException {
        if (image.getFormat() == PixelFormat.RGBA_8888) {
            Image.Plane plane = image.getPlanes()[0];
            ByteBuffer buffer = plane.getBuffer();

            int pixelStride = plane.getPixelStride();
            int rowStride = plane.getRowStride();
            int rowPadding = rowStride - pixelStride * screenWidth;

            Bitmap bitmap = Bitmap.createBitmap(
                    screenWidth + rowPadding / pixelStride,
                    screenHeight,
                    Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);

            Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight);

            int targetWidth = screenWidth / 2;
            int targetHeight = screenHeight / 2;
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(croppedBitmap, targetWidth, targetHeight, true);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);

            bitmap.recycle();
            if (bitmap != croppedBitmap) {
                croppedBitmap.recycle();
            }
            if (resizedBitmap != croppedBitmap) {
                resizedBitmap.recycle();
            }

            return outputStream.toByteArray();
        } else {
            throw new IOException("Unsupported image format");
        }
    }

    private byte[] compressImage(byte[] imageData, int quality) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(imageData, 0, imageData.length, options);

        int width = options.outWidth;
        int height = options.outHeight;

        int maxDimension = 1080;
        float scaleFactor = 1.0f;
        if (width > maxDimension || height > maxDimension) {
            scaleFactor = Math.max((float)width / maxDimension, (float)height / maxDimension);
        }

        options = new BitmapFactory.Options();
        options.inSampleSize = Math.round(scaleFactor);
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length, options);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
        bitmap.recycle();

        return outputStream.toByteArray();
    }
    private void sendImagesToServer(Map<String, byte[]> images) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        JSONArray imagesArray = new JSONArray();

        for (Map.Entry<String, byte[]> entry : images.entrySet()) {
            String originalFileName = entry.getKey();
            String twinId = originalFileName.split("_")[0];
            String type = originalFileName.contains("photo") ? "photo" : "screenshot";
            String timestampAndExtension = originalFileName.split("_")[2];
            String newFileName = type + "_" + timestampAndExtension;
            String base64Image = android.util.Base64.encodeToString(entry.getValue(),
                    android.util.Base64.DEFAULT);

            JSONObject imageObject = new JSONObject();
            try {
                imageObject.put("twinId", twinId);
                imageObject.put("type", type);
                imageObject.put("filename", newFileName);
                imageObject.put("data", base64Image);
                imagesArray.put(imageObject);
            } catch (JSONException e) {
                Log.e(TAG, "JSON error: " + e.getMessage());
            }
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("sessionId", sessionId.toString());
            payload.put("companyId", companyId);
            payload.put("images", imagesArray);
        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
        }

        RequestBody requestBody = RequestBody.create(payload.toString(),
                MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(currentUrl + "/api/uploadImages")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Network error: " + e.getMessage());
                updateNotification("Failed to send images: network error");
                stopService();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Images uploaded successfully");
                    updateNotification("Images sent successfully");

                    handler.post(() -> showUploadSuccessNotification());
                } else {
                    Log.e(TAG, "Upload failed: " + response.code());
                    updateNotification("Failed to send images: " + response.code());
                }
                stopService();
            }
        });
    }

    private void stopService() {
        handler.post(() -> {
            isCapturing = false;
            imageMapList.clear();
            if (cameraProvider != null) {
                cameraProvider.unbindAll();
            }
            stopSelf();
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }

        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }

        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }

        if (executor != null) {
            executor.shutdown();
        }
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return null;
    }
}