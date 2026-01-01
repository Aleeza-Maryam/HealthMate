package com.example.healthmate;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class DietGeneratorActivity extends AppCompatActivity {

    TextView txtCurrentWeight, txtWeightCategory, txtDailyCalories, txtDietPlan;
    RadioGroup rgGoal;
    Spinner spinnerActivity;
    Button btnGenerateDiet, btnSavePlan, btnUpdateWeight;
    LinearLayout cardDietPlan;

    DatabaseReference userRef, dietRef;
    String uid, todayDate;
    double userWeight = 0;
    String selectedGoal = "maintain";
    double activityFactor = 1.2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diet_generator);

        // Initialize views
        txtCurrentWeight = findViewById(R.id.txtCurrentWeight);
        txtWeightCategory = findViewById(R.id.txtWeightCategory);
        txtDailyCalories = findViewById(R.id.txtDailyCalories);
        txtDietPlan = findViewById(R.id.txtDietPlan);
        rgGoal = findViewById(R.id.rgGoal);
        spinnerActivity = findViewById(R.id.spinnerActivity);
        btnGenerateDiet = findViewById(R.id.btnGenerateDiet);
        btnSavePlan = findViewById(R.id.btnSavePlan);
        btnUpdateWeight = findViewById(R.id.btnUpdateWeight);
        cardDietPlan = findViewById(R.id.cardDietPlan);

        // Firebase setup
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
        dietRef = FirebaseDatabase.getInstance().getReference("DietPlans").child(uid);

        // Setup activity level spinner
        setupActivitySpinner();
        // Load user's weight from Firebase
        loadUserWeight();
        // Load existing diet plan if any
        loadExistingDietPlan();

        // Goal selection listener
        rgGoal.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbLoseWeight) selectedGoal = "lose";
            else if (checkedId == R.id.rbMaintain) selectedGoal = "maintain";
            else if (checkedId == R.id.rbGainWeight) selectedGoal = "gain";
        });

        // Generate diet button
        btnGenerateDiet.setOnClickListener(v -> generateDietPlan());

        // Save plan button
        btnSavePlan.setOnClickListener(v -> saveDietPlan());

        // Update weight button
        btnUpdateWeight.setOnClickListener(v -> showUpdateWeightDialog());
    }

    private void setupActivitySpinner() {
        String[] activityLevels = {
                "Sedentary (little or no exercise)",
                "Lightly active (light exercise 1-3 days/week)",
                "Moderately active (moderate exercise 3-5 days/week)",
                "Very active (hard exercise 6-7 days/week)",
                "Extra active (very hard exercise & physical job)"
        };

        Double[] activityFactors = {1.2, 1.375, 1.55, 1.725, 1.9};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, activityLevels
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerActivity.setAdapter(adapter);

        spinnerActivity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                activityFactor = activityFactors[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                activityFactor = 1.2;
            }
        });
    }

    private void loadUserWeight() {
        // Try to get weight from DailyHealth
        DatabaseReference dailyHealthRef = FirebaseDatabase.getInstance()
                .getReference("DailyHealth")
                .child(uid);

        dailyHealthRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.hasChild("weight")) {
                    String weightStr = snapshot.child("weight").getValue(String.class);
                    if (weightStr != null && !weightStr.isEmpty()) {
                        try {
                            userWeight = Double.parseDouble(weightStr);
                            updateWeightDisplay();
                        } catch (NumberFormatException e) {
                            userWeight = 65; // Default weight
                            updateWeightDisplay();
                        }
                    } else {
                        loadWeightFromProfile();
                    }
                } else {
                    loadWeightFromProfile();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                loadWeightFromProfile();
            }
        });
    }

    private void loadWeightFromProfile() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.hasChild("weight")) {
                    String weightStr = snapshot.child("weight").getValue(String.class);
                    if (weightStr != null && !weightStr.isEmpty()) {
                        try {
                            userWeight = Double.parseDouble(weightStr);
                        } catch (NumberFormatException e) {
                            userWeight = 65;
                        }
                    }
                } else {
                    userWeight = 65; // Default weight in kg
                }
                updateWeightDisplay();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                userWeight = 65;
                updateWeightDisplay();
            }
        });
    }

    private void updateWeightDisplay() {
        txtCurrentWeight.setText(String.format(Locale.US, "%.1f kg", userWeight));

        // Simple weight category based on common ranges
        if (userWeight < 50) {
            txtWeightCategory.setText("Underweight - Need to gain weight");
        } else if (userWeight < 70) {
            txtWeightCategory.setText("Normal weight - Healthy range");
        } else if (userWeight < 85) {
            txtWeightCategory.setText("Overweight - Consider losing weight");
        } else {
            txtWeightCategory.setText("Obese - Recommended to lose weight");
        }
    }

    private void generateDietPlan() {
        if (userWeight <= 0) {
            Toast.makeText(this, "Please set your weight first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Calculate Basal Metabolic Rate (BMR)
        double bmr = 10 * userWeight + 6.25 * 170 - 5 * 30 + 5; // Default height=170cm, age=30

        // Adjust for activity level
        double dailyCalories = bmr * activityFactor;

        // Adjust for goal
        switch (selectedGoal) {
            case "lose": dailyCalories -= 500; break; // 500 calorie deficit
            case "gain": dailyCalories += 500; break; // 500 calorie surplus
        }

        // Ensure minimum calories
        dailyCalories = Math.max(1200, dailyCalories);

        // Display calories
        txtDailyCalories.setText(String.format(Locale.US, "%.0f", dailyCalories));

        // Generate diet plan
        String dietPlan = generatePersonalizedDiet(dailyCalories, selectedGoal);
        txtDietPlan.setText(dietPlan);

        // Show the diet plan card
        cardDietPlan.setVisibility(View.VISIBLE);
    }

    private String generatePersonalizedDiet(double calories, String goal) {
        StringBuilder plan = new StringBuilder();

        // Macronutrient distribution
        double proteinPerKg, fatPercentage, carbPercentage;

        switch (goal) {
            case "lose":
                proteinPerKg = 2.0;
                fatPercentage = 0.25;
                carbPercentage = 0.45;
                break;
            case "gain":
                proteinPerKg = 1.8;
                fatPercentage = 0.25;
                carbPercentage = 0.50;
                break;
            default:
                proteinPerKg = 1.5;
                fatPercentage = 0.30;
                carbPercentage = 0.45;
        }

        // Calculate macronutrients
        double proteinGrams = proteinPerKg * userWeight;
        double fatGrams = (calories * fatPercentage) / 9;
        double carbGrams = (calories * carbPercentage) / 4;

        plan.append("📊 Daily Macronutrients:\n");
        plan.append(String.format(Locale.US, "• Protein: %.0f g\n", proteinGrams));
        plan.append(String.format(Locale.US, "• Carbs: %.0f g\n", carbGrams));
        plan.append(String.format(Locale.US, "• Fat: %.0f g\n\n", fatGrams));

        // Meal plan
        plan.append("🍽️ Sample Daily Meal Plan:\n\n");

        if (goal.equals("lose")) {
            plan.append("Breakfast (300 cal):\n");
            plan.append("• 2 boiled eggs\n• 1 slice whole wheat toast\n• Green tea\n\n");

            plan.append("Lunch (400 cal):\n");
            plan.append("• Grilled chicken breast (150g)\n• Mixed vegetables\n• Quinoa (1/2 cup)\n\n");

            plan.append("Snack (150 cal):\n");
            plan.append("• Greek yogurt with berries\n• Handful of almonds\n\n");

            plan.append("Dinner (350 cal):\n");
            plan.append("• Baked fish (150g)\n• Steamed broccoli\n• Sweet potato\n");
        } else if (goal.equals("gain")) {
            plan.append("Breakfast (500 cal):\n");
            plan.append("• 3 egg omelette\n• 2 slices whole wheat toast\n• Banana smoothie\n\n");

            plan.append("Lunch (600 cal):\n");
            plan.append("• Brown rice (1 cup)\n• Chicken curry (200g)\n• Lentils (1/2 cup)\n• Salad\n\n");

            plan.append("Snack (300 cal):\n");
            plan.append("• Protein shake with milk\n• Peanut butter sandwich\n\n");

            plan.append("Dinner (600 cal):\n");
            plan.append("• Beef steak (200g)\n• Mashed potatoes\n• Steamed vegetables\n");
        } else {
            plan.append("Breakfast (400 cal):\n");
            plan.append("• Oatmeal with fruits & nuts\n• 2 boiled eggs\n• Glass of milk\n\n");

            plan.append("Lunch (500 cal):\n");
            plan.append("• Chicken wrap with veggies\n• Fruit salad\n• Yogurt\n\n");

            plan.append("Snack (200 cal):\n");
            plan.append("• Apple with peanut butter\n• Handful of nuts\n\n");

            plan.append("Dinner (400 cal):\n");
            plan.append("• Grilled fish (150g)\n• Brown rice (3/4 cup)\n• Mixed vegetables\n");
        }

        plan.append("\n💧 Hydration:\n");
        plan.append("• Drink 3-4 liters of water daily\n");
        plan.append("• Avoid sugary drinks\n");

        return plan.toString();
    }

    private void saveDietPlan() {
        if (txtDietPlan.getText().toString().isEmpty()) {
            Toast.makeText(this, "Generate a plan first", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> dietData = new HashMap<>();
        dietData.put("date", todayDate);
        dietData.put("weight", userWeight);
        dietData.put("goal", selectedGoal);
        dietData.put("activityLevel", spinnerActivity.getSelectedItemPosition());
        dietData.put("calories", Double.parseDouble(txtDailyCalories.getText().toString()));
        dietData.put("plan", txtDietPlan.getText().toString());

        dietRef.child(todayDate).setValue(dietData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Diet plan saved!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadExistingDietPlan() {
        dietRef.child(todayDate).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Auto-load today's plan
                    cardDietPlan.setVisibility(View.VISIBLE);
                    if (snapshot.hasChild("calories")) {
                        txtDailyCalories.setText(snapshot.child("calories").getValue().toString());
                    }
                    if (snapshot.hasChild("plan")) {
                        txtDietPlan.setText(snapshot.child("plan").getValue(String.class));
                    }

                    // Set goal radio button
                    String savedGoal = snapshot.child("goal").getValue(String.class);
                    if (savedGoal != null) {
                        switch (savedGoal) {
                            case "lose": rgGoal.check(R.id.rbLoseWeight); break;
                            case "gain": rgGoal.check(R.id.rbGainWeight); break;
                            default: rgGoal.check(R.id.rbMaintain);
                        }
                    }

                    // Set activity spinner
                    Long activityLevel = snapshot.child("activityLevel").getValue(Long.class);
                    if (activityLevel != null) {
                        spinnerActivity.setSelection(activityLevel.intValue());
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Do nothing
            }
        });
    }

    private void showUpdateWeightDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Weight");

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.format(Locale.US, "%.1f", userWeight));
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newWeightStr = input.getText().toString().trim();
            if (!newWeightStr.isEmpty()) {
                try {
                    userWeight = Double.parseDouble(newWeightStr);

                    // Save to DailyHealth
                    DatabaseReference dailyHealthRef = FirebaseDatabase.getInstance()
                            .getReference("DailyHealth")
                            .child(uid);

                    dailyHealthRef.child("weight").setValue(newWeightStr);
                    updateWeightDisplay();

                    Toast.makeText(this, "Weight updated!", Toast.LENGTH_SHORT).show();
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid weight value", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}