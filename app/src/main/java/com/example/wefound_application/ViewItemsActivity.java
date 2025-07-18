package com.example.wefound_application;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.PopupMenu;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class ViewItemsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private ReportAdapter adapter;
    private ArrayList<ReportItem> reportList;
    private Spinner filterSpinner;
    private ListenerRegistration listenerRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_items);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recyclerReports);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        filterSpinner = findViewById(R.id.spinnerFilter);
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"All", "Lost", "Found"});
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(filterAdapter);

        reportList = new ArrayList<>();
        adapter = new ReportAdapter(this, reportList);
        recyclerView.setAdapter(adapter);

        findViewById(R.id.btnMakeReport).setOnClickListener(v -> {
            startActivity(new Intent(this, MakeReportActivity.class));
        });




        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                loadReports(selected);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                loadReports("All");
            }
        });
    }

    private void loadReports(String filter) {
        Query query = db.collection("reports").orderBy("timestamp", Query.Direction.DESCENDING);

        if (filter.equals("Lost") || filter.equals("Found")) {
            query = query.whereEqualTo("type", filter);
        }

        // Remove any existing listener
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }

        // Real-time update listener
        listenerRegistration = query.addSnapshotListener((querySnapshot, error) -> {
            if (error != null) {
                Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("ViewItems", "Snapshot error", error);
                return;
            }

            reportList.clear();
            for (QueryDocumentSnapshot doc : querySnapshot) {
                String imageUrl = doc.getString("imageUrl");
                String docId = doc.getId();
                String type = doc.getString("type");

                if (imageUrl != null && type != null) {
                    reportList.add(new ReportItem(imageUrl, docId, type));
                }
            }

            adapter.notifyDataSetChanged();

            if (reportList.isEmpty()) {
                Toast.makeText(this, "No reports found.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}
