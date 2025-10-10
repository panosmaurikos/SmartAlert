package com.example.smartalert.presentation.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.smartalert.domain.model.Incident;
import com.example.smartalert.domain.model.Statistics;
import com.example.smartalert.domain.repository.StatisticsRepository;
import com.example.smartalert.domain.usecase.GetStatisticsUseCase;
import com.example.smartalert.data.repository.StatisticsRepositoryImpl;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class StatisticsViewModel extends ViewModel {
    // Use case for handling statistics operations
    private GetStatisticsUseCase getStatisticsUseCase;

    // LiveData objects for different statistics
    private MutableLiveData<Integer> totalIncidents = new MutableLiveData<>();
    private MutableLiveData<Integer> totalUsers = new MutableLiveData<>();
    private MutableLiveData<Map<String, Integer>> incidentsByTypeCount = new MutableLiveData<>();
    private MutableLiveData<Map<String, List<Incident>>> incidentsByType = new MutableLiveData<>();
    private MutableLiveData<Map<String, Integer>> incidentsByLocation = new MutableLiveData<>();
    private MutableLiveData<Map<String, Integer>> incidentsByDate = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private MutableLiveData<Map<String, Double>> alarmLevels = new MutableLiveData<>();

    public StatisticsViewModel() {
        // Initialize repository and use case
        StatisticsRepository repository = new StatisticsRepositoryImpl();
        getStatisticsUseCase = new GetStatisticsUseCase(repository);
    }

    // LiveData getters for statistics
    public LiveData<Integer> getTotalIncidents() { return totalIncidents; }
    public LiveData<Integer> getTotalUsers() { return totalUsers; } // <-- ADD THIS METHOD
    public LiveData<Map<String, Integer>> getIncidentsByTypeCount() { return incidentsByTypeCount; }
    public LiveData<Map<String, List<Incident>>> getIncidentsByType() { return incidentsByType; }
    public LiveData<Map<String, Integer>> getIncidentsByLocation() { return incidentsByLocation; }
    public LiveData<Map<String, Integer>> getIncidentsByDate() { return incidentsByDate; }
    public LiveData<Map<String, Double>> getAlarmLevels() { return alarmLevels; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    // Load statistics method with filtering options
    public void loadStatistics(Date startDate, Date endDate, String incidentType) {
        isLoading.setValue(true);

        getStatisticsUseCase.execute(startDate, endDate, incidentType,
                new StatisticsRepository.StatisticsCallback() {
                    @Override // Called when statistics are successfully loaded
                    public void onStatisticsLoaded(Statistics statistics) {
                        isLoading.setValue(false);
                        // Update all LiveData objects with new statistics
                        alarmLevels.setValue(statistics.getAlarmLevels());
                        totalIncidents.setValue(statistics.getTotalIncidents());
                        totalUsers.setValue(statistics.getTotalUsers());
                        incidentsByTypeCount.setValue(statistics.getIncidentsByTypeCount());
                        incidentsByType.setValue(statistics.getIncidentsByType());
                        incidentsByLocation.setValue(statistics.getIncidentsByLocation());
                        incidentsByDate.setValue(statistics.getIncidentsByDate());
                    }
                    // Called when there's an error loading statistics
                    @Override
                    public void onError(String error) {
                        isLoading.setValue(false);
                        errorMessage.setValue(error);
                    }
                });
    }
}