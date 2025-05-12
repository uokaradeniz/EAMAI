package com.example.mobile_app.ui.reports;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_app.R;

import java.util.List;
import java.util.Map;

public class ImagePairAdapter extends RecyclerView.Adapter<ImagePairAdapter.ImagePairViewHolder> {
    private final List<Map<String, Report>> twinReports;
    private final ReportAdapter.OnReportClickListener listener;

    public ImagePairAdapter(List<Map<String, Report>> twinReports, ReportAdapter.OnReportClickListener listener) {
        this.twinReports = twinReports;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ImagePairViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_pair, parent, false);
        return new ImagePairViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImagePairViewHolder holder, int position) {
        Map<String, Report> reportPair = twinReports.get(position);
        holder.bind(reportPair, listener);
    }

    @Override
    public int getItemCount() {
        return twinReports.size();
    }

    static class ImagePairViewHolder extends RecyclerView.ViewHolder {
        private final ImageView photoImageView;
        private final ImageView screenshotImageView;
        private final TextView nameTextView;

        ImagePairViewHolder(@NonNull View itemView) {
            super(itemView);
            photoImageView = itemView.findViewById(R.id.photoImageView);
            screenshotImageView = itemView.findViewById(R.id.screenshotImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
        }

        void bind(Map<String, Report> reportPair, ReportAdapter.OnReportClickListener listener) {
            Report photoReport = reportPair.get("photo");
            Report screenshotReport = reportPair.get("screenshot");

            if (photoReport != null) {
                nameTextView.setText(photoReport.getName());

                if (photoReport.getImageData() != null) {
                    Bitmap photoBitmap = BitmapFactory.decodeByteArray(
                            photoReport.getImageData(), 0, photoReport.getImageData().length);
                    photoImageView.setImageBitmap(photoBitmap);
                }

                photoImageView.setOnClickListener(v -> listener.onReportClick(photoReport));
            }

            if (screenshotReport != null && screenshotReport.getImageData() != null) {
                Bitmap screenshotBitmap = BitmapFactory.decodeByteArray(
                        screenshotReport.getImageData(), 0, screenshotReport.getImageData().length);
                screenshotImageView.setImageBitmap(screenshotBitmap);

                screenshotImageView.setOnClickListener(v -> listener.onReportClick(screenshotReport));
            }
        }
    }
}