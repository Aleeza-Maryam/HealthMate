package com.example.healthmate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ProfileActivity extends AppCompatActivity {

    EditText name, age, weight, height;
    TextView genderText;
    LinearLayout genderContainer;
    Button saveBtn;

    DatabaseReference userRef;
    String uid;
    String selectedGender = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // 🔐 SAFETY CHECK
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(uid);

        // Initialize views
        name = findViewById(R.id.fullName);
        age = findViewById(R.id.age);
        weight = findViewById(R.id.weight);
        height = findViewById(R.id.height);
        saveBtn = findViewById(R.id.saveProfileBtn);
        genderText = findViewById(R.id.genderText);
        genderContainer = findViewById(R.id.genderContainer);

        // Back button
        ImageView backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> onBackPressed());

        // Gender dropdown
        final String[] genders = {"Male", "Female", "Other"};

        genderContainer.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
            builder.setTitle("Select Gender")
                    .setItems(genders, (dialog, which) -> {
                        selectedGender = genders[which];
                        genderText.setText(selectedGender);
                        genderText.setTextColor(getResources().getColor(android.R.color.black));
                    });
            builder.create().show();
        });

        saveBtn.setOnClickListener(v -> {
            String n = name.getText().toString().trim();
            String a = age.getText().toString().trim();
            String w = weight.getText().toString().trim();
            String h = height.getText().toString().trim();

            if (n.isEmpty() || a.isEmpty() || selectedGender.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate age
            try {
                int ageValue = Integer.parseInt(a);
                if (ageValue < 1 || ageValue > 120) {
                    Toast.makeText(this, "Please enter a valid age (1-120)", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid age", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save to Firebase
            userRef.child("fullName").setValue(n);
            userRef.child("age").setValue(a);
            userRef.child("gender").setValue(selectedGender);

            if (!w.isEmpty()) {
                userRef.child("weight").setValue(w);
            }

            if (!h.isEmpty()) {
                userRef.child("height").setValue(h);
            }

            userRef.child("profileCompleted").setValue(true);

            Toast.makeText(this, "Profile saved successfully!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        });
    }
}