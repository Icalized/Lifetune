package com.example.miniproj;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import Database.DatabaseHandler;
import Model.Vitals;

public class ReportFragment extends Fragment {

    private static final String TAG = "ReportFragment";
    private BarChart barchart;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_report, container, false);

        // Initialize bar Chart
        barchart = view.findViewById(R.id.barChart);
        DatabaseHandler db = new DatabaseHandler(getContext());

        // Get the BPM and SpO2 data for the week
        List<BarEntry> entries = db.getDailyAverages();

        // Prepare dataset
        BarDataSet dataSet = new BarDataSet(entries, "Vitals");
        dataSet.setColors(new int[]{Color.RED,Color.BLUE}); // Apply material colors
        dataSet.setStackLabels(new String[]{"BPM", "SpO2"}); // Labels for stacks

        // Prepare BarData
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f); // Width of each bar

        // Configure X-Axis Labels
        String[] daysOfWeek = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        barchart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(daysOfWeek));
        barchart.getXAxis().setGranularity(1f);
        barchart.getXAxis().setGranularityEnabled(true);

        // Configure the chart
        barchart.setData(barData);
        barchart.setFitBars(true);
        barchart.getDescription().setEnabled(false);
        barchart.getAxisRight().setEnabled(false); // Disable the right Y-axis
        barchart.getAxisLeft().setAxisMinimum(0f); // Minimum value for Y-axis
        barchart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        barchart.animateY(1000); // Animation for Y-axis
        barchart.invalidate(); // Refresh the chart

        return view;
    }
}