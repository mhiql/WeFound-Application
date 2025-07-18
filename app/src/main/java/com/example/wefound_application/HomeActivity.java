package com.example.wefound_application;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.PopupMenu;


public class HomeActivity extends AppCompatActivity {

    FirebaseAuth auth;
    FirebaseUser user;
    TextView profileText;
    MaterialButton btnSignOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        profileText = findViewById(R.id.welcomeText);
        btnSignOut = findViewById(R.id.btnSignOut);

        user = auth.getCurrentUser();
        if (user != null) {
            profileText.setText("Hi, welcome back!");
        }

        ImageView imgProfile = findViewById(R.id.imgProfile);
        imgProfile.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(HomeActivity.this, v);
            MenuInflater inflater = popupMenu.getMenuInflater();
            inflater.inflate(R.menu.profile_menu_profile, popupMenu.getMenu());

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                popupMenu.getMenu().getItem(0).setTitle("👤 " + user.getEmail());
            };

            popupMenu.show();
        });


        // Sign Out Button Logic (safe version)
        btnSignOut.setOnClickListener(v -> signout());

        // Other buttons
        findViewById(R.id.btnViewList).setOnClickListener(v ->
                startActivity(new Intent(this, ViewItemsActivity.class)));

        findViewById(R.id.btnMakeReport).setOnClickListener(v ->
                startActivity(new Intent(this, MakeReportActivity.class)));

        findViewById(R.id.btnMyReport).setOnClickListener(v ->
                startActivity(new Intent(this, MyReportActivity.class)));

        findViewById(R.id.btnAbout).setOnClickListener(v ->
                startActivity(new Intent(this, AboutActivity.class)));
    }

    public void signout() {
        Toast.makeText(this, "Signing out", Toast.LENGTH_SHORT).show();
        FirebaseAuth.getInstance().signOut();

        // Clear backstack and redirect to MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

}