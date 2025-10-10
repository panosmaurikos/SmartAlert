package com.example.smartalert.data.repository;

import com.example.smartalert.domain.model.Incident;
import com.example.smartalert.domain.model.Statistics;
import com.example.smartalert.domain.repository.StatisticsRepository;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
/**
 * Provides basic statistical analysis
 * Focuses on efficient data grouping and filtering
 */
public class StatisticsRepositoryImpl implements StatisticsRepository {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    /**
     * Retrieves global statistics with date filtering and incident type filtering
     * Performs basic grouping and counting operations
     */
    @Override
    public void getGlobalStatistics(Date startDate, Date endDate, String incidentType, StatisticsCallback callback) {
        Query query = db.collection("incidents");
        // Apply date range filters
        if (startDate != null) {
            query = query.whereGreaterThanOrEqualTo("timestamp", convertDateToTimestamp(startDate));
        }
        // Adjust end date to include the entire day
        if (endDate != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(endDate);
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            Date endOfDay = calendar.getTime();
            query = query.whereLessThanOrEqualTo("timestamp", convertDateToTimestamp(endOfDay));
        }
        // Execute query and process results

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Statistics statistics = processIncidents(task, incidentType);
                // Get total user count for comprehensive statistics

                db.collection("users").get().addOnCompleteListener(userTask -> {
                    if (userTask.isSuccessful()) {
                        statistics.setTotalUsers(userTask.getResult().size());
                        callback.onStatisticsLoaded(statistics);
                    } else {
                        callback.onError("Failed to get users count"); // errors
                    }
                });
            } else {
                callback.onError("Failed to load incidents: " + task.getException().getMessage());
            }
        });
    }
    /**
     * Processes incident data
     * Performs filtering based on incident type
     */
    private Statistics processIncidents(com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> task, String requestedType) {
        Statistics statistics = new Statistics();
        Map<String, List<Incident>> incidentsByType = new HashMap<>();
        Map<String, Integer> incidentsByTypeCount = new HashMap<>();
        Map<String, Integer> incidentsByLocation = new HashMap<>();
        Map<String, Integer> incidentsByDate = new HashMap<>();
        int filteredIncidents = 0;
        // Filter incidents based on type

        List<Incident> allIncidents = new ArrayList<>();
        for (QueryDocumentSnapshot document : task.getResult()) {
            Incident incident = document.toObject(Incident.class);

            if (shouldIncludeIncident(incident, requestedType)) {
                allIncidents.add(incident);
                filteredIncidents++;
            }
        }
        // Categorize incidents into various groups

        for (Incident incident : allIncidents) {
            String typeKey = incident.getType();
            // Count by type

            incidentsByTypeCount.put(typeKey, incidentsByTypeCount.getOrDefault(typeKey, 0) + 1);
            // Group by type (full objects)

            if (!incidentsByType.containsKey(typeKey)) {
                incidentsByType.put(typeKey, new ArrayList<>());
            }
            incidentsByType.get(typeKey).add(incident);
            // Count by location

            String location = incident.getLocation();
            incidentsByLocation.put(location, incidentsByLocation.getOrDefault(location, 0) + 1);
            // Count by date

            String dateKey = formatDate(incident.getTimestamp());
            incidentsByDate.put(dateKey, incidentsByDate.getOrDefault(dateKey, 0) + 1);
        }

        statistics.setAlarmLevels(new HashMap<>());
        // Set all calculated statistics

        statistics.setTotalIncidents(filteredIncidents);
        statistics.setIncidentsByTypeCount(incidentsByTypeCount);
        statistics.setIncidentsByType(incidentsByType);
        statistics.setIncidentsByLocation(incidentsByLocation);
        statistics.setIncidentsByDate(incidentsByDate);

        return statistics;
    }
    /**
     * Determines if an incident should be included based on type filter
     * Supports both English and Greek filter values
     */
    private boolean shouldIncludeIncident(Incident incident, String requestedType) {
        if (requestedType == null || requestedType.equals("All") || requestedType.equals("Όλα")) {
            return true;
        }
        return incident.getType().equals(requestedType);
    }
    /**
     * Converts Java Date to Firebase Timestamp
     */
    private Timestamp convertDateToTimestamp(Date date) {
        return new Timestamp(date);
    }
    /**
     * Formats date to Greek standard
     */
    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Europe/Athens"));
        return sdf.format(date);
    }
}