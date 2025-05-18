package com.example.mobile_app.ui.home;

import static androidx.core.content.ContextCompat.getColor;
import static com.example.mobile_app.ui.api.BackendApiConfig.companyId;
import static com.example.mobile_app.ui.api.BackendApiConfig.currentUrl;
import static com.example.mobile_app.ui.api.BackendApiConfig.isAuthenticated;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
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

import com.example.mobile_app.R;
import com.example.mobile_app.databinding.FragmentHomeBinding;
import com.google.android.material.slider.Slider;
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
import okhttp3.ResponseBody;

public class HomeFragment extends Fragment {
    FragmentHomeBinding viewBinding;
    PreviewView previewView;
    Switch previewSwitch;
    ImageCapture imageCapture;
    private int maxCaptureCount = 3;
    ImageButton beginButton;
    private static final int REQUEST_MEDIA_PROJECTION = 100;
    private int resultCode;
    private Intent resultData;
    private boolean hasProjectionPermission = false;
    private Slider captureCountSlider;
    private Slider delaySlider;
    private TextView captureCountText;
    private TextView delayText;
    private int delayMillis = 3000;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        viewBinding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = viewBinding.getRoot();

        previewView = viewBinding.previewView;
        beginButton = viewBinding.beginButton;
        previewSwitch = viewBinding.previewSwitch;

        captureCountSlider = viewBinding.captureCountSlider;
        delaySlider = viewBinding.delaySlider;
        captureCountText = viewBinding.captureCountText;
        delayText = viewBinding.delayText;

        captureCountSlider.setValue(maxCaptureCount);
        delaySlider.setValue(delayMillis);

        setupSliderListeners();
        if (!isAuthenticated) {
            beginButton.setEnabled(false);
            showCompanyKeyDialog();
        } else
            initializeUIComponents();

        beginButton.setOnClickListener(this::onBeginButtonClick);
        requestMediaProjectionPermission();

        return root;
    }

    private void setupSliderListeners() {
        captureCountSlider.addOnChangeListener((slider, value, fromUser) -> {
            int oddValue = (int) value;
            if (oddValue % 2 == 0) {
                oddValue = Math.max(1, oddValue - 1);
                slider.setValue(oddValue);
            } else {
                maxCaptureCount = oddValue;
                captureCountText.setText("Capture Count: " + maxCaptureCount);
            }
        });

        // Delay slider
        delaySlider.addOnChangeListener((slider, value, fromUser) -> {
            delayMillis = (int) value;
            delayText.setText("Delay: " + delayMillis + "ms");
        });
    }

    private void requestMediaProjectionPermission() {
        MediaProjectionManager projectionManager = (MediaProjectionManager)
                requireContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
    }

    private void showCompanyKeyDialog() {
        Context context = new ContextThemeWrapper(requireContext(), R.style.CustomAlertDialogTheme);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.custom_alert_dialog, null);
        builder.setView(dialogView);

        EditText input = dialogView.findViewById(R.id.dialog_input);
        Button submitButton = dialogView.findViewById(R.id.dialog_submit);

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);

        submitButton.setOnClickListener(v -> {
            String companyKey = input.getText().toString().trim();
            if (companyKey.isEmpty()) {
                Toast.makeText(requireContext(), "Company key cannot be empty", Toast.LENGTH_SHORT).show();
            } else {
                dialog.dismiss();
                validateCompanyKey(companyKey);
            }
        });

        dialog.show();
    }

    private void validateCompanyKey(String companyKey) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            OkHttpClient client = new OkHttpClient();
            String url = currentUrl + "/api/companies/authenticate";
            String errorMessage = null;
            boolean isSuccess = false;

            JSONObject payload = new JSONObject();
            try {
                payload.put("companyKey", companyKey);
            } catch (JSONException e) {
                errorMessage = "Error creating JSON payload: " + e.getMessage();
            }

            if (errorMessage == null) {
                RequestBody requestBody = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
                Request request = new Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        try (ResponseBody responseBody = response.body()) {
                            String responseText = responseBody.string();
                            companyId = Long.parseLong(responseText.trim());
                            isSuccess = true;
                        }
                    } else {
                        errorMessage = "Invalid company key";
                    }
                } catch (IOException | NumberFormatException e) {
                    errorMessage = "Validation failed: " + e.getMessage();
                }
            }

            boolean finalIsSuccess = isSuccess;
            String finalErrorMessage = errorMessage;

            mainHandler.post(() -> {
                if (finalIsSuccess) {
                    Toast.makeText(requireContext(), "Company key validated successfully", Toast.LENGTH_SHORT).show();
                    isAuthenticated = true;
                    initializeUIComponents();
                } else {
                    Toast.makeText(requireContext(), finalErrorMessage, Toast.LENGTH_SHORT).show();
                    showCompanyKeyDialog();
                }
            });
        });
    }

    private void initializeUIComponents() {
        beginButton.setEnabled(true);
        previewSwitch.setChecked(false);
        previewSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                previewView.setForeground(new ColorDrawable(getColor(requireContext(), android.R.color.black)));
            } else {
                previewView.setForeground(null);
            }
        });

        startCamera();
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

        if (hasProjectionPermission && resultData != null) {
            Intent serviceIntent = new Intent(requireContext(), com.example.mobile_app.ui.foreground.ForegroundCameraService.class);
            serviceIntent.putExtra("resultCode", resultCode);
            serviceIntent.putExtra("resultData", resultData);
            serviceIntent.putExtra("maxCaptureCount", maxCaptureCount);
            serviceIntent.putExtra("delayMillis", delayMillis);
            ContextCompat.startForegroundService(requireContext(), serviceIntent);


            Toast.makeText(requireContext(),
                    "Starting capture: " + maxCaptureCount + " images with " + delayMillis + "ms delay",
                    Toast.LENGTH_SHORT).show();

            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            requireContext().startActivity(startMain);
        } else {
            requestMediaProjectionPermission();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_MEDIA_PROJECTION && resultCode == android.app.Activity.RESULT_OK && data != null) {
            this.resultCode = resultCode;
            this.resultData = data;
            hasProjectionPermission = true;
            Toast.makeText(requireContext(), "Screen capture permission granted", Toast.LENGTH_SHORT).show();
        } else if (requestCode == REQUEST_MEDIA_PROJECTION) {
            Toast.makeText(requireContext(), "Screen capture permission denied", Toast.LENGTH_SHORT).show();
            hasProjectionPermission = false;
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}