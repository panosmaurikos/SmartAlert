package com.example.smartalert.presentation.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartalert.R;
import com.example.smartalert.domain.model.Incident;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class IncidentDetailActivity extends AppCompatActivity {

    private static final String PREFS = "app_prefs";
    private static final String PREF_ROLE_PREFIX = "role_";

    private String incidentType; // english key like "fire"
    private int incidentCount;
    private boolean isAdmin = false;
    private FirebaseFirestore db;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incident_detail); // υποθέτω υπάρχει

        db = FirebaseFirestore.getInstance();
        progressBar = findViewById(R.id.progressBar);

        incidentType = getIntent().getStringExtra("incidentType");
        incidentCount = getIntent().getIntExtra("incidentCount", 0);

        checkAdminStatusFromCache();
        initHeader();
        loadIncidentsForType();
    }

    private void checkAdminStatusFromCache() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            isAdmin = false;
            return;
        }
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String cached = prefs.getString(PREF_ROLE_PREFIX + uid, null);
        isAdmin = "admin".equals(cached);
    }

    private void initHeader() {
        TextView typeTitle = findViewById(R.id.incidentTypeTitle);
        TextView incidentCountText = findViewById(R.id.incidentCount);
        TextView alarmLevel = findViewById(R.id.alarmLevel);

        if (typeTitle != null) {
            typeTitle.setText(getTypeDisplayName(incidentType));
        }
        if (incidentCountText != null) {
            incidentCountText.setText("Σύνολο: " + incidentCount + " περιστατικά");
        }
        if (alarmLevel != null) {
            alarmLevel.setText("Επίπεδο Συναγερμού: " + calculateAlarmLevel(incidentCount));
        }
    }

    private void loadIncidentsForType() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        String greekType = convertEnglishToGreekType(incidentType);

        Query query = db.collection("incidents")
                .whereEqualTo("type", greekType);

        query.get().addOnCompleteListener(task -> {
            if (progressBar != null) progressBar.setVisibility(View.GONE);

            if (task.isSuccessful()) {
                List<Incident> incidents = new ArrayList<>();
                for (com.google.firebase.firestore.QueryDocumentSnapshot document : task.getResult()) {
                    Incident incident = document.toObject(Incident.class);
                    // σημαντικό: set id από document.getId()
                    incident.setId(document.getId());
                    incidents.add(incident);
                }

                // ταξινόμηση κατά timestamp desc (αν χρησιμοποιείς Timestamp)
                Collections.sort(incidents, new Comparator<Incident>() {
                    @Override
                    public int compare(Incident o1, Incident o2) {
                        Timestamp t1 = o1.getTimestamp();
                        Timestamp t2 = o2.getTimestamp();
                        if (t1 == null && t2 == null) return 0;
                        if (t1 == null) return 1;
                        if (t2 == null) return -1;
                        return Long.compare(t2.toDate().getTime(), t1.toDate().getTime());
                    }
                });

                displayIncidents(incidents);
            } else {
                Toast.makeText(this, "Σφάλμα φόρτωσης περιστατικών", Toast.LENGTH_LONG).show();
                displayIncidents(new ArrayList<>());
            }
        });
    }

    private void displayIncidents(List<Incident> incidents) {
        LinearLayout container = findViewById(R.id.incidentsContainer);

        if (container != null) {
            container.removeAllViews();

            if (incidents.isEmpty()) {
                TextView noDataText = new TextView(this);
                noDataText.setText("Δεν βρέθηκαν περιστατικά για αυτόν τον τύπο.");
                noDataText.setPadding(16, 16, 16, 16);
                noDataText.setTextSize(14);
                container.addView(noDataText);
            } else {
                for (Incident incident : incidents) {
                    MaterialCardView card = createIncidentCard(incident);
                    container.addView(card);
                }
            }
        }
    }

    private MaterialCardView createIncidentCard(Incident incident) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);
        card.setRadius(8);
        card.setCardElevation(2);

        LinearLayout innerLayout = new LinearLayout(this);
        innerLayout.setOrientation(LinearLayout.VERTICAL);
        innerLayout.setPadding(16, 16, 16, 16);
        card.addView(innerLayout);

        // Location
        TextView locationText = new TextView(this);
        locationText.setText("📍 " + (incident.getLocation() != null ? incident.getLocation() : "Άγνωστη τοποθεσία"));
        locationText.setTextSize(16);
        locationText.setTypeface(null, android.graphics.Typeface.BOLD);
        innerLayout.addView(locationText);

        // Timestamp
        TextView timestampText = new TextView(this);
        timestampText.setText("🕒 " + formatDate(incident.getTimestamp()));
        timestampText.setTextSize(14);
        innerLayout.addView(timestampText);

        // Status
        if (incident.getStatus() != null) {
            TextView statusText = new TextView(this);
            statusText.setText("📊 Κατάσταση: " + incident.getStatus());
            statusText.setTextSize(14);
            innerLayout.addView(statusText);
        }

        // Comments
        if (incident.getComments() != null && !incident.getComments().isEmpty()) {
            TextView commentsText = new TextView(this);
            commentsText.setText("💬 " + incident.getComments());
            commentsText.setTextSize(14);
            commentsText.setPadding(0, 8, 0, 0);
            innerLayout.addView(commentsText);
        }

        // Photo hint (only for admin)
        if (isAdmin && incident.getPhotoUrl() != null && !incident.getPhotoUrl().isEmpty()) {
            TextView photoText = new TextView(this);
            photoText.setText("📷 Διαθέσιμη φωτογραφία");
            photoText.setTextSize(12);
            photoText.setPadding(0, 8, 0, 0);
            innerLayout.addView(photoText);
        }

        // Only admins can open full detail
        if (isAdmin) {
            card.setClickable(true);
            card.setOnClickListener(v -> {
                Intent intent = new Intent(IncidentDetailActivity.this, IncidentFullDetailActivity.class);
                intent.putExtra("incidentId", incident.getId());
                startActivity(intent);
            });
        } else {
            card.setClickable(false);
        }

        return card;
    }

    private String formatDate(Timestamp ts) {
        if (ts == null) return "Άγνωστη ημερομηνία";
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        return sdf.format(ts.toDate());
    }

    private String getTypeDisplayName(String typeKey) {
        if (typeKey == null) return "Άγνωστος τύπος";
        switch (typeKey) {
            case "fire": return "Πυρκαγιά";
            case "flood": return "Πλημμύρα";
            case "earthquake": return "Σεισμός";
            case "storm": return "Καταιγίδα";
            case "other": return "Άλλο";
            default: return typeKey;
        }
    }

    private String convertEnglishToGreekType(String englishType) {
        if (englishType == null) return null;
        switch (englishType) {
            case "fire": return "Πυρκαγιά";
            case "flood": return "Πλημμύρα";
            case "earthquake": return "Σεισμός";
            case "storm": return "Καταιγίδα";
            case "other": return "Άλλο";
            default: return englishType;
        }
    }

    private String calculateAlarmLevel(int count) {
        if (count >= 10) return "Πολύ Υψηλό";
        if (count >= 5) return "Υψηλό";
        if (count >= 3) return "Μέτριο";
        return "Χαμηλό";
    }
}
