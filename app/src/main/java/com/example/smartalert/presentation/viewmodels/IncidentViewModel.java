// presentation/viewmodel/IncidentViewModel.java
package com.example.smartalert.presentation.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.smartalert.domain.model.Incident;
import com.example.smartalert.domain.repository.IncidentRepository;
import com.example.smartalert.domain.usecase.SubmitIncidentUseCase;
import com.example.smartalert.data.repository.IncidentRepositoryImpl;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Date;
import java.util.UUID;

public class IncidentViewModel extends ViewModel {
    private static final String TAG = "IncidentViewModel";

    private SubmitIncidentUseCase submitIncidentUseCase;
    private MutableLiveData<Boolean> submitResult = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public IncidentViewModel() {
        IncidentRepository repository = new IncidentRepositoryImpl();
        submitIncidentUseCase = new SubmitIncidentUseCase(repository);
    }

    public LiveData<Boolean> getSubmitResult() {
        return submitResult;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void submitIncident(String type, String comments, double latitude, double longitude, String photoUrl) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String incidentId = UUID.randomUUID().toString();

        Incident incident = new Incident(userId, type, comments, latitude, longitude, new Date(), photoUrl);
        incident.setId(incidentId);

        // Log the incident details
        System.out.println("Submitting incident: " + incidentId);
        System.out.println("Type: " + type);
        System.out.println("Location: " + latitude + ", " + longitude);
        System.out.println("Photo URL: " + photoUrl);

        submitIncidentUseCase.execute(incident, task -> {
            if (task.isSuccessful()) {
                System.out.println("Incident submitted successfully");
                submitResult.setValue(true);
            } else {
                String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                System.out.println("Incident submission failed: " + error);
                errorMessage.setValue(error);
            }
        });
    }
}
