package com.example.smartalert.domain.usecase;

import com.example.smartalert.domain.model.User;
import com.example.smartalert.domain.repository.AuthRepository;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthResult;
import com.google.android.gms.tasks.Task;

// Authentication Use Case combining all user authentication operations
public class AuthUseCase {
    private AuthRepository repository;

    public AuthUseCase(AuthRepository repository) {
        this.repository = repository;
    }
     // Executes user login with email and password
    public void login(String email, String password, OnCompleteListener<AuthResult> listener) {
        repository.login(email, password, listener);
    }
    // Executes new user registration with email and password
    public void register(String email, String password, OnCompleteListener<AuthResult> listener) {
        repository.register(email, password, listener);
    }
    // Saves user profile information to the database
    public void saveUser(User user, OnCompleteListener<Void> listener) {
        repository.saveUser(user, listener);
    }
    public Task<Void> updateFCMToken(String userId, String fcmToken) {
        return repository.updateFCMToken(userId, fcmToken);
    }
}