package com.example.miniproj;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import Database.DatabaseHandler;
import Model.Vitals;

public class ReportFragment extends Fragment {

    private DatabaseHandler db;

    private RecyclerView rcView;
    private List<Vitals> list;
    private Context context;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_report, container, false);
        context = view.getContext();
        DatabaseHandler db = new DatabaseHandler(context);
        rcView = view.findViewById(R.id.rcView);
        rcView.setHasFixedSize(true);
        rcView.setLayoutManager(new LinearLayoutManager(context));
        list = db.fetchData();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    public void loadData(){
        DatabaseHandler db = new DatabaseHandler(context);
        list.clear();
        list.addAll(db.fetchData());

    }
}