package com.example.smartalert.domain.usecase;

import com.example.smartalert.domain.model.Incident;
import com.example.smartalert.domain.repository.IncidentRepository;
import com.google.android.gms.tasks.OnCompleteListener;

// Use Case for submitting new incidents to the system
public class SubmitIncidentUseCase {
    private IncidentRepository repository; // call the repository
    // to save the incident to the database
   // Constructor with dependency injection
    public SubmitIncidentUseCase(IncidentRepository repository) {
        this.repository = repository;
    }
        // Executes the incident submission process
    public void execute(Incident incident, OnCompleteListener<Void> listener) {
        repository.submitIncident(incident, listener);
    }
}