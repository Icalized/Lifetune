package com.example.miniproj;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private TextView bpmTextView;
    private TextView spo2TextView;
    private TextView username;
    private boolean isFragmentReady = false;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "HealthPrefs";
    private static final String KEY_BPM = "bpm";
    private static final String KEY_SPO2 = "spo2";

    private void saveDataToPreferences(String bpm, String spo2) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_BPM, bpm);
        editor.putString(KEY_SPO2, spo2);
        editor.apply();
    }

    private void loadDataFromPreferences() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String bpm = prefs.getString(KEY_BPM, "--");
        String spo2 = prefs.getString(KEY_SPO2, "--");
        bpmTextView.setText(bpm);
        spo2TextView.setText(spo2);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.d(TAG, "HomeFragment attached to context.");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true); // Retain the fragment across configuration changes
        Log.d(TAG, "HomeFragment created.");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);
        bpmTextView = rootView.findViewById(R.id.bpmText);
        spo2TextView = rootView.findViewById(R.id.spo2Text);
        username = rootView.findViewById(R.id.username);
        sharedPreferences = getActivity().getSharedPreferences("my_pref", Context.MODE_PRIVATE);
        String name = sharedPreferences.getString("username","");
        username.setText(name);
        loadDataFromPreferences();
        Log.d(TAG, "onCreateView: bpmTextView = " + (bpmTextView != null) + ", spo2TextView = " + (spo2TextView != null));
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        isFragmentReady = true;
        ((MainScreen) requireActivity()).processPendingUpdates();
        Log.d(TAG, "HomeFragment view created.");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "HomeFragment resumed.");
    }

    public void updateBpm(final String data) {
        Log.d(TAG, "Updating BPM TextView with data: " + data);
        if (isFragmentReady && bpmTextView != null) {
            bpmTextView.setText(data);
            saveDataToPreferences(data, bpmTextView.getText().toString());
        } else {
            Log.d(TAG, "BPM TextView is null or fragment not ready");
        }
    }

    public void updateSpo2(final String data) {
        Log.d(TAG, "Updating SpO2 TextView with data: " + data);
        if (isFragmentReady && spo2TextView != null) {
            spo2TextView.setText(data);
            saveDataToPreferences(spo2TextView.getText().toString(), data);
        } else {
            Log.d(TAG, "SpO2 TextView is null or fragment not ready");
        }
    }
}
