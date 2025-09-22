package com.example.smartalert.presentation.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.smartalert.R;
import com.example.smartalert.domain.model.IncidentType;
import com.example.smartalert.presentation.viewmodels.IncidentViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class ReportIncidentActivity extends AppCompatActivity {
    private static final String TAG = "ReportIncidentActivity";
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int LOCATION_PERMISSION_CODE = 101;

    private IncidentViewModel viewModel;
    private FusedLocationProviderClient locationClient;
    private double latitude = 0.0;
    private double longitude = 0.0;
    private Uri photoUri;
    private String currentPhotoPath;

    private Spinner incidentType;
    private EditText comments;
    private ImageView photoPreview;
    private PreviewView cameraPreview;
    private Button takePhotoButton;
    private Button submitButton;
    private ProgressBar progressBar;

    private ImageCapture imageCapture;
    private ProcessCameraProvider cameraProvider;
    private boolean isCameraActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_incident);

        viewModel = new ViewModelProvider(this).get(IncidentViewModel.class);
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        setupObservers();
        requestLocationPermission();
    }

    private void initViews() {
        incidentType = findViewById(R.id.incidentType);
        comments = findViewById(R.id.comments);
        photoPreview = findViewById(R.id.photoPreview);
        cameraPreview = findViewById(R.id.cameraPreview);
        takePhotoButton = findViewById(R.id.takePhotoButton);
        submitButton = findViewById(R.id.submitButton);
        progressBar = findViewById(R.id.progressBar);

        // Setup incident types spinner with localization
        List<String> incidentTypes = Arrays.asList(
                getString(R.string.select_incident_type),
                getString(R.string.incident_type_fire),
                getString(R.string.incident_type_flood),
                getString(R.string.incident_type_earthquake),
                getString(R.string.incident_type_storm),
                getString(R.string.incident_type_other)
        );

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, incidentTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        incidentType.setAdapter(adapter);

        takePhotoButton.setOnClickListener(v -> {
            if (!isCameraActive) {
                requestCameraPermission();
            } else {
                takePhoto();
            }
        });

        submitButton.setOnClickListener(v -> submitIncident());
    }

    private void setupObservers() {
        viewModel.getSubmitResult().observe(this, success -> {
            progressBar.setVisibility(View.GONE);
            if (success) {
                Toast.makeText(this, getString(R.string.incident_reported_successfully), Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            progressBar.setVisibility(View.GONE);
            if (error != null) {
                Toast.makeText(this, getString(R.string.error_prefix) + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE);
        } else {
            getCurrentLocation();
        }
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE);
        } else {
            startCamera();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                            Log.d(TAG, "Location obtained: " + latitude + ", " + longitude);
                            Toast.makeText(this, getString(R.string.location_obtained_successfully), Toast.LENGTH_SHORT).show();
                        } else {
                            Log.w(TAG, "Location is null");
                            Toast.makeText(this, getString(R.string.could_not_get_location), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(this, e -> {
                        Log.e(TAG, "Location error: " + e.getMessage());
                        Toast.makeText(this, getString(R.string.location_error_prefix) + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Log.w(TAG, "Location permission not granted");
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

                cameraPreview.setVisibility(View.VISIBLE);
                photoPreview.setVisibility(View.GONE);
                isCameraActive = true;
                takePhotoButton.setText(getString(R.string.take_photo));

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera binding failed: " + e.getMessage());
                Toast.makeText(this, getString(R.string.failed_to_start_camera), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (imageCapture == null) {
            Toast.makeText(this, getString(R.string.camera_not_initialized), Toast.LENGTH_SHORT).show();
            return;
        }

        File photoFile = createImageFile();
        if (photoFile == null) {
            Toast.makeText(this, getString(R.string.error_creating_image_file), Toast.LENGTH_SHORT).show();
            return;
        }

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                photoUri = Uri.fromFile(photoFile);

                cameraPreview.setVisibility(View.GONE);
                photoPreview.setVisibility(View.VISIBLE);
                isCameraActive = false;
                takePhotoButton.setText(getString(R.string.retake_photo));
                takePhotoButton.setOnClickListener(v -> retakePhoto());

                Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                photoPreview.setImageBitmap(bitmap);
                Toast.makeText(ReportIncidentActivity.this, getString(R.string.photo_captured), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "Photo capture failed: " + exception.getMessage());
                Toast.makeText(ReportIncidentActivity.this, getString(R.string.failed_to_capture_photo_prefix) + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void retakePhoto() {
        // Delete previous photo if exists
        if (photoUri != null) {
            File photoFile = new File(photoUri.getPath());
            if (photoFile.exists()) {
                photoFile.delete();
            }
            photoUri = null;
        }

        // Restart camera preview
        cameraPreview.setVisibility(View.VISIBLE);
        photoPreview.setVisibility(View.GONE);
        isCameraActive = true;
        takePhotoButton.setText(getString(R.string.take_photo));
        takePhotoButton.setOnClickListener(v -> takePhoto());

        startCamera();
    }

    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        File image = null;
        try {
            image = File.createTempFile(
                    imageFileName,
                    ".jpg",
                    storageDir
            );
            currentPhotoPath = image.getAbsolutePath();
        } catch (IOException ex) {
            Log.e(TAG, "Error creating image file: " + ex.getMessage());
            Toast.makeText(this, getString(R.string.error_creating_image_file), Toast.LENGTH_SHORT).show();
        }
        return image;
    }

    private void submitIncident() {
        String type = incidentType.getSelectedItem().toString();
        String commentText = comments.getText().toString();

        if (type.equals(getString(R.string.select_incident_type))) {
            Toast.makeText(this, getString(R.string.please_select_incident_type), Toast.LENGTH_SHORT).show();
            return;
        }

        if (commentText.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_enter_comments), Toast.LENGTH_SHORT).show();
            return;
        }

        if (latitude == 0.0 && longitude == 0.0) {
            Toast.makeText(this, getString(R.string.getting_location), Toast.LENGTH_SHORT).show();
            getCurrentLocation();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        viewModel.submitIncident(type, commentText, latitude, longitude,
                photoUri != null ? photoUri.toString() : null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, getString(R.string.camera_permission_denied), Toast.LENGTH_SHORT).show();
            }
        }
    }
}