package com.example.mobile_app.ui.reports;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.mobile_app.R;

import java.util.Objects;

public class ReportDetailFragment extends Fragment {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (requireActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity) requireActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == android.R.id.home) {
                    NavController navController = Navigation.findNavController(requireView());
                    navController.navigateUp();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report_detail, container, false);

        TextView titleText = view.findViewById(R.id.detail_report_title);
        TextView nameText = view.findViewById(R.id.detail_report_name);
        TextView analysisText = view.findViewById(R.id.detail_report_analysis);
        TextView sessionIdText = view.findViewById(R.id.detail_report_session_id);
        TextView twinIdText = view.findViewById(R.id.detail_report_twin_id);
        ImageView imageView = view.findViewById(R.id.detail_report_image);

        Bundle args = getArguments();
        if (args != null) {
            // Get the report type
            String type = args.getString("type", "photo");
            String reportName = args.getString("name", "");
            String twinId = args.getString("twin_id", "Unknown");

            // Format date from filename
            String date = "Unknown date";
            if (reportName.contains("_")) {
                try {
                    String[] parts = reportName.split("_");
                    if (parts.length > 1) {
                        String[] dateArray = parts[1].split("\\.")[0].split("-");
                        if (dateArray.length >= 5) {
                            date = dateArray[0] + "-" + dateArray[1] + "-" + dateArray[2] + " " +
                                    dateArray[3] + ":" + dateArray[4];
                        }
                    }
                } catch (Exception e) {
                    // Use default date if parsing fails
                }
            }

            // Set title based on type
            String titlePrefix = type.equals("screenshot") ? "Screenshot" : "Photo";
            titleText.setText(titlePrefix + " Details");

            nameText.setText("Date: " + date);
            analysisText.setText("Analysis: " + args.getString("analysis", "No analysis available"));
            sessionIdText.setText("Session ID: " + args.getString("session_id", "Unknown"));
            twinIdText.setText("Twin ID: " + twinId);

            // Display image
            byte[] imageBytes = args.getByteArray("image");
            if (imageBytes != null && imageBytes.length > 0) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(R.drawable.ic_launcher_foreground);
            }
        }
        return view;
    }
}