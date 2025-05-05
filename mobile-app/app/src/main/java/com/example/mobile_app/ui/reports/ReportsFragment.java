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
            reportsViewModel.fetchReports();
        }

        return binding.getRoot();
    }

    private void setupObservers() {
        reportsViewModel.getReports().observe(getViewLifecycleOwner(), reports -> {
            if (reports != null && !reports.isEmpty()) {
                Map<String, List<Report>> groupedReports = reports.stream()
                        .collect(Collectors.groupingBy(report -> report.getSessionId().toString()));

                SessionAdapter sessionAdapter = new SessionAdapter(groupedReports, report -> {
                    Bundle bundle = new Bundle();
                    bundle.putString("name", report.getName());
                    bundle.putString("analysis", report.getAnalysis());
                    bundle.putByteArray("image", report.getImageData());
                    bundle.putString("session_id", report.getSessionId().toString());

                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                    navController.navigate(R.id.action_reports_to_reportDetail, bundle);
                });

                binding.reportRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
                binding.reportRecyclerView.setAdapter(sessionAdapter);
            } else {
                Toast.makeText(requireContext(), "No reports found", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void setupUI() {
        binding.deleteReportsButton.setOnClickListener(v -> {
            reportsViewModel.deleteAllReports();

        });

        binding.refreshButton.setOnClickListener(v -> {
            reportsViewModel.fetchReports();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}