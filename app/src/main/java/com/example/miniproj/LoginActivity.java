package com.example.miniproj;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Firebase;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    //Widgets
    EditText email_login, pwd_login;
    Button loginBtn;

    //Firebase auth
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Views and widgets
        email_login = findViewById(R.id.contactBox);
        pwd_login = findViewById(R.id.passwordBox);
        loginBtn = findViewById(R.id.Login_button);

        //Firebase
        firebaseAuth = FirebaseAuth.getInstance();

        String email = getIntent().getStringExtra("email");
        email_login.setText(email);

        //Button events
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginUserEmailPwd(email_login.getText().toString().trim(),pwd_login.getText().toString().trim());
            }
        });


    }

    private void loginUserEmailPwd(String email, String pwd) {
        if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(pwd)) {
            firebaseAuth.signInWithEmailAndPassword(email,pwd).addOnCompleteListener(task -> {
                if(task.isSuccessful()){
                    Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, MainScreen.class);
                    startActivity(intent);
                    finishAffinity();
                }else{
                    Toast.makeText(LoginActivity.this, "Sign In First", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this,SignUpActivity.class);
                    intent.putExtra("email",email);
                    startActivity(intent);
                    finish();
                }
            });
        }

    }
}