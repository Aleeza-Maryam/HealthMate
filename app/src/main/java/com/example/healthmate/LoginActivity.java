package com.example.healthmate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    EditText email, password;
    Button loginBtn;
    TextView goToRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        email = findViewById(R.id.etEmail);
        password = findViewById(R.id.etPassword);
        loginBtn = findViewById(R.id.btnLogin);
        goToRegister = findViewById(R.id.tvRegister);

        loginBtn.setOnClickListener(v -> {
            if(email.getText().toString().isEmpty() ||
                    password.getText().toString().isEmpty()){
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            } else {
                startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                finish();
            }
        });

        goToRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );
    }
}
