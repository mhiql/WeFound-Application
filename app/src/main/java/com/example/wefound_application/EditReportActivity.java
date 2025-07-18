package com.example.wefound_application;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class EditReportActivity extends AppCompatActivity {

    private EditText etItemName, etDescription, etLocation, etDate;
    private Spinner spinnerCategory, spinnerType;
    private ImageView imgReport, iconMap;
    private Button btnUpdate;

    private FirebaseFirestore db;
    private String documentId;
    private String imageUrl;

    private ActivityResultLauncher<Intent> mapPickerLauncher;
    private ActivityResultLauncher<String> locationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_report);

        etItemName = findViewById(R.id.etItemName);
        etDescription = findViewById(R.id.etDescription);
        etLocation = findViewById(R.id.etLocation);
        etDate = findViewById(R.id.etDate);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerType = findViewById(R.id.spinnerType);
        imgReport = findViewById(R.id.imgReport);
        iconMap = findViewById(R.id.iconMap); // added map icon
        btnUpdate = findViewById(R.id.btnUpdate);

        db = FirebaseFirestore.getInstance();
        documentId = getIntent().getStringExtra("documentId");

        // Setup dropdown
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Lost", "Found"}
        );
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Electronics", "Accessories", "Books", "Others"}
        );
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        // Date picker
        etDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) ->
                    etDate.setText(day + "/" + (month + 1) + "/" + year),
                    calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        // Launch map picker
        mapPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String selectedLocation = result.getData().getStringExtra("selectedLocation");
                        etLocation.setText(selectedLocation);
                    }
                }
        );

        // Location permission
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Intent intent = new Intent(EditReportActivity.this, MapPickerActivity.class);
                        mapPickerLauncher.launch(intent);
                    } else {
                        Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Map icon click
        iconMap.setOnClickListener(v ->
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        );

        if (documentId != null) {
            loadReportData(documentId);
        }

        btnUpdate.setOnClickListener(v -> updateReport());
    }

    private void loadReportData(String docId) {
        DocumentReference docRef = db.collection("reports").document(docId);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                etItemName.setText(documentSnapshot.getString("itemName"));
                etDescription.setText(documentSnapshot.getString("description"));
                etLocation.setText(documentSnapshot.getString("location"));
                etDate.setText(documentSnapshot.getString("date"));

                String category = documentSnapshot.getString("category");
                String type = documentSnapshot.getString("type");
                imageUrl = documentSnapshot.getString("imageUrl");

                ArrayAdapter categoryAdapter = (ArrayAdapter) spinnerCategory.getAdapter();
                spinnerCategory.setSelection(categoryAdapter.getPosition(category));

                ArrayAdapter typeAdapter = (ArrayAdapter) spinnerType.getAdapter();
                spinnerType.setSelection(typeAdapter.getPosition(type));

                Glide.with(this).load(imageUrl).into(imgReport);
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to load report", Toast.LENGTH_SHORT).show()
        );
    }

    private void updateReport() {
        String itemName = etItemName.getText().toString();
        String description = etDescription.getText().toString();
        String location = etLocation.getText().toString();
        String date = etDate.getText().toString();
        String category = spinnerCategory.getSelectedItem().toString();
        String type = spinnerType.getSelectedItem().toString();

        if (itemName.isEmpty() || description.isEmpty() || location.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("itemName", itemName);
        updatedData.put("description", description);
        updatedData.put("location", location);
        updatedData.put("date", date);
        updatedData.put("category", category);
        updatedData.put("type", type);

        db.collection("reports").document(documentId)
                .update(updatedData)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Report updated", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
