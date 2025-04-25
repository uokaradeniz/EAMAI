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

    private final List<Report> reportList;

    public ReportAdapter(List<Report> reportList) {
        this.reportList = reportList;
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
        holder.analysisTextView.setText("Analysis: " + report.getAnalysis());
        holder.sessionIdTextView.setText("Session ID: " + report.getSessionId());

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
        TextView analysisTextView;
        TextView sessionIdTextView;
        ImageView imageView;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.report_name);
            analysisTextView = itemView.findViewById(R.id.report_analysis);
            sessionIdTextView = itemView.findViewById(R.id.report_session_id);
            imageView = itemView.findViewById(R.id.report_image);
        }
    }
}