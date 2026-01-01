package com.example.healthmate;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private CardView waterCard, dietBtn, sleepCard, symptomBtn, weightBtn;
    private ImageView notificationIcon;
    private Button btnWaterReminder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();
        checkUser();

        // Initialize CardViews
        // Initialize CardViews (as CardView, not Button)
        CardView waterCard = findViewById(R.id.waterCard);
        CardView dietBtn = findViewById(R.id.dietBtn);
        CardView sleepCard = findViewById(R.id.sleepCard);
        CardView symptomBtn = findViewById(R.id.symptomBtn);
        CardView weightBtn = findViewById(R.id.weightBtn);

        // Initialize Buttons
        btnWaterReminder = findViewById(R.id.btnWaterReminder);

        // Initialize Notification Icon
        notificationIcon = findViewById(R.id.notificationIcon);

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
                startActivity(new Intent(this, DietGeneratorActivity.class))
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
            Intent intent = new Intent(DashboardActivity.this, ChatbotActivity.class);
            startActivity(intent);
        });

        emergencyBtn.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, EmergencyActivity.class);
            startActivity(intent);
        });

        reportsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(
                    DashboardActivity.this,
                    ReportsActivity.class
            );
            startActivity(intent);
        });

        // Set click listeners for buttons
        btnWaterReminder.setOnClickListener(v -> startWaterReminder());

        // Notification Icon click listener
        if (notificationIcon != null) {
            notificationIcon.setOnClickListener(v -> openNotifications());
        }
    }

    // Method to open Notifications Activity
    private void openNotifications() {
        Intent intent = new Intent(DashboardActivity.this, NotificationHelper.class);
        startActivity(intent);
    }

    // Water Reminder Method
    private void startWaterReminder() {
        try {
            // Create notification channel (for Android 8.0+)
            NotificationHelper.createChannel(this);

            // Create intent for BroadcastReceiver
            Intent intent = new Intent(this, WaterReminderReceiver.class);

            // Create PendingIntent
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Get AlarmManager
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

            // Set interval (30 minutes)
            long interval = 5 * 1000; // 1 minute in milliseconds

            // Set repeating alarm
            if (alarmManager != null) {
                alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + interval,
                        interval,
                        pendingIntent
                );

                Toast.makeText(this, "💧 Water reminder started!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to start water reminder: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
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
        Button logoutBtn = findViewById(R.id.logoutBtn);

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
                                welcomeText.setText("Welcome back, ");
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

        return Math.min(score, 100);
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
        mAuth = null;
        userRef = null;
    }
}