package com.example.smartalert.presentation.ui;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.smartalert.R;
import com.example.smartalert.presentation.viewmodels.UserStatisticsViewModel;

import java.util.Map;

public class UserStatisticsActivity extends AppCompatActivity {

    private UserStatisticsViewModel viewModel;
    private TextView totalIncidentsReported;
    private TextView statisticsByType;
    private TextView globalStatistics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_statistics);

        viewModel = new ViewModelProvider(this).get(UserStatisticsViewModel.class);
        initViews();
        setupObservers();

        // Load statistics
        viewModel.loadUserStatistics();
        viewModel.loadGlobalStatistics();
    }

    private void initViews() {
        totalIncidentsReported = findViewById(R.id.totalIncidentsReported);
        statisticsByType = findViewById(R.id.statisticsByType);
        globalStatistics = findViewById(R.id.globalStatistics);
    }

    private void setupObservers() {
        viewModel.getTotalIncidentsReported().observe(this, total -> {
            if (total != null) {
                totalIncidentsReported.setText("Total Incidents Reported: " + total);
            }
        });

        viewModel.getIncidentCountsByType().observe(this, counts -> {
            if (counts != null && !counts.isEmpty()) {
                StringBuilder sb = new StringBuilder("Your Incidents by Type:\n");
                for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                    sb.append("• ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
                statisticsByType.setText(sb.toString());
            } else {
                statisticsByType.setText("Your Incidents by Type:\nNo incidents reported yet");
            }
        });

        viewModel.getGlobalIncidentCounts().observe(this, globalCounts -> {
            if (globalCounts != null && !globalCounts.isEmpty()) {
                StringBuilder sb = new StringBuilder("Global Incidents by Type:\n");
                for (Map.Entry<String, Integer> entry : globalCounts.entrySet()) {
                    sb.append("• ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
                globalStatistics.setText(sb.toString());
            } else {
                globalStatistics.setText("Global Incidents by Type:\nNo incidents in system");
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}