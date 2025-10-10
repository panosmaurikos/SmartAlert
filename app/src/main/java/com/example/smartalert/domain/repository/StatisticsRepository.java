package com.example.smartalert.domain.repository;

import com.example.smartalert.domain.model.Statistics;
import java.util.Date;

public interface StatisticsRepository { // Repository interface for statistical data analysis
// Retrieves comprehensive global statistics with filtering options
    void getGlobalStatistics(Date startDate, Date endDate, String incidentType, StatisticsCallback callback);
//  Callback interface for handling statistics loading results
    interface StatisticsCallback {
        void onStatisticsLoaded(Statistics statistics); // Called when statistics are successfully loaded
        // Called when an error occurs during statistics loading
        void onError(String error);
    }
}