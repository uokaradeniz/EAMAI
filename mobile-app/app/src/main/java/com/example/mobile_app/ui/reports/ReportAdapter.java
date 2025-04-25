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

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {
    public interface OnReportClickListener {
        void onReportClick(Report report);
    }

    private final List<Report> reportList;
    private final OnReportClickListener listener;

    public ReportAdapter(List<Report> reportList, OnReportClickListener listener) {
        this.reportList = reportList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        Report report = reportList.get(position);
        holder.nameTextView.setText("Image Name: " + report.getName());
        holder.sessionIdTextView.setText("Session ID: " + report.getSessionId());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReportClick(report);
            }
        });

        byte[] imageBytes = report.getImageData();
        if (imageBytes != null && imageBytes.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 110, 94, true);
            holder.imageView.setImageBitmap(scaledBitmap);
        } else {
            holder.imageView.setImageResource(R.drawable.ic_launcher_foreground);
        }
    }

    public void updateReports(List<Report> newReports) {
        reportList.clear();
        reportList.addAll(newReports);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView sessionIdTextView;
        ImageView imageView;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.report_name);
            sessionIdTextView = itemView.findViewById(R.id.report_session_id);
            imageView = itemView.findViewById(R.id.report_image);
        }
    }
}