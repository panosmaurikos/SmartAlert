package com.example.smartalert.domain.usecase;

import com.example.smartalert.domain.model.Statistics;
import com.example.smartalert.domain.repository.StatisticsRepository;
import java.util.Date;
// Use Case for retrieving statistical data
public class GetStatisticsUseCase {
    // Dependency injection
    private StatisticsRepository repository;
    // Constructor
    public GetStatisticsUseCase(StatisticsRepository repository) {
        this.repository = repository;
    }
    // Executes the statistical data retrieval process
    public void execute(Date startDate, Date endDate, String incidentType,
                        StatisticsRepository.StatisticsCallback callback) {
        repository.getGlobalStatistics(startDate, endDate, incidentType, callback);
    }
}