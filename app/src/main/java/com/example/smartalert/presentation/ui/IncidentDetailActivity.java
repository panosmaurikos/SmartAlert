package com.example.smartalert.presentation.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.smartalert.R;
import com.example.smartalert.domain.model.Incident;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class IncidentDetailActivity extends AppCompatActivity {
    private String incidentType;
    private boolean isAdmin = false;
    private FirebaseFirestore db;
    private ProgressBar progressBar;
    private List<Incident> allIncidents = new ArrayList<>();
    private List<ClusterData> serverClusters = new ArrayList<>();
    private RequestQueue requestQueue;

    // Data class για τα clusters από τον server
    private static class ClusterData {
        String clusterId;
        String type;
        List<Incident> incidents = new ArrayList<>();
        double centerLat;
        double centerLon;
        long firstReportTime;
        long lastReportTime;
        String mainLocation;
        double alarmLevel;
        String alarmLevelText;
        int uniqueUserCount;
        int totalReports;

        // Βοηθητικές μέθοδοι
        Date getFirstReportTimeAsDate() { return new Date(firstReportTime); }
        Date getLastReportTimeAsDate() { return new Date(lastReportTime); }
        String getAlarmLevelText() { return alarmLevelText; }
        int getUniqueUserCount() { return uniqueUserCount; }
        int getTotalReports() { return totalReports; }
        String getMainLocation() { return mainLocation; }
        double getAlarmLevel() { return alarmLevel; }
        List<Incident> getIncidents() { return incidents; }
        String getType() { return type; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incident_detail);

        db = FirebaseFirestore.getInstance();
        progressBar = findViewById(R.id.progressBar);
        requestQueue = Volley.newRequestQueue(this);

        incidentType = getIntent().getStringExtra("incidentType");

        // Back button listener
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            finish();
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getTypeDisplayName(incidentType));
        }

        checkAdminStatusAndLoad();
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
                        fetchClustersFromServer();
                    });
        } else {
            isAdmin = false;
            initViews();
            fetchClustersFromServer();
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
            incidentCountText.setText(getString(R.string.loading));
        }
        if (alarmLevel != null) {
            alarmLevel.setText(getString(R.string.alarm_level_calculating));
        }
    }

    private void fetchClustersFromServer() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        String serverUrl = "http://10.0.2.2:3000/getClustersByType";

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("incidentType", incidentType);
        } catch (JSONException e) {
            Log.e("IncidentDetailActivity", "Error creating JSON for server request", e);
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                serverUrl,
                jsonBody,
                response -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    try {
                        boolean success = response.getBoolean("success");
                        if (success) {
                            parseServerClusters(response);
                        } else {
                            String error = response.getString("error");
                            Toast.makeText(this, getString(R.string.server_error) + ": " + error, Toast.LENGTH_LONG).show();
                            displayIncidentClusters();
                        }
                    } catch (JSONException e) {
                        Log.e("IncidentDetailActivity", "Error parsing server response", e);
                        Toast.makeText(this, getString(R.string.data_parsing_error), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Log.e("IncidentDetailActivity", "Error fetching clusters from server", error);
                    Toast.makeText(this, getString(R.string.connection_error), Toast.LENGTH_LONG).show();
                    loadIncidentsWithoutClustering();
                }
        );

        requestQueue.add(request);
    }

    private void parseServerClusters(JSONObject response) throws JSONException {
        serverClusters.clear();
        allIncidents.clear();

        JSONArray clustersArray = response.getJSONArray("clusters");

        for (int i = 0; i < clustersArray.length(); i++) {
            JSONObject clusterJson = clustersArray.getJSONObject(i);

            // Parse incidents array
            List<Incident> clusterIncidents = new ArrayList<>();
            JSONArray incidentsArray = clusterJson.getJSONArray("incidents");
            for (int j = 0; j < incidentsArray.length(); j++) {
                JSONObject incidentJson = incidentsArray.getJSONObject(j);
                Incident incident = parseIncidentFromJson(incidentJson);
                clusterIncidents.add(incident);
                allIncidents.add(incident);
            }

            // Create cluster object
            ClusterData cluster = new ClusterData();
            cluster.clusterId = clusterJson.getString("clusterId");
            cluster.type = clusterJson.getString("type");
            cluster.incidents = clusterIncidents;
            cluster.centerLat = clusterJson.getDouble("centerLat");
            cluster.centerLon = clusterJson.getDouble("centerLon");
            cluster.firstReportTime = clusterJson.getLong("firstReportTime");
            cluster.lastReportTime = clusterJson.getLong("lastReportTime");
            cluster.mainLocation = clusterJson.getString("mainLocation");
            cluster.alarmLevel = clusterJson.getDouble("alarmLevel");
            cluster.alarmLevelText = clusterJson.getString("alarmLevelText");
            cluster.uniqueUserCount = clusterJson.getInt("uniqueUserCount");
            cluster.totalReports = clusterJson.getInt("totalReports");

            serverClusters.add(cluster);
        }

        updateClusterViews();
        displayIncidentClusters();
    }

    private Incident parseIncidentFromJson(JSONObject incidentJson) throws JSONException {
        Incident incident = new Incident();
        incident.setId(incidentJson.getString("id"));
        incident.setType(incidentJson.getString("type"));
        incident.setUserId(incidentJson.getString("userId"));
        incident.setLatitude(incidentJson.getDouble("latitude"));
        incident.setLongitude(incidentJson.getDouble("longitude"));
        incident.setLocation(incidentJson.getString("location"));

        long timestamp = incidentJson.getLong("timestamp");
        incident.setTimestamp(new Date(timestamp));

        incident.setComments(incidentJson.optString("comments", ""));
        return incident;
    }

    private void loadIncidentsWithoutClustering() {
        Query query = db.collection("incidents")
                .whereEqualTo("type", incidentType);

        query.get().addOnCompleteListener(task -> {
            if (progressBar != null) progressBar.setVisibility(View.GONE);

            allIncidents.clear();
            serverClusters.clear();

            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Incident incident = document.toObject(Incident.class);
                    incident.setId(document.getId());
                    allIncidents.add(incident);

                    // Create simple cluster for each incident (fallback)
                    ClusterData cluster = new ClusterData();
                    cluster.clusterId = document.getId();
                    cluster.type = incident.getType();
                    cluster.incidents = new ArrayList<>();
                    cluster.incidents.add(incident);
                    cluster.centerLat = incident.getLatitude();
                    cluster.centerLon = incident.getLongitude();
                    cluster.firstReportTime = incident.getTimestamp().getTime();
                    cluster.lastReportTime = incident.getTimestamp().getTime();
                    cluster.mainLocation = incident.getLocation();
                    cluster.alarmLevel = 10.0;
                    cluster.alarmLevelText = "ΧΑΜΗΛΟΣ";
                    cluster.uniqueUserCount = 1;
                    cluster.totalReports = 1;

                    serverClusters.add(cluster);
                }
                updateClusterViews();
                displayIncidentClusters();
            } else {
                Toast.makeText(this, getString(R.string.incident_loading_error), Toast.LENGTH_LONG).show();
                displayIncidentClusters();
            }
        });
    }

    private void updateClusterViews() {
        TextView incidentCountText = findViewById(R.id.incidentCount);
        TextView alarmLevel = findViewById(R.id.alarmLevel);

        int totalClusters = serverClusters.size();
        int totalIncidents = allIncidents.size();

        if (incidentCountText != null) {
            incidentCountText.setText(getString(R.string.total_alerts, totalClusters, totalIncidents));
        }

        if (alarmLevel != null && !serverClusters.isEmpty()) {
            double maxAlarmLevel = serverClusters.get(0).getAlarmLevel();
            String alarmText = serverClusters.get(0).getAlarmLevelText();
            alarmLevel.setText(getString(R.string.alarm_level, maxAlarmLevel, alarmText));
        }
    }

    private void displayIncidentClusters() {
        LinearLayout container = findViewById(R.id.incidentsContainer);
        container.removeAllViews();

        if (serverClusters.isEmpty()) {
            TextView noDataText = new TextView(this);
            noDataText.setText(getString(R.string.no_alerts_found, getTypeDisplayName(incidentType)));
            noDataText.setPadding(16, 16, 16, 16);
            noDataText.setTextSize(16);
            noDataText.setGravity(View.TEXT_ALIGNMENT_CENTER);
            noDataText.setTextColor(ContextCompat.getColor(this, R.color.on_background));
            container.addView(noDataText);
        } else {
            for (ClusterData cluster : serverClusters) {
                MaterialCardView card = createClusterCard(cluster);
                container.addView(card);
            }
        }
    }

    private MaterialCardView createClusterCard(ClusterData cluster) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);
        card.setRadius(12);
        card.setCardElevation(4);

        // Χρήση theme color για dark mode support
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.surface));

        LinearLayout innerLayout = new LinearLayout(this);
        innerLayout.setOrientation(LinearLayout.VERTICAL);
        innerLayout.setPadding(20, 20, 20, 20);
        card.addView(innerLayout);

        TextView clusterHeader = new TextView(this);
        clusterHeader.setText(getString(R.string.alert_header, cluster.getAlarmLevelText()));
        clusterHeader.setTextSize(18);
        clusterHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        clusterHeader.setTextColor(ContextCompat.getColor(this, R.color.on_surface));
        innerLayout.addView(clusterHeader);

        addClusterInfoRow(innerLayout, getString(R.string.severity_level),
                String.format(Locale.getDefault(), "%.1f/100", cluster.getAlarmLevel()));
        addClusterInfoRow(innerLayout, getString(R.string.unique_users),
                cluster.getUniqueUserCount() + " " + getString(R.string.people));
        addClusterInfoRow(innerLayout, getString(R.string.total_reports),
                cluster.getTotalReports() + " " + getString(R.string.reports));
        addClusterInfoRow(innerLayout, getString(R.string.main_location),
                cluster.getMainLocation());
        addClusterInfoRow(innerLayout, getString(R.string.time_frame),
                formatDate(cluster.getFirstReportTimeAsDate()) + " - " + formatDate(cluster.getLastReportTimeAsDate()));

        if (isAdmin) {
            View separator = new View(this);
            separator.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 2
            ));
            separator.setBackgroundColor(ContextCompat.getColor(this, R.color.on_surface));
            separator.setPadding(0, 16, 0, 16);
            innerLayout.addView(separator);

            LinearLayout buttonLayout = new LinearLayout(this);
            buttonLayout.setOrientation(LinearLayout.HORIZONTAL);

            Button approveBtn = new Button(this);
            approveBtn.setText(getString(R.string.approve_cluster));
            approveBtn.setOnClickListener(v -> approveCluster(cluster));

            Button rejectBtn = new Button(this);
            rejectBtn.setText(getString(R.string.reject_cluster));
            rejectBtn.setOnClickListener(v -> rejectCluster(cluster));

            Button notifyBtn = new Button(this);
            notifyBtn.setText(getString(R.string.notify_users));
            notifyBtn.setOnClickListener(v -> sendNotificationToClusterUsers(cluster));

            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
            );
            btnParams.setMargins(4, 0, 4, 0);
            approveBtn.setLayoutParams(btnParams);
            rejectBtn.setLayoutParams(btnParams);
            notifyBtn.setLayoutParams(btnParams);

            buttonLayout.addView(approveBtn);
            buttonLayout.addView(rejectBtn);
            buttonLayout.addView(notifyBtn);
            innerLayout.addView(buttonLayout);

            Button detailsBtn = new Button(this);
            detailsBtn.setText(getString(R.string.incident_details));
            detailsBtn.setOnClickListener(v -> showClusterDetails(cluster));
            LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            detailsParams.setMargins(4, 8, 4, 0);
            detailsBtn.setLayoutParams(detailsParams);
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
        labelView.setTextColor(ContextCompat.getColor(this, R.color.on_surface));
        labelView.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        ));

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextSize(14);
        valueView.setTextColor(ContextCompat.getColor(this, R.color.on_surface));
        valueView.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        ));

        rowLayout.addView(labelView);
        rowLayout.addView(valueView);
        parent.addView(rowLayout);
    }

    private void approveCluster(ClusterData cluster) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.approving_cluster));
        progressDialog.show();

        List<String> ids = new ArrayList<>();
        for (Incident incident : cluster.getIncidents()) {
            ids.add(incident.getId());
        }

        updateStatusForCluster(ids, "approved", progressDialog);
    }

    private void rejectCluster(ClusterData cluster) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.reject_alert_title))
                .setMessage(getString(R.string.reject_alert_message, cluster.getTotalReports()))
                .setPositiveButton(getString(R.string.reject), (dialog, which) -> {
                    List<String> ids = new ArrayList<>();
                    for (Incident incident : cluster.getIncidents()) {
                        ids.add(incident.getId());
                    }
                    updateStatusForCluster(ids, "rejected", null);
                })
                .setNegativeButton(getString(R.string.cancel), null)
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
                                    getString(R.string.completed_for_reports, total), Toast.LENGTH_SHORT).show();
                            fetchClustersFromServer();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (progressDialog != null) progressDialog.dismiss();
                        Toast.makeText(this, getString(R.string.update_error, e.getMessage()), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void sendNotificationToClusterUsers(ClusterData cluster) {
        String msg = getString(R.string.civil_protection_instructions) + " " +
                getTypeDisplayName(cluster.getType()) + " " +
                getString(R.string.in_area) + " " + cluster.getMainLocation() + ". " +
                getString(R.string.number_of_reports) + ": " + cluster.getTotalReports();

        Set<String> userIdsSet = new HashSet<>();
        for (Incident incident : cluster.getIncidents()) {
            userIdsSet.add(incident.getUserId());
        }
        List<String> userIds = new ArrayList<>(userIdsSet);

        if (userIds.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_users_found), Toast.LENGTH_SHORT).show();
            Log.d("IncidentDetailActivity", "No userIds found to retrieve tokens.");
            return;
        }

        Log.d("IncidentDetailActivity", getString(R.string.users_found, userIds.size()));
        sendAlertToServer(userIds, getString(R.string.alert_update), msg, cluster.getType());
    }

    private void sendAlertToServer(List<String> userIds, String title, String message, String incidentType) {
        String serverUrl = "http://10.0.2.2:3000/sendAlert";

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("title", title);
            jsonBody.put("message", message);
            jsonBody.put("incidentType", incidentType);
            jsonBody.put("userIds", new JSONArray(userIds));
        } catch (JSONException e) {
            Log.e("IncidentDetailActivity", "Error creating JSON body for server request", e);
            Toast.makeText(this, getString(R.string.request_preparation_error), Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, serverUrl, jsonBody,
                response -> {
                    Log.d("IncidentDetailActivity", "Server response: " + response.toString());
                    try {
                        boolean success = response.getBoolean("success");
                        if (success) {
                            Toast.makeText(this, getString(R.string.notification_sent), Toast.LENGTH_LONG).show();
                        } else {
                            String errorMsg = response.getString("error");
                            Toast.makeText(this, getString(R.string.server_failure, errorMsg), Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        Log.e("IncidentDetailActivity", "Error parsing server response", e);
                        Toast.makeText(this, getString(R.string.unknown_server_error), Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    Log.e("IncidentDetailActivity", "Error sending to server: ", error);
                    if (error.networkResponse != null) {
                        Log.e("IncidentDetailActivity", "HTTP Status Code: " + error.networkResponse.statusCode);
                    }
                    Toast.makeText(this, getString(R.string.notification_send_error, error.getMessage()), Toast.LENGTH_LONG).show();
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };
        requestQueue.add(request);
    }

    private void showClusterDetails(ClusterData cluster) {
        StringBuilder details = new StringBuilder();
        details.append(getString(R.string.alert_details)).append(":\n\n");
        for (int i = 0; i < cluster.getIncidents().size(); i++) {
            Incident incident = cluster.getIncidents().get(i);
            details.append(i + 1).append(". ").append(getString(R.string.user)).append(": ").append(incident.getUserId())
                    .append("\n   ").append(getString(R.string.location)).append(": ").append(incident.getLocation())
                    .append("\n   ").append(getString(R.string.time)).append(": ").append(formatDate(incident.getTimestamp()))
                    .append("\n   ").append(getString(R.string.comments)).append(": ").append(incident.getComments())
                    .append("\n\n");
        }
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.incident_details) + " " + cluster.getTotalReports() + " " + getString(R.string.reports))
                .setMessage(details.toString())
                .setPositiveButton(getString(R.string.close), null)
                .show();
    }

    private String formatDate(Date date) {
        if (date == null) return getString(R.string.unknown);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(date);
    }

    private String getTypeDisplayName(String typeKey) {
        if (typeKey == null) return getString(R.string.unknown_type);
        switch (typeKey) {
            case "fire": return getString(R.string.fire);
            case "flood": return getString(R.string.flood);
            case "earthquake": return getString(R.string.earthquake);
            case "storm": return getString(R.string.storm);
            case "other": return getString(R.string.other);
            default: return typeKey;
        }
    }
}