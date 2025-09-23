package com.example.smartalert.domain.repository;

import com.example.smartalert.domain.model.Statistics;
import java.util.Date;

public interface StatisticsRepository {
    void getGlobalStatistics(Date startDate, Date endDate, String incidentType, StatisticsCallback callback);

    interface StatisticsCallback {
        void onStatisticsLoaded(Statistics statistics);
        void onError(String error);
    }
}