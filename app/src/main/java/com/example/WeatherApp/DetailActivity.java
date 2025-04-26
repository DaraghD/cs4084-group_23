package com.example.WeatherApp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;
import com.google.gson.Gson;

public class DetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // Deserialize the passed WeatherResponse JSON
        String json = getIntent().getStringExtra("weatherJson");
        WeatherResponse wr = new Gson().fromJson(json, WeatherResponse.class);

        // Wire up your detail views (make sure these IDs exist in activity_detail.xml)
        TextView cityName    = findViewById(R.id.cityName);
        TextView description = findViewById(R.id.description);
        TextView temperature = findViewById(R.id.temperature);
        TextView feelsLike   = findViewById(R.id.feelsLike);
        TextView humidity    = findViewById(R.id.humidity);
        TextView pressure    = findViewById(R.id.pressure);
        TextView visibility  = findViewById(R.id.visibility);
        TextView wind        = findViewById(R.id.wind);
        TextView clouds      = findViewById(R.id.clouds);
        TextView sunrise     = findViewById(R.id.sunrise);
        TextView sunset      = findViewById(R.id.sunset);
        TextView rainVolume  = findViewById(R.id.rainVolume);
        TextView snowVolume  = findViewById(R.id.snowVolume);

        cityName.setText(wr.name);
        description.setText(wr.weather.get(0).description);
        temperature.setText(String.format("%.1f°", wr.main.temp));
        feelsLike.setText(String.format("Feels like: %.1f°", wr.main.feelsLike));
        humidity.setText("Humidity: " + wr.main.humidity + "%");
        pressure.setText("Pressure: " + wr.main.pressure + " hPa");
        visibility.setText("Visibility: " + wr.visibility + " m");
        wind.setText("Wind: " + wr.wind.speed + (wr.wind.deg>=0 ? " m/s, " + wr.wind.deg + "°" : ""));
        clouds.setText("Clouds: " + wr.clouds.all + "%");
        sunrise.setText("Sunrise: " + android.text.format.DateFormat.format("HH:mm", wr.sys.sunrise * 1000));
        sunset.setText("Sunset: "  + android.text.format.DateFormat.format("HH:mm", wr.sys.sunset  * 1000));
        rainVolume.setText("Rain (1h): " + (wr.rain != null ? wr.rain.oneH + " mm" : "0 mm"));
        snowVolume.setText("Snow (1h): " + (wr.snow != null ? wr.snow.oneH + " mm" : "0 mm"));
    }
}
