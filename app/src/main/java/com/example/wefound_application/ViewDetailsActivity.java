package com.example.wefound_application;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class ViewDetailsActivity extends AppCompatActivity {

    private TextView tvItemName, tvCategory, tvDate, tvDescription, tvType;
    private ImageView imgReport;
    private FirebaseFirestore db;
    private String reportId;

    private MapView mapView;
    private GoogleMap gMap;
    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_details);

        imgReport = findViewById(R.id.imgReport);
        tvItemName = findViewById(R.id.tvItemName);
        tvCategory = findViewById(R.id.tvCategory);
        tvDate = findViewById(R.id.tvDate);
        tvDescription = findViewById(R.id.tvDescription);
        tvType = findViewById(R.id.tvType);
        mapView = findViewById(R.id.mapView);

        Button btnCall = findViewById(R.id.btnCall);
        Button btnWhatsapp = findViewById(R.id.btnText);

        db = FirebaseFirestore.getInstance();
        reportId = getIntent().getStringExtra("reportId");

        // Initialize MapView
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(googleMap -> {
            gMap = googleMap;
            if (reportId != null) {
                loadReportDetails(reportId); // Load details after map is ready
            }
        });


    }

    private void loadReportDetails(String reportId) {
        DocumentReference docRef = db.collection("reports").document(reportId);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                tvItemName.setText(documentSnapshot.getString("itemName"));
                tvCategory.setText(documentSnapshot.getString("category"));
                tvDate.setText(documentSnapshot.getString("date"));
                tvDescription.setText(documentSnapshot.getString("description"));
                tvType.setText(documentSnapshot.getString("type"));

                String imageUrl = documentSnapshot.getString("imageUrl");
                Glide.with(this).load(imageUrl).into(imgReport);

                // 🟩 Get buttons
                Button btnCall = findViewById(R.id.btnCall);
                Button btnWhatsapp = findViewById(R.id.btnText);

                // 🟩 Get phone and WhatsApp info from Firestore
                String type = documentSnapshot.getString("type");
                String phoneNumber = documentSnapshot.getString("phoneNumber");
                String whatsappLink = documentSnapshot.getString("whatsappLink");

                // 🟩 Show buttons only if it's a "Found" item
                if ("Found".equalsIgnoreCase(type)) {
                    btnCall.setVisibility(Button.VISIBLE);
                    btnWhatsapp.setVisibility(Button.VISIBLE);

                    btnCall.setOnClickListener(v -> {
                        if (phoneNumber != null && !phoneNumber.isEmpty()) {
                            Intent intent = new Intent(Intent.ACTION_DIAL);
                            intent.setData(Uri.parse("tel:" + phoneNumber));
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
                        }
                    });

                    btnWhatsapp.setOnClickListener(v -> {
                        if (whatsappLink != null && !whatsappLink.isEmpty()) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(whatsappLink));
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "WhatsApp link not available", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    btnCall.setVisibility(Button.GONE);
                    btnWhatsapp.setVisibility(Button.GONE);
                }

                // 🗺️ Parse and show map location
                String locationString = documentSnapshot.getString("location");
                if (locationString != null && locationString.contains(",")) {
                    String[] parts = locationString.split(",");
                    try {
                        double lat = Double.parseDouble(parts[0].trim());
                        double lng = Double.parseDouble(parts[1].trim());
                        LatLng reportLatLng = new LatLng(lat, lng);

                        if (gMap != null) {
                            gMap.clear();
                            gMap.addMarker(new MarkerOptions().position(reportLatLng).title("Reported Location"));
                            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(reportLatLng, 17));
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid coordinates format", Toast.LENGTH_SHORT).show();
                    }
                }

            } else {
                Toast.makeText(this, "Report not found", Toast.LENGTH_SHORT).show();
            }
        });
    }


    // MapView lifecycle
    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        mapView.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle);
        }

        mapView.onSaveInstanceState(mapViewBundle);
}
}