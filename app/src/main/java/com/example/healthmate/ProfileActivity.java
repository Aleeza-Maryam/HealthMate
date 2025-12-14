package com.example.healthmate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ProfileActivity extends AppCompatActivity {

    EditText name, age, gender, weight, height;
    Button saveBtn;

    DatabaseReference userRef;
    String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // 🔐 Firebase user id
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(uid);

        // 🔗 Bind views
        name = findViewById(R.id.fullName);
        age = findViewById(R.id.age);
        gender = findViewById(R.id.gender);
        weight = findViewById(R.id.weight);
        height = findViewById(R.id.height);
        saveBtn = findViewById(R.id.saveProfileBtn);

        saveBtn.setOnClickListener(v -> {

            if (name.getText().toString().isEmpty()
                    || age.getText().toString().isEmpty()
                    || gender.getText().toString().isEmpty()
                    || weight.getText().toString().isEmpty()
                    || height.getText().toString().isEmpty()) {

                Toast.makeText(this,
                        "Please fill all fields",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // 💾 Save data
            userRef.child("fullName").setValue(name.getText().toString());
            userRef.child("age").setValue(age.getText().toString());
            userRef.child("gender").setValue(gender.getText().toString());
            userRef.child("weight").setValue(weight.getText().toString());
            userRef.child("height").setValue(height.getText().toString());
            userRef.child("profileCompleted").setValue(true);

            Toast.makeText(this,
                    "Profile saved successfully",
                    Toast.LENGTH_SHORT).show();

            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        });
    }
}
