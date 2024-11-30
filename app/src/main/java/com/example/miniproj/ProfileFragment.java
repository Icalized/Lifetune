package com.example.miniproj;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileFragment extends Fragment {

    // Initialisation
    private TextView logOut, changePass;
    private TextInputEditText username;
    private AppCompatButton changeName;
    private FirebaseAuth firebaseAuth;
    private SharedPreferences sharedPreferences;
    private Context context;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialisation
        firebaseAuth = FirebaseAuth.getInstance();
        context = view.getContext();
        logOut = view.findViewById(R.id.logOut);
        changePass = view.findViewById(R.id.changePass);
        username = view.findViewById(R.id.usernameBox);
        changeName = view.findViewById(R.id.changeName);
        sharedPreferences = getActivity().getSharedPreferences("my_pref",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // On Click Events
        changeName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!TextUtils.isEmpty(username.getText().toString())){
                    editor.putString("username",username.getText().toString());
                    editor.commit();
                    username.setText("");
                    Toast.makeText(context, "Username Changed", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(context, "Please enter valid name", Toast.LENGTH_SHORT).show();
                }
            }
        });


        changePass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), PasswordChanger.class);
                startActivity(intent);
            }
        });

        logOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutDialog();
            }
        });
        return view;
    }

    private void showLogoutDialog() {
        // Create the alert dialog
        new AlertDialog.Builder(getActivity())
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Perform logout action
                    firebaseAuth.signOut();
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    startActivity(intent);
                    getActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .remove(ProfileFragment.this)
                            .commit();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    // Dismiss the dialog
                    dialog.dismiss();
                })
                .show();
    }
}