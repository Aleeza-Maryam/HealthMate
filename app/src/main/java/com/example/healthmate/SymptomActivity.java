package com.example.healthmate;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import org.tensorflow.lite.Interpreter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

public class SymptomActivity extends AppCompatActivity {

    // CheckBoxes - MUST MATCH PYTHON MODEL (7 symptoms)
    CheckBox cbRunnyNose, cbCough, cbFever, cbSneezing, cbSoreThroat, cbHeadache, cbBodyAches;
    Button btnCheck;
    TextView txtResult, txtAdvice, txtConfidence, txtDisclaimer;
    CardView cardResult;

    // Layouts for clickable rows
    LinearLayout layoutRunnyNose, layoutCough, layoutFever, layoutSneezing,
            layoutSoreThroat, layoutHeadache, layoutBodyAches;

    // TensorFlow Lite
    private Interpreter tflite;
    private List<String> diseaseNames;
    private List<String> symptomNames;
    private float confidenceThreshold = 0.7f; // Default

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_symptom);

        // Initialize UI elements - 7 SYMPTOMS (matching Python)
        cbRunnyNose = findViewById(R.id.cbRunnyNose);
        cbCough = findViewById(R.id.cbCough);
        cbFever = findViewById(R.id.cbFever);
        cbSneezing = findViewById(R.id.cbSneezing);
        cbSoreThroat = findViewById(R.id.cbSoreThroat);
        cbHeadache = findViewById(R.id.cbHeadache);
        cbBodyAches = findViewById(R.id.cbBodyAches);

        layoutRunnyNose = findViewById(R.id.layoutRunnyNose);
        layoutCough = findViewById(R.id.layoutCough);
        layoutFever = findViewById(R.id.layoutFever);
        layoutSneezing = findViewById(R.id.layoutSneezing);
        layoutSoreThroat = findViewById(R.id.layoutSoreThroat);
        layoutHeadache = findViewById(R.id.layoutHeadache);
        layoutBodyAches = findViewById(R.id.layoutBodyAches);

        btnCheck = findViewById(R.id.btnCheck);
        txtResult = findViewById(R.id.txtResult);
        txtAdvice = findViewById(R.id.txtAdvice);
        txtConfidence = findViewById(R.id.txtConfidence);
        txtDisclaimer = findViewById(R.id.txtDisclaimer);
        cardResult = findViewById(R.id.cardResult);

        // Make entire rows clickable
        layoutRunnyNose.setOnClickListener(v -> cbRunnyNose.setChecked(!cbRunnyNose.isChecked()));
        layoutCough.setOnClickListener(v -> cbCough.setChecked(!cbCough.isChecked()));
        layoutFever.setOnClickListener(v -> cbFever.setChecked(!cbFever.isChecked()));
        layoutSneezing.setOnClickListener(v -> cbSneezing.setChecked(!cbSneezing.isChecked()));
        layoutSoreThroat.setOnClickListener(v -> cbSoreThroat.setChecked(!cbSoreThroat.isChecked()));
        layoutHeadache.setOnClickListener(v -> cbHeadache.setChecked(!cbHeadache.isChecked()));
        layoutBodyAches.setOnClickListener(v -> cbBodyAches.setChecked(!cbBodyAches.isChecked()));

        // Set disclaimer
        txtDisclaimer.setText("⚠️ This tool is for informational purposes only. Not a substitute for professional medical advice.");

        // Load TensorFlow Lite model
        loadModelAndMetadata();

        btnCheck.setOnClickListener(v -> {
            // Check if any symptom is selected
            if (!isAnySymptomSelected()) {
                Toast.makeText(this, "⚠️ Please select at least one symptom", Toast.LENGTH_SHORT).show();
                return;
            }

            // Run TensorFlow Lite prediction
            predict();
        });
    }

    private boolean isAnySymptomSelected() {
        return cbRunnyNose.isChecked() || cbCough.isChecked() || cbFever.isChecked() ||
                cbSneezing.isChecked() || cbSoreThroat.isChecked() || cbHeadache.isChecked() ||
                cbBodyAches.isChecked();
    }

    private void loadModelAndMetadata() {
        new Thread(() -> {
            try {
                // 1. Load TFLite model
                InputStream modelStream = getAssets().open("medical_model.tflite");
                byte[] modelData = readAllBytes(modelStream);

                ByteBuffer buffer = ByteBuffer.allocateDirect(modelData.length)
                        .order(ByteOrder.nativeOrder());
                buffer.put(modelData);

                Interpreter.Options options = new Interpreter.Options();
                options.setNumThreads(4);
                tflite = new Interpreter(buffer, options);

                // 2. Load metadata
                InputStream metaStream = getAssets().open("model_metadata.json");
                String json = readStream(metaStream);
                JSONObject metadata = new JSONObject(json);

                // Load disease names
                diseaseNames = new ArrayList<>();
                JSONArray diseasesArray = metadata.getJSONArray("disease_names");
                for (int i = 0; i < diseasesArray.length(); i++) {
                    diseaseNames.add(diseasesArray.getString(i));
                }

                // Load symptom names
                symptomNames = new ArrayList<>();
                JSONArray symptomsArray = metadata.getJSONArray("symptom_names");
                for (int i = 0; i < symptomsArray.length(); i++) {
                    symptomNames.add(symptomsArray.getString(i));
                }

                confidenceThreshold = (float) metadata.getDouble("confidence_threshold");

                runOnUiThread(() ->
                        Toast.makeText(SymptomActivity.this,
                                "✅ AI Model Loaded (" + diseaseNames.size() + " diseases)",
                                Toast.LENGTH_SHORT).show()
                );

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(SymptomActivity.this,
                            "⚠️ Using rule-based analysis",
                            Toast.LENGTH_LONG).show();

                    // Set default values
                    diseaseNames = Arrays.asList(
                            "Common Cold", "Influenza", "Migraine",
                            "Food Poisoning", "COVID-19", "Bronchitis"
                    );
                    symptomNames = Arrays.asList(
                            "Runny Nose", "Cough", "Fever", "Sneezing",
                            "Sore Throat", "Headache", "Body Aches"
                    );
                });
            }
        }).start();
    }

    private void predict() {
        // Show loading state
        cardResult.setVisibility(View.VISIBLE);
        txtResult.setText("🤖 Analyzing symptoms...");
        txtAdvice.setText("Processing with AI...");
        txtConfidence.setText("");

        // Run in background thread
        new Thread(() -> {
            try {
                if (tflite != null && diseaseNames != null) {
                    runTensorFlowPrediction();
                } else {
                    runRuleBasedAnalysis();
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(this::runRuleBasedAnalysis);
            }
        }).start();
    }


    private void runTensorFlowPrediction() {
        try {
            // Prepare input array (7 symptoms - MATCHING PYTHON)
            float[][] input = new float[1][7];
            input[0][0] = cbRunnyNose.isChecked() ? 1.0f : 0.0f;    // Runny Nose
            input[0][1] = cbCough.isChecked() ? 1.0f : 0.0f;        // Cough
            input[0][2] = cbFever.isChecked() ? 1.0f : 0.0f;        // Fever
            input[0][3] = cbSneezing.isChecked() ? 1.0f : 0.0f;     // Sneezing
            input[0][4] = cbSoreThroat.isChecked() ? 1.0f : 0.0f;   // Sore Throat
            input[0][5] = cbHeadache.isChecked() ? 1.0f : 0.0f;     // Headache
            input[0][6] = cbBodyAches.isChecked() ? 1.0f : 0.0f;    // Body Aches

            // Prepare output array (6 diseases - MATCHING PYTHON)
            float[][] output = new float[1][diseaseNames.size()];

            // Run inference
            tflite.run(input, output);

            // Find prediction with highest confidence
            int maxIndex = 0;
            float maxConfidence = output[0][0];

            for (int i = 1; i < output[0].length; i++) {
                if (output[0][i] > maxConfidence) {
                    maxConfidence = output[0][i];
                    maxIndex = i;
                }
            }

// Top predictions
            List<Prediction> predictions = getTopPredictions(output[0], 3);

// ✅ FINAL COPIES (IMPORTANT)
            final int finalMaxIndex = maxIndex;
            final float finalMaxConfidence = maxConfidence;
            final List<Prediction> finalPredictions = predictions;

// ✅ Now SAFE
            runOnUiThread(() ->
                    showAIResult(finalPredictions, finalMaxIndex, finalMaxConfidence)
            );


        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(this::runRuleBasedAnalysis);
        }
    }

    private void runRuleBasedAnalysis() {
        boolean hasRunnyNose = cbRunnyNose.isChecked();
        boolean hasCough = cbCough.isChecked();
        boolean hasFever = cbFever.isChecked();
        boolean hasSneezing = cbSneezing.isChecked();
        boolean hasSoreThroat = cbSoreThroat.isChecked();
        boolean hasHeadache = cbHeadache.isChecked();
        boolean hasBodyAches = cbBodyAches.isChecked();

        // Analyze symptoms based on rules
        String condition = analyzeCondition(hasRunnyNose, hasCough, hasFever, hasSneezing,
                hasSoreThroat, hasHeadache, hasBodyAches);
        String advice = getAdvice(condition);

        // Show results on UI thread
        runOnUiThread(() -> {
            txtResult.setText("🩺 " + condition);
            txtAdvice.setText(advice);
            txtConfidence.setText("Rule-based analysis");
            txtConfidence.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            Toast.makeText(SymptomActivity.this, "✅ Analysis Complete", Toast.LENGTH_SHORT).show();
        });
    }

    private List<Prediction> getTopPredictions(float[] probabilities, int topN) {
        List<Prediction> predictions = new ArrayList<>();

        // Create list of indices
        Integer[] indices = new Integer[probabilities.length];
        for (int i = 0; i < indices.length; i++) indices[i] = i;

        // Sort by probability (descending)
        Arrays.sort(indices, (a, b) -> Float.compare(probabilities[b], probabilities[a]));

        // Get top N
        for (int i = 0; i < Math.min(topN, indices.length); i++) {
            int idx = indices[i];
            if (probabilities[idx] > 0.01) { // Only if >1%
                predictions.add(new Prediction(diseaseNames.get(idx), probabilities[idx]));
            }
        }

        return predictions;
    }

    private void showAIResult(List<Prediction> predictions, int mainIndex, float confidence) {

        String condition = diseaseNames.get(mainIndex);
        String confidencePercent = String.format("%.1f%%", confidence * 100);

        String tempAdvice = getAIDvice(condition, confidence);

        if (confidence < confidenceThreshold) {
            tempAdvice = "⚠️ Please consult a doctor for accurate diagnosis.\n\n" + tempAdvice;
        }

        final String finalAdvice = tempAdvice; // ✅ FINAL variable

        runOnUiThread(() -> {
            txtResult.setText("🔍 " + condition);
            txtAdvice.setText(finalAdvice);   // ✅ safe
            txtConfidence.setText("AI Confidence: " + confidencePercent);
        });
    }

    private String analyzeCondition(boolean runnyNose, boolean cough, boolean fever,
                                    boolean sneezing, boolean soreThroat,
                                    boolean headache, boolean bodyAches) {
        // Enhanced rule-based analysis
        int symptomCount = countTrue(runnyNose, cough, fever, sneezing, soreThroat, headache, bodyAches);

        if (fever && cough && bodyAches) {
            return "Possible Influenza (Flu)";
        } else if (runnyNose && sneezing && !fever) {
            return "Common Cold (Viral)";
        } else if (fever && cough && headache) {
            return "Possible COVID-19 - Consider testing";
        } else if (headache && !fever && !cough) {
            return "Migraine or Tension Headache";
        } else if (soreThroat && fever) {
            return "Strep Throat or Tonsillitis";
        } else if (cough && !fever) {
            return "Bronchitis or Cough Variant";
        } else if (bodyAches) {  // REMOVED: && fatigue (fatigue variable doesn't exist)
            return "Viral Infection or Overexertion";
        } else if (symptomCount >= 3) {
            return "Multiple Symptoms - Viral Infection likely";
        } else if (symptomCount == 1) {
            return "Single Symptom - Monitor and rest";
        } else {
            return "General Illness - Rest and hydrate";
        }
    }

    private String getAdvice(String condition) {
        // Rule-based advice
        Map<String, String> adviceMap = new HashMap<>();
        adviceMap.put("Possible Influenza (Flu)",
                "• Rest and stay hydrated\n• Take fever reducers as needed\n• Isolate to prevent spread\n• See doctor if symptoms worsen");
        adviceMap.put("Common Cold (Viral)",
                "• Rest and drink fluids\n• Use saline nasal spray\n• Honey for cough relief\n• Symptoms improve in 7-10 days");
        adviceMap.put("Possible COVID-19",
                "• Isolate immediately\n• Get tested for COVID-19\n• Monitor oxygen levels\n• Seek medical help if difficulty breathing");
        adviceMap.put("Migraine or Tension Headache",
                "• Rest in dark, quiet room\n• Stay hydrated\n• Avoid triggers\n• Consider OTC pain relief");
        adviceMap.put("Strep Throat or Tonsillitis",
                "• Gargle with warm salt water\n• Drink warm liquids\n• Avoid irritants\n• See doctor for antibiotics if bacterial");
        adviceMap.put("Bronchitis or Cough Variant",
                "• Rest and stay hydrated\n• Use cough syrup if needed\n• Avoid irritants\n• See doctor if cough persists");
        adviceMap.put("Viral Infection or Overexertion",
                "• Rest and allow body to recover\n• Stay hydrated\n• Eat nutritious food\n• Monitor symptoms");

        return adviceMap.getOrDefault(condition,
                "• Get adequate rest\n• Stay hydrated\n• Eat nutritious meals\n• Consult doctor if symptoms persist");
    }

    private String getAIDvice(String condition, float confidence) {
        // AI-based advice with confidence
        StringBuilder advice = new StringBuilder();

        advice.append("📋 Recommendations:\n");

        if (confidence < confidenceThreshold) {
            advice.append("⚠️ Low confidence prediction\n");
            advice.append("Please consult a healthcare professional\n\n");
        }

        // Condition-specific advice
        if (condition.contains("Flu") || condition.contains("Influenza")) {
            advice.append("• Rest and isolate\n");
            advice.append("• Drink plenty of fluids\n");
            advice.append("• Monitor temperature\n");
            advice.append("• Antiviral meds if prescribed\n");
        } else if (condition.contains("Cold")) {
            advice.append("• Rest and stay hydrated\n");
            advice.append("• Steam inhalation\n");
            advice.append("• OTC cold medications\n");
            advice.append("• Symptoms resolve in 7-10 days\n");
        } else if (condition.contains("COVID")) {
            advice.append("• ISOLATE immediately\n");
            advice.append("• Get tested\n");
            advice.append("• Monitor oxygen saturation\n");
            advice.append("• Seek emergency care if severe\n");
        } else if (condition.contains("Migraine")) {
            advice.append("• Rest in dark room\n");
            advice.append("• Stay hydrated\n");
            advice.append("• Avoid triggers\n");
            advice.append("• Medication as prescribed\n");
        } else if (condition.contains("Bronchitis")) {
            advice.append("• Rest and stay hydrated\n");
            advice.append("• Use humidifier\n");
            advice.append("• Avoid smoke and pollutants\n");
            advice.append("• See doctor if symptoms worsen\n");
        } else {
            advice.append("• Get adequate rest\n");
            advice.append("• Stay hydrated\n");
            advice.append("• Eat balanced meals\n");
            advice.append("• Consult doctor if needed\n");
        }

        advice.append("\n⚠️ This is NOT medical advice. Consult a doctor.");

        return advice.toString();
    }

    private int countTrue(boolean... values) {
        int count = 0;
        for (boolean value : values) {
            if (value) count++;
        }
        return count;
    }

    // Helper methods
    private byte[] readAllBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        return buffer.toByteArray();
    }

    private String readStream(InputStream inputStream) throws Exception {
        Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tflite != null) {
            tflite.close();
        }
    }

    // Helper class
    class Prediction {
        String diseaseName;
        float confidence;

        Prediction(String diseaseName, float confidence) {
            this.diseaseName = diseaseName;
            this.confidence = confidence;
        }
    }
}