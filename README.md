HealthMate – AI-Based Symptom Checker Android App
 Project Overview

HealthMate is an Android application that helps users get a preliminary health assessment based on selected symptoms.
The app uses a machine learning model (TensorFlow Lite) to predict possible diseases and provides health advice with confidence scores.

 Disclaimer:
This application is for educational and informational purposes only and does not replace professional medical advice.

 Objectives

To build an AI-powered symptom checker for Android

To demonstrate Machine Learning model training + mobile deployment

To provide fast, offline disease prediction

To combine AI-based prediction with rule-based fallback logic

 How the System Works (High Level)

User selects symptoms (checkboxes)

Symptoms are converted into numeric input (0 / 1)

Input is sent to:

TensorFlow Lite model (AI prediction)

OR Rule-based logic (fallback)

App displays:

Predicted disease

Confidence percentage

Health advice

If confidence is low → user is warned to consult a doctor

 Features
 Android App Features

Symptom selection using checkboxes

AI-based disease prediction

Confidence score display

Medical advice generation

Rule-based fallback if AI model fails

Clean UI with clickable rows

Offline functionality (no internet required)

 AI / ML Features

Custom-trained TensorFlow model

Converted to TensorFlow Lite

Optimized for mobile devices

Uses Softmax classification

Confidence threshold handling

 Supported Symptoms (Input Features)

The model uses 7 symptoms as input:

Runny Nose

Cough

Fever

Sneezing

Sore Throat

Headache

Body Aches

Each symptom is represented as:

1 → Selected

0 → Not selected

 Predicted Diseases (Output Classes)

The model predicts one of the following 6 diseases:

Common Cold

Influenza (Flu)

Migraine

Food Poisoning

COVID-19

Bronchitis


 Is this model trained?

 Yes – custom trained

 Model Training Process
1️⃣ Dataset Generation

Synthetic but realistic medical data

2000 samples generated

Disease-specific symptom probabilities

Noise added for realism

2️⃣ Model Architecture
Input Layer (7 symptoms)
↓
Dense (16 neurons, ReLU)
↓
Dropout (0.2)
↓
Dense (8 neurons, ReLU)
↓
Dropout (0.2)
↓
Output Layer (6 neurons, Softmax)

3️⃣ Training Details

Optimizer: Adam

Loss: Sparse Categorical Crossentropy

Epochs: 30

Batch Size: 32

Validation Split: 10%

Model Files
File Name	Description
medical_model.tflite	Trained TensorFlow Lite model
model_metadata.json	Disease names, symptoms, confidence threshold
symptom_model.py	Python training script
 Android Implementation
Main Activity

SymptomActivity.java

Responsibilities

Load TFLite model from assets

Read metadata JSON

Convert user input into float array

Run inference

Display results on UI thread

Handle errors safely

 Prediction Flow (Android)
User selects symptoms
↓
Convert to float[1][7]
↓
Run tflite.run(input, output)
↓
Find highest probability
↓
Check confidence threshold
↓
Display disease + advice

🛡 Rule-Based Fallback System

If:

Model fails to load

Prediction throws error

 The app automatically switches to rule-based analysis using logical conditions.

Example:

Fever + Cough + Body Aches → Influenza

Runny Nose + Sneezing → Common Cold

 Confidence Handling

Confidence threshold: 70%

If confidence < threshold:

Warning message shown

Doctor consultation advised

 Testing

Tested with multiple symptom combinations

Sample test cases included in test_model.py

Verified confidence outputs

Verified UI thread safety

 Technologies Used
Mobile

Java (Android)

Android Studio

XML Layouts

AI / ML

Python

TensorFlow / Keras

NumPy

TensorFlow Lite
 Disclaimer

This application does not provide medical diagnosis.
It is intended for learning and demonstration purposes only.
Always consult a qualified healthcare professional for medical concerns.

 Future Improvements

Add more symptoms

Improve dataset realism

Add Firebase notifications

Add user history

Support multiple languages

Connect with real medical APIs

 

HealthMate – AI Symptom Checker
Developed as an educational Android + AI project
