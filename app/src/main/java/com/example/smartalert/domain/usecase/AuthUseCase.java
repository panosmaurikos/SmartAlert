package com.example.smartalert.domain.usecase;

import com.example.smartalert.domain.model.User;
import com.example.smartalert.domain.repository.AuthRepository;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthResult;

public class AuthUseCase {
    private AuthRepository repository;

    public AuthUseCase(AuthRepository repository) {
        this.repository = repository;
    }

    public void login(String email, String password, OnCompleteListener<AuthResult> listener) {
        repository.login(email, password, listener);
    }

    public void register(String email, String password, OnCompleteListener<AuthResult> listener) {
        repository.register(email, password, listener);
    }

    public void saveUser(User user, OnCompleteListener<Void> listener) {
        repository.saveUser(user, listener);
    }
}