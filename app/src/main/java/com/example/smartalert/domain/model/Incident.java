package com.example.smartalert.domain.model;

import java.util.Date;
import com.google.firebase.Timestamp;
// Domain model representing an incident reported by users
public class Incident {
    private String id;
    private String userId;
    private String type;
    private String comments;
    private double latitude;
    private double longitude;
    private Object  timestamp;
    private String photoUrl;
    private String status;
    private String location;

    public Incident() {}
    /**
     * Default constructor required for Firebase deserialization
     */
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
    public Date getTimestamp() {
        if (timestamp instanceof Timestamp) {
            return ((Timestamp) timestamp).toDate();
        } else if (timestamp instanceof Date) {
            return (Date) timestamp;
        }
        return new Date();
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getLocation() {
        return location;
    }
    public void setLocation(String location) {
        this.location = location;
    }
}
