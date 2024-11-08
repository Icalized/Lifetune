package com.example.miniproj;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;


public class HomeFragment extends Fragment {

    TextView bpm, spo2;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_home, container, false);
        bpm = view.findViewById(R.id.bpmText);
        spo2 = view.findViewById(R.id.spo2Text);
        return view;
    }

    public void updateBpm(String data){
        if(bpm != null){
            bpm.setText(data);
        }
    }

    public void updateSpo2(String data){
        if(spo2 != null){
            spo2.setText(data);
        }
    }
}