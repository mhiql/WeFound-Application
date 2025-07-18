package com.example.wefound_application;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.maps.android.PolyUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng selectedLatLng;
    private String selectedLocationName = "";
    private Polygon uitmPolygon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        findViewById(R.id.btnConfirmLocation).setOnClickListener(v -> {
            if (selectedLatLng != null) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Confirm Location")
                        .setMessage("Are you sure you want to proceed with this location?\n\n" + selectedLocationName)
                        .setPositiveButton("CONFIRM", (dialog, which) -> {
                            String locationText = selectedLatLng.latitude + ", " + selectedLatLng.longitude;
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("selectedLocation", locationText);
                            resultIntent.putExtra("locationName", selectedLocationName);
                            setResult(RESULT_OK, resultIntent);
                            finish();
                        })
                        .setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss())
                        .show();
            } else {
                Toast.makeText(this, "Please tap on the map to select a location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng center = new LatLng(3.0738, 101.5035);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 16));

        uitmPolygon = mMap.addPolygon(new PolygonOptions()
                .addAll(getUiTMPolygonPoints())
                .strokeColor(0xFF00AAFF)
                .fillColor(0x3300AAFF)
                .strokeWidth(3f));

        mMap.setOnMapClickListener(latLng -> {
            if (PolyUtil.containsLocation(latLng, uitmPolygon.getPoints(), false)) {
                selectedLatLng = latLng;

                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        selectedLocationName = address.getAddressLine(0) != null ?
                                address.getAddressLine(0) : "Unnamed location";
                    } else {
                        selectedLocationName = "Unknown location";
                    }
                } catch (Exception e) {
                    selectedLocationName = "Error getting location name";
                    e.printStackTrace();
                }

                new androidx.appcompat.app.AlertDialog.Builder(MapPickerActivity.this)
                        .setTitle("Confirm Location")
                        .setMessage("Are you sure you want to proceed with this location?\n\n" + selectedLocationName)
                        .setPositiveButton("CONFIRM", (dialog, which) -> {
                            mMap.clear();
                            uitmPolygon = mMap.addPolygon(new PolygonOptions()
                                    .addAll(getUiTMPolygonPoints())
                                    .strokeColor(0xFF00AAFF)
                                    .fillColor(0x3300AAFF)
                                    .strokeWidth(3f));
                            mMap.addMarker(new MarkerOptions()
                                    .position(selectedLatLng)
                                    .title(selectedLocationName));
                        })
                        .setNegativeButton("CANCEL", (dialog, which) -> selectedLatLng = null)
                        .show();

            } else {
                Toast.makeText(this, "Please select a location inside UiTM Shah Alam", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<LatLng> getUiTMPolygonPoints() {
        List<LatLng> points = new ArrayList<>();

        points.add(new LatLng(3.067650, 101.491551)); // backgate uitm
        points.add(new LatLng(3.069741, 101.491705)); // bus parking uitm
        points.add(new LatLng(3.069536, 101.493851)); //
        points.add(new LatLng(3.074385, 101.497764 )); // belakang engineering
        points.add(new LatLng(3.071266, 101.501642)); //
        points.add(new LatLng(3.070718, 101.504543)); // maingate uitm
        points.add(new LatLng(3.069998, 101.505967)); // jalan ilmu
        points.add(new LatLng(3.067353, 101.506886 )); //
        points.add(new LatLng(3.065172, 101.508416)); //
        points.add(new LatLng(3.063990, 101.508600)); //
        points.add(new LatLng(3.064357, 101.491844)); //
        points.add(new LatLng(3.064357, 101.491844)); // main gate Close polygon

        return points;
    }




}