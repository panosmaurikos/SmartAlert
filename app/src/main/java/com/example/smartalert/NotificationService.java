package com.example.smartalert;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class NotificationService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Χειρισμός εισερχόμενων ειδοποιήσεων
    }

    public static void sendNotificationToUsersInRadius(double centerLat, double centerLon, double radiusKm, String message) {
        // Αποστολή ειδοποιήσεων σε χρήστες within radius
        // Αυτό απαιτεί server-side implementation
    }
}