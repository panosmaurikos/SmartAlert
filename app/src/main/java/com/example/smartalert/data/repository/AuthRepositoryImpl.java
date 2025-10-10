package com.example.smartalert.data.repository;

import com.example.smartalert.data.remote.AuthRemoteDataSource;
import com.example.smartalert.domain.model.User;
import com.example.smartalert.domain.repository.AuthRepository;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthResult;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class AuthRepositoryImpl implements AuthRepository {
    // Remote data source for authentication operations
    private AuthRemoteDataSource remoteDataSource = new AuthRemoteDataSource();
    // Firestore instance for direct database operations
    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    /**
     * Authenticates user with email and password
     * Delegates the operation to the remote data source
     */
    @Override
    public void login(String email, String password, OnCompleteListener<AuthResult> listener) {
        remoteDataSource.login(email, password, listener);
    }
    /**
     * Registers a new user with email and password
     * Delegates the operation to the remote data source
     */
    @Override
    public void register(String email, String password, OnCompleteListener<AuthResult> listener) {
        remoteDataSource.register(email, password, listener);
    }
//    Saves user profile to database

    @Override
    public void saveUser(User user, OnCompleteListener<Void> listener) {
        remoteDataSource.saveUser(user, listener);
    }
//    Updates the FCM token for push notifications
//        for  the unique identifier of the user

    /**
     * Updates the FCM token for push notifications
     */
    @Override
    public Task<Void> updateFCMToken(String userId, String fcmToken) {
        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("fcmToken", fcmToken);
        updateMap.put("tokenUpdateTimestamp", com.google.firebase.Timestamp.now());
        return firestore.collection("users").document(userId).update(updateMap);
    }
}