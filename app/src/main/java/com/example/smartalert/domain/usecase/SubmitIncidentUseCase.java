package com.example.smartalert.domain.usecase;

import com.example.smartalert.domain.model.Incident;
import com.example.smartalert.domain.repository.IncidentRepository;
import com.google.android.gms.tasks.OnCompleteListener;

public class SubmitIncidentUseCase {
    private IncidentRepository repository;

    public SubmitIncidentUseCase(IncidentRepository repository) {
        this.repository = repository;
    }

    public void execute(Incident incident, OnCompleteListener<Void> listener) {
        repository.submitIncident(incident, listener);
    }
}