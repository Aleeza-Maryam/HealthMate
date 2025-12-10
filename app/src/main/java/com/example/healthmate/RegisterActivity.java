package com.example.healthmate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    EditText name, email, password;
    Button registerBtn;
    TextView goToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        name = findViewById(R.id.etName);
        email = findViewById(R.id.etEmail);
        password = findViewById(R.id.etPassword);
        registerBtn = findViewById(R.id.btnRegister);
        goToLogin = findViewById(R.id.tvLogin);

        registerBtn.setOnClickListener(v -> {
            if(name.getText().toString().isEmpty() ||
                    email.getText().toString().isEmpty() ||
                    password.getText().toString().isEmpty()){
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Registered Successfully", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            }
        });

        goToLogin.setOnClickListener(v ->
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class))
        );
    }
}
