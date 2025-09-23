package com.example.smartalert.presentation.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import android.widget.AutoCompleteTextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.core.content.ContextCompat;

import com.example.smartalert.domain.model.Incident;
import com.google.android.material.card.MaterialCardView;
import android.graphics.Typeface;

import com.example.smartalert.R;
import com.example.smartalert.presentation.viewmodels.StatisticsViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatisticsActivity extends AppCompatActivity {

    private StatisticsViewModel viewModel;
    private TextView totalIncidentsText, totalUsersText;
    private LinearLayout typesContainer; // <-- ΝΕΟ: Container για τις κάρτες τύπων
    private TextView incidentsByLocationText; // <-- Διατηρείται για τη λίστα τοποθεσιών
    private AutoCompleteTextView typeFilterSpinner;
    private Button startDateButton, endDateButton, applyFiltersButton, backButton;
    private ProgressBar progressBar;

    private Date startDate = null;
    private Date endDate = null;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        viewModel = new ViewModelProvider(this).get(StatisticsViewModel.class);
        initViews();
        setupSpinner();
        typeFilterSpinner = findViewById(R.id.typeFilterSpinner);
        setupObservers();
        setupButtonListeners();

        viewModel.loadStatistics(null, null, null);
    }

    private void initViews() {
        totalIncidentsText = findViewById(R.id.totalIncidents);
        totalUsersText = findViewById(R.id.totalUsers);
        typesContainer = findViewById(R.id.typesContainer);
        incidentsByLocationText = findViewById(R.id.incidentsByLocation);
        typeFilterSpinner = findViewById(R.id.typeFilterSpinner);
        startDateButton = findViewById(R.id.startDateButton);
        endDateButton = findViewById(R.id.endDateButton);
        applyFiltersButton = findViewById(R.id.applyFiltersButton);
        backButton = findViewById(R.id.backButton);
        progressBar = findViewById(R.id.progressBar);

        // Αρχικοποίηση κειμένων κουμπιών
        startDateButton.setText(R.string.start_date);
        endDateButton.setText(R.string.end_date);
        applyFiltersButton.setText(R.string.apply_filters);
        backButton.setText(R.string.back_to_main);

        // Ρύθμιση date pickers
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
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, incidentTypes);
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
        String selectedGreekType = typeFilterSpinner.getText().toString();

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

        // Παραμένει ίδιο
        viewModel.getIncidentsByLocation().observe(this, incidentsByLocation -> {
            StringBuilder sb = new StringBuilder();
            sb.append(getString(R.string.incidents_by_location)).append("\n");
            if (incidentsByLocation != null && !incidentsByLocation.isEmpty()) {
                for (Map.Entry<String, Integer> entry : incidentsByLocation.entrySet()) {
                    String locationName = entry.getKey();
                    sb.append("• ").append(locationName).append(": ").append(entry.getValue()).append("\n");
                }
            } else {
                sb.append(getString(R.string.no_data));
            }
            incidentsByLocationText.setText(sb.toString());
        });

        // ΝΕΟΣ OBSERVER: Για τα πραγματικά περιστατικά ανά τύπο
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

        // Observer για το count (μπορεί να χρησιμοποιηθεί αν θέλουμε να δείξουμε μόνο τον αριθμό)
        viewModel.getIncidentsByTypeCount().observe(this, countMap -> {
            // Μπορούμε να το χρησιμοποιήσουμε για κάτι άλλο, π.χ. γράφημα
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

    // ΝΕΑ ΜΕΘΟΔΟΣ: Δημιουργεί μια κάρτα με λεπτομερείς πληροφορίες
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

        // Τίτλος τύπου
        TextView typeTitle = new TextView(this);
        typeTitle.setText(getString(getTypeStringResource(typeKey)));
        typeTitle.setTextSize(18);
        typeTitle.setTypeface(null, Typeface.BOLD);
        innerLayout.addView(typeTitle);

        // Αριθμός περιστατικών
        TextView countText = new TextView(this);
        countText.setText(getString(R.string.incidents_count, incidents.size()));
        countText.setTextSize(16);
        innerLayout.addView(countText);

        // Λίστα με τα πρώτα 3 περιστατικά (για συντομία)
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
            moreText.setText(String.format(Locale.getDefault(), "+ %d more incidents", incidents.size() - 3));
            moreText.setTextSize(12);
            moreText.setTypeface(null, Typeface.ITALIC);
            innerLayout.addView(moreText);
        }

        return card;
    }

    // Βοηθητική μέθοδος για φιλική ημερομηνία
    private String formatDateForDisplay(Date date) {
        if (date == null) return "Unknown date";
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        return sdf.format(date);
    }

    private int getTypeStringResource(String typeKey) {
        switch (typeKey) {
            case "fire": return R.string.incident_type_fire;
            case "flood": return R.string.incident_type_flood;
            case "earthquake": return R.string.incident_type_earthquake;
            case "storm": return R.string.incident_type_storm;
            default: return R.string.incident_type_other;
        }
    }
}