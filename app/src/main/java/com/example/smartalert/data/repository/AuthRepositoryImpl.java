package com.example.smartalert.data.repository;

import com.example.smartalert.data.remote.AuthRemoteDataSource;
import com.example.smartalert.domain.model.User;
import com.example.smartalert.domain.repository.AuthRepository;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthResult;

public class AuthRepositoryImpl implements AuthRepository {
    private AuthRemoteDataSource remoteDataSource = new AuthRemoteDataSource();

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
}