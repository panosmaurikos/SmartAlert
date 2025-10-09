//package com.example.smartalert.domain.model;
//
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.List;
//import java.util.HashSet;
//import java.util.Set;
//
//public class IncidentCluster {
//    private String clusterId;
//    private String type;
//    private List<Incident> incidents = new ArrayList<>();
//    private double centerLat;
//    private double centerLon;
//    private Date firstReportTime;
//    private Date lastReportTime;
//
//    public IncidentCluster(Incident incident) {
//        this.type = incident.getType();
//        this.firstReportTime = incident.getTimestamp();
//        this.lastReportTime = incident.getTimestamp();
//        this.centerLat = incident.getLatitude();
//        this.centerLon = incident.getLongitude();
//        addIncident(incident);
//    }
//
//    public void addIncident(Incident incident) {
//        incidents.add(incident);
//        if (incident.getTimestamp().before(firstReportTime)) firstReportTime = incident.getTimestamp();
//        if (incident.getTimestamp().after(lastReportTime)) lastReportTime = incident.getTimestamp();
//        recalculateCenter();
//    }
//
//    private void recalculateCenter() {
//        double latSum = 0, lonSum = 0;
//        for (Incident i : incidents) {
//            latSum += i.getLatitude();
//            lonSum += i.getLongitude();
//        }
//        centerLat = latSum / incidents.size();
//        centerLon = lonSum / incidents.size();
//    }
//
//    public int getUniqueUserCount() {
//        Set<String> users = new HashSet<>();
//        for (Incident i : incidents) {
//            users.add(i.getUserId());
//        }
//        return users.size();
//    }
//
//    public int getTotalReports() { return incidents.size(); }
//
//    public double getAlarmLevel() {
//        // Πιο απλός αλγόριθμος: 70% πλήθος χρηστών, 30% γεωγρ. συνοχή
//        double userScore = Math.min(getUniqueUserCount() * 25, 70); // max 70
//        double geoScore = getGeographicScore(); // max 30
//        return Math.min(userScore + geoScore, 100);
//    }
//
//    private double getGeographicScore() {
//        if (incidents.size() < 2) return 30;
//        double maxDist = 0;
//        for (Incident i1 : incidents) {
//            for (Incident i2 : incidents) {
//                double d = haversine(i1.getLatitude(), i1.getLongitude(), i2.getLatitude(), i2.getLongitude());
//                if (d > maxDist) maxDist = d;
//            }
//        }
//        if (maxDist < 1) return 30;
//        if (maxDist < 5) return 20;
//        if (maxDist < 20) return 10;
//        return 0;
//    }
//
//    private double haversine(double lat1, double lon1, double lat2, double lon2) {
//        final double R = 6371; // km
//        double dLat = Math.toRadians(lat2-lat1);
//        double dLon = Math.toRadians(lon2-lon1);
//        double a = Math.sin(dLat/2)*Math.sin(dLat/2) +
//                Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.sin(dLon/2)*Math.sin(dLon/2);
//        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
//        return R * c;
//    }
//
//    public String getAlarmLevelText() {
//        double lvl = getAlarmLevel();
//        if (lvl >= 80) return "ΠΟΛΥ ΥΨΗΛΟΣ";
//        if (lvl >= 60) return "ΥΨΗΛΟΣ";
//        if (lvl >= 40) return "ΜΕΤΡΙΟΣ";
//        return "ΧΑΜΗΛΟΣ";
//    }
//
//    public String getMainLocation() {
//        if (!incidents.isEmpty()) return incidents.get(0).getLocation();
//        return "";
//    }
//
//    // Getters & Setters
//    public String getType() { return type; }
//    public List<Incident> getIncidents() { return incidents; }
//    public double getCenterLat() { return centerLat; }
//    public double getCenterLon() { return centerLon; }
//    public Date getFirstReportTime() { return firstReportTime; }
//    public Date getLastReportTime() { return lastReportTime; }
//    public void setClusterId(String id) { this.clusterId = id; }
//    public String getClusterId() { return clusterId; }
//}