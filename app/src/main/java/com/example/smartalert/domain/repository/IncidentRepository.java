package com.example.smartalert.domain.repository;

import com.example.smartalert.domain.model.Incident;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.QuerySnapshot;

public interface IncidentRepository {
    void submitIncident(Incident incident, OnCompleteListener<Void> listener);
    void getUserIncidents(String userId, OnCompleteListener<QuerySnapshot> listener);
    void getAllIncidents(OnCompleteListener<QuerySnapshot> listener);
}