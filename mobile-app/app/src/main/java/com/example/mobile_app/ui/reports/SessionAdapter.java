package com.example.mobile_app.ui.reports;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_app.R;

import java.util.List;
import java.util.Map;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.SessionViewHolder> {

    public interface OnReportClickListener {
        void onReportClick(Report report);
    }

    private final Map<String, List<Report>> sessionReportsMap;
    private final OnReportClickListener listener;

    public SessionAdapter(Map<String, List<Report>> sessionReportsMap, OnReportClickListener listener) {
        this.sessionReportsMap = sessionReportsMap;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_session, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        String sessionId = (String) sessionReportsMap.keySet().toArray()[position];
        List<Report> reports = sessionReportsMap.get(sessionId);
        String sessionDetail = reports.get(0).getSessionDetails();

        holder.sessionIdTextView.setText("Session ID: " + sessionId);
        holder.sessionDetailTextView.setText("Summary: " + sessionDetail);

        ReportAdapter reportAdapter = new ReportAdapter(reports, report -> listener.onReportClick(report));
        holder.reportsRecyclerView.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
        holder.reportsRecyclerView.setAdapter(reportAdapter);
    }
    @Override
    public int getItemCount() {
        return sessionReportsMap.size();
    }

    static class SessionViewHolder extends RecyclerView.ViewHolder {
        TextView sessionIdTextView;
        RecyclerView reportsRecyclerView;

        TextView sessionDetailTextView;

        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            sessionIdTextView = itemView.findViewById(R.id.session_id_text);
            reportsRecyclerView = itemView.findViewById(R.id.reports_recycler_view);
            sessionDetailTextView = itemView.findViewById(R.id.session_detail_text);
        }
    }
}