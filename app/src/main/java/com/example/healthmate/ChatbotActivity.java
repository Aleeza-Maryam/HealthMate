package com.example.healthmate;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class ChatbotActivity extends AppCompatActivity {

    private TextView chatTextView;
    private EditText inputEditText;
    private Button sendButton;

    private GenerativeModelFutures model;

    // 🛡️ SECURITY NOTE: In a real app, move this to local.properties or a backend
    private static final String API_KEY = "AIzaSyCZTJlJ8oL_itkAVupALix69UvbEHhlni8";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        // Initialize UI Elements
        chatTextView = findViewById(R.id.chatTextView);
        inputEditText = findViewById(R.id.inputEditText);
        sendButton = findViewById(R.id.sendButton);

        // Initialize Gemini Model (Java-friendly Futures wrapper)
        GenerativeModel innerModel = new GenerativeModel("gemini-2.5-flash-lite", API_KEY);
        model = GenerativeModelFutures.from(innerModel);

        sendButton.setOnClickListener(v -> {
            String msg = inputEditText.getText().toString().trim();
            if (!msg.isEmpty()) {
                chatTextView.append("\n\nYou: " + msg);
                inputEditText.setText("");
                sendMessage(msg);
            }
        });
    }

    private void sendMessage(String userMsg) {
        // Build the content object for the request
        Content content = new Content.Builder()
                .addText(userMsg)
                .build();

        // Perform the async request
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        // Handle the response using ContextCompat to remain compatible with API 24+
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String reply = result.getText();
                runOnUiThread(() -> {
                    if (reply != null) {
                        chatTextView.append("\n\nBot: " + reply);
                    } else {
                        chatTextView.append("\n\nBot: (No response text)");
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                runOnUiThread(() ->
                        chatTextView.append("\n\nBot: Error ❌ " + t.getLocalizedMessage())
                );
                t.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }
}