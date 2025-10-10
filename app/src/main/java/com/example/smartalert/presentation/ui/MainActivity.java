package com.example.smartalert.presentation.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.smartalert.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    // view model for user authentication
    private com.example.smartalert.presentation.viewmodels.AuthViewModel viewModel;
    // UI components
    private TextView textViewUserDetails;
    private Button buttonLogout, statisticsButton, btnReport;
    // Initialize the view model
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestNotificationPermission();
        // Initialize the view model
        viewModel = new ViewModelProvider(this).get(com.example.smartalert.presentation.viewmodels.AuthViewModel.class);
        // Initialize UI components
        initViews();
        setupClickListeners();
            // Observe user authentication state
        viewModel.getUserLiveData().observe(this, user -> {
            Log.w(TAG, "userLiveData observer: " + user);
            // If user is not authenticated, navigate to login screen
            if (user == null) {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            } else { // If user is authenticated, display user details
                textViewUserDetails.setText("Logged in as: " + user.getEmail());
                checkUserRole(user.getUid());
            }
        });
    }
    // Handle permission request results
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Permissions", "Notification permission granted");
            } else {
                Log.w("Permissions", "Notification permission denied");
            }
        }
    }
    // Check user role and update UI accordingly
    private void checkUserRole(String uid) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        Log.d(TAG, "User role: " + role);

                        if ("admin".equals(role)) {
                            btnReport.setVisibility(View.GONE);
                        } else {
                            btnReport.setVisibility(View.VISIBLE);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch role", e);
                });
    }

    // Request notification permission if needed
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        1001);
            }
        }
    }
    // Initialize UI components from the layout
    private void initViews() {
        textViewUserDetails = findViewById(R.id.user_details);
        buttonLogout = findViewById(R.id.logout);
        statisticsButton = findViewById(R.id.btn_statistics);
        btnReport = findViewById(R.id.btn_report);
    }
    // Set up click listeners for buttons
    private void setupClickListeners() {
        // Navigate to incident reporting
        btnReport.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ReportIncidentActivity.class));
        });
        // Navigate to statistics screen
        statisticsButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, StatisticsActivity.class));
        });
        // Logout user
        buttonLogout.setOnClickListener(v -> {
            viewModel.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
    // Auto-navigate to login screen if user is not logged in
    @Override
    protected void onStart() {
        super.onStart();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.d(TAG, "onStart: no current user -> LoginActivity");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }
}