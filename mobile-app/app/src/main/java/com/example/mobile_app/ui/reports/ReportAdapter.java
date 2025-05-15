package com.example.mobile_app.ui.reports;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_app.R;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {
    public interface OnReportClickListener {
        void onReportClick(Report report);
    }

    private final List<Report> reportList;
    private final OnReportClickListener listener;
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);


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
        String[] dateArray = Objects.requireNonNull(report.getName().split("_")[1].split("\\.")[0].split("-"));
        String date = dateArray[0] + "-" + dateArray[1] + "-" + dateArray[2] + " " + dateArray[3] + ":" + dateArray[4] + ":" + dateArray[5];
        holder.nameTextView.setText("Date Taken: " + date);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReportClick(report);
            }
        });

        if (report.getImageData() != null && report.getImageData().length > 0) {
            executorService.execute(() -> {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4;
                options.inPreferredConfig = Bitmap.Config.RGB_565;

                final Bitmap bitmap = BitmapFactory.decodeByteArray(
                        report.getImageData(),
                        0,
                        report.getImageData().length,
                        options
                );

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (holder.getAdapterPosition() == position) {
                        holder.imageView.setImageBitmap(bitmap);
                    }
                });
            });
        } else {
            holder.imageView.setImageResource(R.drawable.ic_launcher_foreground);
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        executorService.shutdown();
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
        ImageView imageView;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.report_name);
            imageView = itemView.findViewById(R.id.report_image);
        }
    }


}