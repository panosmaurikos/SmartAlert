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

    private GetStatisticsUseCase getStatisticsUseCase;

    private MutableLiveData<Integer> totalIncidents = new MutableLiveData<>();
    private MutableLiveData<Integer> totalUsers = new MutableLiveData<>();
    private MutableLiveData<Map<String, Integer>> incidentsByTypeCount = new MutableLiveData<>(); // <-- ΝΕΟ ΟΝΟΜΑ
    private MutableLiveData<Map<String, List<Incident>>> incidentsByType = new MutableLiveData<>(); // <-- ΝΕΟ
    private MutableLiveData<Map<String, Integer>> incidentsByLocation = new MutableLiveData<>();
    private MutableLiveData<Map<String, Integer>> incidentsByDate = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    public StatisticsViewModel() {
        StatisticsRepository repository = new StatisticsRepositoryImpl();
        getStatisticsUseCase = new GetStatisticsUseCase(repository);
    }

    public LiveData<Integer> getTotalIncidents() { return totalIncidents; }
    public LiveData<Map<String, Integer>> getIncidentsByTypeCount() { return incidentsByTypeCount; }
    public LiveData<Map<String, List<Incident>>> getIncidentsByType() { return incidentsByType; }
    public LiveData<Map<String, Integer>> getIncidentsByLocation() { return incidentsByLocation; }
    public LiveData<Map<String, Integer>> getIncidentsByDate() { return incidentsByDate; }
    private MutableLiveData<Map<String, Double>> alarmLevels = new MutableLiveData<>();

    public LiveData<Map<String, Double>> getAlarmLevels() { return alarmLevels; }

    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public void loadStatistics(Date startDate, Date endDate, String incidentType) {
        isLoading.setValue(true);

        getStatisticsUseCase.execute(startDate, endDate, incidentType,
                new StatisticsRepository.StatisticsCallback() {
                    @Override
                    public void onStatisticsLoaded(Statistics statistics) {
                        isLoading.setValue(false);
                        alarmLevels.setValue(statistics.getAlarmLevels());
                        totalIncidents.setValue(statistics.getTotalIncidents());
                        incidentsByTypeCount.setValue(statistics.getIncidentsByTypeCount());
                        incidentsByType.setValue(statistics.getIncidentsByType());
                        incidentsByLocation.setValue(statistics.getIncidentsByLocation());
                        incidentsByDate.setValue(statistics.getIncidentsByDate());
                    }

                    @Override
                    public void onError(String error) {
                        isLoading.setValue(false);
                        errorMessage.setValue(error);
                    }
                });
    }
}