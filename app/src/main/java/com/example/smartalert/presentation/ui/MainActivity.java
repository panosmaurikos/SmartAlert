package com.example.smartalert.presentation.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.smartalert.R;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private com.example.smartalert.presentation.viewmodels.AuthViewModel viewModel;
    private TextView textViewUserDetails;
    private Button buttonLogout, statisticsButton, btnReport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(com.example.smartalert.presentation.viewmodels.AuthViewModel.class);

        initViews();

        setupClickListeners();

        viewModel.getUserLiveData().observe(this, user -> {
            Log.w(TAG, "userLiveData observer: " + user);
            if (user == null) {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            } else {
                textViewUserDetails.setText("Logged in as: " + user.getEmail());
            }
        });
    }

    private void initViews() {
        textViewUserDetails = findViewById(R.id.user_details);
        buttonLogout = findViewById(R.id.logout);
        statisticsButton = findViewById(R.id.btn_statistics);
        btnReport = findViewById(R.id.btn_report);
    }

    private void setupClickListeners() {
        btnReport.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ReportIncidentActivity.class));
        });

        statisticsButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, StatisticsActivity.class));
        });

        buttonLogout.setOnClickListener(v -> {
            viewModel.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

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