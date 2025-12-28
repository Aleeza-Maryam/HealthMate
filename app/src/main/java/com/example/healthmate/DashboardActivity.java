package com.example.healthmate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

public class DashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private CardView waterCard, dietBtn, sleepCard, symptomBtn, weightBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();
        checkUser();

        // Initialize CardViews (NOT Buttons)
        waterCard = findViewById(R.id.waterCard);
        dietBtn = findViewById(R.id.dietBtn);
        sleepCard = findViewById(R.id.sleepCard);
        symptomBtn = findViewById(R.id.symptomBtn);
        weightBtn = findViewById(R.id.weightBtn);

        // Set click listeners for CardViews
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

        sleepCard.setOnClickListener(v ->
                startActivity(new Intent(this, SleepActivity.class))
        );

        symptomBtn.setOnClickListener(v -> {
            Intent intent = new Intent(
                    DashboardActivity.this,
                    SymptomActivity.class
            );
            startActivity(intent);
        });

        weightBtn.setOnClickListener(v -> {
            Intent intent = new Intent(
                    DashboardActivity.this,
                    WeightActivityActivity.class
            );
            startActivity(intent);
        });

        // Initialize other cards
        CardView profileBtn = findViewById(R.id.profileBtn);
        CardView chatbotBtn = findViewById(R.id.chatbotBtn);
        CardView emergencyBtn = findViewById(R.id.emergencyBtn);
        CardView reportsBtn = findViewById(R.id.reportsBtn);

        profileBtn.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class))
        );

        chatbotBtn.setOnClickListener(v -> {
            Toast.makeText(this, "Chatbot feature coming soon", Toast.LENGTH_SHORT).show();
        });

        emergencyBtn.setOnClickListener(v -> {
            Toast.makeText(this, "Emergency contact: 112", Toast.LENGTH_SHORT).show();
        });

        reportsBtn.setOnClickListener(v -> {
            Toast.makeText(this, "Reports feature coming soon", Toast.LENGTH_SHORT).show();
        });
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
        TextView welcomeText = findViewById(R.id.welcomeText);
        Button logoutBtn = findViewById(R.id.logoutBtn); // This is a Button in XML

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
                                welcomeText.setText("Welcome back, " + name.split(" ")[0] + "!");
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        // Silent fail - keep default "User"
                    }
                });

        // Setup logout button click listener
        logoutBtn.setOnClickListener(v -> logoutUser());

        // Load today's score and steps
        loadTodayData(uid);
    }

    private void loadTodayData(String uid) {
        DatabaseReference healthRef = FirebaseDatabase.getInstance()
                .getReference("DailyHealth").child(uid);

        healthRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                TextView todaysScore = findViewById(R.id.todaysScore);
                TextView stepsToday = findViewById(R.id.stepsToday);

                if (snapshot.exists()) {
                    // Calculate score based on health data
                    int score = calculateHealthScore(snapshot);
                    todaysScore.setText(String.valueOf(score));

                    // Get steps
                    String steps = snapshot.child("steps").getValue(String.class);
                    if (steps != null && !steps.isEmpty()) {
                        stepsToday.setText(formatSteps(steps));
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Handle error
            }
        });
    }

    private int calculateHealthScore(DataSnapshot snapshot) {
        int score = 0;

        // Check water intake
        if (snapshot.child("water").exists()) {
            Integer water = snapshot.child("water").getValue(Integer.class);
            if (water != null && water >= 8) score += 30;
            else if (water != null && water >= 4) score += 20;
        }

        // Check steps
        if (snapshot.child("steps").exists()) {
            String stepsStr = snapshot.child("steps").getValue(String.class);
            try {
                int steps = Integer.parseInt(stepsStr);
                if (steps >= 10000) score += 40;
                else if (steps >= 5000) score += 25;
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        // Check weight tracking
        if (snapshot.child("weight").exists()) {
            score += 20;
        }

        // Check sleep
        if (snapshot.child("sleep").exists()) {
            score += 10;
        }

        return Math.min(score, 100); // Cap at 100
    }

    private String formatSteps(String steps) {
        try {
            int stepsInt = Integer.parseInt(steps);
            if (stepsInt >= 1000) {
                return String.format("%.1fk", stepsInt / 1000.0);
            }
            return steps;
        } catch (NumberFormatException e) {
            return "0";
        }
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