package com.example.healthmate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

public class DashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    CardView waterCard;
    CardView sleepcard;
    CardView dietBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();
        checkUser();
        waterCard = findViewById(R.id.waterCard);
        dietBtn = findViewById(R.id.dietBtn);
        waterCard.setOnClickListener(v -> {
            Intent intent = new Intent(
                    DashboardActivity.this,
                    HealthTrackingActivity.class
            );
            startActivity(intent);
        });
        dietBtn.setOnClickListener(v -> {
            Intent intent = new Intent(
                    DashboardActivity.this,
                    DietActivity.class
            );
            startActivity(intent);
        });
        CardView sleepCard = findViewById(R.id.sleepCard);
        sleepCard.setOnClickListener(v ->
                startActivity(new Intent(this, SleepActivity.class))
        );

    }


    private void checkUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initUI(currentUser.getUid());
    }

    private void initUI(String uid) {
        // Initialize views
        TextView userName = findViewById(R.id.userName);
        Button logoutBtn = findViewById(R.id.logoutBtn);
        CardView profileBtn = findViewById(R.id.profileBtn);

        // Setup database
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);

        // Load user data
        userRef.child("fullName").addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String name = snapshot.getValue(String.class);
                            if (name != null && !name.isEmpty()) {
                                userName.setText(name);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        // Silent fail - keep default "User"
                    }
                });

        // Setup click listeners
        profileBtn.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class))
        );


        logoutBtn.setOnClickListener(v -> logoutUser());
    }

    private void logoutUser() {
        mAuth.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up to prevent memory leaks
        mAuth = null;
        userRef = null;
    }
}