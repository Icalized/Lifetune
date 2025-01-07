package com.example.miniproj;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;

import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import Database.DatabaseHandler;
import Model.Vitals;

public class ReportFragment extends Fragment {

    private static final String TAG = "ReportFragment";
    private BarChart barchart;
    private TextView isApnea, apneaDesc, Timestamp, sessionStart, sessionEnd, duration;
    AppCompatButton generate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_report, container, false);

        isApnea = view.findViewById(R.id.apneaText);
        apneaDesc = view.findViewById(R.id.apneadesc);
        Timestamp = view.findViewById(R.id.timestamp);
        sessionStart = view.findViewById(R.id.sessionStart);
        sessionEnd = view.findViewById(R.id.sessionEnd);
        duration = view.findViewById(R.id.duration);
        generate = view.findViewById(R.id.generateReport);
        // Initialize bar Chart
        barchart = view.findViewById(R.id.barChart);
        DatabaseHandler db = new DatabaseHandler(getContext());

        // Get the BPM and SpO2 data for the week
        List<BarEntry> entries = db.getDailyAverages();

        // Prepare dataset
        BarDataSet dataSet = new BarDataSet(entries, "Vitals");
        dataSet.setColors(new int[]{Color.rgb(255, 153, 153), Color.rgb(144, 238, 144)});
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

        // Apnea Showing on UI
        boolean sleepApnea = db.checkSleepApnea(75, 90);

        ArrayList<Long> array = db.timestamps();

        Long[] sessions = db.getFirstAndLastTimeForToday();
        Long firstTime = sessions[0]; // Extract first time
        Long lastTime = sessions[1]; // Extract last time

        String sessionStartTime = longToStringTime(firstTime);
        String sessionFinishTime = longToStringTime(lastTime);
        String Duration = calculateDuration(firstTime,lastTime);

        generate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generate.setVisibility(View.INVISIBLE);
                sessionStart.setVisibility(View.VISIBLE);
                sessionEnd.setVisibility(View.VISIBLE);
               // duration.setVisibility(View.VISIBLE);
                Timestamp.setVisibility(View.VISIBLE);
                isApnea.setVisibility(View.VISIBLE);
                apneaDesc.setVisibility(View.VISIBLE);
                if (sleepApnea) {
                    isApnea.setTextColor(Color.parseColor("#ea1f15"));
                    isApnea.setText("Notice: Possible Signs of Sleep Apnea Detected");
                } else {
                    isApnea.setText("No Possible Signs of Sleep Apnea Detected");
                }
                if (!array.isEmpty()) {
                    String startTimestamp = longToStringTime(array.get(0));
                    String finishTimestamp = longToStringTime(array.get(array.size() - 1));
                    Timestamp.setTextColor(Color.parseColor("#ea1f15"));
                    Timestamp.setText("Apnea duration detected from " + startTimestamp + " to " + finishTimestamp);
                }
                sessionStart.setTextColor(Color.parseColor("#ea1f15"));
                sessionEnd.setTextColor(Color.parseColor("#ea1f15"));
                sessionStart.setText("Today's session start time is " + sessionStartTime);
                sessionEnd.setText("Today's session end time is " + sessionFinishTime);
                //duration.setText("Duration : " + Duration);

            }
        });

        return view;
    }

    private String longToStringTime(long time) {
        String currentTimeinString = String.valueOf(time);
        String hours = currentTimeinString.substring(8,10);
        String mins = currentTimeinString.substring(10,12);
        String seconds = currentTimeinString.substring(12,14);
        return new String(hours + ":" + mins + ":" + seconds);
    }

    public static String calculateDuration(long startTime, long endTime) {
        // Define the date format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());

        try {
            // Parse the long values into Date objects
            String startStr = String.valueOf(startTime);
            String endStr = String.valueOf(endTime);

            long diffInMillis = sdf.parse(endStr).getTime() - sdf.parse(startStr).getTime();

            // Convert the difference to hours, minutes, and seconds
            long diffInSeconds = diffInMillis / 1000;
            long hours = diffInSeconds / 3600;
            long minutes = (diffInSeconds % 3600) / 60;
            long seconds = diffInSeconds % 60;

            // Return the duration as a formatted string
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);

        } catch (ParseException e) {
            e.printStackTrace();
            return "Invalid time format";
        }
    }

}