package com.example.WeatherApp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;

public class DebugActivity extends AppCompatActivity {
    private static final String API_KEY = "0ec2f3bfd7e39f7e44f11e00bbd31a81";
    private OkHttpClient http = new OkHttpClient();
    private TextView output;
    // Hard-code a lat/lon for testing:
    private double lat = 51.5074, lon = -0.1278; // London

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.debug_activity);
        output = findViewById(R.id.debugOutput);

        findViewById(R.id.btnCurrent).setOnClickListener(v ->
                fetch("https://api.openweathermap.org/data/2.5/weather"
                        + "?lat=" + lat + "&lon=" + lon
                        + "&units=metric&appid=" + API_KEY)
        );

        findViewById(R.id.btnForecast3h).setOnClickListener(v ->
                fetch("https://api.openweathermap.org/data/2.5/forecast"
                        + "?lat=" + lat + "&lon=" + lon
                        + "&units=metric&appid=" + API_KEY)
        );

        findViewById(R.id.btnOneCall).setOnClickListener(v ->
                fetch("https://api.openweathermap.org/data/2.5/onecall"
                        + "?lat=" + lat + "&lon=" + lon
                        + "&exclude=minutely,alerts"
                        + "&units=metric&appid=" + API_KEY)
        );

        findViewById(R.id.btnAir).setOnClickListener(v ->
                fetch("https://api.openweathermap.org/data/2.5/air_pollution"
                        + "?lat=" + lat + "&lon=" + lon
                        + "&appid=" + API_KEY)
        );

        // inside onCreate after your other listeners…
        Button btnHourly4d = findViewById(R.id.btnHourly4d);
        btnHourly4d.setOnClickListener(v -> fetch(
                "https://pro.openweathermap.org/data/2.5/forecast/hourly"
                        + "?lat=" + lat + "&lon=" + lon
                        + "&appid=" + API_KEY));

        Button btnDaily16d = findViewById(R.id.btnDaily16d);
        btnDaily16d.setOnClickListener(v -> fetch(
                "https://api.openweathermap.org/data/2.5/forecast/daily"
                        + "?lat=" + lat + "&lon=" + lon
                        + "&cnt=16"
                        + "&appid=" + API_KEY));

        Button btnClimate30d = findViewById(R.id.btnClimate30d);
        btnClimate30d.setOnClickListener(v -> fetch(
                "https://api.openweathermap.org/data/2.5/forecast/climate"
                        + "?lat=" + lat + "&lon=" + lon
                        + "&cnt=30"
                        + "&appid=" + API_KEY));

        Button btnHistory1y = findViewById(R.id.btnHistory1y);
        btnHistory1y.setOnClickListener(v -> fetch(
                "https://api.openweathermap.org/data/2.5/onecall/timemachine"
                        + "?lat=" + lat + "&lon=" + lon
                        + "&dt=" + (System.currentTimeMillis()/1000 - 86400)  // example: 1 day ago; you can loop for deeper archive
                        + "&appid=" + API_KEY));

        Button btnStats = findViewById(R.id.btnStats);
        btnStats.setOnClickListener(v -> fetch(
                "https://api.openweathermap.org/data/2.5/statistical"
                        + "?lat=" + lat + "&lon=" + lon
                        + "&start=1622505600"            // Hard-coded start date
                        + "&end=1625097600"
                        + "&appid=" + API_KEY));

        Button btnAccum = findViewById(R.id.btnAccum);
        btnAccum.setOnClickListener(v -> fetch(
                "https://api.openweathermap.org/data/2.5/accumulated"
                        + "?lat=" + lat + "&lon=" + lon
                        + "&start=1622505600"            // Hard-coded start date
                        + "&end=1625097600"
                        + "&appid=" + API_KEY));

    }


    private void fetch(String url) {
        output.setText("Loading from:\n" + url + "\n\n");
        Request req = new Request.Builder().url(url).build();
        http.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call c, IOException e) {
                runOnUiThread(() -> output.append("Error: " + e.getMessage()));
            }
            @Override public void onResponse(Call c, Response r) throws IOException {
                final String body = r.body().string();
                runOnUiThread(() -> output.append(body));
            }
        });
    }
}

