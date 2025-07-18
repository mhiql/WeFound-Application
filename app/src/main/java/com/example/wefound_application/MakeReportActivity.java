package com.example.wefound_application;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONObject;

import okhttp3.*;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.UUID;



public class MakeReportActivity extends AppCompatActivity {

    private ImageView imgUpload;
    private Uri imageUri;
    private FirebaseStorage storage;
    private FirebaseFirestore firestore;
    private StorageReference storageRef;
    private EditText etDate;

    private Spinner spinnerCategory, spinnerType;

    private ActivityResultLauncher<String> filePickerLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;

    // At the top (class-level)
    private EditText etLocation;
    private ImageView iconMap;
    private ActivityResultLauncher<Intent> mapPickerLauncher;

    private ActivityResultLauncher<String> locationPermissionLauncher;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_report);

        // Show back button in the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Report Item"); // Optional title
        }


        // Init Firebase
        storage = FirebaseStorage.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storageRef = storage.getReference();

        // Bind UI
        imgUpload = findViewById(R.id.imgUpload);
        etDate = findViewById(R.id.etDate);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerType = findViewById(R.id.spinnerType);

        etLocation = findViewById(R.id.etLocation);
        iconMap = findViewById(R.id.iconMap);


// 👇 Register activity launcher for result from MapPickerActivity
        mapPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String selectedLocation = result.getData().getStringExtra("selectedLocation");
                        etLocation.setText(selectedLocation);
                    }
                });

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // If permission granted, open MapPicker
                        Intent intent = new Intent(MakeReportActivity.this, MapPickerActivity.class);
                        mapPickerLauncher.launch(intent);
                    } else {
                        Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
                    }
                });


// 👇 When iconMap is clicked, open the map picker
        iconMap.setOnClickListener(v -> {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        });



        // Spinner setup
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Lost", "Found"});
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);

        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = spinnerType.getSelectedItem().toString();
                if (selectedType.equalsIgnoreCase("Found")) {
                    showPhoneNumberDialog();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });


        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Electronics", "Clothing", "Accessories", "Others"});
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        // Date picker
        etDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                etDate.setText(day + "/" + (month + 1) + "/" + year);
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Activity Launchers
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        imgUpload.setImageURI(uri);
                        imageUri = uri;
                    }
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && imageUri != null) {
                        imgUpload.setImageURI(imageUri);
                    }
                });



        imgUpload.setOnClickListener(v -> showImageSelectionDialog());

        findViewById(R.id.btnSubmit).setOnClickListener(v -> uploadReport());
        findViewById(R.id.btnCancel).setOnClickListener(v -> resetForm());

    }

    @Override
    public boolean onSupportNavigateUp() {
        finish(); // Close the current activity
        return true;
    }


    private String phoneNumber = null; // Class-level variable

    private void showPhoneNumberDialog() {
        EditText input = new EditText(this);
        input.setHint("e.g. 0179860946");

        new android.app.AlertDialog.Builder(this)
                .setTitle("Enter Phone Number")
                .setMessage("Since you selected 'Found', please provide your contact number:")
                .setView(input)
                .setPositiveButton("Submit", (dialog, which) -> {
                    phoneNumber = input.getText().toString().trim();
                    if (phoneNumber.length() < 10) {
                        Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
                        phoneNumber = null;
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    phoneNumber = null;
                    spinnerType.setSelection(0); // Reset to "Lost"
                })
                .show();
    }


    private void showImageSelectionDialog() {
        String[] options = {"Choose from File", "Take Photo"};
        new android.app.AlertDialog.Builder(this)
                .setTitle("Select Image")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        filePickerLauncher.launch("image/*");
                    } else if (which == 1) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            launchCamera();
                        } else {
                            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
                        }
                    }
                }).show();
    }

    private void launchCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        cameraLauncher.launch(imageUri);
    }

    private void uploadReport() {
        if (imageUri == null) {
            Toast.makeText(this, "Please select or capture an image", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = "reports/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference imgRef = storageRef.child(fileName);

        imgRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> imgRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    saveReportToFirestore(uri.toString());
                }))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );

    }
    private void resetForm() {
        ((EditText) findViewById(R.id.etItemName)).setText("");
        ((EditText) findViewById(R.id.etDescription)).setText("");
        ((EditText) findViewById(R.id.etLocation)).setText("");
        etDate.setText("");
        spinnerCategory.setSelection(0);
        spinnerType.setSelection(0);
        imgUpload.setImageResource(0); // clear image
        imageUri = null;
        phoneNumber = null;
    }


    private void sendPushNotificationToTopic(String title, String message) {
        String serverKey = "YOUR_SERVER_KEY_HERE"; // 🔴 NEVER put in production app
        String fcmUrl = "https://fcm.googleapis.com/fcm/send";

        OkHttpClient client = new OkHttpClient();

        JSONObject json = new JSONObject();
        try {
            json.put("to", "/topics/all_users");
            JSONObject notification = new JSONObject();
            notification.put("title", title);
            notification.put("body", message);
            json.put("notification", notification);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        RequestBody body = RequestBody.create(
                json.toString(), MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(fcmUrl)
                .post(body)
                .addHeader("Authorization", "key=" + serverKey)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("FCM", "Failed to send notification", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("FCM", "Notification sent: " + response.body().string());
            }
        });


    }


    private void saveReportToFirestore(String imageUrl) {

        String itemName = ((EditText) findViewById(R.id.etItemName)).getText().toString();
        String description = ((EditText) findViewById(R.id.etDescription)).getText().toString();
        String location = ((EditText) findViewById(R.id.etLocation)).getText().toString();
        String date = etDate.getText().toString();
        String category = spinnerCategory.getSelectedItem().toString();
        String type = spinnerType.getSelectedItem().toString();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid(); // ✅ Add this


        HashMap<String, Object> report = new HashMap<>();
        report.put("itemName", itemName);
        report.put("description", description);
        report.put("location", location);
        report.put("date", date);
        report.put("category", category);
        report.put("type", type);
        report.put("imageUrl", imageUrl);
        report.put("timestamp", System.currentTimeMillis());
        report.put("userId", FirebaseAuth.getInstance().getCurrentUser().getUid());

        if ("Found".equalsIgnoreCase(type)) {
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                Toast.makeText(this, "Phone number is required for 'Found' items", Toast.LENGTH_SHORT).show();
                return;
            }
            report.put("phoneNumber", phoneNumber);
            report.put("whatsappLink", "https://wa.me/6" + phoneNumber); // e.g. https://wa.me/60179860946
        }


        firestore.collection("reports")
                .add(report)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Report submitted!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to submit: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }




}