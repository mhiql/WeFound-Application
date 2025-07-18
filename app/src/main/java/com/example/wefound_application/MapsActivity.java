package com.example.wefound_application;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.wefound_application.databinding.ActivityMapsBinding;
import com.google.android.gms.tasks.Task;

import android.Manifest;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private FusedLocationProviderClient client;

    private Button btnConfirmLocation;
    private LatLng pickedLatLng;

    private LatLng currentLatLng;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 44;
    private static final String API_KEY = "";
    private ActivityMapsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        supportMapFragment.getMapAsync(this);

        client = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        Button btnMosque = findViewById(R.id.btnMosque);
        Button btnSchool = findViewById(R.id.btnSchool);
        Button btnHospital = findViewById(R.id.btnHospital);
        btnMosque.setOnClickListener(v -> searchNearby("mosque"));
        btnSchool.setOnClickListener(v -> searchNearby("school"));
        btnHospital.setOnClickListener(v -> searchNearby("hospital"));


        btnConfirmLocation = findViewById(R.id.btnConfirmLocation);
        btnConfirmLocation.setOnClickListener(v -> {
            if (pickedLatLng != null) {
                Toast.makeText(this, "Location confirmed: " + pickedLatLng.latitude + ", " + pickedLatLng.longitude, Toast.LENGTH_SHORT).show();

                // TODO: Do something with the picked location
                // For example: send it back to previous Activity or save it to Firestore
            }
        });


    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            getCurrentLocation();
        }

        mMap.setOnMapClickListener(latLng -> {
            pickedLatLng = latLng;

            // Clear previous markers
            mMap.clear();

            // Add new marker
            mMap.addMarker(new MarkerOptions().position(latLng).title("Selected Location"));

            // Move camera
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));

            // Show confirm button
            btnConfirmLocation.setVisibility(View.VISIBLE);
        });

    }


    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        client.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    // Always lock to UiTM Shah Alam, regardless of actual location
                    currentLatLng = new LatLng(3.0738, 101.5039); // UiTM coordinates

                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                    mMap.addMarker(new MarkerOptions().position(currentLatLng).title("UiTM Shah Alam"));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MapsActivity.this, "Unable to fetch location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mMap.setMyLocationEnabled(true);
                getCurrentLocation();
            }
        } else {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
        }
    }

    private void searchNearby(String type) {
        // Coordinates of UiTM Shah Alam
        double uitmLat = 3.0738;
        double uitmLng = 101.5039;

        // Radius in meters (e.g., 1500 meters ~ UiTM campus)
        int radius = 1500;

        // Force currentLatLng to UiTM Shah Alam
        currentLatLng = new LatLng(uitmLat, uitmLng);

        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=" + uitmLat + "," + uitmLng +
                "&radius=" + radius +
                "&type=" + type +
                "&key=" + API_KEY;

        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(currentLatLng).title("UiTM Shah Alam"));

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        for (int i = 0; i < Math.min(results.length(), 10); i++) {
                            JSONObject place = results.getJSONObject(i);
                            JSONObject location = place.getJSONObject("geometry").getJSONObject("location");
                            String name = place.getString("name");

                            LatLng latLng = new LatLng(location.getDouble("lat"), location.getDouble("lng"));
                            mMap.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title(name)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Error parsing results", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Request failed", Toast.LENGTH_SHORT).show());

        queue.add(request);

        Toast.makeText(this, "Searching nearby " + type + " in UiTM Shah Alam", Toast.LENGTH_SHORT).show();
    }

}