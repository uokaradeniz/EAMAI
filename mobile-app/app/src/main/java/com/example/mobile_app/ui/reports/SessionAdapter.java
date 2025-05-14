package com.example.mobile_app.ui.reports;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_app.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.SessionViewHolder> {
    private final List<String> sessionIds;
    private final Map<String, List<Report>> groupedReports;
    private final ReportAdapter.OnReportClickListener listener;

    public SessionAdapter(Map<String, List<Report>> groupedReports, ReportAdapter.OnReportClickListener listener) {
        this.groupedReports = groupedReports;
        this.sessionIds = new ArrayList<>(groupedReports.keySet());
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
        String sessionId = sessionIds.get(position);
        List<Report> sessionReports = groupedReports.get(sessionId);
        String sessionSummary = sessionReports.get(position).getSessionDetails();

        Map<String, Map<String, Report>> twinIdReports = new HashMap<>();
        for (Report report : sessionReports) {
            String twinId = report.getTwinId();
            if (!twinIdReports.containsKey(twinId)) {
                twinIdReports.put(twinId, new HashMap<>());
            }
            twinIdReports.get(twinId).put(report.getType(), report);
        }

        holder.bind(sessionId, sessionSummary,new ArrayList<>(twinIdReports.values()), listener);
    }

    @Override
    public int getItemCount() {
        return sessionIds.size();
    }

    static class SessionViewHolder extends RecyclerView.ViewHolder {
        private final TextView sessionIdText;
        private final TextView sessionSummaryText;

        private final RecyclerView reportRecyclerView;

        SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            sessionIdText = itemView.findViewById(R.id.sessionIdText);
            sessionSummaryText = itemView.findViewById(R.id.sessionSummaryText);
            reportRecyclerView = itemView.findViewById(R.id.reportRecyclerView);
        }

        void bind(String sessionId, String sessionSummary,List<Map<String, Report>> twinReports, ReportAdapter.OnReportClickListener listener) {
            sessionIdText.setText("Session: " + sessionId);
            sessionSummaryText.setText("Summary: " + sessionSummary);
            ImagePairAdapter adapter = new ImagePairAdapter(twinReports, listener);
            reportRecyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            reportRecyclerView.setAdapter(adapter);
        }
    }
}