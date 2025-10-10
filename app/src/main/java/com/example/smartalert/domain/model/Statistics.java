package com.example.smartalert.domain.model;

import java.util.Date;
import java.util.List;
import java.util.Map;
//  statistics model for incident analysis and reporting
public class Statistics {
    private int totalIncidents;
    private int totalUsers;
    private Map<String, Integer> incidentsByTypeCount;
    private Map<String, List<Incident>> incidentsByType;
    private Map<String, Integer> incidentsByLocation;
    private Map<String, Integer> incidentsByDate;
    private Date startDate;
    private Date endDate;
    private Map<String, Double> alarmLevels;

     // Default constructor
    public Statistics() {}

    // Getters and Setters
    public int getTotalIncidents() { return totalIncidents; }
    public void setTotalIncidents(int totalIncidents) { this.totalIncidents = totalIncidents; }

    public int getTotalUsers() { return totalUsers; }
    public void setTotalUsers(int totalUsers) { this.totalUsers = totalUsers; }
    public Map<String, Double> getAlarmLevels() {
        return alarmLevels;
    }

    public void setAlarmLevels(Map<String, Double> alarmLevels) {
        this.alarmLevels = alarmLevels;
    }

    public Map<String, Integer> getIncidentsByLocation() { return incidentsByLocation; }
    public void setIncidentsByLocation(Map<String, Integer> incidentsByLocation) { this.incidentsByLocation = incidentsByLocation; }
    public Map<String, Integer> getIncidentsByTypeCount() { return incidentsByTypeCount; }
    public void setIncidentsByTypeCount(Map<String, Integer> incidentsByTypeCount) { this.incidentsByTypeCount = incidentsByTypeCount; }
    public Map<String, List<Incident>> getIncidentsByType() { return incidentsByType; }
    public void setIncidentsByType(Map<String, List<Incident>> incidentsByType) { this.incidentsByType = incidentsByType; }
    public Map<String, Integer> getIncidentsByDate() { return incidentsByDate; }
    public void setIncidentsByDate(Map<String, Integer> incidentsByDate) { this.incidentsByDate = incidentsByDate; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }
}