package com.example.healthmate;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import org.tensorflow.lite.Interpreter;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SymptomActivity extends AppCompatActivity {

    CheckBox cbFever, cbCough, cbHeadache, cbFatigue, cbThroat;
    Button btnCheck;
    TextView txtResult, txtAdvice;
    CardView cardResult;

    LinearLayout layoutFever, layoutCough, layoutHeadache, layoutFatigue, layoutThroat;

    Interpreter tflite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_symptom);

        // Initialize UI elements
        cbFever = findViewById(R.id.cbFever);
        cbCough = findViewById(R.id.cbCough);
        cbHeadache = findViewById(R.id.cbHeadache);
        cbFatigue = findViewById(R.id.cbFatigue);
        cbThroat = findViewById(R.id.cbThroat);

        layoutFever = findViewById(R.id.layoutFever);
        layoutCough = findViewById(R.id.layoutCough);
        layoutHeadache = findViewById(R.id.layoutHeadache);
        layoutFatigue = findViewById(R.id.layoutFatigue);
        layoutThroat = findViewById(R.id.layoutThroat);

        btnCheck = findViewById(R.id.btnCheck);
        txtResult = findViewById(R.id.txtResult);
        txtAdvice = findViewById(R.id.txtAdvice);
        cardResult = findViewById(R.id.cardResult);

        // Make entire row clickable
        layoutFever.setOnClickListener(v -> cbFever.setChecked(!cbFever.isChecked()));
        layoutCough.setOnClickListener(v -> cbCough.setChecked(!cbCough.isChecked()));
        layoutHeadache.setOnClickListener(v -> cbHeadache.setChecked(!cbHeadache.isChecked()));
        layoutFatigue.setOnClickListener(v -> cbFatigue.setChecked(!cbFatigue.isChecked()));
        layoutThroat.setOnClickListener(v -> cbThroat.setChecked(!cbThroat.isChecked()));

        // Load TensorFlow Lite model
        loadModel();

        btnCheck.setOnClickListener(v -> {
            // Check if any symptom is selected
            if (!cbFever.isChecked() && !cbCough.isChecked() && !cbHeadache.isChecked() &&
                    !cbFatigue.isChecked() && !cbThroat.isChecked()) {
                Toast.makeText(this, "⚠️ Please select at least one symptom", Toast.LENGTH_SHORT).show();
                return;
            }

            // Run TensorFlow Lite prediction
            predict();
        });
    }

    private void loadModel() {
        try {
            // Try to load the TensorFlow Lite model
            InputStream is = getAssets().open("symptom_model.tflite");
            byte[] model = new byte[is.available()];
            is.read(model);
            ByteBuffer buffer = ByteBuffer.allocateDirect(model.length)
                    .order(ByteOrder.nativeOrder());
            buffer.put(model);
            tflite = new Interpreter(buffer);

            Toast.makeText(this, "✅ AI Model Loaded Successfully", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            // If model fails to load, use rule-based analysis as fallback
            e.printStackTrace();
            Toast.makeText(this, "⚠️ Using rule-based analysis", Toast.LENGTH_SHORT).show();
            tflite = null;
        }
    }

    private void predict() {
        // Show loading state
        cardResult.setVisibility(View.VISIBLE);
        txtResult.setText("🤖 Analyzing symptoms...");
        txtAdvice.setText("Please wait while AI processes your symptoms");

        try {
            if (tflite != null) {
                // Use TensorFlow Lite for prediction
                runTensorFlowPrediction();
            } else {
                // Fallback to rule-based analysis
                runRuleBasedAnalysis();
            }

        } catch (Exception e) {
            // If anything fails, use rule-based analysis
            e.printStackTrace();
            runRuleBasedAnalysis();
        }
    }

    private void runTensorFlowPrediction() {
        // Prepare input array (5 symptoms)
        float[][] input = new float[1][5];
        input[0][0] = cbFever.isChecked() ? 1.0f : 0.0f;    // Fever
        input[0][1] = cbCough.isChecked() ? 1.0f : 0.0f;    // Cough
        input[0][2] = cbHeadache.isChecked() ? 1.0f : 0.0f; // Headache
        input[0][3] = cbFatigue.isChecked() ? 1.0f : 0.0f;  // Fatigue
        input[0][4] = cbThroat.isChecked() ? 1.0f : 0.0f;   // Sore Throat

        // Prepare output array (4 conditions)
        float[][] output = new float[1][4];

        // Run inference
        tflite.run(input, output);

        // Get the predicted condition
        int predictedCondition = getMaxIndex(output[0]);

        // Display results
        showAIResult(predictedCondition, output[0]);
    }

    private void runRuleBasedAnalysis() {
        boolean hasFever = cbFever.isChecked();
        boolean hasCough = cbCough.isChecked();
        boolean hasHeadache = cbHeadache.isChecked();
        boolean hasFatigue = cbFatigue.isChecked();
        boolean hasThroat = cbThroat.isChecked();

        // Analyze symptoms based on rules
        String condition = analyzeCondition(hasFever, hasCough, hasHeadache, hasFatigue, hasThroat);
        String advice = getAdvice(condition);

        // Show results
        txtResult.setText("🩺 " + condition);
        txtAdvice.setText(advice);

        Toast.makeText(this, "✅ Analysis Complete", Toast.LENGTH_SHORT).show();
    }

    private int getMaxIndex(float[] probabilities) {
        int maxIndex = 0;
        float maxProb = probabilities[0];

        for (int i = 1; i < probabilities.length; i++) {
            if (probabilities[i] > maxProb) {
                maxProb = probabilities[i];
                maxIndex = i;
            }
        }

        return maxIndex;
    }

    private void showAIResult(int conditionIndex, float[] probabilities) {
        String condition = "";
        String advice = "";
        String confidence = String.format("%.1f%%", probabilities[conditionIndex] * 100);

        switch (conditionIndex) {
            case 0: // Common Cold
                condition = "Common Cold 🤧";
                advice = "• Rest and stay hydrated\n• Use honey for cough relief\n• Take steam inhalation\n• Symptoms improve in 7-10 days";
                break;
            case 1: // Flu
                condition = "Influenza (Flu) 🤒";
                advice = "• Isolate and rest\n• Drink plenty of fluids\n• Monitor temperature\n• Consult doctor if symptoms persist";
                break;
            case 2: // Migraine/Stress
                condition = "Migraine/Stress 🤕";
                advice = "• Rest in dark, quiet room\n• Stay hydrated\n• Limit screen time\n• Try relaxation techniques";
                break;
            case 3: // Viral Infection
                condition = "Viral Infection 🦠";
                advice = "• Get adequate rest\n• Stay hydrated with water\n• Eat nutritious meals\n• Monitor symptoms daily";
                break;
        }

        // Show results
        txtResult.setText(condition + "\nConfidence: " + confidence);
        txtAdvice.setText(advice);

        Toast.makeText(this, "🤖 AI Analysis Complete", Toast.LENGTH_SHORT).show();
    }

    private String analyzeCondition(boolean fever, boolean cough, boolean headache, boolean fatigue, boolean throat) {
        if (fever && cough && fatigue) {
            return "Possible Flu or COVID-19 - Consider testing";
        } else if (cough && throat && !fever) {
            return "Common Cold - Likely viral infection";
        } else if (headache && fatigue) {
            return "Stress or Migraine - Could be tension";
        } else if (fever && headache) {
            return "Possible Infection - Monitor temperature";
        } else if (cough && throat) {
            return "Throat Infection - May need medication";
        } else if (fatigue && headache) {
            return "Fatigue & Headache - Check sleep patterns";
        } else if (fever) {
            return "Fever Present - Rest and monitor";
        } else if (cough) {
            return "Cough - Stay hydrated and rest";
        } else if (headache) {
            return "Headache - Rest in quiet environment";
        } else if (fatigue) {
            return "Fatigue - Ensure adequate sleep";
        } else if (throat) {
            return "Sore Throat - Gargle with warm salt water";
        } else {
            return "No concerning symptoms detected";
        }
    }

    private String getAdvice(String condition) {
        if (condition.contains("COVID-19") || condition.contains("Flu")) {
            return "• Isolate yourself and rest\n• Drink plenty of fluids\n• Monitor temperature regularly\n• Consult a doctor if symptoms worsen";
        } else if (condition.contains("Cold")) {
            return "• Rest and stay hydrated\n• Use honey for cough relief\n• Take steam inhalation\n• Use saline nasal spray";
        } else if (condition.contains("Fever")) {
            return "• Rest and stay hydrated\n• Take fever reducer if needed\n• Use cool compress\n• See doctor if fever > 3 days";
        } else if (condition.contains("Headache")) {
            return "• Rest in a quiet, dark room\n• Stay hydrated\n• Limit screen time\n• Try relaxation techniques";
        } else {
            return "• Get adequate rest\n• Stay hydrated with water\n• Eat nutritious meals\n• Practice good hygiene";
        }
    }
}