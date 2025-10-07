package com.example.smartalert.presentation.ui;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.app.AlertDialog;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.smartalert.R;
import com.example.smartalert.domain.model.Incident;
import com.example.smartalert.domain.model.IncidentCluster;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.text.SimpleDateFormat;
import java.util.*;

public class IncidentDetailActivity extends AppCompatActivity {
    private String incidentType;
    private boolean isAdmin = false;
    private FirebaseFirestore db;
    private ProgressBar progressBar;
    private List<Incident> allIncidents = new ArrayList<>();
    private List<IncidentCluster> incidentClusters = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incident_detail);

        db = FirebaseFirestore.getInstance();
        progressBar = findViewById(R.id.progressBar);

        incidentType = getIntent().getStringExtra("incidentType");

        checkAdminStatusAndLoad();
    }

    private void checkAdminStatusAndLoad() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        String role = doc.getString("role");
                        isAdmin = "employee".equals(role) || "admin".equals(role);
                        initViews();
                        loadIncidentsForType();
                    });
        } else {
            isAdmin = false;
            initViews();
            loadIncidentsForType();
        }
    }

    private void initViews() {
        TextView typeTitle = findViewById(R.id.incidentTypeTitle);
        TextView incidentCountText = findViewById(R.id.incidentCount);
        TextView alarmLevel = findViewById(R.id.alarmLevel);

        if (typeTitle != null) {
            typeTitle.setText(getTypeDisplayName(incidentType));
        }
        if (incidentCountText != null) {
            incidentCountText.setText("Φόρτωση...");
        }
        if (alarmLevel != null) {
            alarmLevel.setText("Επίπεδο Συναγερμού: Υπολογισμός...");
        }
    }

    private void loadIncidentsForType() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        Query query = db.collection("incidents")
                .whereEqualTo("type", incidentType);

        query.get().addOnCompleteListener(task -> {
            if (progressBar != null) progressBar.setVisibility(View.GONE);

            allIncidents.clear();
            incidentClusters.clear();

            if (task.isSuccessful()) {
                for (com.google.firebase.firestore.QueryDocumentSnapshot document : task.getResult()) {
                    Incident incident = document.toObject(Incident.class);
                    incident.setId(document.getId());
                    allIncidents.add(incident);
                }

                clusterIncidents();
                updateClusterViews();
                displayIncidentClusters();
            } else {
                Toast.makeText(this, "Σφάλμα φόρτωσης περιστατικών", Toast.LENGTH_LONG).show();
                displayIncidentClusters();
            }
        });
    }

    private void clusterIncidents() {
        final double CLUSTER_DISTANCE_KM = 10.0;
        final long TIME_WINDOW_MS = 2 * 60 * 60 * 1000; // 2 ώρες

        for (Incident incident : allIncidents) {
            boolean added = false;
            for (IncidentCluster cluster : incidentClusters) {
                if (isIncidentInCluster(incident, cluster, CLUSTER_DISTANCE_KM, TIME_WINDOW_MS)) {
                    cluster.addIncident(incident);
                    added = true;
                    break;
                }
            }
            if (!added) {
                IncidentCluster c = new IncidentCluster(incident);
                c.setClusterId(UUID.randomUUID().toString());
                incidentClusters.add(c);
            }
        }
        incidentClusters.sort((c1, c2) -> Double.compare(c2.getAlarmLevel(), c1.getAlarmLevel()));
    }

    private boolean isIncidentInCluster(Incident incident, IncidentCluster cluster,
                                        double maxDistKm, long maxTimeDiff) {
        if (!incident.getType().equals(cluster.getType())) return false;
        long timeDiff = Math.abs(incident.getTimestamp().getTime() - cluster.getFirstReportTime().getTime());
        if (timeDiff > maxTimeDiff) return false;
        double dist = haversine(incident.getLatitude(), incident.getLongitude(),
                cluster.getCenterLat(), cluster.getCenterLon());
        return dist <= maxDistKm;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371;
        double dLat = Math.toRadians(lat2-lat1);
        double dLon = Math.toRadians(lon2-lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.sin(dLon/2)*Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    private void updateClusterViews() {
        TextView incidentCountText = findViewById(R.id.incidentCount);
        TextView alarmLevel = findViewById(R.id.alarmLevel);

        int totalClusters = incidentClusters.size();
        int totalIncidents = allIncidents.size();

        if (incidentCountText != null) {
            incidentCountText.setText(String.format("Σύνολο: %d συναγερμοί, %d αναφορές", totalClusters, totalIncidents));
        }

        if (alarmLevel != null && !incidentClusters.isEmpty()) {
            double maxAlarmLevel = incidentClusters.get(0).getAlarmLevel();
            alarmLevel.setText(String.format("Επίπεδο Συναγερμού: %.1f/100 (%s)",
                    maxAlarmLevel, incidentClusters.get(0).getAlarmLevelText()));
        }
    }

    private void displayIncidentClusters() {
        LinearLayout container = findViewById(R.id.incidentsContainer);
        container.removeAllViews();

        if (incidentClusters.isEmpty()) {
            TextView noDataText = new TextView(this);
            noDataText.setText("Δεν βρέθηκαν συναγερμοί για τον τύπο: " + getTypeDisplayName(incidentType));
            noDataText.setPadding(16, 16, 16, 16);
            noDataText.setTextSize(16);
            noDataText.setGravity(View.TEXT_ALIGNMENT_CENTER);
            container.addView(noDataText);
        } else {
            for (IncidentCluster cluster : incidentClusters) {
                MaterialCardView card = createClusterCard(cluster);
                container.addView(card);
            }
        }
    }

    private MaterialCardView createClusterCard(IncidentCluster cluster) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);
        card.setRadius(12);
        card.setCardElevation(4);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.primaryColor));

        LinearLayout innerLayout = new LinearLayout(this);
        innerLayout.setOrientation(LinearLayout.VERTICAL);
        innerLayout.setPadding(20, 20, 20, 20);
        card.addView(innerLayout);

        TextView clusterHeader = new TextView(this);
        clusterHeader.setText("🚨 ΣΥΝΑΓΕΡΜΟΣ - " + cluster.getAlarmLevelText());
        clusterHeader.setTextSize(18);
        clusterHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        clusterHeader.setTextColor(ContextCompat.getColor(this, R.color.white));
        innerLayout.addView(clusterHeader);

        addClusterInfoRow(innerLayout, "📊 Επίπεδο Βαρύτητας:", String.format(Locale.getDefault(), "%.1f/100", cluster.getAlarmLevel()));
        addClusterInfoRow(innerLayout, "👥 Μοναδικοί Χρήστες:", cluster.getUniqueUserCount() + " άτομα");
        addClusterInfoRow(innerLayout, "📋 Συνολικές Αναφορές:", cluster.getTotalReports() + " αναφορές");
        addClusterInfoRow(innerLayout, "📍 Κύρια Τοποθεσία:", cluster.getMainLocation());
        addClusterInfoRow(innerLayout, "⏰ Χρονικό Πλαίσιο:", formatDate(cluster.getFirstReportTime()) + " - " + formatDate(cluster.getLastReportTime()));

        if (isAdmin) {
            View separator = new View(this);
            separator.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 2
            ));
            separator.setBackgroundColor(ContextCompat.getColor(this, R.color.white));
            separator.setPadding(0, 16, 0, 16);
            innerLayout.addView(separator);

            LinearLayout buttonLayout = new LinearLayout(this);
            buttonLayout.setOrientation(LinearLayout.HORIZONTAL);

            Button approveBtn = new Button(this);
            approveBtn.setText("✅ Έγκριση Ομάδας");
            approveBtn.setOnClickListener(v -> approveCluster(cluster));

            Button rejectBtn = new Button(this);
            rejectBtn.setText("❌ Απόρριψη Ομάδας");
            rejectBtn.setOnClickListener(v -> rejectCluster(cluster));

            Button notifyBtn = new Button(this);
            notifyBtn.setText("📢 Ειδοποίηση Χρηστών");
            notifyBtn.setOnClickListener(v -> sendNotificationToClusterUsers(cluster));

            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
            );
            approveBtn.setLayoutParams(btnParams);
            rejectBtn.setLayoutParams(btnParams);
            notifyBtn.setLayoutParams(btnParams);

            buttonLayout.addView(approveBtn);
            buttonLayout.addView(rejectBtn);
            buttonLayout.addView(notifyBtn);
            innerLayout.addView(buttonLayout);

            Button detailsBtn = new Button(this);
            detailsBtn.setText("📋 Λεπτομέρειες Αναφορών");
            detailsBtn.setOnClickListener(v -> showClusterDetails(cluster));
            innerLayout.addView(detailsBtn);
        }

        return card;
    }

    private void addClusterInfoRow(LinearLayout parent, String label, String value) {
        LinearLayout rowLayout = new LinearLayout(this);
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
        rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextSize(14);
        labelView.setTypeface(null, android.graphics.Typeface.BOLD);
        labelView.setTextColor(ContextCompat.getColor(this, R.color.white));
        labelView.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        ));

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextSize(14);
        valueView.setTextColor(ContextCompat.getColor(this, R.color.white));
        valueView.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        ));

        rowLayout.addView(labelView);
        rowLayout.addView(valueView);
        parent.addView(rowLayout);
    }

    private void approveCluster(IncidentCluster cluster) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Έγκριση ομάδας περιστατικών...");
        progressDialog.show();

        List<String> ids = new ArrayList<>();
        for (Incident incident : cluster.getIncidents()) {
            ids.add(incident.getId());
        }

        updateStatusForCluster(ids, "approved", progressDialog);
    }

    private void rejectCluster(IncidentCluster cluster) {
        new AlertDialog.Builder(this)
                .setTitle("Απόρριψη Συναγερμού")
                .setMessage("Θέλετε να απορρίψετε αυτόν τον συναγερμό και όλες τις " + cluster.getTotalReports() + " αναφορές;")
                .setPositiveButton("Απόρριψη", (dialog, which) -> {
                    List<String> ids = new ArrayList<>();
                    for (Incident incident : cluster.getIncidents()) {
                        ids.add(incident.getId());
                    }
                    updateStatusForCluster(ids, "rejected", null);
                })
                .setNegativeButton("Ακύρωση", null)
                .show();
    }

    private void updateStatusForCluster(List<String> ids, String newStatus, ProgressDialog progressDialog) {
        final int total = ids.size();
        final int[] completed = {0};
        for (String id : ids) {
            db.collection("incidents").document(id)
                    .update("status", newStatus, newStatus + "At", new Date())
                    .addOnSuccessListener(aVoid -> {
                        completed[0]++;
                        if (completed[0] == total) {
                            if (progressDialog != null) progressDialog.dismiss();
                            Toast.makeText(this,
                                    "Ολοκληρώθηκε για " + total + " αναφορές", Toast.LENGTH_SHORT).show();
                            loadIncidentsForType();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (progressDialog != null) progressDialog.dismiss();
                        Toast.makeText(this, "Σφάλμα ενημέρωσης: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void sendNotificationToClusterUsers(IncidentCluster cluster) {
        // Εδώ βάλε πραγματικό notification logic (cloud function call). Demo:
        String msg = "Οδηγίες Πολιτικής Προστασίας για " + getTypeDisplayName(cluster.getType()) +
                " στην περιοχή " + cluster.getMainLocation() + ". Αριθμός αναφορών: " + cluster.getTotalReports();
        Toast.makeText(this, "Notification: " + msg, Toast.LENGTH_LONG).show();

    }

    private void showClusterDetails(IncidentCluster cluster) {
        StringBuilder details = new StringBuilder();
        details.append("Λεπτομέρειες Συναγερμού:\n\n");
        for (int i = 0; i < cluster.getIncidents().size(); i++) {
            Incident incident = cluster.getIncidents().get(i);
            details.append(i + 1).append(". Χρήστης: ").append(incident.getUserId())
                    .append("\n   Τοποθεσία: ").append(incident.getLocation())
                    .append("\n   Χρόνος: ").append(formatDate(incident.getTimestamp()))
                    .append("\n   Σχόλια: ").append(incident.getComments())
                    .append("\n\n");
        }
        new AlertDialog.Builder(this)
                .setTitle("Λεπτομέρειες " + cluster.getTotalReports() + " Αναφορών")
                .setMessage(details.toString())
                .setPositiveButton("Κλείσιμο", null)
                .show();
    }

    private String formatDate(Date date) {
        if (date == null) return "Άγνωστο";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(date);
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
}