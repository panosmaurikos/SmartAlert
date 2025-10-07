package com.example.smartalert.presentation.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.core.content.ContextCompat;

import com.example.smartalert.domain.model.Incident;
import com.google.android.material.card.MaterialCardView;
import android.graphics.Typeface;

import com.example.smartalert.R;
import com.example.smartalert.presentation.viewmodels.StatisticsViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class StatisticsActivity extends AppCompatActivity {

    private static final String TAG = "StatisticsActivity";
    private static final String PREFS = "app_prefs";
    private static final String PREF_ROLE_PREFIX = "role_";

    private StatisticsViewModel viewModel;
    private TextView totalIncidentsText, totalUsersText;
    private LinearLayout typesContainer;
    private Spinner typeFilterSpinner;
    private Button startDateButton, endDateButton, applyFiltersButton, backButton;
    private ProgressBar progressBar;

    private Date startDate = null;
    private Date endDate = null;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private boolean isAdmin = false;
    private boolean roleLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        viewModel = new ViewModelProvider(this).get(StatisticsViewModel.class);
        initViews();
        setupSpinner();
        setupObservers();
        setupButtonListeners();

        fetchRoleThenLoad();
    }

    private void initViews() {
        totalIncidentsText = findViewById(R.id.totalIncidents);
        totalUsersText = findViewById(R.id.totalUsers);
        typesContainer = findViewById(R.id.typesContainer);
        typeFilterSpinner = findViewById(R.id.typeFilterSpinner); // Spinner in layout
        startDateButton = findViewById(R.id.startDateButton);
        endDateButton = findViewById(R.id.endDateButton);
        applyFiltersButton = findViewById(R.id.applyFiltersButton);
        backButton = findViewById(R.id.backButton);
        progressBar = findViewById(R.id.progressBar);

        startDateButton.setText(R.string.start_date);
        endDateButton.setText(R.string.end_date);
        applyFiltersButton.setText(R.string.apply_filters);
        backButton.setText(R.string.back_to_main);

        startDateButton.setOnClickListener(v -> showDatePicker(true));
        endDateButton.setOnClickListener(v -> showDatePicker(false));
    }

    private void setupSpinner() {
        String[] incidentTypes = {
                getString(R.string.all),
                getString(R.string.incident_type_fire),
                getString(R.string.incident_type_flood),
                getString(R.string.incident_type_earthquake),
                getString(R.string.incident_type_storm),
                getString(R.string.incident_type_other)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, incidentTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeFilterSpinner.setAdapter(adapter);
    }

    private void setupButtonListeners() {
        applyFiltersButton.setOnClickListener(v -> applyFilters());
        backButton.setOnClickListener(v -> finish());
    }

    private void showDatePicker(boolean isStartDate) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, day) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(year, month, day);
                    Date selectedDate = selectedCalendar.getTime();
                    if (isStartDate) {
                        startDate = selectedDate;
                        startDateButton.setText(dateFormat.format(selectedDate));
                    } else {
                        endDate = selectedDate;
                        endDateButton.setText(dateFormat.format(selectedDate));
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void applyFilters() {
        String selectedType = null;
        String selectedGreekType = typeFilterSpinner.getSelectedItem().toString();

        if (!selectedGreekType.equals(getString(R.string.all))) {
            selectedType = convertGreekToEnglishType(selectedGreekType);
        }

        viewModel.loadStatistics(startDate, endDate, selectedType);
    }

    private String convertGreekToEnglishType(String greekType) {
        if (greekType.equals(getString(R.string.incident_type_fire))) return "fire";
        if (greekType.equals(getString(R.string.incident_type_flood))) return "flood";
        if (greekType.equals(getString(R.string.incident_type_earthquake))) return "earthquake";
        if (greekType.equals(getString(R.string.incident_type_storm))) return "storm";
        return "other";
    }

    private void setupObservers() {
        viewModel.getTotalIncidents().observe(this, total -> {
            totalIncidentsText.setText(getString(R.string.total_incidents, total != null ? total : 0));
        });

        viewModel.getTotalUsers().observe(this, total -> {
            totalUsersText.setText(getString(R.string.total_users, total != null ? total : 0));
        });

        viewModel.getIncidentsByType().observe(this, incidentsByType -> {
            typesContainer.removeAllViews();

            if (incidentsByType != null && !incidentsByType.isEmpty()) {
                for (Map.Entry<String, List<Incident>> entry : incidentsByType.entrySet()) {
                    String typeKey = entry.getKey();
                    List<Incident> incidents = entry.getValue();
                    MaterialCardView card = createDetailedTypeCard(typeKey, incidents);
                    typesContainer.addView(card);
                }
            } else {
                TextView noDataText = new TextView(this);
                noDataText.setText(getString(R.string.no_data));
                noDataText.setPadding(16, 16, 16, 16);
                typesContainer.addView(noDataText);
            }
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            applyFiltersButton.setEnabled(!isLoading);
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, getString(R.string.error_occurred, error), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void fetchRoleThenLoad() {
        progressBar.setVisibility(View.VISIBLE);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            isAdmin = false;
            roleLoaded = true;
            progressBar.setVisibility(View.GONE);
            viewModel.loadStatistics(null, null, null);
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String cached = prefs.getString(PREF_ROLE_PREFIX + uid, null);
        if (cached != null) {
            isAdmin = "admin".equals(cached);
            roleLoaded = true;
            progressBar.setVisibility(View.GONE);
            // now safe to load statistics and render cards appropriately
            viewModel.loadStatistics(null, null, null);
            return;
        }

        // fetch from Firestore
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        if (role != null) {
                            isAdmin = "admin".equals(role);
                            // cache it
                            prefs.edit().putString(PREF_ROLE_PREFIX + uid, role).apply();
                        } else {
                            isAdmin = false;
                        }
                    } else {
                        isAdmin = false;
                    }
                    roleLoaded = true;
                    viewModel.loadStatistics(null, null, null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch user role", e);
                    // fallback to non-admin so we are safe
                    isAdmin = false;
                    roleLoaded = true;
                    progressBar.setVisibility(View.GONE);
                    viewModel.loadStatistics(null, null, null);
                });
    }

    private MaterialCardView createDetailedTypeCard(String typeKey, List<Incident> incidents) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 16);
        card.setLayoutParams(cardParams);
        card.setRadius(12);
        card.setCardElevation(4f);
        card.setStrokeWidth(2);
        card.setStrokeColor(ContextCompat.getColor(this, R.color.primaryColor));

        LinearLayout innerLayout = new LinearLayout(this);
        innerLayout.setOrientation(LinearLayout.VERTICAL);
        innerLayout.setPadding(16, 16, 16, 16);
        card.addView(innerLayout);

        TextView typeTitle = new TextView(this);
        typeTitle.setText(getTypeDisplayName(typeKey));
        typeTitle.setTextSize(18);
        typeTitle.setTypeface(null, Typeface.BOLD);
        innerLayout.addView(typeTitle);

        TextView countText = new TextView(this);
        countText.setText(getString(R.string.incidents_count, incidents.size()));
        countText.setTextSize(16);
        innerLayout.addView(countText);

        for (int i = 0; i < Math.min(3, incidents.size()); i++) {
            Incident inc = incidents.get(i);
            String location = inc.getLocation() != null ? inc.getLocation() : "Unknown";
            String date = formatDateForDisplay(inc.getTimestamp());

            TextView incidentInfo = new TextView(this);
            incidentInfo.setText(String.format(Locale.getDefault(), "📍 %s\n📅 %s", location, date));
            incidentInfo.setTextSize(14);
            incidentInfo.setPadding(0, 8, 0, 8);
            innerLayout.addView(incidentInfo);
        }

        if (incidents.size() > 3) {
            TextView moreText = new TextView(this);
            moreText.setText(String.format(Locale.getDefault(), "+ %d περιστατικά", incidents.size() - 3));
            moreText.setTextSize(12);
            moreText.setTypeface(null, Typeface.ITALIC);
            innerLayout.addView(moreText);
        }

        // Only enable clicks for admin
        if (roleLoaded && isAdmin) {
            card.setClickable(true);
            card.setFocusable(true);
            card.setOnClickListener(v -> {
                if (typeKey != null) {
                    Intent intent = new Intent(StatisticsActivity.this, IncidentDetailActivity.class);
                    intent.putExtra("incidentType", typeKey);
                    intent.putExtra("incidentCount", incidents.size());
                    startActivity(intent);
                }
            });
        } else {
            card.setClickable(false);
            card.setFocusable(false);
            // card.setAlpha(1.0f);
        }

        return card;
    }

    private String formatDateForDisplay(Date date) {
        if (date == null) return "Άγνωστη ημερομηνία";
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Europe/Athens"));
        return sdf.format(date);
    }

    private String getTypeDisplayName(String typeKey) {
        switch (typeKey) {
            case "fire": return getString(R.string.incident_type_fire);
            case "flood": return getString(R.string.incident_type_flood);
            case "earthquake": return getString(R.string.incident_type_earthquake);
            case "storm": return getString(R.string.incident_type_storm);
            default: return getString(R.string.incident_type_other);
        }
    }
}
