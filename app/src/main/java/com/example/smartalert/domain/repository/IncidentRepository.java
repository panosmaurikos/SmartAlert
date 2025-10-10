package com.example.smartalert.domain.repository;

import com.example.smartalert.domain.model.Incident;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.QuerySnapshot;
// Repository interface for incident management operations
public interface IncidentRepository {
    void submitIncident(Incident incident, OnCompleteListener<Void> listener); // Submits a new incident to the system
    void getUserIncidents(String userId, OnCompleteListener<QuerySnapshot> listener); //  Retrieves all incidents reported by a specific user
    void getAllIncidents(OnCompleteListener<QuerySnapshot> listener); // Retrieves all incidents from the database
}