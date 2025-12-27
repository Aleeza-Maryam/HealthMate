package com.example.healthmate;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.*;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class DietActivity extends AppCompatActivity {

    EditText edtFood;
    Button btnAdd;
    TextView txtCalories, txtTip;

    DatabaseReference dietRef;
    String uid;

    int totalCalories = 0;

    // 🔑 Try these Edamam credentials (free tier)
    String APP_ID = "53e86b5b";  // New App ID
    String APP_KEY = "cab28a4c417c27b7c09a15dfc484a2d4"; // New App Key

    // Alternative: Use free USDA API as backup
    String USDA_API_KEY = "DEMO_KEY"; // Free for testing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diet);

        edtFood = findViewById(R.id.edtFood);
        btnAdd = findViewById(R.id.btnAddFood);
        txtCalories = findViewById(R.id.txtCalories);
        txtTip = findViewById(R.id.txtTip);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        dietRef = FirebaseDatabase.getInstance()
                .getReference("DietLogs")
                .child(uid);

        loadCalories();

        btnAdd.setOnClickListener(v -> {
            String food = edtFood.getText().toString().trim();
            if (!food.isEmpty()) {
                // Try Edamam first, then fallback to local database
                fetchCaloriesEdamam(food);
            } else {
                Toast.makeText(this, "Please enter a food item", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Method 1: Try Edamam API
    private void fetchCaloriesEdamam(String food) {
        try {
            String formattedFood = formatFoodQuery(food);
            String encodedFood = URLEncoder.encode(formattedFood, "UTF-8");

            String url = "https://api.edamam.com/api/food-database/v2/parser"
                    + "?app_id=" + APP_ID
                    + "&app_key=" + APP_KEY
                    + "&ingr=" + encodedFood;

            RequestQueue queue = Volley.newRequestQueue(this);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.GET, url, null,
                    response -> {
                        try {
                            JSONArray hints = response.optJSONArray("hints");

                            if (hints == null || hints.length() == 0) {
                                // Fallback to local database
                                fetchCaloriesLocal(food);
                                return;
                            }

                            JSONObject firstHint = hints.getJSONObject(0);
                            JSONObject foodItem = firstHint.getJSONObject("food");
                            JSONObject nutrients = foodItem.getJSONObject("nutrients");

                            int calories = nutrients.optInt("ENERC_KCAL", -1);

                            if (calories <= 0) {
                                fetchCaloriesLocal(food);
                                return;
                            }

                            String foodLabel = foodItem.optString("label", food);
                            double quantity = parseQuantity(food);
                            double calculatedCalories = calories * quantity;

                            addCalories(foodLabel, (int) calculatedCalories);

                        } catch (Exception e) {
                            fetchCaloriesLocal(food);
                        }
                    },
                    error -> {
                        // If Edamam fails, try local database
                        fetchCaloriesLocal(food);
                    }
            );

            queue.add(request);

        } catch (Exception e) {
            fetchCaloriesLocal(food);
        }
    }

    // Method 2: Local database of common foods (fallback)
    private void fetchCaloriesLocal(String food) {
        food = food.toLowerCase().trim();

        // Common foods calorie database (per 100g or standard serving)
        HashMap<String, Integer> foodCalories = new HashMap<>();
        foodCalories.put("milk", 42); // per 100ml
        foodCalories.put("1 cup milk", 103); // 244ml
        foodCalories.put("glass of milk", 103);
        foodCalories.put("apple", 52);
        foodCalories.put("banana", 89);
        foodCalories.put("egg", 78);
        foodCalories.put("2 eggs", 156);
        foodCalories.put("chicken", 165);
        foodCalories.put("100g chicken", 165);
        foodCalories.put("rice", 130);
        foodCalories.put("bread", 79);
        foodCalories.put("slice of bread", 79);
        foodCalories.put("pasta", 131);
        foodCalories.put("potato", 77);
        foodCalories.put("tomato", 18);
        foodCalories.put("orange", 47);
        foodCalories.put("yogurt", 59);
        foodCalories.put("cheese", 402);
        foodCalories.put("butter", 717);
        foodCalories.put("oil", 884);
        foodCalories.put("sugar", 387);
        foodCalories.put("salt", 0);
        foodCalories.put("water", 0);
        foodCalories.put("coffee", 1);
        foodCalories.put("tea", 1);
        foodCalories.put("fish", 206);
        foodCalories.put("beef", 250);
        foodCalories.put("pork", 242);
        foodCalories.put("salad", 15);
        foodCalories.put("carrot", 41);
        foodCalories.put("onion", 40);
        foodCalories.put("garlic", 149);
        foodCalories.put("lemon", 29);
        foodCalories.put("cucumber", 15);
        foodCalories.put("spinach", 23);
        foodCalories.put("broccoli", 34);

        // Try to find exact match
        Integer calories = foodCalories.get(food);

        // If not found, search for partial matches
        if (calories == null) {
            for (String key : foodCalories.keySet()) {
                if (food.contains(key) || key.contains(food)) {
                    calories = foodCalories.get(key);
                    break;
                }
            }
        }

        // If still not found, use average
        if (calories == null || calories <= 0) {
            // Try to extract quantity
            double quantity = parseQuantity(food);
            calories = (int)(100 * quantity); // Assume 100 cal per serving
            addCalories(food, calories);
            Toast.makeText(this, "Using estimated calories", Toast.LENGTH_SHORT).show();
        } else {
            // Adjust for quantity
            double quantity = parseQuantity(food);
            int calculatedCalories = (int)(calories * quantity);
            addCalories(food, calculatedCalories);
        }
    }

    // Method 3: Alternative free API (USDA)
    private void fetchCaloriesUSDA(String food) {
        try {
            String encodedFood = URLEncoder.encode(food, "UTF-8");
            String url = "https://api.nal.usda.gov/fdc/v1/foods/search"
                    + "?api_key=" + USDA_API_KEY
                    + "&query=" + encodedFood
                    + "&pageSize=1";

            RequestQueue queue = Volley.newRequestQueue(this);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.GET, url, null,
                    response -> {
                        try {
                            JSONArray foods = response.optJSONArray("foods");
                            if (foods == null || foods.length() == 0) {
                                fetchCaloriesLocal(food);
                                return;
                            }

                            JSONObject firstFood = foods.getJSONObject(0);
                            JSONArray nutrients = firstFood.optJSONArray("foodNutrients");

                            int calories = 0;
                            for (int i = 0; i < nutrients.length(); i++) {
                                JSONObject nutrient = nutrients.getJSONObject(i);
                                if (nutrient.optInt("nutrientId") == 1008) { // Energy ID
                                    calories = nutrient.optInt("value", 0);
                                    break;
                                }
                            }

                            if (calories > 0) {
                                double quantity = parseQuantity(food);
                                int calculatedCalories = (int)(calories * quantity / 100); // USDA is per 100g
                                addCalories(food, calculatedCalories);
                            } else {
                                fetchCaloriesLocal(food);
                            }

                        } catch (Exception e) {
                            fetchCaloriesLocal(food);
                        }
                    },
                    error -> {
                        fetchCaloriesLocal(food);
                    }
            );

            queue.add(request);

        } catch (Exception e) {
            fetchCaloriesLocal(food);
        }
    }

    private void addCalories(String foodName, int calories) {
        totalCalories += calories;
        txtCalories.setText("Total Calories: " + totalCalories);

        // Save to Firebase
        saveCalories();

        // Show success message
        Toast.makeText(this,
                "✓ Added " + foodName + " (" + calories + " cal)",
                Toast.LENGTH_SHORT).show();

        showTip();
        edtFood.setText("");
    }

    private String formatFoodQuery(String food) {
        food = food.toLowerCase().trim();

        // Standardize common measurements
        food = food.replaceAll("\\s+(glass|glasses)", " cup");
        food = food.replaceAll("\\s+(bowl|bowls)", " cup");
        food = food.replaceAll("\\s+(slice|slices)", " piece");

        // Add default quantity
        if (!food.matches(".*\\d.*")) {
            food = "1 " + food;
        }

        return food.trim();
    }

    private double parseQuantity(String food) {
        try {
            String[] parts = food.split("\\s+");
            if (parts.length > 0) {
                String quantityStr = parts[0];
                quantityStr = quantityStr.replaceAll("[^\\d.]", "");

                if (!quantityStr.isEmpty()) {
                    double quantity = Double.parseDouble(quantityStr);

                    // Check for common serving sizes
                    if (food.contains("cup")) {
                        return quantity * 240; // ml to grams approximation
                    } else if (food.contains("glass")) {
                        return quantity * 250;
                    } else if (food.contains("tbsp") || food.contains("tablespoon")) {
                        return quantity * 15;
                    } else if (food.contains("tsp") || food.contains("teaspoon")) {
                        return quantity * 5;
                    } else if (food.contains("piece") || food.contains("slice")) {
                        return quantity * 50; // average piece weight
                    }

                    return quantity;
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return 1.0;
    }

    private void saveCalories() {
        dietRef.child("calories").setValue(totalCalories);
        dietRef.child("lastUpdated").setValue(System.currentTimeMillis());

        // Also save individual food items for history
        String food = edtFood.getText().toString().trim();
        if (!food.isEmpty()) {
            dietRef.child("foods").push().setValue(food);
        }
    }

    private void loadCalories() {
        dietRef.child("calories").addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            totalCalories = snapshot.getValue(Integer.class);
                            txtCalories.setText("Total Calories: " + totalCalories);
                            showTip();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(DietActivity.this,
                                "Failed to load calories", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void showTip() {
        String tip;
        if (totalCalories < 1000) {
            tip = "Add more nutritious food! 🥗";
        } else if (totalCalories <= 1600) {
            tip = "Good for weight loss 💪";
        } else if (totalCalories <= 2200) {
            tip = "Perfect balance! 👍";
        } else if (totalCalories <= 2800) {
            tip = "Moderate intake 📊";
        } else {
            tip = "Consider lighter options ⚠️";
        }
        txtTip.setText(tip);
    }

    // Quick add methods for common foods
    public void onAppleClick(android.view.View view) {
        edtFood.setText("1 apple");
        fetchCaloriesEdamam("1 apple");
    }

    public void onMilkClick(android.view.View view) {
        edtFood.setText("1 cup milk");
        fetchCaloriesEdamam("1 cup milk");
    }

    public void onChickenClick(android.view.View view) {
        edtFood.setText("100g chicken");
        fetchCaloriesEdamam("100g chicken");
    }

    public void onEggClick(android.view.View view) {
        edtFood.setText("2 eggs");
        fetchCaloriesEdamam("2 eggs");
    }
}