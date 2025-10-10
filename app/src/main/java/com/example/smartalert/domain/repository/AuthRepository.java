package com.example.smartalert.domain.repository;

import com.example.smartalert.domain.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthResult;
import com.google.android.gms.tasks.Task;

public interface AuthRepository {
    //Authenticates a user with email and password
    void login(String email, String password, OnCompleteListener<AuthResult> listener);
   // Registers a new user with email and password
    void register(String email, String password, OnCompleteListener<AuthResult> listener);
    // Saves user profile data to the database
    void saveUser(User user, OnCompleteListener<Void> listener);
    //Updates the Firebase Cloud Messaging token for push notifications
    Task<Void> updateFCMToken(String userId, String fcmToken);

}