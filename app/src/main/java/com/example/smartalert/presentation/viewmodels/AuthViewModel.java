package com.example.smartalert.presentation.viewmodels;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.smartalert.domain.model.User;
import com.example.smartalert.domain.usecase.AuthUseCase;
import com.example.smartalert.data.repository.AuthRepositoryImpl;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

public class AuthViewModel extends ViewModel {
    private static final String TAG = "AuthViewModel";

    // Dependencies and LiveData objects
    private final AuthUseCase authUseCase = new AuthUseCase(new AuthRepositoryImpl());
    private final MutableLiveData<FirebaseUser> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    // Firebase services
    private final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private final FirebaseMessaging firebaseMessaging = FirebaseMessaging.getInstance();

    public AuthViewModel() {
        // Initialize with current user if exists
        FirebaseUser current = firebaseAuth.getCurrentUser();
        Log.d(TAG, "AuthViewModel constructor - current user: " + current);
        userLiveData.setValue(current);
    }

    // LiveData getters for observing user state and errors
    public LiveData<FirebaseUser> getUserLiveData() { return userLiveData; }
    public LiveData<String> getErrorLiveData() { return errorLiveData; }
    // Login method
    public void login(String email, String password) {
        Log.d(TAG, "login() called for: " + email);
        authUseCase.login(email, password, (OnCompleteListener<AuthResult>) task -> {
            if (task.isSuccessful() && firebaseAuth.getCurrentUser() != null) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                Log.d(TAG, "login success uid=" + user.getUid());
                userLiveData.setValue(user);
                updateTokenForCurrentUser();
            } else {
                String msg = "Authentication failed";
                if (task.getException() != null) msg = task.getException().getMessage();
                Log.w(TAG, "login failed: " + msg);
                errorLiveData.setValue(msg);
            }
        });
    }
    // Register method
    public void register(String email, String password, String role) {
        Log.d(TAG, "register() called for: " + email);
        authUseCase.register(email, password, (OnCompleteListener<AuthResult>) task -> {
            if (task.isSuccessful() && firebaseAuth.getCurrentUser() != null) {
                FirebaseUser fbUser = firebaseAuth.getCurrentUser();
                Log.d(TAG, "register success uid=" + fbUser.getUid());
                // Create user object and save to Firestore
                User user = new User(fbUser.getUid(), email, role);
                authUseCase.saveUser(user, saveTask -> {
                    if (saveTask.isSuccessful()) {
                        Log.d(TAG, "saveUser success for uid=" + fbUser.getUid());
                        userLiveData.setValue(fbUser);
                        updateTokenForCurrentUser();
                    } else {
                        String err = "Failed to save user: " + (saveTask.getException() != null ? saveTask.getException().getMessage() : "unknown");
                        Log.w(TAG, err);
                        errorLiveData.setValue(err);
                    }
                });
            } else {
                String msg = "Registration failed";
                if (task.getException() != null) msg = task.getException().getMessage();
                Log.w(TAG, "register failed: " + msg);
                errorLiveData.setValue(msg);
            }
        });
    }

    public void logout() {
        Log.d(TAG, "logout()");
        firebaseAuth.signOut();
        userLiveData.setValue(null);
    }

    public Task<Void> updateFCMToken(String userId, String fcmToken) {
        Log.d(TAG, "updateFCMToken() called for user: " + userId);
        return authUseCase.updateFCMToken(userId, fcmToken);
    }

    private void updateTokenForCurrentUser() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            Log.d(TAG, "Attempting to fetch and update FCM token for user: " + userId);

            // Get FCM token and update it for the current user
            firebaseMessaging.getToken()
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        String token = task.getResult();
                        if (token != null && !token.isEmpty()) {
                            Log.d(TAG, "FCM Token retrieved: " + token);
                            // Use updateFCMToken from UseCase
                            Task<Void> updateTask = updateFCMToken(userId, token);
                            updateTask.addOnSuccessListener(aVoid -> Log.d(TAG, "Token saved successfully for user: " + userId))
                                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save token for user: " + userId, e));
                        } else {
                            Log.w(TAG, "FCM token is null or empty, cannot save.");
                        }
                    });
        } else {
            Log.w(TAG, "updateTokenForCurrentUser: No user is currently logged in.");
        }
    }
}