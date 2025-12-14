package com.example.healthmate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        EditText email = findViewById(R.id.emailInput);
        EditText password = findViewById(R.id.passwordInput);
        Button loginBtn = findViewById(R.id.loginBtn);
        TextView registerRedirect = findViewById(R.id.registerRedirect);

        registerRedirect.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        loginBtn.setOnClickListener(v -> {

            String e = email.getText().toString().trim();
            String p = password.getText().toString().trim();

            if (e.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, "Enter Email & Password", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(e, p)
                    .addOnCompleteListener(task -> {

                        if (task.isSuccessful()) {

                            String uid = mAuth.getCurrentUser().getUid();
                            userRef = FirebaseDatabase.getInstance()
                                    .getReference("Users")
                                    .child(uid);

                            // 🔥 CHECK PROFILE COMPLETED
                            userRef.child("profileCompleted")
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot snapshot) {

                                            boolean completed = snapshot.exists()
                                                    && snapshot.getValue(Boolean.class);

                                            if (completed) {
                                                startActivity(new Intent(
                                                        LoginActivity.this,
                                                        DashboardActivity.class));
                                            } else {
                                                startActivity(new Intent(
                                                        LoginActivity.this,
                                                        ProfileActivity.class));
                                            }

                                            finish();
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError error) {
                                            Toast.makeText(LoginActivity.this,
                                                    "Database Error",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });

                        } else {
                            Toast.makeText(this,
                                    "Login Failed",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}
