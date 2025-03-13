package com.example.mobile_app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
//    http://192.168.0.78:8080
//    http://10.0.2.2:8080
    public static final String URL = "http://192.168.0.78:8080";
    private PreviewView previewView;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private Handler handler;
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int CAPTURE_INTERVAL = 2000;
    private static final int CAPTURE_COUNT = 2;
    private static int captureCounter = 0;

    UUID sessionId;

    public static int getCaptureCounter() {
        return captureCounter;
    }

    public static void setCaptureCounter(int captureCounter) {
        MainActivity.captureCounter = captureCounter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sessionId = UUID.randomUUID();

        previewView = findViewById(R.id.previewView);
        cameraExecutor = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                Preview preview = new Preview.Builder().build();
                imageCapture = new ImageCapture.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

                handler.postDelayed(this::takePhoto, CAPTURE_INTERVAL);

            } catch (Exception e) {
                Log.e("CameraX", "Kamera başlatılamadı.", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                byte[] byteArray = null;
                try (image) {
                    // Convert ImageProxy to ByteArray
                    ImageProxy.PlaneProxy[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();
                    byteArray = new byte[buffer.remaining()];
                    buffer.get(byteArray);
                } catch (Exception e) {
                    Log.e("CameraX", "Error converting image", e);
                }

                if (byteArray != null) {
                    uploadFile(byteArray);
                }

                handler.postDelayed(MainActivity.this::takePhoto, CAPTURE_INTERVAL);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e("CameraX", "Error taking photo: " + exception.getMessage());
            }
        });
    }

    private void uploadFile(byte[] byteArray) {
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "image" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".jpg",
                        RequestBody.create(byteArray, MediaType.parse("image/jpeg")))
                .addFormDataPart("sessionId", sessionId.toString())
                .build();

        Request request = new Request.Builder()
                .url(URL + "/api/uploadImage")
                .post(requestBody)
                .build();

        captureCounter++;

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("Upload", "Upload failed: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                assert response.body() != null;
                Log.d("Upload", "Capture Count: " + captureCounter + "Response: " + response.body().string());
                if (captureCounter >= CAPTURE_COUNT) {
                    finish();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Kamera izni reddedildi.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}