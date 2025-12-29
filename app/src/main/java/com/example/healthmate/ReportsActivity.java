package com.example.healthmate;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.*;

import java.util.ArrayList;

public class ReportsActivity extends AppCompatActivity {

    BarChart sleepChart, waterChart, weightChart;
    TextView txtSummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        sleepChart = findViewById(R.id.sleepChart);
        waterChart = findViewById(R.id.waterChart);
        weightChart = findViewById(R.id.weightChart);
        txtSummary = findViewById(R.id.txtSummary);

        loadSleepChart();
        loadWaterChart();
        loadWeightChart();

        txtSummary.setText("👍 You are improving this week. Keep it up!");
    }

    private void loadSleepChart() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(1, 6));
        entries.add(new BarEntry(2, 7));
        entries.add(new BarEntry(3, 8));
        entries.add(new BarEntry(4, 6));
        entries.add(new BarEntry(5, 7));
        entries.add(new BarEntry(6, 8));
        entries.add(new BarEntry(7, 7));

        BarDataSet dataSet = new BarDataSet(entries, "Sleep Hours");
        BarData data = new BarData(dataSet);
        sleepChart.setData(data);

        Description d = new Description();
        d.setText("Sleep (Hours)");
        sleepChart.setDescription(d);
        sleepChart.invalidate();
    }

    private void loadWaterChart() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(1, 5));
        entries.add(new BarEntry(2, 6));
        entries.add(new BarEntry(3, 7));
        entries.add(new BarEntry(4, 6));
        entries.add(new BarEntry(5, 8));
        entries.add(new BarEntry(6, 7));
        entries.add(new BarEntry(7, 8));

        BarDataSet dataSet = new BarDataSet(entries, "Water Glasses");
        BarData data = new BarData(dataSet);
        waterChart.setData(data);

        Description d = new Description();
        d.setText("Water Intake");
        waterChart.setDescription(d);
        waterChart.invalidate();
    }

    private void loadWeightChart() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(1, 60));
        entries.add(new BarEntry(2, 60.5f));
        entries.add(new BarEntry(3, 60));
        entries.add(new BarEntry(4, 59.8f));
        entries.add(new BarEntry(5, 59.5f));
        entries.add(new BarEntry(6, 59.3f));
        entries.add(new BarEntry(7, 59));

        BarDataSet dataSet = new BarDataSet(entries, "Weight (kg)");
        BarData data = new BarData(dataSet);
        weightChart.setData(data);

        Description d = new Description();
        d.setText("Weight Progress");
        weightChart.setDescription(d);
        weightChart.invalidate();
    }
}
