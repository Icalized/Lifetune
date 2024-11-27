package com.example.miniproj;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class HomeFragment extends Fragment {

    TextView bpm, spo2, username;
    private SharedPreferences sharedPreferences;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_home, container, false);
        bpm = view.findViewById(R.id.bpmText);
        spo2 = view.findViewById(R.id.spo2Text);
        username = view.findViewById(R.id.username);

        sharedPreferences = getActivity().getSharedPreferences("my_pref", Context.MODE_PRIVATE);
        String name = sharedPreferences.getString("username","");
        username.setText(name);

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        bpm = null;
        spo2 = null;
    }

    public void updateBpm(final String data) {
        if (bpm != null) {
            bpm.setText(data);
        }
    }

    public void updateSpo2(final String data) {
        if (spo2 != null) {
            spo2.setText(data);
        }
    }
}
