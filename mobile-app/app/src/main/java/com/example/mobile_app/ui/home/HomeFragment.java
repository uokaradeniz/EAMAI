package com.example.mobile_app.ui.home;

import static androidx.core.content.ContextCompat.getColor;
import static com.example.mobile_app.ui.api.BackendApiConfig.URL_PHYSICAL;
import static com.example.mobile_app.ui.api.BackendApiConfig.URL_VIRTUAL;
import static com.example.mobile_app.ui.api.BackendApiConfig.currentUrl;
import static com.example.mobile_app.ui.api.BackendApiConfig.isEmulator;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
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
import com.example.mobile_app.ui.utils.DeviceUtils;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
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
    FragmentHomeBinding viewBinding;
    PreviewView previewView;
    Switch previewSwitch;
    ImageCapture imageCapture;
    EditText ipEditText;
    Handler handler = new Handler(Looper.getMainLooper());

    UUID sessionId;
    int delayMillis = 3000;

    private final Map<String, byte[]> imageMapList = new HashMap<>();

    private final int maxCaptureCount = 3;

    private int counter = 0;
    private Runnable timerRunnable;
    boolean maxNumOfImagesReached;
    String physicalAddress;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        viewBinding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = viewBinding.getRoot();

        previewView = viewBinding.previewView;
        ImageButton beginButton = viewBinding.beginButton;
        previewSwitch = viewBinding.previewSwitch;
        previewSwitch.setChecked(false);
        ipEditText = viewBinding.ipEditText;
        if (isEmulator) {
            ipEditText.setVisibility(View.GONE);
            currentUrl = URL_VIRTUAL;
        } else {
            ipEditText.setVisibility(View.VISIBLE);
            physicalAddress = URL_PHYSICAL + ipEditText.getText().toString() + ":8080";
            currentUrl = physicalAddress;
        }

        beginButton.setOnClickListener(this::onBeginButtonClick);

        startCamera();

        previewSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                previewView.setForeground(new ColorDrawable(getColor(requireContext(), android.R.color.black)));
            } else {
                previewView.setForeground(null);
            }
        });

        return root;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    public void onBeginButtonClick(View v) {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
        viewBinding.previewSwitch.setEnabled(false);
        viewBinding.ipEditText.setEnabled(false);
        physicalAddress = URL_PHYSICAL + ipEditText.getText().toString() + ":8080";
        currentUrl = isEmulator ? URL_VIRTUAL : physicalAddress;
        startTimer();
        viewBinding.beginButton.setEnabled(false);
        viewBinding.beginButton.setBackgroundColor(getColor(requireContext(), android.R.color.darker_gray));
        captureAndSendImages(maxCaptureCount, delayMillis);
        sessionId = UUID.randomUUID();
    }

    private void startTimer() {
        counter = 0;
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                viewBinding.timerTextView.setText("Session in progress...\n Time elapsed: " + counter + "s");
                counter++;
                handler.postDelayed(this, 1000);
            }
        };

        handler.post(timerRunnable);
    }

    private void stopTimer() {
        handler.removeCallbacks(timerRunnable);
        viewBinding.timerTextView.setText("Finalizing...");
        viewBinding.previewView.setForeground(null);
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
                    byte[] imageData = imageToByteArray(image);
                    String filename = "image_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".jpg";
                    imageMapList.put(filename, imageData);
                    Toast.makeText(requireContext(), "Image Captured: " + filename, Toast.LENGTH_SHORT).show();
                    maxNumOfImagesReached = imageMapList.size() == maxCaptureCount;
                    if (maxNumOfImagesReached) {
                        stopTimer();
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
                .url(currentUrl + "/api/uploadImages")
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
                        resetSessionViews();
                    } else {
                        Toast.makeText(requireContext(), "Failed to send images", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void resetSessionViews() {
        viewBinding.beginButton.setEnabled(true);
        viewBinding.timerTextView.setText("Ready");
        viewBinding.previewSwitch.setEnabled(true);
        viewBinding.ipEditText.setEnabled(true);
        imageMapList.clear();
    }
}