package com.example.WeatherApp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;
import com.google.android.libraries.places.api.Places;

public class WelcomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        Button loginBtn    = findViewById(R.id.welcomeLogin);
        Button registerBtn = findViewById(R.id.welcomeRegister);
        if(!Places.isInitialized())
            Places.initialize(this.getApplicationContext(), "YOUR_API_KEY");


        SharedPreferences pref = getSharedPreferences("user", MODE_PRIVATE);
        String uid = pref.getString("uid", null);
        if (uid != null) {
            Intent i = new Intent(this, MainActivity.class);
            i.putExtra("uid", uid);
            startActivity(i);
            finish();
        }


        loginBtn.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class))
        );
        registerBtn.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );
    }
}
