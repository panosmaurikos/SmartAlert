package com.example.smartalert.domain.usecase;

import com.example.smartalert.domain.model.Statistics;
import com.example.smartalert.domain.repository.StatisticsRepository;
import java.util.Date;

public class GetStatisticsUseCase {
    private StatisticsRepository repository;

    public GetStatisticsUseCase(StatisticsRepository repository) {
        this.repository = repository;
    }

    public void execute(Date startDate, Date endDate, String incidentType,
                        StatisticsRepository.StatisticsCallback callback) {
        repository.getGlobalStatistics(startDate, endDate, incidentType, callback);
    }
}