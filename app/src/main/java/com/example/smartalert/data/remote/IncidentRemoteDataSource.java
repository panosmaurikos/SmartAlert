package com.example.smartalert.data.remote;

import com.example.smartalert.domain.model.Incident;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.QuerySnapshot;
/**
 * Remote Data Source for incident-related operations
 * Handles all communication with Firestore for incident data management
 */
public class IncidentRemoteDataSource {
    // Firestore database instance for incident data storage and retrieval
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    /**
     * Submits a new incident to the Firestore database
     *  save Incident object containing incident details
     */
    public void submitIncident(Incident incident, OnCompleteListener<Void> listener) {
        db.collection("incidents")
                .document(incident.getId())
                .set(incident, SetOptions.merge())
                .addOnCompleteListener(listener);
    }
    /**
     * Retrieves all incidents reported by a specific user
     *  userId The unique identifier of the user
     *  listener call for handling the query result
     */
    public void getUserIncidents(String userId, OnCompleteListener<QuerySnapshot> listener) {
        db.collection("incidents").whereEqualTo("userId", userId).get().addOnCompleteListener(listener);
    }
    /**
     * Retrieves all incidents from the database
     */
    public void getAllIncidents(OnCompleteListener<QuerySnapshot> listener) {
        db.collection("incidents").get().addOnCompleteListener(listener);
    }
}
