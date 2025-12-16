package com.example.healthmate;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ProfileActivity extends AppCompatActivity {

    EditText name, age, weight, height;
    TextView genderText;
    LinearLayout genderContainer;
    Button saveBtn;
    ImageView profileImage, backBtn;

    DatabaseReference userRef;
    StorageReference storageRef;
    String uid;
    String selectedGender = "";
    Uri imageUri;

    private static final int PICK_IMAGE_REQUEST = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // 🔐 SAFETY CHECK
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
        storageRef = FirebaseStorage.getInstance().getReference("profile_images");

        // Initialize views
        name = findViewById(R.id.fullName);
        age = findViewById(R.id.age);
        weight = findViewById(R.id.weight);
        height = findViewById(R.id.height);
        saveBtn = findViewById(R.id.saveProfileBtn);
        genderText = findViewById(R.id.genderText);
        genderContainer = findViewById(R.id.genderContainer);
        profileImage = findViewById(R.id.profileImage);
        backBtn = findViewById(R.id.backBtn);

        // Set up profile image click listener
        profileImage.setOnClickListener(v -> openGallery());

        // Load existing profile image if available
        loadProfileImage();

        // Back button
        backBtn.setOnClickListener(v -> onBackPressed());

        // Gender dropdown
        final String[] genders = {"Male", "Female", "Other"};

        genderContainer.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
            builder.setTitle("Select Gender")
                    .setItems(genders, (dialog, which) -> {
                        selectedGender = genders[which];
                        genderText.setText(selectedGender);
                        genderText.setTextColor(getResources().getColor(android.R.color.black));
                    });
            builder.create().show();
        });

        saveBtn.setOnClickListener(v -> {
            String n = name.getText().toString().trim();
            String a = age.getText().toString().trim();
            String w = weight.getText().toString().trim();
            String h = height.getText().toString().trim();

            if (n.isEmpty() || a.isEmpty() || selectedGender.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate age
            try {
                int ageValue = Integer.parseInt(a);
                if (ageValue < 1 || ageValue > 120) {
                    Toast.makeText(this, "Please enter a valid age (1-120)", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid age", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save to Firebase
            userRef.child("fullName").setValue(n);
            userRef.child("age").setValue(a);
            userRef.child("gender").setValue(selectedGender);

            if (!w.isEmpty()) {
                userRef.child("weight").setValue(w);
            }

            if (!h.isEmpty()) {
                userRef.child("height").setValue(h);
            }

            userRef.child("profileCompleted").setValue(true);

            Toast.makeText(this, "Profile saved successfully!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            profileImage.setImageURI(imageUri);
            uploadImage();
        }
    }

    private void uploadImage() {
        if (imageUri == null) return;

        StorageReference fileRef = storageRef.child(uid + ".jpg");

        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        userRef.child("profileImage").setValue(uri.toString());
                        Toast.makeText(this, "Profile picture uploaded", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadProfileImage() {
        userRef.child("profileImage").addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String url = snapshot.getValue(String.class);
                            if (url != null && !url.isEmpty()) {
                                Glide.with(ProfileActivity.this)
                                        .load(url)
                                        .placeholder(R.drawable.ic_profile_avatar)
                                        .into(profileImage);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        // Handle error if needed
                    }
                });
    }
}