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
import java.util.stream.Collectors;

public class StatisticsRepositoryImpl implements StatisticsRepository {

    private static final String TAG = "StatisticsRepository";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Map για backward compatibility (Ελληνικά -> Αγγλικά)
    private Map<String, String> greekToEnglishMap;

    public StatisticsRepositoryImpl() {
        initGreekToEnglishMap();
    }

    private void initGreekToEnglishMap() {
        greekToEnglishMap = new HashMap<>();
        greekToEnglishMap.put("Πυρκαγιά", "fire");
        greekToEnglishMap.put("Πλημμύρα", "flood");
        greekToEnglishMap.put("Σεισμός", "earthquake");
        greekToEnglishMap.put("Καταιγίδα", "storm");
        greekToEnglishMap.put("Άλλο", "other");
    }

    @Override
    public void getGlobalStatistics(Date startDate, Date endDate, String incidentType, StatisticsCallback callback) {
        Query query = db.collection("incidents");

        if (startDate != null) {
            query = query.whereGreaterThanOrEqualTo("timestamp", convertDateToTimestamp(startDate));
        }
        if (endDate != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(endDate);
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            Date endOfDay = calendar.getTime();
            query = query.whereLessThanOrEqualTo("timestamp", convertDateToTimestamp(endOfDay));
        }

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Statistics statistics = processIncidents(task, incidentType);
                db.collection("users").get().addOnCompleteListener(userTask -> {
                    if (userTask.isSuccessful()) {
                        statistics.setTotalUsers(userTask.getResult().size());
                        callback.onStatisticsLoaded(statistics);
                    } else {
                        callback.onError("Failed to get users count");
                    }
                });
            } else {
                callback.onError("Failed to load incidents: " + task.getException().getMessage());
            }
        });
    }

    private Statistics processIncidents(com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> task, String requestedType) {
        Statistics statistics = new Statistics();
        Map<String, List<Incident>> incidentsByType = new HashMap<>();
        Map<String, Integer> incidentsByTypeCount = new HashMap<>();
        Map<String, Integer> incidentsByLocation = new HashMap<>();
        Map<String, Integer> incidentsByDate = new HashMap<>();
        int filteredIncidents = 0;

        List<Incident> allIncidents = new ArrayList<>();
        for (QueryDocumentSnapshot document : task.getResult()) {
            Incident incident = document.toObject(Incident.class);
            if (shouldIncludeIncident(incident, requestedType)) {
                allIncidents.add(incident);
                filteredIncidents++;
            }
        }

        // Ομαδοποίηση ανά τύπο
        for (Incident incident : allIncidents) {
            String typeKey = convertToEnglishType(incident.getType());

            incidentsByTypeCount.put(typeKey, incidentsByTypeCount.getOrDefault(typeKey, 0) + 1);

            if (!incidentsByType.containsKey(typeKey)) {
                incidentsByType.put(typeKey, new ArrayList<>());
            }
            incidentsByType.get(typeKey).add(incident);

            String location = incident.getLocation();
            incidentsByLocation.put(location, incidentsByLocation.getOrDefault(location, 0) + 1);

            String dateKey = formatDate(incident.getTimestamp());
            incidentsByDate.put(dateKey, incidentsByDate.getOrDefault(dateKey, 0) + 1);
        }

        // Υπολογισμός Alarm Levels
        Map<String, Double> alarmLevels = calculateAlarmLevels(allIncidents);
        statistics.setAlarmLevels(alarmLevels);

        statistics.setTotalIncidents(filteredIncidents);
        statistics.setIncidentsByTypeCount(incidentsByTypeCount);
        statistics.setIncidentsByType(incidentsByType);
        statistics.setIncidentsByLocation(incidentsByLocation);
        statistics.setIncidentsByDate(incidentsByDate);
        return statistics;
    }

    private Map<String, Double> calculateAlarmLevels(List<Incident> incidents) {
        Map<String, Double> alarmLevels = new HashMap<>();
        Map<String, List<Incident>> incidentsByLocation = new HashMap<>();

        // Ομαδοποίηση ανά τοποθεσία
        for (Incident incident : incidents) {
            String location = incident.getLocation();
            incidentsByLocation.computeIfAbsent(location, k -> new ArrayList<>()).add(incident);
        }

        // Υπολογισμός βάρους για κάθε τοποθεσία
        for (String location : incidentsByLocation.keySet()) {
            List<Incident> list = incidentsByLocation.get(location);
            double weight = calculateWeightForLocation(list);
            alarmLevels.put(location, weight);
        }

        return alarmLevels;
    }

    private double calculateWeightForLocation(List<Incident> incidents) {
        if (incidents.isEmpty()) return 0.0;

        // Κριτήριο 1: Πλήθος μοναδικών χρηστών
        long userCount = incidents.stream()
                .map(Incident::getUserId)
                .distinct()
                .count();

        // Κριτήριο 2: Γεωγραφική συνοχή (απόσταση)
        double distanceWeight = 1.0;
        if (incidents.size() > 1) {
            double totalDistance = 0.0;
            int pairs = 0;
            for (int i = 0; i < incidents.size(); i++) {
                for (int j = i + 1; j < incidents.size(); j++) {
                    double dist = calculateDistance(
                            incidents.get(i).getLatitude(), incidents.get(i).getLongitude(),
                            incidents.get(j).getLatitude(), incidents.get(j).getLongitude()
                    );
                    totalDistance += dist;
                    pairs++;
                }
            }
            if (pairs > 0) {
                double avgDistance = totalDistance / pairs;
                // Αν η μέση απόσταση > 200km, μειώνουμε το βάρος
                if (avgDistance > 200_000) {
                    distanceWeight = 0.1; // ή άλλη τιμή (π.χ. 0.0)
                }
            }
        }

        return userCount * distanceWeight;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Earth’s radius in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private boolean shouldIncludeIncident(Incident incident, String requestedType) {
        if (requestedType == null || requestedType.equals("All")) {
            return true;
        }
        String incidentType = convertToEnglishType(incident.getType());
        return incidentType.equals(requestedType);
    }

    private String convertToEnglishType(String type) {
        return greekToEnglishMap.getOrDefault(type, type);
    }

    private Timestamp convertDateToTimestamp(Date date) {
        return new Timestamp(date);
    }

    private String formatDate(Date date) {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date);
    }
}