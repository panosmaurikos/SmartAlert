// presentation/viewmodel/IncidentViewModel.java
package com.example.smartalert.presentation.viewmodels;

import static androidx.lifecycle.AndroidViewModel_androidKt.getApplication;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.app.Application;
import android.location.Geocoder;
import android.location.Address;


import com.example.smartalert.domain.model.Incident;
import com.example.smartalert.domain.repository.IncidentRepository;
import com.example.smartalert.domain.usecase.SubmitIncidentUseCase;
import com.example.smartalert.data.repository.IncidentRepositoryImpl;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseAuth;

import java.io.IOException;
import java.util.List;

import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class IncidentViewModel extends AndroidViewModel {
    private static final String TAG = "IncidentViewModel";

    private SubmitIncidentUseCase submitIncidentUseCase;
    private MutableLiveData<Boolean> submitResult = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private Geocoder geocoder;

    public IncidentViewModel(Application application) {
        super(application);
        IncidentRepository repository = new IncidentRepositoryImpl();
        submitIncidentUseCase = new SubmitIncidentUseCase(repository);
        this.geocoder = new Geocoder(application.getApplicationContext(), Locale.getDefault());
    }
    // LiveData getters for submit result
    public LiveData<Boolean> getSubmitResult() {
        return submitResult;
    }
    // LiveData getters for error messages
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    // Submit incident method with location name
    public void submitIncident(String type, String comments, double latitude, double longitude, String photoUrl) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String incidentId = UUID.randomUUID().toString();

        Incident incident = new Incident(userId, type, comments, latitude, longitude, new Date(), photoUrl);
        incident.setId(incidentId);

        // Calculate location name with Geocoder
        String locationName = getLocationName(latitude, longitude);
        incident.setLocation(locationName);

        // Log the incident details
        System.out.println("Submitting incident: " + incidentId);
        System.out.println("Type: " + type);
        System.out.println("Location: " + latitude + ", " + longitude);
        System.out.println("Location Name: " + locationName);
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
    // Helper method to get location name from coordinates
    private String getLocationName(double latitude, double longitude) {
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                // Constructing the location name
                StringBuilder sb = new StringBuilder();
                if (address.getLocality() != null) {
                    sb.append(address.getLocality()).append(", ");
                }
                if (address.getAdminArea() != null) {
                    sb.append(address.getAdminArea()).append(", ");
                }
                if (address.getCountryName() != null) {
                    sb.append(address.getCountryName());
                }
                return sb.toString().trim();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // If it fails, we return the coordinates as a fallback
        return String.format(Locale.getDefault(), "%.2f, %.2f", latitude, longitude);
    }
}

