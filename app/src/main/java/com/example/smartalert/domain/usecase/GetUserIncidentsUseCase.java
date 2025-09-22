package com.example.smartalert.domain.usecase;

import com.example.smartalert.domain.repository.IncidentRepository;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.QuerySnapshot;

public class GetUserIncidentsUseCase {
    private IncidentRepository repository;

    public GetUserIncidentsUseCase(IncidentRepository repository) {
        this.repository = repository;
    }

    public void execute(String userId, OnCompleteListener<QuerySnapshot> listener) {
        repository.getUserIncidents(userId, listener);
    }
}