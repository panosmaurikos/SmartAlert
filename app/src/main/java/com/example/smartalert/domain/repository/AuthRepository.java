package com.example.smartalert.domain.repository;

import com.example.smartalert.domain.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthResult;

public interface AuthRepository {
    void login(String email, String password, OnCompleteListener<AuthResult> listener);
    void register(String email, String password, OnCompleteListener<AuthResult> listener);
    void saveUser(User user, OnCompleteListener<Void> listener);
}