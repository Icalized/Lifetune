package com.example.miniproj;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import Database.DatabaseHandler;
import Model.Vitals;

public class ReportFragment extends Fragment {

    private static final String TAG = "ReportFragment";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_report, container, false);
        return view;
    }

    private boolean filterDataDay(long time){
        long dateFromDb = reduceTime(time);
        long currentDate = currentDate();

        if(currentDate != -1 && currentDate == dateFromDb){
            return true;
        }
        return false;
    }

    private long reduceTime(long date){
        long onlyDate = date/1000000;
        return onlyDate;
    }

    private long currentDate(){
        LocalDateTime now = LocalDateTime.now();

        // Format as yyyyMMdd
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        String formattedDateTime = now.format(formatter);
        Log.d(TAG, "Formatted DateTime: " + formattedDateTime);

        // Convert to integer
        try {
            long dateTimeInt = Long.parseLong(formattedDateTime); // Use long for large numbers
            Log.d(TAG,"date converted");
            return dateTimeInt;
        } catch (NumberFormatException e) {
            Log.d(TAG,"Error converting date/time to integer: " + e.getMessage());
        }
        return -1;
    }
}