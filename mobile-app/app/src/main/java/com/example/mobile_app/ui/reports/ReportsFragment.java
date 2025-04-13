package com.example.mobile_app.ui.reports;

import static com.example.mobile_app.ui.api.BackendApiConfig.currentUrl;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mobile_app.databinding.FragmentReportsBinding;

public class ReportsFragment extends Fragment {

    private FragmentReportsBinding binding;
    private ReportAdapter reportAdapter;
    private ReportsViewModel reportsViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentReportsBinding.inflate(inflater, container, false);
        reportsViewModel = new ViewModelProvider(this).get(ReportsViewModel.class);

        setupObservers();
        setupUI();

        if (reportsViewModel.getReports().getValue() == null) {
            reportsViewModel.fetchReports(currentUrl);
        }

        return binding.getRoot();
    }

    private void setupObservers() {
        reportsViewModel.getReports().observe(getViewLifecycleOwner(), reports -> {
            if (reports != null && !reports.isEmpty()) {
                reportAdapter = new ReportAdapter(reports);
                binding.reportRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
                binding.reportRecyclerView.setAdapter(reportAdapter);
                Toast.makeText(requireContext(), "Reports loaded successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "No reports found", Toast.LENGTH_SHORT).show();
            }
        });

        reportsViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null && isLoading) {
                Toast.makeText(requireContext(), "Loading reports...", Toast.LENGTH_SHORT).show();
            }
        });

        reportsViewModel.getDeleteSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null) {
                if (success) {
                    Toast.makeText(requireContext(), "Reports deleted successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Failed to delete reports", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupUI() {
        binding.deleteReportsButton.setOnClickListener(v -> {
            reportsViewModel.deleteAllReports();
        });

        binding.refreshButton.setOnClickListener(v -> {
            reportsViewModel.fetchReports(currentUrl);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}