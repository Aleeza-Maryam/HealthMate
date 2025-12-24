package com.example.healthmate;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;

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

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
        storageRef = FirebaseStorage.getInstance().getReference("profile_images");

        name = findViewById(R.id.fullName);
        age = findViewById(R.id.age);
        weight = findViewById(R.id.weight);
        height = findViewById(R.id.height);
        saveBtn = findViewById(R.id.saveProfileBtn);
        genderText = findViewById(R.id.genderText);
        genderContainer = findViewById(R.id.genderContainer);
        profileImage = findViewById(R.id.profileImage);
        backBtn = findViewById(R.id.backBtn);

        profileImage.setOnClickListener(v -> openGallery());
        backBtn.setOnClickListener(v -> onBackPressed());

        loadUserData();
        loadProfileImage();

        String[] genders = {"Male", "Female", "Other"};
        genderContainer.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select Gender")
                    .setItems(genders, (dialog, which) -> {
                        selectedGender = genders[which];
                        genderText.setText(selectedGender);
                    }).show();
        });

        saveBtn.setOnClickListener(v -> saveProfile());
    }

    private void saveProfile() {
        String n = name.getText().toString().trim();
        String a = age.getText().toString().trim();
        String w = weight.getText().toString().trim();
        String h = height.getText().toString().trim();

        if (n.isEmpty() || a.isEmpty() || selectedGender.isEmpty()) {
            Toast.makeText(this, "Fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        HashMap<String, Object> map = new HashMap<>();
        map.put("fullName", n);
        map.put("age", a);
        map.put("gender", selectedGender);
        map.put("weight", w);
        map.put("height", h);
        map.put("profileCompleted", true);

        userRef.updateChildren(map).addOnSuccessListener(unused -> {
            Toast.makeText(this, "Profile Saved", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        });
    }

    private void loadUserData() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot s) {
                if (s.exists()) {
                    name.setText(s.child("fullName").getValue(String.class));
                    age.setText(s.child("age").getValue(String.class));
                    weight.setText(s.child("weight").getValue(String.class));
                    height.setText(s.child("height").getValue(String.class));
                    selectedGender = s.child("gender").getValue(String.class);
                    genderText.setText(selectedGender);
                }
            }

            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    private void openGallery() {
        Intent i = new Intent(Intent.ACTION_PICK);
        i.setType("image/*");
        startActivityForResult(i, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int r, int c, @Nullable Intent d) {
        super.onActivityResult(r, c, d);
        if (r == PICK_IMAGE_REQUEST && c == RESULT_OK && d != null) {
            imageUri = d.getData();
            profileImage.setImageURI(imageUri);
            uploadImage();
        }
    }

    private void uploadImage() {
        StorageReference ref = storageRef.child(uid + ".jpg");
        ref.putFile(imageUri).addOnSuccessListener(task ->
                ref.getDownloadUrl().addOnSuccessListener(uri ->
                        userRef.child("profileImage").setValue(uri.toString())
                ));
    }

    private void loadProfileImage() {
        userRef.child("profileImage").addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot s) {
                        String url = s.getValue(String.class);
                        if (url != null)
                            Glide.with(ProfileActivity.this).load(url)
                                    .placeholder(R.drawable.ic_profile_avatar)
                                    .into(profileImage);
                    }

                    @Override public void onCancelled(DatabaseError error) {}
                });
    }
}
