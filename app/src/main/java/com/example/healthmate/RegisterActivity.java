package com.example.healthmate;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        userRef = FirebaseDatabase.getInstance().getReference("Users");

        EditText name = findViewById(R.id.regName);
        EditText email = findViewById(R.id.regEmail);
        EditText password = findViewById(R.id.regPassword);
        Button registerBtn = findViewById(R.id.registerBtn);
        TextView loginRedirect = findViewById(R.id.loginRedirect);
        ImageView backBtn = findViewById(R.id.backBtn); // New back button

        // Back button functionality (optional enhancement)
        backBtn.setOnClickListener(v -> onBackPressed());

        loginRedirect.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));

        registerBtn.setOnClickListener(v -> {
            String n = name.getText().toString().trim();
            String e = email.getText().toString().trim();
            String p = password.getText().toString().trim();

            if (n.isEmpty() || e.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, "Please fill all fields!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (p.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(e, p)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String uid = mAuth.getCurrentUser().getUid();

                            userRef.child(uid).child("fullName").setValue(n);
                            userRef.child(uid).child("email").setValue(e);

                            Toast.makeText(this, "Account Created Successfully!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, LoginActivity.class));
                            finish();
                        } else {
                            Toast.makeText(this,
                                    task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}