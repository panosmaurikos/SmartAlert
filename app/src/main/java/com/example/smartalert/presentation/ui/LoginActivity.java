package com.example.smartalert.presentation.ui;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.smartalert.R;
import com.example.smartalert.presentation.viewmodels.AuthViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

// Handles user authentication UI and interactions
public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    // UI components
    private AuthViewModel viewModel; // ViewModel
    private TextInputEditText editTextEmail, editTextPassword;
    private MaterialButton buttonLogin;
    private ProgressBar progressBar;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Initialize ViewModel using ViewModelProvider
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        // UI components from layout
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        buttonLogin = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progressBar);
        textView = findViewById(R.id.registerNow);

        textView.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });
        // Login button click listener
        buttonLogin.setOnClickListener(v -> {
            String email = editTextEmail.getText() != null ? editTextEmail.getText().toString().trim() : "";
            String password = editTextPassword.getText() != null ? editTextPassword.getText().toString().trim() : "";
            // validation
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }
            // Show loading
            progressBar.setVisibility(View.VISIBLE);
            viewModel.login(email, password);
        });
        // Observe user authentication state

        viewModel.getUserLiveData().observe(this, user -> {
            Log.d(TAG, "userLiveData observer fired: user=" + user);
            if (progressBar.getVisibility() == View.VISIBLE) progressBar.setVisibility(View.GONE);
            if (user != null) {
                // If user is authenticated, navigate to main screen
                Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Starting MainActivity");
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        });
        // Observe authentication errors

        viewModel.getErrorLiveData().observe(this, error -> {
            if (progressBar.getVisibility() == View.VISIBLE) progressBar.setVisibility(View.GONE);
            if (error != null) {
                Log.w(TAG, "Auth error: " + error);
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    // Auto-navigate to main screen if user is already logged in
    @Override
    protected void onStart() {
        super.onStart();
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
            Log.d(TAG, "onStart: already logged in -> MainActivity");
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }
}