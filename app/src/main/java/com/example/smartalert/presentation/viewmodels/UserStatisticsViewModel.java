package com.example.smartalert.presentation.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.smartalert.domain.model.Incident;
import com.example.smartalert.domain.repository.IncidentRepository;
import com.example.smartalert.domain.usecase.GetAllIncidentsUseCase;
import com.example.smartalert.domain.usecase.GetUserIncidentsUseCase;
import com.example.smartalert.data.repository.IncidentRepositoryImpl;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserStatisticsViewModel extends ViewModel {

    private MutableLiveData<Integer> totalIncidentsReported = new MutableLiveData<>();
    private MutableLiveData<Map<String, Integer>> incidentCountsByType = new MutableLiveData<>();
    private MutableLiveData<Map<String, Integer>> globalIncidentCounts = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private GetUserIncidentsUseCase getUserIncidentsUseCase;
    private GetAllIncidentsUseCase getAllIncidentsUseCase;

    public UserStatisticsViewModel() {
        IncidentRepository repository = new IncidentRepositoryImpl();
        getUserIncidentsUseCase = new GetUserIncidentsUseCase(repository);
        getAllIncidentsUseCase = new GetAllIncidentsUseCase(repository);
    }

    public LiveData<Integer> getTotalIncidentsReported() {
        return totalIncidentsReported;
    }

    public LiveData<Map<String, Integer>> getIncidentCountsByType() {
        return incidentCountsByType;
    }

    public LiveData<Map<String, Integer>> getGlobalIncidentCounts() {
        return globalIncidentCounts;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadUserStatistics() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        getUserIncidentsUseCase.execute(userId, task -> {
            if (task.isSuccessful()) {
                List<Incident> incidents = task.getResult().toObjects(Incident.class);
                totalIncidentsReported.setValue(incidents.size());

                Map<String, Integer> countsByType = new HashMap<>();
                for (Incident incident : incidents) {
                    String type = incident.getType();
                    countsByType.put(type, countsByType.getOrDefault(type, 0) + 1);
                }
                incidentCountsByType.setValue(countsByType);
            } else {
                totalIncidentsReported.setValue(0);
                incidentCountsByType.setValue(new HashMap<>());
                errorMessage.setValue("Failed to load user incidents: " +
                        (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
            }
        });
    }

    public void loadGlobalStatistics() {
        getAllIncidentsUseCase.execute(task -> {
            if (task.isSuccessful()) {
                List<Incident> allIncidents = task.getResult().toObjects(Incident.class);
                Map<String, Integer> globalCounts = new HashMap<>();
                for (Incident incident : allIncidents) {
                    String type = incident.getType();
                    globalCounts.put(type, globalCounts.getOrDefault(type, 0) + 1);
                }
                globalIncidentCounts.setValue(globalCounts);
            } else {
                globalIncidentCounts.setValue(new HashMap<>());
                errorMessage.setValue("Failed to load global incidents: " +
                        (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
            }
        });
    }
}