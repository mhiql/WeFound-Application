package com.example.wefound_application;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

public class MyReportActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ArrayList<ImageView> imageViews;
    private ArrayList<DocumentSnapshot> documents;
    private GridLayout imageGrid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_report);

        db = FirebaseFirestore.getInstance();
        imageViews = new ArrayList<>();
        documents = new ArrayList<>();
        imageGrid = findViewById(R.id.imageGrid);

        imageViews.add(findViewById(R.id.img1));
        imageViews.add(findViewById(R.id.img2));
        imageViews.add(findViewById(R.id.img3));
        imageViews.add(findViewById(R.id.img4));
        imageViews.add(findViewById(R.id.img5));
        imageViews.add(findViewById(R.id.img6));

        loadImagesFromFirestore();
    }

    private void loadImagesFromFirestore() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("reports")
                .whereEqualTo("userId", currentUserId)
                .limit(6)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int index = 0;

                    // Hide all first
                    for (ImageView img : imageViews) {
                        img.setVisibility(View.GONE);
                    }

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        if (index >= imageViews.size()) break;

                        String imageUrl = doc.getString("imageUrl");
                        documents.add(doc);

                        ImageView imgView = imageViews.get(index);
                        imgView.setVisibility(View.VISIBLE);

                        Glide.with(this).load(imageUrl)
                                .placeholder(R.drawable.border_square)
                                .into(imgView);

                        imgView.setOnClickListener(v -> showSelectionDialog(doc));

                        index++;
                    }

                    if (index == 0) {
                        imageGrid.setVisibility(View.GONE);
                        Toast.makeText(this, "No reports yet for your account.", Toast.LENGTH_SHORT).show();
                    } else {
                        imageGrid.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load images", Toast.LENGTH_SHORT).show()
                );
    }

    private void showSelectionDialog(DocumentSnapshot doc) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Selection")
                .setItems(new CharSequence[]{"View Report", "Edit Report", "Delete Report"}, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            viewReport(doc);
                            break;
                        case 1:
                            editReport(doc);
                            break;
                        case 2:
                            confirmDelete(doc);
                            break;
                    }
                })
                .show();
    }

    private void viewReport(DocumentSnapshot doc) {
        Intent intent = new Intent(this, ViewDetailsActivity.class);
        intent.putExtra("reportId", doc.getId());
        startActivity(intent);
    }

    private void editReport(DocumentSnapshot doc) {
        Intent intent = new Intent(this, EditReportActivity.class);
        intent.putExtra("documentId", doc.getId());
        startActivity(intent);
    }

    private void confirmDelete(DocumentSnapshot doc) {
        new AlertDialog.Builder(this)
                .setTitle("Are you sure?")
                .setMessage("This report will be permanently deleted.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> deleteReport(doc))
                .show();
    }

    private void deleteReport(DocumentSnapshot doc) {
        String docId = doc.getId();
        String imageUrl = doc.getString("imageUrl");

        db.collection("reports").document(docId)
                .delete()
                .addOnSuccessListener(unused -> {
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
                        imageRef.delete();
                    }
                    Toast.makeText(this, "Report deleted", Toast.LENGTH_SHORT).show();
                    recreate();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
