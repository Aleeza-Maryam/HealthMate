package com.example.healthmate;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class WeightActivityActivity extends AppCompatActivity {

    EditText edtWeight, edtSteps;
    Button btnSave;
    TextView txtResult;

    DatabaseReference healthRef;
    String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weight_activity);

        edtWeight = findViewById(R.id.edtWeight);
        edtSteps = findViewById(R.id.edtSteps);
        btnSave = findViewById(R.id.btnSave);
        txtResult = findViewById(R.id.txtResult);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        healthRef = FirebaseDatabase.getInstance()
                .getReference("DailyHealth")
                .child(uid);

        loadData();

        btnSave.setOnClickListener(v -> saveData());
    }

    private void saveData() {
        String weightStr = edtWeight.getText().toString();
        String stepsStr = edtSteps.getText().toString();

        if (weightStr.isEmpty() || stepsStr.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int steps = Integer.parseInt(stepsStr);
        String activityLevel;

        if (steps < 3000) {
            activityLevel = "Low Activity ";
        } else if (steps <= 7000) {
            activityLevel = "Moderate Activity ";
        } else {
            activityLevel = "Active ";
        }

        healthRef.child("weight").setValue(weightStr);
        healthRef.child("steps").setValue(stepsStr);
        healthRef.child("activity").setValue(activityLevel);

        txtResult.setText(
                "Weight: " + weightStr + " kg\n" +
                        "Steps: " + stepsStr + "\n" +
                        "Activity: " + activityLevel
        );

        Toast.makeText(this, "Data Saved", Toast.LENGTH_SHORT).show();
    }

    private void loadData() {
        healthRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String weight = snapshot.child("weight").getValue(String.class);
                    String steps = snapshot.child("steps").getValue(String.class);
                    String activity = snapshot.child("activity").getValue(String.class);

                    edtWeight.setText(weight);
                    edtSteps.setText(steps);

                    txtResult.setText(
                            "Weight: " + weight + " kg\n" +
                                    "Steps: " + steps + "\n" +
                                    "Activity: " + activity
                    );
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }
}