    package com.example.smartalert.domain.model;

    public class User {
        private String uid;
        private String email;
        private String role; // "user" or "admin"
        private String fcmToken;
        public User() {} // Default constructor for Firestore

        // Constructor for user
        public User(String uid, String email, String role) {
            this.uid = uid;
            this.email = email;
            this.role = role;
        }
        // Getters and setters
        public String getUid() { return uid; }
        public void setUid(String uid) { this.uid = uid; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getFcmToken() { return fcmToken; }
        public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
    }