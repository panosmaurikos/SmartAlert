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
    import android.os.Looper;
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
    import com.example.smartalert.presentation.viewmodels.IncidentViewModel;
    import com.google.android.gms.location.FusedLocationProviderClient;
    import com.google.android.gms.location.LocationCallback;
    import com.google.android.gms.location.LocationServices;
    import com.google.android.gms.location.LocationRequest;
    import com.google.common.util.concurrent.ListenableFuture;

    import java.io.File;
    import java.io.IOException;
    import java.text.SimpleDateFormat;
    import java.util.Arrays;
    import java.util.Date;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;
    import java.util.Locale;
    import com.google.android.gms.location.LocationResult;
    import java.util.concurrent.ExecutionException;

    // Report incident activity
    public class ReportIncidentActivity extends AppCompatActivity {
        private static final String TAG = "ReportIncidentActivity";
        private static final int CAMERA_PERMISSION_CODE = 100;
        private static final int LOCATION_PERMISSION_CODE = 101;
        // UI components
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
        // CameraX components
        private ImageCapture imageCapture;
        private ProcessCameraProvider cameraProvider;
        private boolean isCameraActive = false;
        private Map<String, String> incidentTypeMap;

        // Initializes activity, ViewModel, and requests location permissions
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_report_incident);
            // Initialize the view model
            viewModel = new ViewModelProvider(this).get(IncidentViewModel.class);
            locationClient = LocationServices.getFusedLocationProviderClient(this);
            // Initialize UI components
            initIncidentTypeMap();
            initViews();
            setupObservers();
            requestLocationPermission();
        }
        // Creates mapping between Greek display names and English database keys
        private void initIncidentTypeMap() {
            incidentTypeMap = new HashMap<>();
            incidentTypeMap.put(getString(R.string.incident_type_fire), "fire");
            incidentTypeMap.put(getString(R.string.incident_type_flood), "flood");
            incidentTypeMap.put(getString(R.string.incident_type_earthquake), "earthquake");
            incidentTypeMap.put(getString(R.string.incident_type_storm), "storm");
            incidentTypeMap.put(getString(R.string.incident_type_other), "other");
        }
        // Initializes all UI components and sets up event handlers
        private void initViews() {
            incidentType = findViewById(R.id.incidentType);
            comments = findViewById(R.id.comments);
            photoPreview = findViewById(R.id.photoPreview);
            cameraPreview = findViewById(R.id.cameraPreview);
            takePhotoButton = findViewById(R.id.takePhotoButton);
            submitButton = findViewById(R.id.submitButton);
            progressBar = findViewById(R.id.progressBar);

            //   Setup localized incident types spinner
            List<String> incidentTypes = Arrays.asList(
                    getString(R.string.select_incident_type),
                    getString(R.string.incident_type_fire),
                    getString(R.string.incident_type_flood),
                    getString(R.string.incident_type_earthquake),
                    getString(R.string.incident_type_storm),
                    getString(R.string.incident_type_other)
            );
                // Set up adapter
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, incidentTypes);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            incidentType.setAdapter(adapter);
            // Setup click listeners with conditional camera behavior
            takePhotoButton.setOnClickListener(v -> {
                if (!isCameraActive) { // If camera is not active, request permission
                    requestCameraPermission();
                } else {
                    takePhoto();// Take photo
                }
            });
                // Submit incident
            submitButton.setOnClickListener(v -> submitIncident());
        }
        // Observe view model
        private void setupObservers() {
            viewModel.getSubmitResult().observe(this, success -> {
                progressBar.setVisibility(View.GONE); // Hide progress bar
                if (success) {
                    Toast.makeText(this, getString(R.string.incident_reported_successfully), Toast.LENGTH_SHORT).show();
                    finish();// Finish activity
                }
            });
                // Observe error
            viewModel.getErrorMessage().observe(this, error -> {
                progressBar.setVisibility(View.GONE);
                if (error != null) {
                    Toast.makeText(this, getString(R.string.error_prefix) + error, Toast.LENGTH_SHORT).show();
                }
            });
        }
            // Request location permission
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
            // Request camera permission
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
        // Get current location
        private void getCurrentLocation() {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                LocationRequest locationRequest = new LocationRequest.Builder(1000)
                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                        .setMaxUpdates(1)
                        .build();
                // Request location updates
                locationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        Location location = locationResult.getLastLocation();
                        if (location != null) {
                            // Update UI with location
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                            Log.d(TAG, "Location obtained: " + latitude + ", " + longitude);
                            Toast.makeText(ReportIncidentActivity.this, getString(R.string.location_obtained_successfully), Toast.LENGTH_SHORT).show();
                        } else {
                            Log.w(TAG, "Location is null");
                            Toast.makeText(ReportIncidentActivity.this, getString(R.string.could_not_get_location), Toast.LENGTH_SHORT).show();
                        }
                    }
                }, Looper.getMainLooper()); // Use main looper for UI updates
            } else {
                Log.w(TAG, "Location permission not granted");
            }
        }
            // Start camera preview
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
                    // Show camera preview
                    cameraPreview.setVisibility(View.VISIBLE);
                    photoPreview.setVisibility(View.GONE);
                    isCameraActive = true;
                    takePhotoButton.setText(getString(R.string.take_photo));
                    // Set up click listeners
                } catch (ExecutionException | InterruptedException e) {
                    Log.e(TAG, "Camera binding failed: " + e.getMessage());
                    Toast.makeText(this, getString(R.string.failed_to_start_camera), Toast.LENGTH_SHORT).show();
                }
            }, ContextCompat.getMainExecutor(this));
        }

        private void takePhoto() { // Take photo
            if (imageCapture == null) { // Check if camera is initialized
                Toast.makeText(this, getString(R.string.camera_not_initialized), Toast.LENGTH_SHORT).show();
                return;
            }

            File photoFile = createImageFile();
            if (photoFile == null) { // Check if file creation was successful
                Toast.makeText(this, getString(R.string.error_creating_image_file), Toast.LENGTH_SHORT).show();
                return;
            }

            ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

            imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
                @Override // Handle image saved
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

        private File createImageFile() { // Create image file
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

        private void submitIncident() {  // Submit incident
            String selectedGreekType = incidentType.getSelectedItem().toString();
            String commentText = comments.getText().toString();

            if (selectedGreekType.equals(getString(R.string.select_incident_type))) {
                Toast.makeText(this, getString(R.string.please_select_incident_type), Toast.LENGTH_SHORT).show();
                return;
            }
            // Validation for comment
            if (commentText.isEmpty()) {
                Toast.makeText(this, getString(R.string.please_enter_comments), Toast.LENGTH_SHORT).show();
                return;
            }
            // Submit incident with location
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_CODE);
                return;
            }

            progressBar.setVisibility(View.VISIBLE);

            LocationRequest locationRequest = new LocationRequest.Builder(1000)
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setMaxUpdates(1)
                    .build();
            locationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        double currentLat = location.getLatitude();
                        double currentLon = location.getLongitude();
                        Log.d(TAG, "Submitting with fresh location: " + currentLat + ", " + currentLon);

                        String englishType = incidentTypeMap.get(selectedGreekType);
                        if (englishType == null) englishType = "other";

                        viewModel.submitIncident(englishType, commentText, currentLat, currentLon,
                                photoUri != null ? photoUri.toString() : null);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ReportIncidentActivity.this, getString(R.string.could_not_get_location), Toast.LENGTH_SHORT).show();
                    }
                }
            }, Looper.getMainLooper());
        }

        @Override // Cleans up camera resources when activity is destroyed
        protected void onDestroy() {
            super.onDestroy();
            if (cameraProvider != null) {
                cameraProvider.unbindAll();
            }
        }

        @Override // Handles permission request results for both location and camera
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