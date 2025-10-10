package com.example.smartalert;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.auth.FirebaseAuth;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.Intent;
import androidx.core.content.ContextCompat;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.smartalert.data.repository.AuthRepositoryImpl;
import com.example.smartalert.domain.repository.AuthRepository;

public class NotificationService extends FirebaseMessagingService {
    private static final String TAG = "NotificationService";
    private static final String CHANNEL_ID = "emergency_alerts";
    private static final String CHANNEL_NAME = "Emergency Alerts";
    private AuthRepository authRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        this.authRepository = new AuthRepositoryImpl();
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);
        saveTokenToFirestore(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "Received notification from: " + remoteMessage.getFrom());

        // First check if there is approval status in data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            String approvalStatus = remoteMessage.getData().get("approvalStatus");
            String incidentType = remoteMessage.getData().get("incidentType");
            String message = remoteMessage.getData().get("message");

            // If approval status exists, show appropriate message
            if (approvalStatus != null) {
                handleApprovalNotification(approvalStatus, incidentType, message);
                return;
            }

            // Regular notifications
            String title = remoteMessage.getData().get("title");
            handleMessageNow(title, message, incidentType);
        }

        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            showNotification(title, body, "general");
        }
    }

    private void handleApprovalNotification(String approvalStatus, String incidentType, String message) {
        String title;
        String notificationMessage;

        if ("approved".equals(approvalStatus)) {
            title = "Approved Incident";
            notificationMessage = message != null ? message :
                    "Your incident for " + getTypeDisplayName(incidentType) + " has been approved";
        } else if ("rejected".equals(approvalStatus)) {
            title = "Rejected Incident";
            notificationMessage = message != null ? message :
                    "Your incident for " + getTypeDisplayName(incidentType) + " has been rejected";
        } else {
            title = "Status Update";
            notificationMessage = message != null ? message :
                    "The status of your incident for " + getTypeDisplayName(incidentType) + " has changed";
        }

        showNotification(title, notificationMessage, incidentType);
        broadcastNotificationToActivity(title, notificationMessage, incidentType);
    }

    private void handleMessageNow(String title, String message, String incidentType) {
        showNotification(title, message, incidentType);
        broadcastNotificationToActivity(title, message, incidentType);
    }

    private void broadcastNotificationToActivity(String title, String message, String incidentType) {
        // Broadcast notification to activities for real-time updates
        Intent intent = new Intent("NEW_EMERGENCY_NOTIFICATION");
        intent.putExtra("title", title);
        intent.putExtra("message", message);
        intent.putExtra("incidentType", incidentType);
        sendBroadcast(intent);
    }

    private void saveTokenToFirestore(String token) {
        // Save FCM token to Firestore for push notifications
        String userId = getCurrentUserId();
        if (userId != null) {
            authRepository.updateFCMToken(userId, token)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Token saved successfully for user: " + userId))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save token for user: " + userId, e));
        } else {
            Log.w(TAG, "Cannot save token: userId is null. Token will be saved on next login/register.");
        }
    }

    private String getCurrentUserId() {
        // Get current user ID from Firebase Auth
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        return null;
    }

    private void createNotificationChannel() {
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Emergency incident alerts");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            channel.enableLights(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void showNotification(String title, String message, String incidentType) {
        // Check if app has notification permission
        if (!hasNotificationPermission()) {
            Log.w(TAG, "Notification permission not granted");
            return;
        }

        int smallIcon = getApplicationInfo().icon;

        // Build notification with high priority
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(smallIcon)
                .setContentTitle(title != null ? title : "Emergency Alert")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 1000, 500, 1000})
                .setLights(0xFFF44336, 1000, 1000);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        int notificationId = (int) System.currentTimeMillis();

        try {
            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "Notification displayed successfully with ID: " + notificationId);
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException when notifying: ", e);
        } catch (Exception e) {
            Log.e(TAG, "Error showing notification: ", e);
        }
    }

    private boolean hasNotificationPermission() {
        // Check notification permission for Android 13 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private String getTypeDisplayName(String typeKey) {
        // Convert incident type key to display name
        if (typeKey == null) return "Unknown Type";
        switch (typeKey) {
            case "fire": return "Fire";
            case "flood": return "Flood";
            case "earthquake": return "Earthquake";
            case "storm": return "Storm";
            case "other": return "Other";
            default: return typeKey;
        }
    }
}