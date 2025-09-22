package com.example.smartalert.data.remote;

import com.example.smartalert.domain.model.Incident;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.QuerySnapshot;

public class IncidentRemoteDataSource {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void submitIncident(Incident incident, OnCompleteListener<Void> listener) {
        db.collection("incidents")
                .document(incident.getId())
                .set(incident, SetOptions.merge())
                .addOnCompleteListener(listener);
    }
    public void getUserIncidents(String userId, OnCompleteListener<QuerySnapshot> listener) {
        db.collection("incidents").whereEqualTo("userId", userId).get().addOnCompleteListener(listener);
    }

    public void getAllIncidents(OnCompleteListener<QuerySnapshot> listener) {
        db.collection("incidents").get().addOnCompleteListener(listener);
    }
}
