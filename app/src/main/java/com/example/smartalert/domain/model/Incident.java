package com.example.smartalert.domain.model;

import java.util.Date;

public class Incident {
    private String id;
    private String userId;
    private String type;
    private String comments;
    private double latitude;
    private double longitude;
    private Date timestamp;
    private String photoUrl;
    private String status;

    public Incident() {}

    public Incident(String userId, String type, String comments,
                    double latitude, double longitude, Date timestamp,
                    String photoUrl) {
        this.userId = userId;
        this.type = type;
        this.comments = comments;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.photoUrl = photoUrl;
        this.status = "pending";
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
