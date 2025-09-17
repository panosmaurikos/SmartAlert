package com.example.smartalert.presentation.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.smartalert.domain.model.User;
import com.example.smartalert.domain.usecase.AuthUseCase;
import com.example.smartalert.data.repository.AuthRepositoryImpl;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthViewModel extends ViewModel {
    private AuthUseCase authUseCase = new AuthUseCase(new AuthRepositoryImpl());
    private MutableLiveData<FirebaseUser> userLiveData = new MutableLiveData<>();
    private MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    public LiveData<FirebaseUser> getUserLiveData() { return userLiveData; }
    public LiveData<String> getErrorLiveData() { return errorLiveData; }

    public void login(String email, String password) {
        authUseCase.login(email, password, task -> {
            if (task.isSuccessful()) {
                userLiveData.setValue(task.getResult().getUser());
            } else {
                errorLiveData.setValue(task.getException().getMessage());
            }
        });
    }

    public void register(String email, String password, String role) {
        authUseCase.register(email, password, task -> {
            if (task.isSuccessful()) {
                FirebaseUser fbUser = task.getResult().getUser();
                if (fbUser != null) {
                    User user = new User(fbUser.getUid(), email, role);
                    authUseCase.saveUser(user, saveTask -> {
                        if (saveTask.isSuccessful()) {
                            userLiveData.setValue(fbUser);
                        } else {
                            errorLiveData.setValue("Failed to save user: " + saveTask.getException().getMessage());
                        }
                    });
                }
            } else {
                errorLiveData.setValue(task.getException().getMessage());
            }
        });
    }

    public void logout() {
        FirebaseAuth.getInstance().signOut();
        userLiveData.setValue(null);
    }
}