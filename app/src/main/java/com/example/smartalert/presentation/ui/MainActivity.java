package com.example.smartalert.presentation.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.smartalert.R;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
    private com.example.smartalert.presentation.viewmodels.AuthViewModel viewModel;
    private TextView textViewUserDetails;
    private Button buttonLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(com.example.smartalert.presentation.viewmodels.AuthViewModel.class);

        textViewUserDetails = findViewById(R.id.user_details);
        buttonLogout = findViewById(R.id.logout);

        viewModel.getUserLiveData().observe(this, user -> {
            if (user == null) {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            } else {
                textViewUserDetails.setText("Logged in as: " + user.getEmail());
            }
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
        if (viewModel.getUserLiveData().getValue() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }
}