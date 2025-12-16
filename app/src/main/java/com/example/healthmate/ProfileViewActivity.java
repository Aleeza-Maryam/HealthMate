package com.example.healthmate;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileViewActivity extends AppCompatActivity {

    TextView name, age, gender, weight, height;
    ImageView profileImage;

    DatabaseReference userRef;
    String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_view);

        // Initialize views
        name = findViewById(R.id.nameText);
        age = findViewById(R.id.ageText);
        gender = findViewById(R.id.genderText);
        weight = findViewById(R.id.weightText);
        height = findViewById(R.id.heightText);
        profileImage = findViewById(R.id.profileImage);

        // Initialize Firebase
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        // Safety check
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        uid = mAuth.getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);

        loadProfile();
    }

    private void loadProfile() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Get values with null checks
                    String fullName = snapshot.child("fullName").getValue(String.class);
                    String ageValue = snapshot.child("age").getValue(String.class);
                    String genderValue = snapshot.child("gender").getValue(String.class);
                    String weightValue = snapshot.child("weight").getValue(String.class);
                    String heightValue = snapshot.child("height").getValue(String.class);

                    // Set text with default values if null
                    name.setText(fullName != null ? fullName : "Not set");
                    age.setText(ageValue != null ? ageValue + " years" : "Not set");
                    gender.setText(genderValue != null ? genderValue : "Not set");
                    weight.setText(weightValue != null ? weightValue + " kg" : "Not set");
                    height.setText(heightValue != null ? heightValue + " cm" : "Not set");

                    // Load profile image
                    String img = snapshot.child("profileImage").getValue(String.class);
                    if (img != null && !img.isEmpty()) {
                        Glide.with(ProfileViewActivity.this)
                                .load(img)
                                .placeholder(R.drawable.ic_profile_avatar) // Add this drawable
                                .error(R.drawable.ic_profile_avatar) // Add this drawable
                                .into(profileImage);
                    }
                } else {
                    Toast.makeText(ProfileViewActivity.this,
                            "Profile data not found",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ProfileViewActivity.this,
                        "Failed to load profile: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}