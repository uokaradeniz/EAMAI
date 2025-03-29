package com.example.mobile_app.ui.reports;

import static com.example.mobile_app.ui.api.BackendApiConfig.URL_VIRTUAL;
import static com.example.mobile_app.ui.api.BackendApiConfig.currentUrl;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mobile_app.databinding.FragmentReportsBinding;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ReportsFragment extends Fragment {

    private FragmentReportsBinding binding;
    private ReportAdapter reportAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentReportsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        GetReports();
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void GetReports() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(currentUrl + "/api/reports")
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
                if (response.isSuccessful()) {
                    assert response.body() != null;
                    String responseData = response.body().string();
                    if (responseData.isEmpty()) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "Response body is empty", Toast.LENGTH_SHORT).show()
                        );
                    } else {
                        List<Report> reports = Report.mapJsonToReports(responseData);
                        requireActivity().runOnUiThread(() -> {
                            reportAdapter = new ReportAdapter(reports);
                            binding.reportRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
                            binding.reportRecyclerView.setAdapter(reportAdapter);
                            Toast.makeText(requireContext(), "Reports fetched successfully", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Failed to fetch reports", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }
}