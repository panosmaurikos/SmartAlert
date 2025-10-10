package com.example.smartalert.data.remote;

import com.example.smartalert.domain.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
/**
 * Remote Data Source for authentication operations
 * Handles all communication with Firebase Authentication and Firestore
 */
public class AuthRemoteDataSource {
    // Firebase Authentication instance for user management
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    // Firestore database instance for user data storage
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    /**
     * Authenticates user with email and password
     *  listener Callback for handling authentication result
     */
    public void login(String email, String password, OnCompleteListener<AuthResult> listener) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(listener);
    }
    /**
     * Registers a new user with email and password
     * listener Callback for handling registration result
     */
    public void register(String email, String password, OnCompleteListener<AuthResult> listener) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(listener);
    }
    /**
     * Saves user profile data to Firestore database
     * user object containing profile information
     * listener Callback for handling save operation result
     */
    public void saveUser(User user, OnCompleteListener<Void> listener) {
        db.collection("users").document(user.getUid()).set(user).addOnCompleteListener(listener);
    }
}