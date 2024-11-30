package com.example.miniproj;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class PasswordChanger extends AppCompatActivity {

    private TextInputEditText emailBox, change;
    private ProgressBar progress;
    private AppCompatButton resetPass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_password_changer);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        emailBox = findViewById(R.id.contactBox);
        progress = findViewById(R.id.progress);
        resetPass  = findViewById(R.id.resetPass);

        progress.setVisibility(View.INVISIBLE);
        resetPass.setVisibility(View.VISIBLE);

        resetPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!TextUtils.isEmpty(emailBox.getText().toString())){
                    String email = emailBox.getText().toString().trim();
                    progress.setVisibility(View.VISIBLE);
                    resetPass.setVisibility(View.INVISIBLE);
                    resetPassword(email);
                }else{
                    Toast.makeText(PasswordChanger.this, "Please enter email", Toast.LENGTH_SHORT).show();

                }
            }
        });
    }

    private void resetPassword(String email){
        FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                Toast.makeText(PasswordChanger.this, "Please check your email", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(PasswordChanger.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }else{
                Toast.makeText(PasswordChanger.this, "Problem sending email", Toast.LENGTH_SHORT).show();
            }
        });
    }
}