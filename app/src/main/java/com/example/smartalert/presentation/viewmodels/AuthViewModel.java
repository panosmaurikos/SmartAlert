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

public class AuthViewModel extends ViewModel {
    private static final String TAG = "AuthViewModel";

    private final AuthUseCase authUseCase = new AuthUseCase(new AuthRepositoryImpl());
    private final MutableLiveData<FirebaseUser> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    private final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

    public AuthViewModel() {
        FirebaseUser current = firebaseAuth.getCurrentUser();
        Log.d(TAG, "AuthViewModel ctor - current user: " + current);
        userLiveData.setValue(current);
    }

    public LiveData<FirebaseUser> getUserLiveData() { return userLiveData; }
    public LiveData<String> getErrorLiveData() { return errorLiveData; }

    public void login(String email, String password) {
        Log.d(TAG, "login() called for: " + email);
        authUseCase.login(email, password, (OnCompleteListener<AuthResult>) task -> {
            if (task.isSuccessful() && firebaseAuth.getCurrentUser() != null) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                Log.d(TAG, "login success uid=" + user.getUid());
                userLiveData.setValue(user);
            } else {
                String msg = "Authentication failed";
                if (task.getException() != null) msg = task.getException().getMessage();
                Log.w(TAG, "login failed: " + msg);
                errorLiveData.setValue(msg);
            }
        });
    }

    public void register(String email, String password, String role) {
        Log.d(TAG, "register() called for: " + email);
        authUseCase.register(email, password, (OnCompleteListener<AuthResult>) task -> {
            if (task.isSuccessful() && firebaseAuth.getCurrentUser() != null) {
                FirebaseUser fbUser = firebaseAuth.getCurrentUser();
                Log.d(TAG, "register success uid=" + fbUser.getUid());
                User user = new User(fbUser.getUid(), email, role);
                authUseCase.saveUser(user, saveTask -> {
                    if (saveTask.isSuccessful()) {
                        Log.d(TAG, "saveUser success for uid=" + fbUser.getUid());
                        userLiveData.setValue(fbUser);
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
}