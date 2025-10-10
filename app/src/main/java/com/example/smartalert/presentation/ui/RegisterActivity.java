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

public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "RegisterActivity";
    // UI components
    private AuthViewModel viewModel;
    private TextInputEditText editTextEmail, editTextPassword;
    private MaterialButton buttonReg;
    private ProgressBar progressBar;
    private TextView textView;
    // Initialize the view model
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        // Initialize the view model
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
       // UI components from layout
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        buttonReg = findViewById(R.id.btn_register);
        progressBar = findViewById(R.id.progressBar);
        textView = findViewById(R.id.loginNow);
            // Set up click listener for login text view
        textView.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
            // Set up click listener for register button
        buttonReg.setOnClickListener(v -> {
            String email = editTextEmail.getText() != null ? editTextEmail.getText().toString().trim() : "";
            String password = editTextPassword.getText() != null ? editTextPassword.getText().toString().trim() : "";
            String role = "user";
                // validation
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }
                // Show loading
            progressBar.setVisibility(View.VISIBLE);
            viewModel.register(email, password, role);
        });
            // Observe user registration state
        viewModel.getUserLiveData().observe(this, user -> {
            Log.d(TAG, "userLiveData observer fired: user=" + user);
            if (progressBar.getVisibility() == View.VISIBLE) progressBar.setVisibility(View.GONE);
            if (user != null) { // If user is registered, navigate to main screen
                Toast.makeText(this, "Account created", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        });
        // Observe registration errors
        viewModel.getErrorLiveData().observe(this, error -> {
            if (progressBar.getVisibility() == View.VISIBLE) progressBar.setVisibility(View.GONE);
            if (error != null) { // error
                Log.w(TAG, "Register error: " + error);
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
        // Auto-navigate to main screen if user is already logged in
    @Override
    protected void onStart() {
        super.onStart();
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }
}