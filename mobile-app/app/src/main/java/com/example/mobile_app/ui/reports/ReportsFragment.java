package com.example.mobile_app.ui.reports;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mobile_app.R;
import com.example.mobile_app.databinding.FragmentReportsBinding;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportsFragment extends Fragment {

    private FragmentReportsBinding binding;
    private ReportsViewModel reportsViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentReportsBinding.inflate(inflater, container, false);
        reportsViewModel = new ViewModelProvider(this).get(ReportsViewModel.class);

        setupObservers();
        setupUI();

        if (reportsViewModel.getReports().getValue() == null) {
            showLoading(true);
            reportsViewModel.fetchReports();
        }

        return binding.getRoot();
    }

    private void setupObservers() {
        reportsViewModel.getReports().observe(getViewLifecycleOwner(), reports -> {
            showLoading(false);

            if (reports != null && !reports.isEmpty()) {
                binding.reportRecyclerView.setVisibility(View.VISIBLE);
                binding.noReportsText.setVisibility(View.GONE);

                Map<String, List<Report>> groupedReports = reports.stream()
                        .collect(Collectors.groupingBy(report -> report.getSessionId().toString()));

                SessionAdapter sessionAdapter = new SessionAdapter(groupedReports, report -> {
                    Bundle bundle = new Bundle();
                    bundle.putString("name", report.getName());
                    bundle.putString("analysis", report.getAnalysis());
                    bundle.putByteArray("image", report.getImageData());
                    bundle.putString("session_id", report.getSessionId().toString());
                    bundle.putString("type", report.getType());
                    bundle.putString("twin_id", report.getTwinId());

                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                    navController.navigate(R.id.action_reports_to_reportDetail, bundle);
                });

                binding.reportRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
                binding.reportRecyclerView.setAdapter(sessionAdapter);
            } else {
                binding.reportRecyclerView.setVisibility(View.GONE);
                binding.noReportsText.setVisibility(View.VISIBLE);
                binding.noReportsText.setText("No reports found");
            }
        });

        // Observe loading state if you have one in your ViewModel
        reportsViewModel.getIsLoading().observe(getViewLifecycleOwner(), this::showLoading);
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.reportRecyclerView.setVisibility(View.GONE);
        } else {
            binding.progressBar.setVisibility(View.GONE);
        }
    }

    private void setupUI() {
        binding.deleteReportsButton.setOnClickListener(v -> {
            showLoading(true);
            reportsViewModel.deleteAllReports();
        });

        binding.refreshButton.setOnClickListener(v -> {
            showLoading(true);
            reportsViewModel.fetchReports();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}