package com.example.WeatherApp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        Button loginBtn    = findViewById(R.id.welcomeLogin);
        Button registerBtn = findViewById(R.id.welcomeRegister);

        loginBtn.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class))
        );
        registerBtn.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );
    }
}
