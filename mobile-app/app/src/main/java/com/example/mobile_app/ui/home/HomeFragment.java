package com.example.mobile_app.ui.home;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.mobile_app.databinding.FragmentHomeBinding;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HomeFragment extends Fragment {
    //    http://192.168.0.78:8080
//    http://10.0.2.2:8080
    public static final String URL = "http://10.0.2.2:8080";
    HomeViewModel homeViewModel;
    FragmentHomeBinding viewBinding;
    PreviewView previewView;
    ImageCapture imageCapture;
    Handler handler = new Handler(Looper.getMainLooper());

    UUID sessionId;
    int delayMillis = 1685;

//    private final List<byte[]> imageList = new ArrayList<>();
//    private final List<String> filenameList = new ArrayList<>();

    private Map<String, byte[]> imageMapList = new HashMap<>();

    private final int maxCaptureCount = 3;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        viewBinding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = viewBinding.getRoot();

        previewView = viewBinding.previewView;
        Button button = viewBinding.button;
        button.setOnClickListener(this::onButtonClick);
        startCamera();

        return root;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // Handle any errors (including cancellation) here.
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        imageCapture = new ImageCapture.Builder().build();

        CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewBinding = null;
    }

    public void onButtonClick(View v) {
        captureAndSendImages(maxCaptureCount, delayMillis);
        sessionId = UUID.randomUUID();
    }

    private void captureAndSendImages(int count, int delayMillis) {
        imageMapList.clear();
        for (int i = 0; i < count; i++) {
            handler.postDelayed(this::captureImage, (long) i * delayMillis);
        }
    }

    private void captureImage() {
        imageCapture.takePicture(ContextCompat.getMainExecutor(requireContext()), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                try (image) {
                    // Convert ImageProxy to byte array
                    byte[] imageData = imageToByteArray(image);
                    String filename = "image_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".jpg";
                    imageMapList.put(filename, imageData);
                    Toast.makeText(requireContext(), "Image Captured: " + filename, Toast.LENGTH_SHORT).show();
                    if (imageMapList.size() == maxCaptureCount) {
                        sendImagesToServer(imageMapList);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Toast.makeText(requireContext(), "Failed to capture image: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private byte[] imageToByteArray(ImageProxy image) throws IOException {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(bytes);

        return outputStream.toByteArray();
    }

    private void sendImagesToServer(Map<String, byte[]> images) {
        OkHttpClient client = new OkHttpClient();
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("sessionId", sessionId.toString());

        for (Map.Entry<String, byte[]> entry : images.entrySet()) {
            builder.addFormDataPart("images", entry.getKey(),
                    RequestBody.create(entry.getValue(), MediaType.parse("image/jpeg")));
        }
        RequestBody requestBody = builder.build();
        Request request = new Request.Builder()
                .url(URL + "/api/uploadImages")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                requireActivity().runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(requireContext(), "Images sent successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Failed to send images", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}