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
    private AuthRemoteDataSource remoteDataSource = new AuthRemoteDataSource();
    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    @Override
    public void login(String email, String password, OnCompleteListener<AuthResult> listener) {
        remoteDataSource.login(email, password, listener);
    }

    @Override
    public void register(String email, String password, OnCompleteListener<AuthResult> listener) {
        remoteDataSource.register(email, password, listener);
    }

    @Override
    public void saveUser(User user, OnCompleteListener<Void> listener) {
        remoteDataSource.saveUser(user, listener);
    }

    @Override
    public Task<Void> updateFCMToken(String userId, String fcmToken) {
        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("fcmToken", fcmToken);
        updateMap.put("tokenUpdateTimestamp", com.google.firebase.Timestamp.now());
        return firestore.collection("users").document(userId).update(updateMap);
    }
}