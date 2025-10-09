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
import com.google.firebase.firestore.FirebaseFirestore; // Added
import com.example.smartalert.data.repository.AuthRepositoryImpl; // Added
import com.example.smartalert.domain.repository.AuthRepository; // Added

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

    // This replaces the old onTokenRefresh
    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);
        saveTokenToFirestore(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "Received notification from: " + remoteMessage.getFrom());

        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            String title = remoteMessage.getData().get("title");
            String message = remoteMessage.getData().get("message");
            String incidentType = remoteMessage.getData().get("incidentType");
            String latitude = remoteMessage.getData().get("latitude");
            String longitude = remoteMessage.getData().get("longitude");

            handleMessageNow(title, message, incidentType);
        }

        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            showNotification(title, body, "general");
        }
    }

    private void handleMessageNow(String title, String message, String incidentType) {
        showNotification(title, message, incidentType);

        broadcastNotificationToActivity(title, message, incidentType);
    }

    private void broadcastNotificationToActivity(String title, String message, String incidentType) {
        Intent intent = new Intent("NEW_EMERGENCY_NOTIFICATION");
        intent.putExtra("title", title);
        intent.putExtra("message", message);
        intent.putExtra("incidentType", incidentType);
        sendBroadcast(intent);
    }

    private void saveTokenToFirestore(String token) {
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
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        return null;
    }

    private void createNotificationChannel() {
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
        // First, we check for permission
        if (!hasNotificationPermission()) {
            Log.w(TAG, "Notification permission not granted");
            return; // If there is no permission, we don't proceed
        }

        // Use the default icon (or the app icon, as you had)
        int smallIcon = getApplicationInfo().icon; // This is always available

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(smallIcon)
                .setContentTitle(title != null ? title : "Emergency Alert")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message)) // Added this for longer text
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 1000, 500, 1000}) // Added this if you want vibration
                .setLights(0xFFF44336, 1000, 1000); // Added this for LED flash

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // We get the ID for the notification
        int notificationId = (int) System.currentTimeMillis();

        // Wrap notify() with a try-catch to handle a potential SecurityException
        try {
            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "Notification displayed successfully with ID: " + notificationId);
        } catch (SecurityException e) {
            // This catch will catch the exception if, despite the check, permission is not granted
            Log.e(TAG, "SecurityException when notifying: ", e);
        } catch (Exception e) {
            // Cover other possible exceptions as well
            Log.e(TAG, "Error showing notification: ", e);
        }
    }

    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }
}