package com.example.healthmate;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SleepActivity extends AppCompatActivity {

    EditText edtSleep;
    TextView txtStatus;
    Button btnSave;

    DatabaseReference sleepRef;
    String uid, todayDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep);

        edtSleep = findViewById(R.id.edtSleepHours);
        txtStatus = findViewById(R.id.txtSleepStatus);
        btnSave = findViewById(R.id.btnSaveSleep);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());

        sleepRef = FirebaseDatabase.getInstance()
                .getReference("SleepLogs")
                .child(uid)
                .child(todayDate);

        loadSleepData();

        btnSave.setOnClickListener(v -> saveSleep());
    }

    private void saveSleep() {
        String hoursStr = edtSleep.getText().toString().trim();

        if (hoursStr.isEmpty()) {
            Toast.makeText(this, "Enter sleep hours", Toast.LENGTH_SHORT).show();
            return;
        }

        float hours = Float.parseFloat(hoursStr);
        String quality = getSleepQuality(hours);

        sleepRef.child("hours").setValue(hoursStr);
        sleepRef.child("quality").setValue(quality);

        txtStatus.setText("Sleep Quality: " + quality);
        Toast.makeText(this, "Sleep data saved", Toast.LENGTH_SHORT).show();
    }

    private void loadSleepData() {
        sleepRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    edtSleep.setText(snapshot.child("hours").getValue(String.class));
                    txtStatus.setText("Sleep Quality: " +
                            snapshot.child("quality").getValue(String.class));
                }
            }

            @Override
            public void onCancelled(DatabaseError error) { }
        });
    }

    private String getSleepQuality(float h) {
        if (h < 5) return "Poor 😴";
        else if (h < 7) return "Average 🙂";
        else if (h <= 9) return "Good 😃";
        else return "Oversleep ⚠️";
    }
}
