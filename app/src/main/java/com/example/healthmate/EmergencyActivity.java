package com.example.healthmate;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EmergencyActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private FusedLocationProviderClient fusedLocationClient;
    private TextView contactNameTextView, contactNumberTextView, locationStatusText;

    // Corrected Types: These must match activity_emergency.xml tags exactly
    private Button sosButton;
    private CardView callButton, smsButton, locationButton;

    private String emergencyContactName = "Emergency Services";
    private String emergencyContactNumber = "112";
    private String currentLocation = "Location not available";
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency);

        // 1. Initialize Firebase and Safety Check
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        // 2. Initialize Views
        contactNameTextView = findViewById(R.id.contactNameTextView);
        contactNumberTextView = findViewById(R.id.contactNumberTextView);

        // Find the specific TextView inside the Location CardView to avoid casting errors
        locationStatusText = findViewById(R.id.locationStatusText);

        // Match these to your XML Tags
        sosButton = findViewById(R.id.sosButton); // <Button> in XML
        callButton = findViewById(R.id.callButton); // <androidx.cardview.widget.CardView> in XML
        smsButton = findViewById(R.id.smsButton); // <androidx.cardview.widget.CardView> in XML
        locationButton = findViewById(R.id.locationButton); // <androidx.cardview.widget.CardView> in XML

        // 3. Initialize Location and Database
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());

        // 4. Setup Page
        loadEmergencyContact();
        setupButtonListeners();
        getCurrentLocation();
    }

    private void loadEmergencyContact() {
        userRef.child("emergencyContact").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    emergencyContactName = snapshot.child("name").getValue(String.class);
                    emergencyContactNumber = snapshot.child("phone").getValue(String.class);

                    if (emergencyContactName != null) contactNameTextView.setText(emergencyContactName);
                    if (emergencyContactNumber != null) contactNumberTextView.setText(emergencyContactNumber);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EmergencyActivity.this, "Failed to load contact", Toast.LENGTH_SHORT).show();
            }
        });
    }
    public void callPolice(View view) {
        callNumber("100");
    }

    public void callAmbulance(View view) {
        callNumber("102");
    }

    public void callFire(View view) {
        callNumber("101");
    }

    private void callNumber(String number) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + number));
        startActivity(intent);
    }

    private void setupButtonListeners() {
        sosButton.setOnClickListener(v -> showSOSConfirmationDialog());

        callButton.setOnClickListener(v -> makeEmergencyCall());

        smsButton.setOnClickListener(v -> sendEmergencySMS());

        locationButton.setOnClickListener(v -> {
            getCurrentLocation();
            shareLocation();
        });

    }

    private void showSOSConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("🚨 Emergency SOS")
                .setMessage("Are you sure you want to trigger Emergency SOS?\n\nThis will alert " + emergencyContactName)
                .setPositiveButton("YES, SEND SOS", (dialog, which) -> triggerSOS())
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void triggerSOS() {
        // Visual feedback on the Button
        sosButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
        sosButton.setText("SOS SENT!");

        makeEmergencyCall();
        sendEmergencySMS();
        logEmergencyEvent();

        // Reset button after 3 seconds
        sosButton.postDelayed(() -> {
            sosButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
            sosButton.setText("HOLD FOR SOS");
        }, 3000);
    }

    private void makeEmergencyCall() {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + emergencyContactNumber));
        startActivity(intent);
    }



    private void sendEmergencySMS() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("smsto:" + emergencyContactNumber));
        intent.putExtra("sms_body",
                "🚨 EMERGENCY ALERT!\nI need help!\nMy location:\n" + currentLocation);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {

            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                makeEmergencyCall(); // permission milte hi call lagao

            } else {
                Toast.makeText(this,
                        "Permission denied. Enable from Settings.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }


    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            double lat = location.getLatitude();
                            double lon = location.getLongitude();
                            currentLocation = "https://www.google.com/maps?q=" + lat + "," + lon;

                            // Update the TextView inside the card, not the CardView itself
                            if (locationStatusText != null) {
                                locationStatusText.setText("📍 Location Ready");
                            }
                        }
                    });
        }
    }

    private void shareLocation() {
        if (currentLocation.equals("Location not available")) {
            Toast.makeText(this, "Waiting for GPS...", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "My Current Emergency Location: " + currentLocation);
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    private void logEmergencyEvent() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        DatabaseReference logRef = FirebaseDatabase.getInstance().getReference("EmergencyLogs")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid()).push();

        EmergencyLog log = new EmergencyLog(timestamp, emergencyContactName, emergencyContactNumber, currentLocation);
        logRef.setValue(log);
    }

    private void requestPermissions() {
        String[] permissions = {
                Manifest.permission.CALL_PHONE,
                Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION
        };
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }

    // Model class for Firebase logs
    public static class EmergencyLog {
        public String timestamp, contactName, contactNumber, location;
        public EmergencyLog() {}
        public EmergencyLog(String t, String n, String p, String l) {
            this.timestamp = t; this.contactName = n; this.contactNumber = p; this.location = l;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fusedLocationClient = null;
        userRef = null;
    }
}