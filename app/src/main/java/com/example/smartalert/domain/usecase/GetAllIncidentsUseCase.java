package com.example.smartalert.domain.usecase;


import com.example.smartalert.domain.repository.IncidentRepository;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.QuerySnapshot;

public class GetAllIncidentsUseCase {
    private IncidentRepository repository;

    public GetAllIncidentsUseCase(IncidentRepository repository) {
        this.repository = repository;
    }

    public void execute(OnCompleteListener<QuerySnapshot> listener) {
        repository.getAllIncidents(listener);
    }
}