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

public class StatisticsRepositoryImpl implements StatisticsRepository {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

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
        Map<String, List<Incident>> incidentsByCluster = clusterIncidentsByProximity(incidents);

        for (Map.Entry<String, List<Incident>> entry : incidentsByCluster.entrySet()) {
            List<Incident> cluster = entry.getValue();
            double alarmLevel = calculateClusterAlarmLevel(cluster);
            alarmLevels.put(entry.getKey(), alarmLevel);
        }

        return alarmLevels;
    }

    private Map<String, List<Incident>> clusterIncidentsByProximity(List<Incident> incidents) {
        Map<String, List<Incident>> clusters = new HashMap<>();
        double CLUSTER_DISTANCE_KM = 50.0;

        for (Incident incident : incidents) {
            boolean addedToExistingCluster = false;

            for (String clusterKey : clusters.keySet()) {
                Incident clusterCenter = parseClusterKey(clusterKey);
                double distance = calculateDistance(
                        clusterCenter.getLatitude(), clusterCenter.getLongitude(),
                        incident.getLatitude(), incident.getLongitude()
                );

                if (distance <= CLUSTER_DISTANCE_KM * 1000) {
                    clusters.get(clusterKey).add(incident);
                    addedToExistingCluster = true;
                    break;
                }
            }

            if (!addedToExistingCluster) {
                String newClusterKey = createClusterKey(incident);
                List<Incident> newCluster = new ArrayList<>();
                newCluster.add(incident);
                clusters.put(newClusterKey, newCluster);
            }
        }

        return clusters;
    }

    private double calculateClusterAlarmLevel(List<Incident> cluster) {
        if (cluster.isEmpty()) return 0.0;

        long uniqueUsers = cluster.stream()
                .map(Incident::getUserId)
                .distinct()
                .count();
        double userScore = (uniqueUsers / (double) Math.max(cluster.size(), 1)) * 0.4;

        double timeDensityScore = calculateTimeDensityScore(cluster) * 0.3;

        double geographicCohesionScore = calculateGeographicCohesionScore(cluster) * 0.3;

        return (userScore + timeDensityScore + geographicCohesionScore) * 10;
    }

    private double calculateTimeDensityScore(List<Incident> cluster) {
        if (cluster.size() < 2) return 0.5;

        long timeRange = 24 * 60 * 60 * 1000;
        long firstTimestamp = cluster.stream()
                .mapToLong(inc -> inc.getTimestamp().getTime())
                .min().orElse(0);
        long lastTimestamp = cluster.stream()
                .mapToLong(inc -> inc.getTimestamp().getTime())
                .max().orElse(0);

        long actualRange = lastTimestamp - firstTimestamp;
        if (actualRange == 0) return 1.0;

        return Math.min(1.0, (double) timeRange / actualRange);
    }

    private double calculateGeographicCohesionScore(List<Incident> cluster) {
        if (cluster.size() < 2) return 0.5;

        double totalDistance = 0;
        int comparisons = 0;

        for (int i = 0; i < cluster.size(); i++) {
            for (int j = i + 1; j < cluster.size(); j++) {
                totalDistance += calculateDistance(
                        cluster.get(i).getLatitude(), cluster.get(i).getLongitude(),
                        cluster.get(j).getLatitude(), cluster.get(j).getLongitude()
                );
                comparisons++;
            }
        }

        double avgDistance = totalDistance / comparisons;
        return Math.max(0, 1.0 - (avgDistance / 200000.0));
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private String createClusterKey(Incident incident) {
        return incident.getLatitude() + "," + incident.getLongitude();
    }

    private Incident parseClusterKey(String clusterKey) {
        String[] parts = clusterKey.split(",");
        Incident incident = new Incident();
        incident.setLatitude(Double.parseDouble(parts[0]));
        incident.setLongitude(Double.parseDouble(parts[1]));
        return incident;
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
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Europe/Athens"));
        return sdf.format(date);
    }
}