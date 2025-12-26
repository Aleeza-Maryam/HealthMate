package com.example.healthmate;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class HealthTrackingActivity extends AppCompatActivity {

    Button btnAddWater, btnSave;
    TextView txtWater;
    EditText edtSleep, edtWeight;

    int waterCount = 0;

    DatabaseReference healthRef;
    String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health_tracking);

        btnAddWater = findViewById(R.id.btnAddWater);
        btnSave = findViewById(R.id.btnSave);
        txtWater = findViewById(R.id.txtWater);
        edtSleep = findViewById(R.id.edtSleep);
        edtWeight = findViewById(R.id.edtWeight);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        healthRef = FirebaseDatabase.getInstance()
                .getReference("HealthLogs")
                .child(uid);

        loadData();

        btnAddWater.setOnClickListener(v -> {
            waterCount++;
            txtWater.setText(String.valueOf(waterCount));
        });

        btnSave.setOnClickListener(v -> saveData());
    }

    private void saveData() {
        String sleep = edtSleep.getText().toString();
        String weight = edtWeight.getText().toString();

        healthRef.child("water").setValue(waterCount);
        healthRef.child("sleep").setValue(sleep);
        healthRef.child("weight").setValue(weight);

        Toast.makeText(this, "Health Data Saved", Toast.LENGTH_SHORT).show();
    }

    private void loadData() {
        healthRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                if (snapshot.exists()) {

                    if (snapshot.child("water").exists()) {
                        waterCount = snapshot.child("water").getValue(Integer.class);
                        txtWater.setText(String.valueOf(waterCount));
                    }

                    if (snapshot.child("sleep").exists()) {
                        edtSleep.setText(snapshot.child("sleep").getValue(String.class));
                    }

                    if (snapshot.child("weight").exists()) {
                        edtWeight.setText(snapshot.child("weight").getValue(String.class));
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(HealthTrackingActivity.this,
                        "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
